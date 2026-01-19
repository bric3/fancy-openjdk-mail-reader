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
import dev.brice.fancymail.config.PathsConfig;
import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    void setUp() {
        markdownConverter = new MarkdownConverter();
        LinkRewriter linkRewriter = new LinkRewriter(new PathsConfig());
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
