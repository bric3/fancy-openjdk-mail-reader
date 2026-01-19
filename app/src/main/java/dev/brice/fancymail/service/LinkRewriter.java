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

import dev.brice.fancymail.config.PathsConfig;
import jakarta.inject.Singleton;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to rewrite links in parsed email content to point to the local proxy server.
 */
@Singleton
public class LinkRewriter {

    private static final Logger LOG = LoggerFactory.getLogger(LinkRewriter.class);

    private final PathsConfig pathsConfig;

    public LinkRewriter(PathsConfig pathsConfig) {
        this.pathsConfig = pathsConfig;
    }

    // Pattern to match OpenJDK mailing list URLs
    private static final Pattern OPENJDK_MAIL_PATTERN = Pattern.compile(
            "https?://mail\\.openjdk\\.org/pipermail/([^/]+)/([^/]+)/(\\d+)\\.html"
    );

    // Pattern to match relative links (e.g., "004306.html")
    private static final Pattern RELATIVE_MAIL_PATTERN = Pattern.compile(
            "^(\\d+)\\.html$"
    );

    /**
     * Rewrite all links in the given element to point to the local proxy.
     *
     * @param element the element containing links to rewrite
     */
    public void rewriteLinks(Element element) {
        Elements links = element.select("a[href]");
        for (Element link : links) {
            String href = link.attr("href");
            String rewritten = rewriteLink(href);
            if (!rewritten.equals(href)) {
                LOG.debug("Rewriting link: {} -> {}", href, rewritten);
                link.attr("href", rewritten);
            }
        }
    }

    /**
     * Rewrite a single link URL.
     *
     * @param href the original href
     * @return the rewritten href, or the original if no rewrite is needed
     */
    public String rewriteLink(String href) {
        if (href == null || href.isBlank()) {
            return href;
        }

        // Check for full OpenJDK mail URLs
        Matcher fullMatcher = OPENJDK_MAIL_PATTERN.matcher(href);
        if (fullMatcher.matches()) {
            String list = fullMatcher.group(1);
            String yearMonth = fullMatcher.group(2);
            String id = fullMatcher.group(3);
            return pathsConfig.toRenderedPath(list, yearMonth, id);
        }

        // Skip navigation links and external links
        if (href.startsWith("date.html") || href.startsWith("thread.html") ||
            href.startsWith("subject.html") || href.startsWith("author.html") ||
            href.startsWith("http://") || href.startsWith("https://") ||
            href.startsWith("mailto:") || href.startsWith("#")) {
            return href;
        }

        // Note: Relative links (e.g., "004306.html") are kept as-is
        // They will be resolved relative to the current rendered URL
        // which maintains the correct path structure

        return href;
    }

    // Pattern to match markdown links with OpenJDK mail URLs: [text](url) or just (url) in markdown link syntax
    private static final Pattern MARKDOWN_LINK_WITH_OPENJDK_URL = Pattern.compile(
            "\\]\\((https?://mail\\.openjdk\\.org/pipermail/([^/]+)/([^/]+)/(\\d+)\\.html)\\)"
    );

    /**
     * Rewrite markdown content links.
     * Only rewrites URLs inside markdown link syntax [text](url), not bare URLs in text.
     *
     * @param markdown the markdown content
     * @param currentList the current mailing list (for relative links)
     * @param currentYearMonth the current year-month (for relative links)
     * @return the markdown with rewritten links
     */
    public String rewriteMarkdownLinks(String markdown, String currentList, String currentYearMonth) {
        if (markdown == null) {
            return null;
        }

        // Only rewrite OpenJDK mail URLs that are inside markdown link syntax [text](url)
        // This preserves bare URLs like "full message: https://mail.openjdk.org/..."
        Matcher linkMatcher = MARKDOWN_LINK_WITH_OPENJDK_URL.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        while (linkMatcher.find()) {
            String list = linkMatcher.group(2);
            String yearMonth = linkMatcher.group(3);
            String id = linkMatcher.group(4);
            String replacement = "](" + pathsConfig.toRenderedPath(list, yearMonth, id) + ")";
            linkMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(sb);

        return sb.toString();
    }
}
