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

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import jakarta.inject.Singleton;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to convert HTML content to Markdown using Flexmark.
 */
@Singleton
public class MarkdownConverter {

    private static final Logger LOG = LoggerFactory.getLogger(MarkdownConverter.class);

    private final FlexmarkHtmlConverter converter;

    public MarkdownConverter() {
        MutableDataSet options = new MutableDataSet();
        // Configure Flexmark options for better conversion
        this.converter = FlexmarkHtmlConverter.builder(options).build();
    }

    /**
     * Convert HTML string to Markdown.
     *
     * @param html the HTML content
     * @return the Markdown representation
     */
    public String toMarkdown(String html) {
        LOG.debug("Converting HTML to Markdown");
        return converter.convert(html);
    }

    /**
     * Convert a Jsoup Element to Markdown.
     *
     * @param element the Jsoup element
     * @return the Markdown representation
     */
    public String toMarkdown(Element element) {
        return toMarkdown(element.html());
    }

    /**
     * Format a parsed mail as a complete Markdown document.
     *
     * @param subject the email subject
     * @param from    the author name
     * @param email   the author email (may be null)
     * @param date    the email date
     * @param list    the mailing list name
     * @param body    the email body in Markdown
     * @param originalUrl the original URL
     * @return the formatted Markdown document
     */
    public String formatMailAsMarkdown(String subject, String from, String email,
                                        String date, String list, String body,
                                        String originalUrl) {
        StringBuilder sb = new StringBuilder();

        // Title
        sb.append("# ").append(subject).append("\n\n");

        // Metadata
        sb.append("**From:** ").append(from);
        if (email != null && !email.isBlank()) {
            sb.append(" (").append(email).append(")");
        }
        sb.append("\n");
        sb.append("**Date:** ").append(date).append("\n");
        sb.append("**List:** ").append(list).append("\n\n");

        // Separator
        sb.append("---\n\n");

        // Body
        sb.append(body);

        // Footer with source link
        sb.append("\n\n---\n\n");
        sb.append("*Source: [").append(list).append(" mailing list](").append(originalUrl).append(")*\n");

        return sb.toString();
    }
}
