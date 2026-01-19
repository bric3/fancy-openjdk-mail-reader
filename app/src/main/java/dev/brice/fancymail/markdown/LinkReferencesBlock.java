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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Block node that contains the collected link references.
 * This is rendered as the footnotes section at the end of the document.
 */
public class LinkReferencesBlock extends Block {

    private final Map<String, String> references;

    public LinkReferencesBlock(Map<String, String> references) {
        super(BasedSequence.NULL);
        this.references = new LinkedHashMap<>(references);
    }

    public Map<String, String> getReferences() {
        return Collections.unmodifiableMap(references);
    }

    @Override
    public @NotNull BasedSequence[] getSegments() {
        return EMPTY_SEGMENTS;
    }
}
