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
import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Fixture validation tests for MailParser.
 * These tests validate parsing against manually curated expected output files.
 * They are separated from unit tests to allow independent execution.
 */
@Tag("fixture-validation")
class MailParserFixtureValidationTest {

    private MailParser parser;

    @BeforeEach
    void setUp() {
        MarkdownConverter markdownConverter = new MarkdownConverter();
        LinkRewriter linkRewriter = new LinkRewriter(new PathsConfig());
        parser = new MailParser(markdownConverter, linkRewriter);
    }

    private String loadFixture(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + path)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String loadExpectedMarkdown(String path) throws IOException {
        String mdPath = path.replace(".html", ".md");
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + mdPath)) {
            if (is == null) {
                throw new IOException("Expected markdown not found: " + mdPath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parse_fixture004306_extractsSubject() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.subject()).isEqualTo("Amber features 2026");
    }

    @Test
    void parse_fixture004306_extractsDate() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.date()).isEqualTo("Fri Jan 9 23:08:05 UTC 2026");
    }

    @Test
    void parse_fixture004306_extractsEmail() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.email()).isEqualTo("gavin.bierman@oracle.com");
    }

    @Test
    void parse_fixture004306_extractsList() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.list()).isEqualTo("amber-spec-experts");
    }

    @Test
    void parse_fixture004306_extractsOriginalUrl() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.originalUrl())
                .isEqualTo("https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004306.html");
    }

    @Test
    void parse_fixture004306_bodyContainsContent() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyMarkdown() + parsed.bodyHtml())
                .containsIgnoringCase("spec experts");
    }

    @Test
    void parse_fixture004307_extractsSubject() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.subject()).isEqualTo("Data Oriented Programming, Beyond Records");
    }

    @Test
    void parse_fixture004307_extractsDate() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.date()).isEqualTo("Tue Jan 13 21:52:47 UTC 2026");
    }

    @Test
    void parse_fixture004307_extractsEmail() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.email()).isEqualTo("brian.goetz@oracle.com");
    }

    @Test
    void parse_fixture004307_bodyContainsCarrierClasses() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyMarkdown().toLowerCase() + parsed.bodyHtml().toLowerCase())
                .contains("carrier");
    }

    @Test
    void parse_fixture004317_codeInsideListItems() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004317.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004317");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyMarkdown())
                .contains("- you mutate variables when you reduce accumulators in a loop")
                .contains("- you mutate variables after a condition")
                .contains("- you mutate variables when you transfer values in between scopes");

        assertThat(parsed.bodyMarkdown()).contains("```");
        assertThat(parsed.bodyMarkdown()).contains("var v1 = ...");
        assertThat(parsed.bodyMarkdown()).contains("for(...) {");
        assertThat(parsed.bodyMarkdown()).contains("if (...) {");
        assertThat(parsed.bodyMarkdown()).contains("try(...) {");

        assertThat(parsed.bodyHtml()).contains("<ul>");
        assertThat(parsed.bodyHtml()).contains("<li>");
    }

    @Test
    void parse_fixture004323_columnZeroCodeDetectedAsFenced() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004323.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004323");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyMarkdown())
                .contains("interface Pair")
                .contains("record Impl");

        assertThat(parsed.bodyMarkdown()).contains("```");

        assertThat(parsed.bodyHtml()).contains("<pre><code>");
        assertThat(parsed.bodyHtml()).contains("interface Pair");
    }

    @Test
    void parse_fixture004324_quotedEmailHeadersNotTreatedAsCode() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004324.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004324");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyMarkdown())
                .contains("*From: *")
                .contains("*To: *")
                .contains("*Subject: *");

        String markdown = parsed.bodyMarkdown();
        int fromIndex = markdown.indexOf("*From: *");
        assertThat(fromIndex).isGreaterThan(-1);

        String beforeFrom = markdown.substring(0, fromIndex);
        int fenceCount = countOccurrences(beforeFrom, "```");
        assertThat(fenceCount % 2)
                .as("Email header *From: * should not be inside an unclosed code fence")
                .isEqualTo(0);

        assertThat(parsed.bodyMarkdown()).contains("deconstructor is not a method at all");
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    @ParameterizedTest(name = "fixture {0} markdown matches expected")
    @ValueSource(strings = {
            "amber-spec-experts/2026-January/004306",
            "amber-spec-experts/2026-January/004307",
            "amber-spec-experts/2026-January/004308",
            "amber-spec-experts/2026-January/004316",
            "amber-spec-experts/2026-January/004317",
            "amber-spec-experts/2026-January/004323",
            "amber-spec-experts/2026-January/004324",
            "panama-dev/2026-January/021257"
    })
    void parse_fixture_markdownMatchesExpected(String fixture) throws IOException {
        String html = loadFixture(fixture + ".html");
        String expectedMarkdown = loadExpectedMarkdown(fixture + ".html");
        String[] parts = fixture.split("/");
        MailPath mailPath = new MailPath(parts[0], parts[1], parts[2]);

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyMarkdown()).isEqualTo(expectedMarkdown);
    }
}
