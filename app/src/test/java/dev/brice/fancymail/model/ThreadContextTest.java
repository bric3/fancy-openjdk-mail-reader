/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ThreadContextTest {

    @ParameterizedTest
    @CsvSource({
            "2026-January, 2026-01",
            "2026-February, 2026-02",
            "2026-March, 2026-03",
            "2026-April, 2026-04",
            "2026-May, 2026-05",
            "2026-June, 2026-06",
            "2026-July, 2026-07",
            "2026-August, 2026-08",
            "2026-September, 2026-09",
            "2026-October, 2026-10",
            "2026-November, 2026-11",
            "2026-December, 2026-12",
            "2025-March, 2025-03"
    })
    void yearMonthIso_convertsToIsoFormat(String pipermail, String expectedIso) {
        ThreadEntry entry = new ThreadEntry("001", "Subject", "Author", 0);
        ThreadContext context = new ThreadContext(entry, List.of(), List.of(), entry, "list", pipermail, null);

        assertThat(context.yearMonthIso()).isEqualTo(expectedIso);
    }

    @Test
    void yearMonthIso_nullYearMonth_returnsNull() {
        ThreadEntry entry = new ThreadEntry("001", "Subject", "Author", 0);
        ThreadContext context = new ThreadContext(entry, List.of(), List.of(), entry, "list", null, null);

        assertThat(context.yearMonthIso()).isNull();
    }

    @Test
    void isRoot_whenAncestorsEmpty_returnsTrue() {
        ThreadEntry entry = new ThreadEntry("001", "Subject", "Author", 0);
        ThreadContext context = new ThreadContext(entry, List.of(), List.of(), entry, "list", "2026-January", null);

        assertThat(context.isRoot()).isTrue();
    }

    @Test
    void isRoot_whenHasAncestors_returnsFalse() {
        ThreadEntry root = new ThreadEntry("001", "Subject", "Author", 0);
        ThreadEntry child = new ThreadEntry("002", "Re: Subject", "Reply Author", 1);
        ThreadContext context = new ThreadContext(child, List.of(root), List.of(), root, "list", "2026-January", null);

        assertThat(context.isRoot()).isFalse();
    }

    @Test
    void threadMessageCount_returnsCorrectCount() {
        ThreadEntry root = new ThreadEntry("001", "Subject", "Author", 0);
        ThreadEntry reply1 = new ThreadEntry("002", "Re: Subject", "Reply Author", 1);
        ThreadEntry reply2 = new ThreadEntry("003", "Re: Subject", "Another Author", 1);
        root.replies().add(reply1);
        root.replies().add(reply2);

        ThreadContext context = new ThreadContext(root, List.of(), List.of(), root, "list", "2026-January", null);

        assertThat(context.threadMessageCount()).isEqualTo(3);
    }

    @Test
    void currentDepth_returnsEntryDepth() {
        ThreadEntry entry = new ThreadEntry("001", "Subject", "Author", 2);
        ThreadContext context = new ThreadContext(entry, List.of(), List.of(), entry, "list", "2026-January", null);

        assertThat(context.currentDepth()).isEqualTo(2);
    }
}
