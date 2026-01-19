/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathsConfigTest {

    @Test
    void defaultValues_areCorrect() {
        PathsConfig config = new PathsConfig();

        assertThat(config.getRendered()).isEqualTo("rendered");
        assertThat(config.getMarkdown()).isEqualTo("markdown");
        assertThat(config.rendered()).isEqualTo("rendered");
        assertThat(config.markdown()).isEqualTo("markdown");
    }

    @Test
    void toRenderedPath_buildsCorrectPath() {
        PathsConfig config = new PathsConfig();

        String path = config.toRenderedPath("amber-spec-experts", "2026-January", "004307");

        assertThat(path).isEqualTo("/rendered/amber-spec-experts/2026-January/004307.html");
    }

    @Test
    void toMarkdownPath_buildsCorrectPath() {
        PathsConfig config = new PathsConfig();

        String path = config.toMarkdownPath("amber-spec-experts", "2026-January", "004307");

        assertThat(path).isEqualTo("/markdown/amber-spec-experts/2026-January/004307.md");
    }

    @Test
    void customRenderedPrefix_buildsCorrectPath() {
        PathsConfig config = new PathsConfig();
        config.setRendered("mail");

        String path = config.toRenderedPath("amber-spec-experts", "2026-January", "004307");

        assertThat(path).isEqualTo("/mail/amber-spec-experts/2026-January/004307.html");
    }

    @Test
    void customMarkdownPrefix_buildsCorrectPath() {
        PathsConfig config = new PathsConfig();
        config.setMarkdown("md");

        String path = config.toMarkdownPath("amber-spec-experts", "2026-January", "004307");

        assertThat(path).isEqualTo("/md/amber-spec-experts/2026-January/004307.md");
    }
}
