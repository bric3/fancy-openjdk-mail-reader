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

import dev.brice.fancymail.cache.MailCache;
import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private MailFetcher mailFetcher;

    @Mock
    private MailCache mailCache;

    private MailParser mailParser;
    private MarkdownConverter markdownConverter;
    private MailService mailService;

    private String loadFixture(String filename) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + filename)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @BeforeEach
    void setUp() {
        markdownConverter = new MarkdownConverter();
        LinkRewriter linkRewriter = new LinkRewriter();
        mailParser = new MailParser(markdownConverter, linkRewriter);
        mailService = new MailService(mailFetcher, mailParser, mailCache, markdownConverter);
    }

    @Test
    void getMail_cacheHit_returnsCachedMail() {
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");
        ParsedMail cachedMail = new ParsedMail(
                "Cached Subject", "Author", "email@test.com", "Date",
                "test-list", "body md", "body html", "url", null
        );
        when(mailCache.get(mailPath)).thenReturn(Optional.of(cachedMail));

        ParsedMail result = mailService.getMail(mailPath);

        assertThat(result.subject()).isEqualTo("Cached Subject");
        verify(mailFetcher, never()).fetch(anyString(), anyString(), anyString());
    }

    @Test
    void getMail_cacheMiss_fetchesAndCaches() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        when(mailCache.get(mailPath)).thenReturn(Optional.empty());
        when(mailFetcher.fetch("amber-spec-experts", "2026-January", "004306")).thenReturn(html);

        ParsedMail result = mailService.getMail(mailPath);

        assertThat(result.subject()).isEqualTo("Amber features 2026");
        verify(mailCache).put(eq(mailPath), any(ParsedMail.class));
    }

    @Test
    void getMail_byUrl_parsesAndFetches() throws IOException {
        String url = "https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004306.html";
        String html = loadFixture("amber-spec-experts/2026-January/004306.html");

        when(mailCache.get(any(MailPath.class))).thenReturn(Optional.empty());
        when(mailFetcher.fetch("amber-spec-experts", "2026-January", "004306")).thenReturn(html);

        ParsedMail result = mailService.getMail(url);

        assertThat(result.subject()).isEqualTo("Amber features 2026");
    }

    @Test
    void getMail_byComponents_fetchesCorrectly() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004307.html");

        when(mailCache.get(any(MailPath.class))).thenReturn(Optional.empty());
        when(mailFetcher.fetch("amber-spec-experts", "2026-January", "004307")).thenReturn(html);

        ParsedMail result = mailService.getMail("amber-spec-experts", "2026-January", "004307");

        assertThat(result.subject()).isEqualTo("Data Oriented Programming, Beyond Records");
    }

    @Test
    void getMailAsMarkdown_byUrl_returnsFormattedMarkdown() throws IOException {
        String url = "https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004306.html";
        String html = loadFixture("amber-spec-experts/2026-January/004306.html");

        when(mailCache.get(any(MailPath.class))).thenReturn(Optional.empty());
        when(mailFetcher.fetch("amber-spec-experts", "2026-January", "004306")).thenReturn(html);

        String markdown = mailService.getMailAsMarkdown(url);

        assertThat(markdown)
                .contains("# Amber features 2026")
                .contains("**From:**")
                .contains("**Date:**")
                .contains("**List:**");
    }

    @Test
    void getMailAsMarkdown_byMailPath_returnsFormattedMarkdown() throws IOException {
        String html = loadFixture("amber-spec-experts/2026-January/004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        when(mailCache.get(mailPath)).thenReturn(Optional.empty());
        when(mailFetcher.fetch("amber-spec-experts", "2026-January", "004307")).thenReturn(html);

        String markdown = mailService.getMailAsMarkdown(mailPath);

        assertThat(markdown).contains("# Data Oriented Programming, Beyond Records");
    }

    @Test
    void getMail_cacheHit_doesNotCallFetcher() {
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");
        ParsedMail cachedMail = new ParsedMail(
                "Subject", "Author", "email@test.com", "Date",
                "test-list", "body", "body", "url", null
        );
        when(mailCache.get(mailPath)).thenReturn(Optional.of(cachedMail));

        mailService.getMail(mailPath);

        verifyNoInteractions(mailFetcher);
    }
}
