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

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Flexmark extension for mailing list style link references.
 * <p>
 * This extension handles the common mailing list pattern where references
 * are written as {@code [n]} inline and defined at the end as {@code [n] URL}.
 * <p>
 * Example:
 * <pre>
 * See the documentation[1] and example[2].
 *
 * [1] https://example.com/docs
 * [2] https://example.com/example
 * </pre>
 * <p>
 * The extension converts inline {@code [n]} to superscript links and
 * renders the definitions as a footnotes section.
 */
public class MailingListLinkRefExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    private MailingListLinkRefExtension() {
    }

    public static MailingListLinkRefExtension create() {
        return new MailingListLinkRefExtension();
    }

    @Override
    public void rendererOptions(@NotNull MutableDataHolder options) {
        // No renderer options needed
    }

    @Override
    public void parserOptions(@NotNull MutableDataHolder options) {
        // No parser options needed
    }

    @Override
    public void extend(@NotNull Parser.Builder parserBuilder) {
        parserBuilder.postProcessorFactory(new MailingListLinkRefPostProcessor.Factory());
    }

    @Override
    public void extend(@NotNull HtmlRenderer.Builder htmlRendererBuilder, @NotNull String rendererType) {
        if (htmlRendererBuilder.isRendererType("HTML")) {
            htmlRendererBuilder.nodeRendererFactory(new MailingListLinkRefRenderer.Factory());
        }
    }
}
