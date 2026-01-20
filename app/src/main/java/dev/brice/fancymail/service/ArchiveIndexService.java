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

import dev.brice.fancymail.cache.ArchiveIndexCache;
import dev.brice.fancymail.model.ArchiveIndex;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service to orchestrate archive index fetching, parsing, and caching.
 */
@Singleton
public class ArchiveIndexService {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveIndexService.class);

    private final MailFetcher mailFetcher;
    private final ArchiveIndexParser archiveIndexParser;
    private final ArchiveIndexCache archiveIndexCache;

    public ArchiveIndexService(
            MailFetcher mailFetcher,
            ArchiveIndexParser archiveIndexParser,
            ArchiveIndexCache archiveIndexCache) {
        this.mailFetcher = mailFetcher;
        this.archiveIndexParser = archiveIndexParser;
        this.archiveIndexCache = archiveIndexCache;
    }

    /**
     * Get the archive index for a mailing list, using cache if available.
     *
     * @param list the mailing list name
     * @return the archive index containing available months
     */
    public ArchiveIndex getArchiveIndex(String list) {
        // Check cache first
        Optional<ArchiveIndex> cached = archiveIndexCache.get(list);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Fetch and parse
        LOG.info("Fetching archive index for {}", list);
        String html = mailFetcher.fetchArchiveIndex(list);
        ArchiveIndex index = archiveIndexParser.parse(html, list);

        // Cache the result
        archiveIndexCache.put(index);

        return index;
    }
}
