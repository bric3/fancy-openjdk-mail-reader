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

import dev.brice.fancymail.model.MailNavigation;
import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import jakarta.inject.Singleton;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.parser.Parser;

/**
 * Service to parse HTML content from OpenJDK mailing list emails.
 */
@Singleton
public class MailParser {

    private static final Logger LOG = LoggerFactory.getLogger(MailParser.class);

    // Pattern to extract email from "name at domain" format
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([\\w.-]+)\\s+at\\s+([\\w.-]+)");

    // Pattern to match date format like "Tue Jan 13 21:52:47 UTC 2026"
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(Mon|Tue|Wed|Thu|Fri|Sat|Sun)\\s+" +
            "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+" +
            "\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+\\w+\\s+\\d{4}"
    );

    private final MarkdownConverter markdownConverter;
    private final LinkRewriter linkRewriter;

    public MailParser(MarkdownConverter markdownConverter, LinkRewriter linkRewriter) {
        this.markdownConverter = markdownConverter;
        this.linkRewriter = linkRewriter;
    }

    /**
     * Parse the HTML content and extract email information.
     *
     * @param html     the raw HTML content
     * @param mailPath the mail path for context
     * @return a ParsedMail record containing extracted information
     */
    public ParsedMail parse(String html, MailPath mailPath) {
        Document doc = Jsoup.parse(html);

        // Extract subject from title tag - pipermail format is "[list] subject"
        String subject = extractSubject(doc);

        // Extract author info from strong tag
        String from = extractFrom(doc);
        String email = extractEmail(doc);

        // Extract date from em tag or text
        String date = extractDate(doc);

        // Extract body content - the main message
        // Pipermail emails have the body in a PRE tag which is already markdown-formatted
        Element preElement = doc.body().selectFirst("pre");
        String bodyMarkdown;
        String bodyHtml;

        if (preElement != null) {
            // PRE content is already markdown-like - extract and clean it
            bodyMarkdown = extractPreContentAsMarkdown(preElement, mailPath);
            // For HTML view, wrap cleaned markdown in a pre-like div
            bodyHtml = "<div class=\"email-body\">" + markdownToSimpleHtml(bodyMarkdown) + "</div>";
        } else {
            // Fallback for non-standard format
            Element body = extractBodyElement(doc);
            if (body != null) {
                linkRewriter.rewriteLinks(body);
            }
            bodyMarkdown = body != null ? markdownConverter.toMarkdown(body) : "";
            bodyHtml = body != null ? body.html() : "";
        }

        // Extract navigation links
        MailNavigation navigation = extractNavigation(doc, mailPath);

        LOG.debug("Parsed mail: subject='{}', from='{}', email='{}', date='{}'",
                subject, from, email, date);

        return new ParsedMail(
                subject,
                from,
                email,
                date,
                mailPath.list(),
                bodyMarkdown,
                bodyHtml,
                mailPath.toOriginalUrl(),
                navigation
        );
    }

    private String extractSubject(Document doc) {
        // Try the title tag first - pipermail format is "[list] subject"
        String title = doc.title();
        if (title != null && !title.isBlank()) {
            // Often the title is formatted like "[list] Subject"
            int bracketEnd = title.indexOf(']');
            if (bracketEnd > 0 && bracketEnd < title.length() - 1) {
                return title.substring(bracketEnd + 1).trim();
            }
            return title.trim();
        }

        // Fallback: look for h1 or the first text block
        Element h1 = doc.selectFirst("h1");
        if (h1 != null) {
            return h1.text();
        }

        return "Unknown Subject";
    }

    private String extractFrom(Document doc) {
        // Look for the author name - pipermail uses <B> tag (not <strong>)
        // Try <B> first (pipermail format), then <strong> as fallback
        Element authorTag = doc.body().selectFirst("b");
        if (authorTag == null) {
            authorTag = doc.body().selectFirst("strong");
        }
        if (authorTag != null) {
            String text = authorTag.text();
            if (!text.isBlank()) {
                return text.trim();
            }
        }

        return "Unknown Author";
    }

    private String extractEmail(Document doc) {
        // Look for email pattern in the text following the author tag
        // Pipermail uses <B> for author, followed by email in <A> tag or text
        Element authorTag = doc.body().selectFirst("b");
        if (authorTag == null) {
            authorTag = doc.body().selectFirst("strong");
        }
        if (authorTag != null) {
            // Get the text node following the author tag
            Node next = authorTag.nextSibling();
            while (next != null) {
                if (next instanceof TextNode) {
                    String text = ((TextNode) next).text();
                    Matcher matcher = EMAIL_PATTERN.matcher(text);
                    if (matcher.find()) {
                        return matcher.group(1) + "@" + matcher.group(2);
                    }
                } else if (next instanceof Element) {
                    Element elem = (Element) next;
                    // Check if it's a link containing email
                    if (elem.tagName().equalsIgnoreCase("a")) {
                        String text = elem.text();
                        Matcher matcher = EMAIL_PATTERN.matcher(text);
                        if (matcher.find()) {
                            return matcher.group(1) + "@" + matcher.group(2);
                        }
                    }
                    // Don't go past the date (em/i tag)
                    if (elem.tagName().equalsIgnoreCase("em") || elem.tagName().equalsIgnoreCase("i")) {
                        break;
                    }
                }
                next = next.nextSibling();
            }
        }

        // Also try looking at the whole body text
        String bodyText = doc.body().text();
        Matcher matcher = EMAIL_PATTERN.matcher(bodyText);
        if (matcher.find()) {
            return matcher.group(1) + "@" + matcher.group(2);
        }

        return null;
    }

    private String extractDate(Document doc) {
        // Look for date - pipermail uses <I> tag (not <em>)
        // Try <I> first, then <em> as fallback
        Element dateTag = doc.body().selectFirst("i");
        if (dateTag == null) {
            dateTag = doc.body().selectFirst("em");
        }
        if (dateTag != null) {
            String text = dateTag.text();
            if (!text.isBlank()) {
                return text.trim();
            }
        }

        // Try to find date pattern in the body text
        String bodyText = doc.body().text();
        Matcher matcher = DATE_PATTERN.matcher(bodyText);
        if (matcher.find()) {
            return matcher.group();
        }

        return "Unknown Date";
    }

    private Element extractBodyElement(Document doc) {
        // Clone the body to avoid modifying the original
        Element body = doc.body().clone();

        // Remove navigation lists
        body.select("ul").stream()
                .filter(this::isNavigationList)
                .forEach(Element::remove);

        // Remove header elements: first strong, following text, em, br elements
        Element firstStrong = body.selectFirst("strong");
        if (firstStrong != null) {
            removeHeaderElements(firstStrong);
        }

        // Remove horizontal rules
        body.select("hr").forEach(Element::remove);

        // Find the main content - typically starts after the header
        // Look for the first pre tag or significant content
        Element pre = body.selectFirst("pre");
        if (pre != null) {
            return pre;
        }

        // If no pre tag, return the cleaned body
        return body;
    }

    private void removeHeaderElements(Element firstStrong) {
        // Remove the strong tag and surrounding header content
        Element parent = firstStrong.parent();
        if (parent == null) return;

        // Find the em tag (date) which marks the end of header
        Element em = null;
        Node current = firstStrong;
        while (current != null) {
            if (current instanceof Element) {
                Element elem = (Element) current;
                if (elem.tagName().equals("em")) {
                    em = elem;
                    break;
                }
            }
            current = current.nextSibling();
        }

        // Remove elements from strong to em (inclusive)
        current = firstStrong;
        while (current != null) {
            Node next = current.nextSibling();
            current.remove();
            if (current == em) break;
            current = next;
        }

        // Also remove br tags at the start
        while (parent.childNodeSize() > 0) {
            Node first = parent.childNode(0);
            if (first instanceof Element && ((Element) first).tagName().equals("br")) {
                first.remove();
            } else if (first instanceof TextNode && ((TextNode) first).isBlank()) {
                first.remove();
            } else {
                break;
            }
        }
    }

    private boolean isNavigationList(Element ul) {
        // Navigation lists contain links to date.html, thread.html, etc.
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
     * Extract navigation links from pipermail HTML.
     * Pipermail has navigation at the top and bottom with prev/next message links
     * and links to sorted indexes (by date, thread, subject, author).
     */
    private MailNavigation extractNavigation(Document doc, MailPath mailPath) {
        MailNavigation.NavLink prevMessage = null;
        MailNavigation.NavLink nextMessage = null;
        String dateIndexUrl = null;
        String threadIndexUrl = null;
        String subjectIndexUrl = null;
        String authorIndexUrl = null;

        // Find navigation UL elements
        Elements uls = doc.select("ul");
        for (Element ul : uls) {
            if (!isNavigationList(ul)) {
                continue;
            }

            // Extract prev/next message links
            Elements lis = ul.select("li");
            for (Element li : lis) {
                String text = li.text().toLowerCase();
                Element link = li.selectFirst("a");

                if (link != null) {
                    String href = link.attr("href");
                    String linkText = link.text().trim();

                    if (text.contains("previous message")) {
                        // Rewrite to local server URL
                        String localUrl = rewriteNavLink(href, mailPath);
                        prevMessage = new MailNavigation.NavLink(localUrl, linkText);
                    } else if (text.contains("next message")) {
                        String localUrl = rewriteNavLink(href, mailPath);
                        nextMessage = new MailNavigation.NavLink(localUrl, linkText);
                    }
                }
            }

            // Extract index links (keep pointing to pipermail)
            Elements indexLinks = ul.select("a");
            String baseUrl = "https://mail.openjdk.org/pipermail/" + mailPath.list() + "/" + mailPath.yearMonth() + "/";
            for (Element link : indexLinks) {
                String href = link.attr("href");
                if (href.contains("date.html")) {
                    dateIndexUrl = baseUrl + href;
                } else if (href.contains("thread.html")) {
                    threadIndexUrl = baseUrl + href;
                } else if (href.contains("subject.html")) {
                    subjectIndexUrl = baseUrl + href;
                } else if (href.contains("author.html")) {
                    authorIndexUrl = baseUrl + href;
                }
            }

            // Only need to process the first navigation list
            if (prevMessage != null || nextMessage != null) {
                break;
            }
        }

        return new MailNavigation(
                prevMessage,
                nextMessage,
                dateIndexUrl,
                threadIndexUrl,
                subjectIndexUrl,
                authorIndexUrl
        );
    }

    /**
     * Rewrite a navigation link (like "004313.html") to a local server URL.
     */
    private String rewriteNavLink(String href, MailPath mailPath) {
        if (href == null || href.isBlank()) {
            return href;
        }
        // Extract message ID from href like "004313.html"
        if (href.matches("\\d+\\.html")) {
            String messageId = href.replace(".html", "");
            return "/rendered/" + mailPath.list() + "/" + mailPath.yearMonth() + "/" + messageId + ".html";
        }
        return href;
    }

    /**
     * Extract PRE content as markdown, cleaning up HTML entities and links.
     * Pipermail PRE content is already markdown-formatted text.
     */
    private String extractPreContentAsMarkdown(Element preElement, MailPath mailPath) {
        // Get the inner HTML to preserve structure
        String content = preElement.html();

        // Convert HTML links to markdown links
        // Pattern: <A HREF="url">text</A> or <a href="url">text</a>
        // Note: Pipermail often has markdown links with HTML inside: [text](<A HREF="url">url</A>)
        // In that case, we just need to extract the URL, not create a new markdown link
        Pattern linkPattern = Pattern.compile(
                "<[Aa]\\s+[Hh][Rr][Ee][Ff]=\"([^\"]+)\"[^>]*>([^<]*)</[Aa]>",
                Pattern.CASE_INSENSITIVE);
        Matcher linkMatcher = linkPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (linkMatcher.find()) {
            String url = linkMatcher.group(1);
            String text = linkMatcher.group(2).trim();
            // Rewrite openjdk mail links to our rendered path
            String rewrittenUrl = linkRewriter.rewriteLink(url);

            // Check if this link is already inside a markdown link syntax
            // by looking at what comes before the match
            int matchStart = linkMatcher.start();
            boolean insideMarkdownLink = matchStart > 0 && content.charAt(matchStart - 1) == '(';

            String replacement;
            if (insideMarkdownLink || text.equals(url) || text.startsWith("http")) {
                // Just output the URL - either we're inside markdown syntax or the text is the URL
                replacement = rewrittenUrl;
            } else {
                // Create a full markdown link
                replacement = "[" + text + "](" + rewrittenUrl + ")";
            }
            linkMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(sb);
        content = sb.toString();

        // Remove pipermail italic tags used for quoted content
        // Pipermail wraps quoted lines in <i>...</I> tags (case varies)
        // These must be removed before markdown parsing or they break blockquote detection
        content = content.replaceAll("(?i)</?i>", "");

        // Decode HTML entities
        content = Parser.unescapeEntities(content, false);

        // Normalize blockquote lines: ensure > is followed by a space
        // This is required for proper markdown parsing, especially for code blocks
        // inside blockquotes which need ">     code" (> + space + 4 space indent)
        // Pipermail gives us ">    code" (> + 4 spaces) which isn't enough
        content = content.replaceAll("(?m)^>(\\S)", "> $1");    // >text -> > text
        content = content.replaceAll("(?m)^>( {2,})", "> $1");  // >  + spaces -> >   + spaces (adds one for code blocks)

        // Replace non-breaking spaces with regular spaces
        content = content.replace('\u00A0', ' ');

        // Remove attachment notices (lines starting with "-------------- next part")
        content = content.replaceAll("(?m)^-{10,} next part.*(?:\\r?\\n.*)*$", "");

        // Convert "----- Original Message -----" to a styled separator
        // Uses Unicode box-drawing characters for a clean titled border look
        // Also handles nested blockquotes like "> ----- Original Message -----"
        // Don't add leading newline for blockquotes as it breaks continuity
        Pattern originalMsgPattern = Pattern.compile(
                "(?m)^((?:> ?)*)-{3,}\\s*(Original Message|Forwarded Message)\\s*-{3,}$");
        content = originalMsgPattern.matcher(content).replaceAll(match -> {
            String prefix = match.group(1);
            String msgType = match.group(2);
            // Only add leading newline if not in a blockquote
            String leadingNewline = (prefix == null || prefix.isEmpty()) ? "\n" : "";
            return leadingNewline + (prefix != null ? prefix : "") + "**───── " + msgType + " ─────**\n";
        });

        // Convert lightly-indented code lines (2-3 spaces) to proper code blocks (4 spaces)
        // This handles cases like "  case Point(0, 0) -> ..." which would otherwise
        // be merged into the paragraph above
        content = convertLightlyIndentedCodeToBlocks(content);

        // Join lines that were artificially wrapped by pipermail's line length limit
        // This handles orphan short fragments that got pushed to the next line
        content = joinPipermailWrappedLines(content);

        // Fix orphan continuation lines from email wrapping in code blocks and lists
        // When email wrapping breaks a line mid-code-block, the continuation starts at column 0,
        // which prematurely terminates the code block. Re-indent single orphan lines.
        content = fixOrphanContinuationLines(content);

        // Trim trailing whitespace from each line but preserve line breaks
        content = content.lines()
                .map(String::stripTrailing)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // Add soft line breaks (two trailing spaces) to short lines ending with punctuation
        // This preserves line breaks for greetings like "Hello Gavin," and signatures like "Gavin"
        content = addSoftBreaksToShortLines(content);

        // Remove excessive blank lines (more than 2 consecutive)
        content = content.replaceAll("\n{3,}", "\n\n");

        // Convert indented code blocks to fenced code blocks
        content = convertIndentedToFencedCodeBlocks(content);

        // Detect and fence code inside list items (using looksLikeCode heuristic)
        content = convertListItemCodeToFenced(content);

        return content.trim();
    }

    /**
     * Convert indented code blocks (4+ space indentation) to fenced code blocks (```).
     * <p>
     * This handles:
     * <ul>
     *   <li>Regular code blocks (4 space indent)</li>
     *   <li>Code blocks inside list items (preserves list indentation)</li>
     *   <li>Code blocks inside blockquotes (preserves > prefix)</li>
     * </ul>
     * <p>
     * Content inside existing fenced code blocks is left untouched.
     * Indented lines inside list items (continuations) are not converted.
     */
    private String convertIndentedToFencedCodeBlocks(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        StringBuilder codeBlock = new StringBuilder();
        String codeBlockPrefix = "";  // Prefix for list/blockquote context
        boolean inIndentedCodeBlock = false;
        boolean inExistingFencedBlock = false;
        boolean inListContext = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Track existing fenced code blocks - don't modify content inside them
            if (trimmed.startsWith("```")) {
                inExistingFencedBlock = !inExistingFencedBlock;
                // If we were building an indented code block, close it first
                if (inIndentedCodeBlock) {
                    result.append(codeBlock);
                    result.append(codeBlockPrefix).append("```\n");
                    codeBlock.setLength(0);
                    inIndentedCodeBlock = false;
                    codeBlockPrefix = "";
                }
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            // If inside an existing fenced block, pass through unchanged
            if (inExistingFencedBlock) {
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            // Track list context - list items start with - * or number.
            // We're in a list until we see a blank line or non-indented non-list line
            boolean isListItem = trimmed.matches("^([-*]|\\d+\\.)\\s.*");
            if (isListItem) {
                inListContext = true;
            } else if (trimmed.isEmpty()) {
                // Blank line might end list context, but could also be between items
                // Keep list context if next non-blank line is a list item or indented
            } else if (!line.startsWith(" ") && !line.startsWith("\t")) {
                // Non-indented, non-list line ends list context
                inListContext = false;
            }

            // Check if this line is an indented code line
            // Skip if we're in a list context (it's likely a list continuation)
            IndentedCodeInfo codeInfo = null;
            if (!inListContext) {
                codeInfo = getIndentedCodeInfo(line);
            }

            if (codeInfo != null) {
                if (!inIndentedCodeBlock) {
                    // Starting a new code block
                    inIndentedCodeBlock = true;
                    codeBlockPrefix = codeInfo.prefix;
                    // Ensure blank line before code block if previous line wasn't blank
                    // BUT NOT for blockquote code - blank lines break blockquote continuity
                    if (codeBlockPrefix.isEmpty() && result.length() > 0) {
                        String resultStr = result.toString();
                        if (!resultStr.endsWith("\n\n") && !resultStr.endsWith("\n")) {
                            result.append("\n");
                        }
                        if (!resultStr.endsWith("\n\n")) {
                            result.append("\n");
                        }
                    }
                    // Add opening fence with same prefix (add space after > for readability)
                    result.append(formatBlockquotePrefix(codeBlockPrefix)).append("```\n");
                }
                // Add code line with prefix but without the code indentation
                codeBlock.append(formatBlockquotePrefix(codeBlockPrefix)).append(codeInfo.code).append("\n");
            } else {
                // Check if this is a blank line within a blockquote code block
                // A line like "> > >" (just the prefix) should continue the code block
                // if the next code line has the same prefix
                boolean isBlankBlockquoteLine = inIndentedCodeBlock &&
                        !codeBlockPrefix.isEmpty() &&
                        isBlankBlockquoteLineWithPrefix(line, codeBlockPrefix);

                if (isBlankBlockquoteLine && hasMoreCodeWithPrefix(lines, i + 1, codeBlockPrefix)) {
                    // Include blank line in code block
                    codeBlock.append(formatBlockquotePrefix(codeBlockPrefix)).append("\n");
                } else if (inIndentedCodeBlock) {
                    // Ending code block
                    result.append(codeBlock);
                    result.append(formatBlockquotePrefix(codeBlockPrefix)).append("```\n");
                    codeBlock.setLength(0);
                    inIndentedCodeBlock = false;
                    codeBlockPrefix = "";
                    result.append(line);
                    if (i < lines.length - 1) {
                        result.append("\n");
                    }
                } else {
                    result.append(line);
                    if (i < lines.length - 1) {
                        result.append("\n");
                    }
                }
            }
        }

        // Close any remaining code block
        if (inIndentedCodeBlock) {
            result.append(codeBlock);
            result.append(formatBlockquotePrefix(codeBlockPrefix)).append("```");
        }

        return result.toString();
    }

    /**
     * Format a blockquote prefix for output, ensuring proper spacing.
     * Adds a trailing space if the prefix is non-empty and doesn't already have one.
     */
    private String formatBlockquotePrefix(String prefix) {
        if (prefix.isEmpty()) {
            return prefix;
        }
        // Add space after > for readability if not already present
        if (!prefix.endsWith(" ")) {
            return prefix + " ";
        }
        return prefix;
    }

    /**
     * Detect and fence code inside list items using the looksLikeCode heuristic.
     * <p>
     * Handles cases like:
     * <pre>
     * - you mutate variables when you reduce accumulators in a loop
     *   var v1 = ...
     *   for(...) {
     *     (v1, v2) = f(v1, v2);
     *   }
     * </pre>
     * Where the code is indented as list continuation but should be a code block.
     */
    private String convertListItemCodeToFenced(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        StringBuilder codeBlock = new StringBuilder();
        boolean inListItem = false;
        boolean inCodeSection = false;
        String listIndent = "";  // Indentation to use for fenced block

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Detect list item start
            if (trimmed.matches("^([-*]|\\d+\\.)\\s.*")) {
                // Close any open code section
                if (inCodeSection) {
                    result.append(listIndent).append("```\n");
                    result.append(codeBlock);
                    result.append(listIndent).append("```\n");
                    codeBlock.setLength(0);
                    inCodeSection = false;
                }
                inListItem = true;
                // Calculate the indentation for code blocks (align with list text)
                int markerEnd = line.indexOf(trimmed) + trimmed.indexOf(' ') + 1;
                listIndent = " ".repeat(Math.max(2, line.indexOf(trimmed.charAt(0)) + 2));
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            // Check if we're still in a list context
            if (inListItem) {
                // Blank line might end list or separate items
                if (trimmed.isEmpty()) {
                    if (inCodeSection) {
                        result.append(listIndent).append("```\n");
                        result.append(codeBlock);
                        result.append(listIndent).append("```\n");
                        codeBlock.setLength(0);
                        inCodeSection = false;
                    }
                    result.append(line);
                    if (i < lines.length - 1) {
                        result.append("\n");
                    }
                    continue;
                }

                // Non-indented, non-list line ends list context
                if (!line.startsWith(" ") && !line.startsWith("\t")) {
                    if (inCodeSection) {
                        result.append(listIndent).append("```\n");
                        result.append(codeBlock);
                        result.append(listIndent).append("```\n");
                        codeBlock.setLength(0);
                        inCodeSection = false;
                    }
                    inListItem = false;
                    result.append(line);
                    if (i < lines.length - 1) {
                        result.append("\n");
                    }
                    continue;
                }

                // Indented line - check if it looks like code
                if (looksLikeCode(trimmed)) {
                    if (!inCodeSection) {
                        // Starting code section - add blank line and opening fence
                        if (result.length() > 0 && !result.toString().endsWith("\n\n")) {
                            if (!result.toString().endsWith("\n")) {
                                result.append("\n");
                            }
                        }
                        inCodeSection = true;
                    }
                    codeBlock.append(listIndent).append(trimmed).append("\n");
                    continue;
                } else if (inCodeSection) {
                    // Non-code line while in code section
                    // Check if it might be part of the code (like a closing brace or continuation)
                    if (trimmed.matches("^[}\\]);]+$") || trimmed.startsWith("//")) {
                        codeBlock.append(listIndent).append(trimmed).append("\n");
                        continue;
                    }
                    // End code section
                    result.append(listIndent).append("```\n");
                    result.append(codeBlock);
                    result.append(listIndent).append("```\n");
                    codeBlock.setLength(0);
                    inCodeSection = false;
                }
            }

            result.append(line);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        // Close any remaining code section
        if (inCodeSection) {
            result.append(listIndent).append("```\n");
            result.append(codeBlock);
            result.append(listIndent).append("```");
        }

        return result.toString();
    }

    /**
     * Information about an indented code line.
     */
    private record IndentedCodeInfo(String prefix, String code) {}

    // Pattern to match blockquote prefixes like ">", "> >", ">>", "> > >", etc.
    // Uses ">(?:[ ]?>)*" to avoid greedily consuming trailing space before code indent
    private static final Pattern BLOCKQUOTE_PREFIX_PATTERN = Pattern.compile("^(>(?:[ ]?>)*)");

    /**
     * Check if a line is an indented code block line and extract its components.
     * Returns null if not an indented code line.
     * <p>
     * Handles nested blockquotes like "> > >    code" or ">>>    code".
     * Note: Blockquote normalization may add extra spaces, so we strip leading space from code.
     */
    private IndentedCodeInfo getIndentedCodeInfo(String line) {
        // Regular indented code block (4+ spaces)
        if (line.startsWith("    ")) {
            return new IndentedCodeInfo("", line.substring(4));
        }

        // Blockquote with indented code (handles nested blockquotes)
        if (line.startsWith(">")) {
            Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
            if (matcher.find()) {
                String prefix = matcher.group(1);
                String rest = line.substring(prefix.length());
                // Standard: 4+ spaces for a code block
                if (rest.startsWith("    ")) {
                    // Strip exactly 4 spaces, then strip any additional leading spaces
                    // (blockquote normalization may have added 1-2 extra spaces)
                    String code = rest.substring(4);
                    // Strip up to 2 leading spaces from normalization
                    if (code.startsWith("  ")) {
                        code = code.substring(2);
                    } else if (code.startsWith(" ")) {
                        code = code.substring(1);
                    }
                    return new IndentedCodeInfo(prefix, code);
                }
                // Fallback: 2-3 spaces if content looks like code
                // (handles inconsistent indentation in emails)
                if (rest.startsWith("  ") || rest.startsWith("   ")) {
                    String trimmed = rest.trim();
                    if (looksLikeCode(trimmed)) {
                        return new IndentedCodeInfo(prefix, trimmed);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if a line is a blank blockquote line matching the given prefix.
     * For example, "> > >" with prefix "> > " would return true.
     */
    private boolean isBlankBlockquoteLineWithPrefix(String line, String expectedPrefix) {
        if (!line.startsWith(">")) {
            return false;
        }
        // Normalize and compare: the line should be just blockquote markers
        String trimmed = line.trim();
        // Check if it's only > characters and spaces
        if (!trimmed.matches("^(>[ ]?)+$")) {
            return false;
        }
        // Check if it matches or is compatible with our prefix
        Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
        if (matcher.find()) {
            String linePrefix = matcher.group(1);
            // The prefix should match (allowing for trailing space differences)
            return linePrefix.replace(" ", "").equals(expectedPrefix.replace(" ", ""));
        }
        return false;
    }

    /**
     * Check if there's more indented code with the same prefix in upcoming lines.
     */
    private boolean hasMoreCodeWithPrefix(String[] lines, int startIndex, String expectedPrefix) {
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            // Skip blank blockquote lines
            if (line.trim().matches("^(>[ ]?)*$")) {
                continue;
            }
            // Check if this line is code with matching prefix
            IndentedCodeInfo info = getIndentedCodeInfo(line);
            if (info != null) {
                // Check if prefix matches (allowing for space variations)
                return info.prefix.replace(" ", "").equals(expectedPrefix.replace(" ", ""));
            }
            // Non-blank, non-code line - no more code coming
            return false;
        }
        return false;
    }

    // Pattern for detecting code-like content
    // Java keywords, operators, and common code patterns
    // Note: `;$` was removed as it's too broad - prose can end with semicolons too
    // Most code lines ending with ; also have other indicators (keywords, parens, etc.)
    private static final Pattern CODE_PATTERN = Pattern.compile(
            "\\b(case|switch|if|else|for|while|do|return|break|continue|class|interface|enum|record|" +
            "void|int|long|double|float|boolean|char|byte|short|var|final|static|public|private|protected|" +
            "new|null|true|false|this|super|throws?|try|catch|finally|instanceof|extends|implements)\\b|" +
            "->|=>|==|!=|<=|>=|&&|\\|\\||\\{|\\}|\\(.*\\)|//|/\\*|\\*/|\\+\\+|--"
    );

    /**
     * Convert lightly-indented lines (2-3 spaces) that look like code into proper
     * markdown code blocks (4 space indentation).
     * <p>
     * This handles cases like:
     * <pre>
     * Just a question, are you proposing that
     *   case Point(0, 0) -> ...
     * </pre>
     * Where the "case" line would otherwise be merged into the paragraph.
     * <p>
     * Markdown requires a blank line before indented code blocks, so we add one
     * when the previous line is not blank and not already a code block.
     * <p>
     * Lines that look like list items (starting with - or * after indentation)
     * are excluded from this conversion.
     */
    private String convertLightlyIndentedCodeToBlocks(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        boolean previousWasConvertedCode = false;  // Track consecutive code conversions

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String prevLine = i > 0 ? lines[i - 1] : "";
            String trimmed = line.trim();

            // Check for lines with 2-3 space indentation (not already 4+)
            // Exclude list items (lines starting with - or * or number followed by space)
            boolean isListItem = trimmed.matches("^([-*]|\\d+\\.)\\s.*");
            if (line.matches("^ {2,3}\\S.*") && !isListItem && looksLikeCode(trimmed)) {
                // Add blank line before code block if:
                // - Previous line is not blank
                // - Previous line is not already an indented code block (4+ spaces)
                // - Previous line was not just converted to code (consecutive code lines)
                if (!prevLine.isBlank() && !prevLine.startsWith("    ") && !previousWasConvertedCode) {
                    result.append("\n");
                }
                // Convert to 4-space indentation for proper code block
                result.append("    ").append(trimmed);
                previousWasConvertedCode = true;
            } else {
                result.append(line);
                previousWasConvertedCode = false;
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Heuristic check if a line looks like code based on common patterns.
     */
    private boolean looksLikeCode(String line) {
        return CODE_PATTERN.matcher(line).find();
    }

    // Maximum length for an orphan fragment (short word/phrase pushed to next line)
    private static final int MAX_ORPHAN_LENGTH = 15;
    // Minimum length for a "long" line that likely hit the wrap limit
    private static final int MIN_LONG_LINE_LENGTH = 65;
    // Pattern for signature/greeting lines that should not be joined
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile(
            "(?i).*(regards|cheers|thanks|thank you|best|sincerely|cordialement|greetings),?\\s*$");

    /**
     * Join lines that were artificially wrapped by pipermail's line length limit.
     * <p>
     * Pipermail wraps long lines at ~72-76 characters. When a word would exceed the limit,
     * it gets pushed to the next line, creating short "orphan" fragments. For example:
     * <pre>
     * One might think that we would need some marking on the `x` and `y`
     * components of
     * `Point3d` to indicate that they map to the corresponding components of
     * `Point`,
     * </pre>
     * becomes:
     * <pre>
     * One might think that we would need some marking on the `x` and `y` components of
     * `Point3d` to indicate that they map to the corresponding components of `Point`,
     * </pre>
     * <p>
     * The heuristic:
     * <ul>
     *   <li>Previous line is LONG (likely hit the wrap limit, > 65 chars)</li>
     *   <li>Current line starts at column 0 (no indentation)</li>
     *   <li>Current line is SHORT (orphan fragment, ≤ 15 chars)</li>
     *   <li>Not a signature pattern (e.g., "regards," followed by name)</li>
     * </ul>
     */
    private String joinPipermailWrappedLines(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            result.append(line);

            // Look ahead for orphan lines to join
            while (i + 1 < lines.length) {
                String currentLine = lines[i];
                String nextLine = lines[i + 1];

                if (isOrphanWrappedLine(currentLine, nextLine)) {
                    // Remove trailing whitespace and join with single space
                    while (result.length() > 0 && Character.isWhitespace(result.charAt(result.length() - 1))) {
                        result.setLength(result.length() - 1);
                    }
                    result.append(" ").append(nextLine.trim());
                    // Update lines[i] for next iteration check (in case of multiple orphans)
                    lines[i] = lines[i].stripTrailing() + " " + nextLine.trim();
                    i++;
                } else {
                    break;
                }
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
            i++;
        }

        return result.toString();
    }

    /**
     * Check if a line is an orphan fragment from pipermail line wrapping.
     */
    private boolean isOrphanWrappedLine(String prevLine, String line) {
        // Not an orphan if empty
        if (line.isEmpty()) {
            return false;
        }

        // Not an orphan if it starts with whitespace (indented content)
        if (Character.isWhitespace(line.charAt(0))) {
            return false;
        }

        // Not an orphan if it starts with > (blockquote)
        if (line.startsWith(">")) {
            return false;
        }

        // Not an orphan if it's a fenced code block marker
        if (line.startsWith("```")) {
            return false;
        }

        // Not an orphan if it's too long (it's a full line, not a fragment)
        if (line.length() > MAX_ORPHAN_LENGTH) {
            return false;
        }

        // Not an orphan if previous line is short (intentional line break)
        String prevTrimmed = prevLine.stripTrailing();
        if (prevTrimmed.length() < MIN_LONG_LINE_LENGTH) {
            return false;
        }

        // Not an orphan if previous line looks like a signature/greeting
        if (SIGNATURE_PATTERN.matcher(prevTrimmed).matches()) {
            return false;
        }

        return true;
    }

    // Pattern to extract leading indentation (spaces or blockquote prefix "> " followed by spaces)
    private static final Pattern INDENT_PATTERN = Pattern.compile("^((?:> ?)?[ ]+)");

    // Pattern to detect list items (optional indent + - or * or number. + space)
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^( {0,3})([-*]|\\d+\\.)\\s");

    /**
     * Fix orphan continuation lines caused by email line wrapping.
     * <p>
     * When email clients wrap long lines in code blocks or list items, the continuation
     * often starts at column 0, which breaks the formatting. For example:
     * <pre>
     *     throw new IllegalArgumentException("denominator cannot
     * be zero");
     * </pre>
     * becomes:
     * <pre>
     *     throw new IllegalArgumentException("denominator cannot be zero");
     * </pre>
     * <p>
     * This method detects such orphan lines and joins them with the previous line.
     * It only fixes SINGLE orphan lines (where the next line resumes normal indentation
     * or is blank), to avoid incorrectly modifying intentionally unindented text.
     * <p>
     * Handles: code blocks (4+ space indent), list items, and blockquotes.
     */
    private String fixOrphanContinuationLines(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String prevLine = i > 0 ? lines[i - 1] : "";
            String nextLine = i < lines.length - 1 ? lines[i + 1] : "";

            // Check if this is an orphan continuation line:
            // 1. Current line starts at column 0 (no leading whitespace, not a blockquote)
            // 2. Previous line was indented (code block, list item, or blockquote)
            // 3. Current line is not blank
            // 4. Next line either resumes indentation or is blank (single orphan)
            boolean isOrphanLine = false;

            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && !line.startsWith(">")) {
                // Check for code block continuation (4+ spaces)
                Matcher prevIndentMatcher = INDENT_PATTERN.matcher(prevLine);
                if (prevIndentMatcher.find()) {
                    String prevIndent = prevIndentMatcher.group(1);
                    int spaceCount = prevIndent.length() - prevIndent.replace(" ", "").length();

                    if (spaceCount >= 4) {
                        boolean nextLineResumes = nextLine.isBlank() ||
                                INDENT_PATTERN.matcher(nextLine).find() ||
                                nextLine.startsWith("```");

                        if (nextLineResumes) {
                            isOrphanLine = true;
                        }
                    }
                }

                // Check for list item continuation
                if (!isOrphanLine && LIST_ITEM_PATTERN.matcher(prevLine).find()) {
                    // Next line should resume with indentation or be blank
                    boolean nextLineResumes = nextLine.isBlank() ||
                            nextLine.startsWith(" ") ||
                            LIST_ITEM_PATTERN.matcher(nextLine).find();

                    if (nextLineResumes) {
                        isOrphanLine = true;
                    }
                }
            }

            if (isOrphanLine) {
                // Join with previous line: remove trailing newline/whitespace and add single space
                if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
                    result.setLength(result.length() - 1);
                }
                // Also remove any trailing whitespace from previous line
                while (result.length() > 0 && Character.isWhitespace(result.charAt(result.length() - 1))) {
                    result.setLength(result.length() - 1);
                }
                result.append(" ");
                result.append(line);
            } else {
                result.append(line);
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    // Maximum length for a "short line" that should preserve its line break
    private static final int SHORT_LINE_MAX_LENGTH = 60;

    /**
     * Add markdown soft line breaks (two trailing spaces) to short lines ending with punctuation.
     * <p>
     * In markdown, consecutive lines are merged into a paragraph. This causes issues for:
     * - Greetings: "Hello Gavin," followed by the message body
     * - Signatures: "Wishing you a happy 2026!" followed by "Gavin"
     * <p>
     * By adding two trailing spaces, we create a soft break ({@code <br>}) in HTML.
     */
    private String addSoftBreaksToShortLines(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String nextLine = i < lines.length - 1 ? lines[i + 1] : "";

            // Check if this line should have a soft break:
            // - Short line (under threshold)
            // - Ends with punctuation (. ! ? ,)
            // - Next line is not blank (otherwise paragraph break is fine)
            // - Not inside a code block (doesn't start with 4 spaces)
            // - Not a blockquote marker line (just ">")
            String trimmed = line.trim();
            boolean isShortLine = trimmed.length() > 0 && trimmed.length() <= SHORT_LINE_MAX_LENGTH;
            boolean endsWithPunctuation = trimmed.matches(".*[.!?,]$");
            boolean nextLineNotBlank = !nextLine.isBlank();
            boolean notCodeBlock = !line.startsWith("    ");
            boolean notBlockquoteOnly = !trimmed.equals(">");

            if (isShortLine && endsWithPunctuation && nextLineNotBlank && notCodeBlock && notBlockquoteOnly) {
                result.append(line).append("  "); // Add two spaces for soft break
            } else {
                result.append(line);
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Convert markdown to simple HTML for the HTML view.
     * This is a basic conversion for display purposes.
     */
    private String markdownToSimpleHtml(String markdown) {
        // Use the markdown converter to convert markdown to HTML
        return markdownConverter.toHtml(markdown);
    }
}
