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

class CfvMessageFilterTest {

    private final CfvMessageFilter filter = CfvMessageFilter.instance();

    @ParameterizedTest
    @ValueSource(strings = {
            "CFV: New JDK Committer: John Doe",
            "CFV: New JDK Member: Jane Smith",
            "CFV: New Serviceability Committer: Alice Johnson",
            "CFV: New Valhalla Member: Bob Wilson",
            "Result: New JDK Committer: John Doe",
            "Result: New JDK Member: Jane Smith",
            "Result: New Serviceability Committer: Alice Johnson",
            "Result: New Valhalla Member: Bob Wilson"
    })
    void test_cfvMessages_returnsTrue(String subject) {
        assertThat(filter.test(subject)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Re: CFV: New JDK Committer: John Doe",
            "Re: CFV: New JDK Member: Jane Smith",
            "Re: Result: New JDK Committer: John Doe",
            "Re: Result: New JDK Member: Jane Smith",
            "Re: Re: CFV: New JDK Committer: John Doe",
            "Re:   CFV: New JDK Committer: John Doe"
    })
    void test_cfvMessagesWithRePrefix_returnsTrue(String subject) {
        assertThat(filter.test(subject)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "cfv: new jdk committer: john doe",
            "CFV: NEW JDK COMMITTER: JOHN DOE",
            "result: new jdk member: jane smith",
            "RESULT: NEW JDK MEMBER: JANE SMITH"
    })
    void test_cfvMessagesCaseInsensitive_returnsTrue(String subject) {
        assertThat(filter.test(subject)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Discussion about project architecture",
            "Question: How to use the new API?",
            "RFR: 8365967: Fix something",
            "Integrated: 8365967: Fix something",
            "CFV: Vote on new feature",
            "Result: Vote outcome",
            "New Committer announcement"
    })
    void test_nonCfvMessages_returnsFalse(String subject) {
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
        assertThat(CfvMessageFilter.instance()).isSameAs(CfvMessageFilter.instance());
    }
}
