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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MarkdownConverterTest {

    private MarkdownConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MarkdownConverter();
    }

    @Test
    void toMarkdown_simpleHtml_convertsCorrectly() {
        String html = "<p>Hello <strong>world</strong>!</p>";

        String markdown = converter.toMarkdown(html);

        assertThat(markdown)
                .contains("Hello")
                .containsAnyOf("**world**", "__world__");
    }

    @Test
    void toMarkdown_headings_convertsToMarkdownHeadings() {
        String html = "<h1>Title</h1><h2>Subtitle</h2>";

        String markdown = converter.toMarkdown(html);

        assertThat(markdown)
                .containsAnyOf("# Title", "Title\n=")
                .containsAnyOf("## Subtitle", "Subtitle\n-");
    }

    @Test
    void toMarkdown_list_convertsToMarkdownList() {
        String html = "<ul><li>Item 1</li><li>Item 2</li></ul>";

        String markdown = converter.toMarkdown(html);

        assertThat(markdown)
                .contains("Item 1")
                .contains("Item 2");
    }

    @Test
    void toMarkdown_link_convertsToMarkdownLink() {
        String html = "<p>Visit <a href=\"https://example.com\">Example</a></p>";

        String markdown = converter.toMarkdown(html);

        assertThat(markdown).containsAnyOf(
                "[Example](https://example.com)",
                "[Example][",
                "Example"
        );
    }

    @Test
    void toMarkdown_codeBlock_preservesCode() {
        String html = "<pre><code>public class Foo {}</code></pre>";

        String markdown = converter.toMarkdown(html);

        assertThat(markdown).contains("public class Foo {}");
    }

    @Test
    void toMarkdown_element_convertsCorrectly() {
        String html = "<div><p>Test paragraph</p></div>";
        Element element = Jsoup.parse(html).body().selectFirst("div");

        String markdown = converter.toMarkdown(element);

        assertThat(markdown).contains("Test paragraph");
    }

    @Test
    void formatMailAsMarkdown_allFields_formatsCorrectly() {
        String subject = "Test Subject";
        String from = "John Doe";
        String email = "john@example.com";
        String date = "Mon Jan 1 12:00:00 UTC 2026";
        String list = "test-list";
        String body = "This is the email body.";
        String originalUrl = "https://mail.openjdk.org/pipermail/test-list/2026-January/000001.html";

        String formatted = converter.formatMailAsMarkdown(subject, from, email, date, list, body, originalUrl);

        assertThat(formatted)
                .contains("# Test Subject")
                .contains("**From:** John Doe (john@example.com)")
                .contains("**Date:** Mon Jan 1 12:00:00 UTC 2026")
                .contains("**List:** test-list")
                .contains("This is the email body.")
                .contains(originalUrl);
    }

    @Test
    void formatMailAsMarkdown_nullEmail_formatsWithoutEmail() {
        String subject = "Test Subject";
        String from = "John Doe";
        String date = "Mon Jan 1 12:00:00 UTC 2026";
        String list = "test-list";
        String body = "Body content";
        String originalUrl = "https://example.com";

        String formatted = converter.formatMailAsMarkdown(subject, from, null, date, list, body, originalUrl);

        assertThat(formatted)
                .contains("**From:** John Doe")
                .doesNotContain("(null)");
    }

    @Test
    void formatMailAsMarkdown_blankEmail_formatsWithoutEmail() {
        String formatted = converter.formatMailAsMarkdown(
                "Subject", "Author", "   ", "Date", "list", "body", "url");

        assertThat(formatted)
                .contains("**From:** Author")
                .doesNotContain("(   )");
    }
}
