/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.brice.fancymail.model.ArchiveIndex;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

/**
 * In-memory cache for archive indexes using Caffeine.
 * Uses a long TTL since archive index rarely changes (only when a new month starts).
 */
@Singleton
public class ArchiveIndexCache {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveIndexCache.class);

    @Value("${fancymail.archive-index-cache.max-size:50}")
    private int maxSize;

    @Value("${fancymail.archive-index-cache.expire-after-write:PT24H}")
    private Duration expireAfterWrite;

    private Cache<String, ArchiveIndex> cache;

    @PostConstruct
    void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWrite)
                .recordStats()
                .build();
        LOG.info("Archive index cache initialized with maxSize={}, expireAfterWrite={}", maxSize, expireAfterWrite);
    }

    /**
     * Get a cached archive index by mailing list name.
     *
     * @param list the mailing list name
     * @return the cached archive index, or empty if not cached
     */
    public Optional<ArchiveIndex> get(String list) {
        ArchiveIndex index = cache.getIfPresent(list);
        if (index != null) {
            LOG.debug("Archive index cache hit for: {}", list);
        } else {
            LOG.debug("Archive index cache miss for: {}", list);
        }
        return Optional.ofNullable(index);
    }

    /**
     * Cache an archive index.
     *
     * @param index the archive index to cache
     */
    public void put(ArchiveIndex index) {
        cache.put(index.list(), index);
        LOG.debug("Cached archive index: {}", index.list());
    }

    /**
     * Get cache statistics.
     *
     * @return a string representation of cache stats
     */
    public String getStats() {
        var stats = cache.stats();
        return String.format("hits=%d, misses=%d, hitRate=%.2f%%, size=%d",
                stats.hitCount(), stats.missCount(), stats.hitRate() * 100, cache.estimatedSize());
    }

    /**
     * Clear the cache.
     */
    public void clear() {
        cache.invalidateAll();
        LOG.info("Archive index cache cleared");
    }
}
