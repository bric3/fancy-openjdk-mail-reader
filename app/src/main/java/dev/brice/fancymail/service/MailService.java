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
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main service that coordinates fetching, parsing, and caching of mails.
 */
@Singleton
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private final MailFetcher mailFetcher;
    private final MailParser mailParser;
    private final MailCache mailCache;
    private final MarkdownConverter markdownConverter;

    public MailService(MailFetcher mailFetcher, MailParser mailParser,
                       MailCache mailCache, MarkdownConverter markdownConverter) {
        this.mailFetcher = mailFetcher;
        this.mailParser = mailParser;
        this.mailCache = mailCache;
        this.markdownConverter = markdownConverter;
    }

    /**
     * Get a parsed mail from URL, using cache if available.
     *
     * @param url the OpenJDK mailing list URL
     * @return the parsed mail
     */
    public ParsedMail getMail(String url) {
        MailPath mailPath = MailPath.fromUrl(url);
        return getMail(mailPath);
    }

    /**
     * Get a parsed mail from path components, using cache if available.
     *
     * @param mailPath the mail path
     * @return the parsed mail
     */
    public ParsedMail getMail(MailPath mailPath) {
        // Check cache first
        return mailCache.get(mailPath)
                .orElseGet(() -> fetchAndCache(mailPath));
    }

    /**
     * Get a parsed mail from path string.
     *
     * @param list      the mailing list name
     * @param yearMonth the year-month
     * @param id        the message ID
     * @return the parsed mail
     */
    public ParsedMail getMail(String list, String yearMonth, String id) {
        return getMail(new MailPath(list, yearMonth, id));
    }

    private ParsedMail fetchAndCache(MailPath mailPath) {
        LOG.info("Fetching mail: {}", mailPath.toOriginalUrl());

        // Fetch HTML
        String html = mailFetcher.fetch(mailPath.list(), mailPath.yearMonth(), mailPath.id());

        // Parse
        ParsedMail parsedMail = mailParser.parse(html, mailPath);

        // Cache
        mailCache.put(mailPath, parsedMail);

        return parsedMail;
    }

    /**
     * Get a mail formatted as Markdown.
     *
     * @param url the OpenJDK mailing list URL
     * @return the formatted Markdown content
     */
    public String getMailAsMarkdown(String url) {
        ParsedMail mail = getMail(url);
        return markdownConverter.formatMailAsMarkdown(
                mail.subject(),
                mail.from(),
                mail.email(),
                mail.date(),
                mail.list(),
                mail.bodyMarkdown(),
                mail.originalUrl()
        );
    }

    /**
     * Get a mail formatted as Markdown.
     *
     * @param mailPath the mail path
     * @return the formatted Markdown content
     */
    public String getMailAsMarkdown(MailPath mailPath) {
        ParsedMail mail = getMail(mailPath);
        return markdownConverter.formatMailAsMarkdown(
                mail.subject(),
                mail.from(),
                mail.email(),
                mail.date(),
                mail.list(),
                mail.bodyMarkdown(),
                mail.originalUrl()
        );
    }
}
