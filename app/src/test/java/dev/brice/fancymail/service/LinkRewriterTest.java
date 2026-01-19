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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class LinkRewriterTest {

    private LinkRewriter linkRewriter;

    @BeforeEach
    void setUp() {
        PathsConfig pathsConfig = new PathsConfig();
        linkRewriter = new LinkRewriter(pathsConfig);
    }

    @Test
    void rewriteLink_fullOpenjdkUrl_rewritesToRenderedPath() {
        String href = "https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004306.html";

        String rewritten = linkRewriter.rewriteLink(href);

        assertThat(rewritten).isEqualTo("/rendered/amber-spec-experts/2026-January/004306.html");
    }

    @Test
    void rewriteLink_httpUrl_rewritesToRenderedPath() {
        String href = "http://mail.openjdk.org/pipermail/core-libs-dev/2025-December/123456.html";

        String rewritten = linkRewriter.rewriteLink(href);

        assertThat(rewritten).isEqualTo("/rendered/core-libs-dev/2025-December/123456.html");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "date.html",
            "thread.html",
            "subject.html",
            "author.html",
            "date.html#4306",
            "thread.html#4306"
    })
    void rewriteLink_navigationLink_notRewritten(String href) {
        String rewritten = linkRewriter.rewriteLink(href);

        assertThat(rewritten).isEqualTo(href);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://openjdk.org/jeps/468",
            "https://example.com/page",
            "http://other-site.org/path"
    })
    void rewriteLink_externalUrl_notRewritten(String href) {
        String rewritten = linkRewriter.rewriteLink(href);

        assertThat(rewritten).isEqualTo(href);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "mailto:test@example.com",
            "#anchor",
            ""
    })
    void rewriteLink_specialLinks_notRewritten(String href) {
        String rewritten = linkRewriter.rewriteLink(href);

        assertThat(rewritten).isEqualTo(href);
    }

    @Test
    void rewriteLink_nullHref_returnsNull() {
        String rewritten = linkRewriter.rewriteLink(null);

        assertThat(rewritten).isNull();
    }

    @Test
    void rewriteLinks_elementWithMultipleLinks_rewritesOpenjdkLinks() {
        String html = """
                <div>
                    <p>See <a href="https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004306.html">previous email</a></p>
                    <p>Check <a href="https://openjdk.org/jeps/468">JEP 468</a></p>
                    <p>Browse by <a href="date.html">date</a></p>
                </div>
                """;
        Document doc = Jsoup.parse(html);
        Element element = doc.body().selectFirst("div");

        linkRewriter.rewriteLinks(element);

        assertThat(element.selectFirst("a[href*=rendered]").attr("href"))
                .isEqualTo("/rendered/amber-spec-experts/2026-January/004306.html");
        assertThat(element.select("a").get(1).attr("href"))
                .isEqualTo("https://openjdk.org/jeps/468");
        assertThat(element.select("a").get(2).attr("href"))
                .isEqualTo("date.html");
    }

    @Test
    void rewriteMarkdownLinks_replacesOpenjdkUrls() {
        String markdown = """
                See [previous email](https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004306.html)
                and [JEP 468](https://openjdk.org/jeps/468).
                """;

        String rewritten = linkRewriter.rewriteMarkdownLinks(markdown, "amber-spec-experts", "2026-January");

        assertThat(rewritten)
                .contains("/rendered/amber-spec-experts/2026-January/004306.html")
                .contains("https://openjdk.org/jeps/468");
    }

    @Test
    void rewriteMarkdownLinks_nullInput_returnsNull() {
        String rewritten = linkRewriter.rewriteMarkdownLinks(null, "list", "month");

        assertThat(rewritten).isNull();
    }

    @Test
    void rewriteLink_customPrefix_usesConfiguredPrefix() {
        PathsConfig customConfig = new PathsConfig();
        customConfig.setRendered("mail");
        LinkRewriter customRewriter = new LinkRewriter(customConfig);

        String href = "https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004306.html";
        String rewritten = customRewriter.rewriteLink(href);

        assertThat(rewritten).isEqualTo("/mail/amber-spec-experts/2026-January/004306.html");
    }
}
