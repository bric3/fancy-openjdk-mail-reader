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

import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class MailParserTest {

    private MailParser parser;
    private MarkdownConverter markdownConverter;
    private LinkRewriter linkRewriter;

    @BeforeEach
    void setUp() {
        markdownConverter = new MarkdownConverter();
        linkRewriter = new LinkRewriter();
        parser = new MailParser(markdownConverter, linkRewriter);
    }

    private String loadFixture(String filename) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + filename)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parse_fixture004306_extractsSubject() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.subject()).isEqualTo("Amber features 2026");
    }

    @Test
    void parse_fixture004306_extractsDate() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Note: JSoup normalizes multiple spaces to single space
        assertThat(parsed.date()).isEqualTo("Fri Jan 9 23:08:05 UTC 2026");
    }

    @Test
    void parse_fixture004306_extractsEmail() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.email()).isEqualTo("gavin.bierman@oracle.com");
    }

    @Test
    void parse_fixture004306_extractsList() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.list()).isEqualTo("amber-spec-experts");
    }

    @Test
    void parse_fixture004306_extractsOriginalUrl() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.originalUrl())
                .isEqualTo("https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004306.html");
    }

    @Test
    void parse_fixture004306_bodyContainsContent() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyMarkdown() + parsed.bodyHtml())
                .containsIgnoringCase("spec experts");
    }

    @Test
    void parse_fixture004307_extractsSubject() throws IOException {
        String html = loadFixture("004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.subject()).isEqualTo("Data Oriented Programming, Beyond Records");
    }

    @Test
    void parse_fixture004307_extractsDate() throws IOException {
        String html = loadFixture("004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.date()).isEqualTo("Tue Jan 13 21:52:47 UTC 2026");
    }

    @Test
    void parse_fixture004307_extractsEmail() throws IOException {
        String html = loadFixture("004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.email()).isEqualTo("brian.goetz@oracle.com");
    }

    @Test
    void parse_fixture004307_bodyContainsCarrierClasses() throws IOException {
        String html = loadFixture("004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        // The body should contain discussion about carrier classes
        assertThat(parsed.bodyMarkdown().toLowerCase() + parsed.bodyHtml().toLowerCase())
                .contains("carrier");
    }

    @Test
    void parse_simpleHtml_extractsBasicInfo() {
        String html = """
                <!DOCTYPE HTML>
                <HTML>
                <HEAD><TITLE>Test Subject</TITLE></HEAD>
                <BODY>
                <H1>Test Subject</H1>
                <B>Test Author</B>
                test.author at example.com
                <BR>
                <I>Mon Jan 1 12:00:00 UTC 2026</I>
                <HR>
                <PRE>This is the email body content.</PRE>
                </BODY>
                </HTML>
                """;
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.subject()).isEqualTo("Test Subject");
        assertThat(parsed.from()).isEqualTo("Test Author");
        assertThat(parsed.date()).isEqualTo("Mon Jan 1 12:00:00 UTC 2026");
        assertThat(parsed.email()).isEqualTo("test.author@example.com");
    }

    @Test
    void parse_htmlWithNavigationLinks_removesNavigation() {
        String html = """
                <!DOCTYPE HTML>
                <HTML>
                <HEAD><TITLE>Test</TITLE></HEAD>
                <BODY>
                <UL>
                    <LI><a href="date.html">date</a></LI>
                    <LI><a href="thread.html">thread</a></LI>
                </UL>
                <PRE>Body content</PRE>
                <UL>
                    <LI><a href="date.html">date</a></LI>
                </UL>
                </BODY>
                </HTML>
                """;
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Navigation links should not appear in the body
        assertThat(parsed.bodyHtml())
                .doesNotContain("date.html")
                .doesNotContain("thread.html");
    }

    @Test
    void parse_pipermailItalicTags_strippedFromBlockquotes() {
        // Pipermail uses <i>...</I> tags to italicize quoted content
        String html = """
                <!DOCTYPE HTML>
                <HTML>
                <HEAD><TITLE>Test</TITLE></HEAD>
                <BODY>
                <H1>Test</H1>
                <PRE>&gt;<i> This is quoted text
                </I>&gt;<i> More quoted text
                </I>
                Reply text here.
                </PRE>
                </BODY>
                </HTML>
                """;
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Italic tags should be stripped, blockquote should render
        assertThat(parsed.bodyHtml()).contains("<blockquote>");
        assertThat(parsed.bodyHtml()).doesNotContain("<i>");
        assertThat(parsed.bodyHtml()).doesNotContain("</i>");
        assertThat(parsed.bodyMarkdown())
                .contains("> This is quoted text")
                .doesNotContain("<i>")
                .doesNotContain("</i>");
    }

    @Test
    void parse_codeBlockInsideBlockquote_rendersAsCodeBlock() {
        // Code blocks inside blockquotes need proper spacing: > + space + 4 spaces
        String html = """
                <!DOCTYPE HTML>
                <HTML>
                <HEAD><TITLE>Test</TITLE></HEAD>
                <BODY>
                <H1>Test</H1>
                <PRE>&gt;<i> Here is some code:
                </I>&gt;<i>
                </I>&gt;<i>     void example() {
                </I>&gt;<i>         return;
                </I>&gt;<i>     }
                </I>&gt;<i>
                </I>&gt;<i> End of code.
                </I></PRE>
                </BODY>
                </HTML>
                """;
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Code block should render with <pre><code> inside blockquote
        assertThat(parsed.bodyHtml()).contains("<blockquote>");
        assertThat(parsed.bodyHtml()).contains("<pre><code>");
        assertThat(parsed.bodyHtml()).contains("void example()");
    }

    @Test
    void parse_lightlyIndentedCodeWithKeywords_rendersAsCodeBlock() {
        // Lines with 2-3 space indentation containing Java keywords should become code blocks
        // Using explicit \n and spaces to ensure indentation is preserved
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Just a question, are you proposing that\n" +
                "  case Point(0, 0) -&gt; ...\n" +
                "\n" +
                "is semantically equivalent to\n" +
                "  case Point(var x, var y) when x == 0 -&gt; ...</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // The indented code lines should render as code blocks, not merged into paragraphs
        assertThat(parsed.bodyHtml()).contains("<pre><code>");
        assertThat(parsed.bodyHtml()).contains("case Point(0, 0)");
        // Should not be merged into the paragraph
        assertThat(parsed.bodyHtml()).doesNotContain("proposing that case");
    }

    @Test
    void parse_lightlyIndentedCodeWithOperators_rendersAsCodeBlock() {
        // Lines with operators like ->, ==, != should be detected as code
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Consider this example:\n" +
                "  x == 0 &amp;&amp; y == 0\n" +
                "  result -&gt; process()</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyHtml()).contains("<pre><code>");
        assertThat(parsed.bodyHtml()).contains("x == 0");
    }

    @Test
    void parse_lightlyIndentedNonCode_remainsAsParagraph() {
        // Lines with 2-3 space indentation but no code patterns should stay as text
        // Note: avoid Java keywords like "this", "new", "class" in the test text
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Here is my thought:\n" +
                "  just some regular text\n" +
                "  nothing special here</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should be rendered as regular paragraph text, not code
        assertThat(parsed.bodyHtml()).doesNotContain("<pre><code>just some");
    }
}
