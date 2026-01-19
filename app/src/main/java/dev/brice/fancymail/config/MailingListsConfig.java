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

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Configuration for a single mailing list entry.
 * Configured via fancymail.mailing-lists.* in application.yml.
 */
@EachProperty("fancymail.mailing-lists")
public class MailingListsConfig {

    private final String name;
    private String description;

    public MailingListsConfig(@Parameter String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }
}
