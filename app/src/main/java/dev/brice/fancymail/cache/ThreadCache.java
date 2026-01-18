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
import dev.brice.fancymail.model.ThreadTree;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

/**
 * In-memory cache for thread trees using Caffeine.
 * Uses a longer TTL than the mail cache since thread structure changes less frequently.
 */
@Singleton
public class ThreadCache {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadCache.class);

    @Value("${fancymail.thread-cache.max-size:100}")
    private int maxSize;

    @Value("${fancymail.thread-cache.expire-after-write:PT6H}")
    private Duration expireAfterWrite;

    private Cache<String, ThreadTree> cache;

    @PostConstruct
    void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWrite)
                .recordStats()
                .build();
        LOG.info("Thread cache initialized with maxSize={}, expireAfterWrite={}", maxSize, expireAfterWrite);
    }

    /**
     * Get a cached thread tree by list and year-month.
     *
     * @param list      the mailing list name
     * @param yearMonth the year-month
     * @return the cached thread tree, or empty if not cached
     */
    public Optional<ThreadTree> get(String list, String yearMonth) {
        String key = toCacheKey(list, yearMonth);
        ThreadTree tree = cache.getIfPresent(key);
        if (tree != null) {
            LOG.debug("Thread cache hit for: {}", key);
        } else {
            LOG.debug("Thread cache miss for: {}", key);
        }
        return Optional.ofNullable(tree);
    }

    /**
     * Cache a thread tree.
     *
     * @param tree the thread tree to cache
     */
    public void put(ThreadTree tree) {
        String key = toCacheKey(tree.list(), tree.yearMonth());
        cache.put(key, tree);
        LOG.debug("Cached thread tree: {}", key);
    }

    /**
     * Generate cache key from list and year-month.
     */
    private String toCacheKey(String list, String yearMonth) {
        return list + "/" + yearMonth;
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
        LOG.info("Thread cache cleared");
    }
}
