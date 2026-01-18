/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.service;

import dev.brice.fancymail.model.ThreadEntry;
import dev.brice.fancymail.model.ThreadTree;
import jakarta.inject.Singleton;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to parse thread.html from OpenJDK mailing lists into a ThreadTree.
 * <p>
 * The pipermail thread.html uses nested ul/li elements to represent thread hierarchy:
 * <pre>
 * &lt;ul&gt;
 *   &lt;li&gt;
 *     &lt;a href="004306.html"&gt;Subject&lt;/a&gt; &lt;em&gt;Author&lt;/em&gt;
 *     &lt;ul&gt;
 *       &lt;li&gt;&lt;a href="004307.html"&gt;Re: Subject&lt;/a&gt; &lt;em&gt;Reply Author&lt;/em&gt;&lt;/li&gt;
 *     &lt;/ul&gt;
 *   &lt;/li&gt;
 * &lt;/ul&gt;
 * </pre>
 */
@Singleton
public class ThreadParser {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadParser.class);

    // Pattern to extract message ID from href like "004307.html"
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("(\\d+)\\.html");

    /**
     * Parse thread.html content into a ThreadTree.
     *
     * @param html      the HTML content of thread.html
     * @param list      the mailing list name
     * @param yearMonth the year-month
     * @return a ThreadTree containing all thread entries (without Merkle hashes yet)
     */
    public ThreadTree parse(String html, String list, String yearMonth) {
        Document doc = Jsoup.parse(html);

        // Find the main thread list - it's typically after the navigation ul elements
        // The thread structure is nested ul/li elements
        List<ThreadEntry> roots = new ArrayList<>();
        int totalMessages = 0;

        // Strategy: Find the main thread UL which should have nested structure (replies)
        // and contain the most total messages when counting recursively
        Elements allUls = doc.select("ul");
        Element bestUl = null;
        int bestScore = 0;

        for (Element ul : allUls) {
            // Skip navigation lists
            if (isNavigationList(ul)) continue;

            // Score based on: total message links (recursive) + bonus for nesting
            int totalLinks = countAllMessageLinks(ul);
            int nestingBonus = hasNestedStructure(ul) ? 100 : 0;
            int score = totalLinks + nestingBonus;

            if (score > bestScore) {
                bestScore = score;
                bestUl = ul;
            }
        }

        if (bestUl != null) {
            int directCount = countDirectMessageLinks(bestUl);
            int totalCount = countAllMessageLinks(bestUl);
            LOG.debug("Found thread list with {} direct links, {} total links, nested={}",
                    directCount, totalCount, hasNestedStructure(bestUl));
            roots = parseThreadList(bestUl, 0);
        }

        // Count total messages
        for (ThreadEntry root : roots) {
            totalMessages += root.totalCount();
        }

        LOG.info("Parsed thread tree for {}/{}: {} root threads, {} total messages",
                list, yearMonth, roots.size(), totalMessages);

        return new ThreadTree(list, yearMonth, roots, null, totalMessages);
    }

    /**
     * Count direct li children that contain message links (not navigation links).
     */
    private int countDirectMessageLinks(Element ul) {
        int count = 0;
        for (Element li : ul.children()) {
            if (!li.tagName().equals("li")) continue;
            Element link = li.selectFirst("> a, > strong > a");
            if (link != null) {
                String href = link.attr("href");
                if (MESSAGE_ID_PATTERN.matcher(href).find()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Count all message links recursively in the ul tree.
     */
    private int countAllMessageLinks(Element ul) {
        int count = 0;
        Elements links = ul.select("a");
        for (Element link : links) {
            String href = link.attr("href");
            if (MESSAGE_ID_PATTERN.matcher(href).find()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if the ul has nested structure (ul > li > ul pattern).
     */
    private boolean hasNestedStructure(Element ul) {
        // Check if any direct li child has a nested ul
        for (Element li : ul.children()) {
            if (!li.tagName().equals("li")) continue;
            if (li.selectFirst("> ul") != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse a ul element into a list of ThreadEntry objects.
     */
    private List<ThreadEntry> parseThreadList(Element ul, int depth) {
        List<ThreadEntry> entries = new ArrayList<>();

        // Direct li children only
        for (Element child : ul.children()) {
            if (!child.tagName().equals("li")) continue;
            ThreadEntry entry = parseListItem(child, depth);
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * Parse a single li element into a ThreadEntry.
     */
    private ThreadEntry parseListItem(Element li, int depth) {
        // Find the link to the message - try multiple patterns
        // Pattern 1: direct <a> child
        // Pattern 2: <strong><a> pattern
        // Pattern 3: first <a> that's not in a nested ul
        Element link = li.selectFirst("> a");
        if (link == null) {
            link = li.selectFirst("> strong > a");
        }
        if (link == null) {
            // Find first link not inside nested ul
            for (Element a : li.select("a")) {
                // Check this link isn't inside a nested ul
                Element parent = a.parent();
                boolean inNestedUl = false;
                while (parent != null && parent != li) {
                    if (parent.tagName().equals("ul")) {
                        inNestedUl = true;
                        break;
                    }
                    parent = parent.parent();
                }
                if (!inNestedUl) {
                    link = a;
                    break;
                }
            }
        }

        if (link == null) {
            return null;
        }

        String href = link.attr("href");
        Matcher matcher = MESSAGE_ID_PATTERN.matcher(href);
        if (!matcher.find()) {
            return null;
        }

        String id = matcher.group(1);
        String subject = link.text().trim();

        // Find author - typically in em or i tag after the link
        String author = "Unknown";
        // Try direct em/i children first
        Element authorElement = li.selectFirst("> em, > i");
        if (authorElement != null) {
            author = authorElement.text().trim();
        } else {
            // Find em/i that's not inside nested ul
            for (Element em : li.select("em, i")) {
                Element parent = em.parent();
                boolean inNestedUl = false;
                while (parent != null && parent != li) {
                    if (parent.tagName().equals("ul")) {
                        inNestedUl = true;
                        break;
                    }
                    parent = parent.parent();
                }
                if (!inNestedUl) {
                    author = em.text().trim();
                    break;
                }
            }
        }

        ThreadEntry entry = new ThreadEntry(id, subject, author, depth);

        // Parse nested replies (child ul elements)
        Element nestedUl = li.selectFirst("> ul");
        if (nestedUl != null) {
            List<ThreadEntry> replies = parseThreadList(nestedUl, depth + 1);
            entry.replies().addAll(replies);
        }

        return entry;
    }

    /**
     * Check if a ul is a navigation list (containing links to date.html, thread.html, etc.)
     */
    private boolean isNavigationList(Element ul) {
        Elements links = ul.select("a");
        for (Element link : links) {
            String href = link.attr("href");
            if (href.contains("date.html") || href.contains("thread.html") ||
                href.contains("subject.html") || href.contains("author.html")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a ul element has thread entries (links to message html files).
     */
    private boolean hasThreadEntries(Element ul) {
        Elements links = ul.select("a");
        for (Element link : links) {
            String href = link.attr("href");
            if (MESSAGE_ID_PATTERN.matcher(href).find()) {
                return true;
            }
        }
        return false;
    }
}
