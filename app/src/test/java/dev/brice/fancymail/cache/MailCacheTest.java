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

import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@MicronautTest
class MailCacheTest {

    @Inject
    MailCache mailCache;

    private ParsedMail createTestMail(String id) {
        return new ParsedMail(
                "Test Subject " + id,
                "Test Author",
                "test@example.com",
                "Mon Jan 1 12:00:00 UTC 2026",
                "test-list",
                "# Body markdown",
                "<h1>Body HTML</h1>",
                "https://mail.openjdk.org/pipermail/test-list/2026-January/" + id + ".html"
        );
    }

    @BeforeEach
    void setUp() {
        mailCache.clear();
    }

    @Test
    void get_emptyCache_returnsEmpty() {
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        Optional<ParsedMail> result = mailCache.get(mailPath);

        assertThat(result).isEmpty();
    }

    @Test
    void put_thenGet_returnsCachedValue() {
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");
        ParsedMail mail = createTestMail("000001");

        mailCache.put(mailPath, mail);
        Optional<ParsedMail> result = mailCache.get(mailPath);

        assertThat(result).isPresent();
        assertThat(result.get().subject()).isEqualTo("Test Subject 000001");
    }

    @Test
    void put_multipleMails_allRetrievable() {
        MailPath path1 = new MailPath("list1", "2026-January", "000001");
        MailPath path2 = new MailPath("list2", "2026-February", "000002");
        ParsedMail mail1 = createTestMail("000001");
        ParsedMail mail2 = createTestMail("000002");

        mailCache.put(path1, mail1);
        mailCache.put(path2, mail2);

        assertThat(mailCache.get(path1)).isPresent();
        assertThat(mailCache.get(path2)).isPresent();
        assertThat(mailCache.get(path1).get().subject()).isEqualTo("Test Subject 000001");
        assertThat(mailCache.get(path2).get().subject()).isEqualTo("Test Subject 000002");
    }

    @Test
    void clear_afterPut_cacheIsEmpty() {
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");
        ParsedMail mail = createTestMail("000001");

        mailCache.put(mailPath, mail);
        mailCache.clear();
        Optional<ParsedMail> result = mailCache.get(mailPath);

        assertThat(result).isEmpty();
    }

    @Test
    void getStats_returnsStatsString() {
        String stats = mailCache.getStats();

        assertThat(stats)
                .isNotNull()
                .contains("hits=")
                .contains("misses=");
    }

    @Test
    void get_sameKeyTwice_returnsSameValue() {
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");
        ParsedMail mail = createTestMail("000001");

        mailCache.put(mailPath, mail);
        Optional<ParsedMail> first = mailCache.get(mailPath);
        Optional<ParsedMail> second = mailCache.get(mailPath);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(second.get().subject()).isEqualTo(first.get().subject());
    }
}
