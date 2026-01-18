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

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

/**
 * Configuration for development mode.
 * When enabled, shows debug information like full Merkle hashes and detailed error messages.
 */
@Singleton
public class DevModeConfig {

    private final boolean enabled;

    public DevModeConfig(@Value("${fancymail.dev-mode:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
