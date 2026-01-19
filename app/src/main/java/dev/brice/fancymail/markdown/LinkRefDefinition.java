/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.markdown;

import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a link reference definition like {@code [1] https://example.com}.
 * These are typically found at the end of mailing list emails.
 */
public class LinkRefDefinition extends Block {

    private final String refNumber;
    private final String url;

    public LinkRefDefinition(BasedSequence chars, String refNumber, String url) {
        super(chars);
        this.refNumber = refNumber;
        this.url = url;
    }

    public String getRefNumber() {
        return refNumber;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public @NotNull BasedSequence[] getSegments() {
        return EMPTY_SEGMENTS;
    }
}
