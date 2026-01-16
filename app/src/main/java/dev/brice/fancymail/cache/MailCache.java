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
import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

/**
 * In-memory cache for parsed mails using Caffeine.
 */
@Singleton
public class MailCache {

    private static final Logger LOG = LoggerFactory.getLogger(MailCache.class);

    @Value("${fancymail.cache.max-size:1000}")
    private int maxSize;

    @Value("${fancymail.cache.expire-after-write:PT1H}")
    private Duration expireAfterWrite;

    private Cache<String, ParsedMail> cache;

    @PostConstruct
    void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWrite)
                .recordStats()
                .build();
        LOG.info("Mail cache initialized with maxSize={}, expireAfterWrite={}", maxSize, expireAfterWrite);
    }

    /**
     * Get a cached mail by its path.
     *
     * @param mailPath the mail path
     * @return the cached mail, or empty if not cached
     */
    public Optional<ParsedMail> get(MailPath mailPath) {
        String key = mailPath.toCacheKey();
        ParsedMail mail = cache.getIfPresent(key);
        if (mail != null) {
            LOG.debug("Cache hit for: {}", key);
        } else {
            LOG.debug("Cache miss for: {}", key);
        }
        return Optional.ofNullable(mail);
    }

    /**
     * Cache a parsed mail.
     *
     * @param mailPath   the mail path
     * @param parsedMail the parsed mail
     */
    public void put(MailPath mailPath, ParsedMail parsedMail) {
        String key = mailPath.toCacheKey();
        cache.put(key, parsedMail);
        LOG.debug("Cached mail: {}", key);
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
        LOG.info("Cache cleared");
    }
}
