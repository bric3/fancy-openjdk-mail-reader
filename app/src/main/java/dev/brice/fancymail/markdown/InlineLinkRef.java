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

import com.vladsch.flexmark.util.ast.DoNotDecorate;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an inline link reference like {@code [1]} in the text.
 * This node replaces Text nodes that contain the reference pattern.
 */
public class InlineLinkRef extends Node implements DoNotDecorate {

    private final String refNumber;
    private String resolvedUrl;

    public InlineLinkRef(BasedSequence chars, String refNumber) {
        super(chars);
        this.refNumber = refNumber;
    }

    public String getRefNumber() {
        return refNumber;
    }

    public String getResolvedUrl() {
        return resolvedUrl;
    }

    public void setResolvedUrl(String resolvedUrl) {
        this.resolvedUrl = resolvedUrl;
    }

    public boolean isResolved() {
        return resolvedUrl != null && !resolvedUrl.isEmpty();
    }

    @Override
    public @NotNull BasedSequence[] getSegments() {
        return EMPTY_SEGMENTS;
    }
}
