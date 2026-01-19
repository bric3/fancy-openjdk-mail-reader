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

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.List;
import jakarta.inject.Singleton;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to convert between HTML and Markdown using Flexmark.
 */
@Singleton
public class MarkdownConverter {

    private static final Logger LOG = LoggerFactory.getLogger(MarkdownConverter.class);

    private final FlexmarkHtmlConverter htmlToMdConverter;
    private final Parser mdParser;
    private final HtmlRenderer mdToHtmlRenderer;

    public MarkdownConverter() {
        MutableDataSet options = new MutableDataSet();
        // Enable extensions:
        // - Typographic: smart quotes and dashes (converts " -- " to em-dash, "..." to ellipsis)
        // - Autolink: automatically converts bare URLs to clickable links
        options.set(Parser.EXTENSIONS, List.of(
                TypographicExtension.create(),
                AutolinkExtension.create()
        ));

        this.htmlToMdConverter = FlexmarkHtmlConverter.builder(options).build();
        this.mdParser = Parser.builder(options).build();
        this.mdToHtmlRenderer = HtmlRenderer.builder(options).build();
    }

    /**
     * Convert HTML string to Markdown.
     *
     * @param html the HTML content
     * @return the Markdown representation
     */
    public String toMarkdown(String html) {
        LOG.debug("Converting HTML to Markdown");
        return htmlToMdConverter.convert(html);
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
     * Convert Markdown string to HTML.
     *
     * @param markdown the Markdown content
     * @return the HTML representation
     */
    public String toHtml(String markdown) {
        LOG.debug("Converting Markdown to HTML");
        Node document = mdParser.parse(markdown);
        return mdToHtmlRenderer.render(document);
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
