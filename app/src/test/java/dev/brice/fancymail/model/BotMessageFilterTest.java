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
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BotMessageFilterTest {

    private final BotMessageFilter filter = BotMessageFilter.instance();

    @ParameterizedTest
    @ValueSource(strings = {
            "RFR: 8365967: Fix something",
            "Re: RFR: 8365967: Fix something",
            "Integrated: 8365967: Fix something",
            "Re: Integrated: 8365967: Fix something",
            "Withdrawn: 8365967: Fix something",
            "Re: Withdrawn: 8365967: Fix something",
            "git: some commit message"
    })
    void test_botMessagesWithoutPrefix_returnsTrue(String subject) {
        assertThat(filter.test(subject)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[vectorIntrinsics+fp16] RFR: 8365967: C2 compiler support",
            "[vectorIntrinsics+fp16] Re: RFR: 8365967: C2 compiler support",
            "[vectorIntrinsics+fp16] Integrated: 8365967: C2 compiler support",
            "[vectorIntrinsics+fp16] Re: Integrated: 8365967: C2 compiler support",
            "[vectorIntrinsics+fp16] Withdrawn: 8365967: C2 compiler support",
            "[vectorIntrinsics+fp16] Re: Withdrawn: 8365967: C2 compiler support",
            "[loom] RFR: 8365967: Virtual thread improvements",
            "[panama-foreign] Integrated: 8365967: Foreign memory API"
    })
    void test_botMessagesWithTagPrefix_returnsTrue(String subject) {
        assertThat(filter.test(subject)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Discussion about project architecture",
            "Question: How to use the new API?",
            "Re: Discussion about project architecture",
            "[loom] Question about virtual threads",
            "Feature request: Add support for X",
            "Bug report: Something is broken"
    })
    void test_humanMessages_returnsFalse(String subject) {
        assertThat(filter.test(subject)).isFalse();
    }

    @Test
    void test_nullSubject_returnsFalse() {
        assertThat(filter.test(null)).isFalse();
    }

    @Test
    void test_emptySubject_returnsFalse() {
        assertThat(filter.test("")).isFalse();
    }

    @Test
    void instance_returnsSameInstance() {
        assertThat(BotMessageFilter.instance()).isSameAs(BotMessageFilter.instance());
    }
}
