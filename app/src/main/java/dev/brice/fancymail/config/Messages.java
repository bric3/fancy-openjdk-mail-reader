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

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * UI messages configuration loaded from application.yml.
 */
@ConfigurationProperties("messages")
public class Messages {

    // Index page messages
    @ConfigurationProperties("index")
    public static class Index {
        private String title = "Fancy Mail";
        private String subtitle = "Beautify OpenJDK mailing list emails";
        private String formLabel = "Paste an OpenJDK mailing list URL:";
        private String formPlaceholder = "https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004307.html";
        private String formSubmit = "Beautify";
        private String exampleTitle = "Example URL";
        private String featureCleanTitle = "Clean Layout";
        private String featureCleanDesc = "Removes navigation clutter and formats content beautifully";
        private String featureCodeTitle = "Code Highlighting";
        private String featureCodeDesc = "Properly formatted code blocks with syntax highlighting";
        private String featureLinksTitle = "Link Rewriting";
        private String featureLinksDesc = "Links to other mails open in this reader too";

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getFormLabel() { return formLabel; }
        public void setFormLabel(String formLabel) { this.formLabel = formLabel; }
        public String getFormPlaceholder() { return formPlaceholder; }
        public void setFormPlaceholder(String formPlaceholder) { this.formPlaceholder = formPlaceholder; }
        public String getFormSubmit() { return formSubmit; }
        public void setFormSubmit(String formSubmit) { this.formSubmit = formSubmit; }
        public String getExampleTitle() { return exampleTitle; }
        public void setExampleTitle(String exampleTitle) { this.exampleTitle = exampleTitle; }
        public String getFeatureCleanTitle() { return featureCleanTitle; }
        public void setFeatureCleanTitle(String featureCleanTitle) { this.featureCleanTitle = featureCleanTitle; }
        public String getFeatureCleanDesc() { return featureCleanDesc; }
        public void setFeatureCleanDesc(String featureCleanDesc) { this.featureCleanDesc = featureCleanDesc; }
        public String getFeatureCodeTitle() { return featureCodeTitle; }
        public void setFeatureCodeTitle(String featureCodeTitle) { this.featureCodeTitle = featureCodeTitle; }
        public String getFeatureCodeDesc() { return featureCodeDesc; }
        public void setFeatureCodeDesc(String featureCodeDesc) { this.featureCodeDesc = featureCodeDesc; }
        public String getFeatureLinksTitle() { return featureLinksTitle; }
        public void setFeatureLinksTitle(String featureLinksTitle) { this.featureLinksTitle = featureLinksTitle; }
        public String getFeatureLinksDesc() { return featureLinksDesc; }
        public void setFeatureLinksDesc(String featureLinksDesc) { this.featureLinksDesc = featureLinksDesc; }

        // Template-friendly accessors
        public String title() { return title; }
        public String subtitle() { return subtitle; }
        public String formLabel() { return formLabel; }
        public String formPlaceholder() { return formPlaceholder; }
        public String formSubmit() { return formSubmit; }
        public String exampleTitle() { return exampleTitle; }
        public String featureCleanTitle() { return featureCleanTitle; }
        public String featureCleanDesc() { return featureCleanDesc; }
        public String featureCodeTitle() { return featureCodeTitle; }
        public String featureCodeDesc() { return featureCodeDesc; }
        public String featureLinksTitle() { return featureLinksTitle; }
        public String featureLinksDesc() { return featureLinksDesc; }
    }

    // Rendered page messages
    @ConfigurationProperties("rendered")
    public static class Rendered {
        private String back = "Back to Fancy Mail";
        private String errorLabel = "Error:";
        private String errorGoback = "Go back and try another URL";
        private String metaFrom = "From:";
        private String metaDate = "Date:";
        private String metaList = "List:";
        private String metaOriginal = "Original:";
        private String navNoPrevious = "No previous message";
        private String navNoNext = "No next message";
        private String navSortBy = "Sort by:";
        private String navDate = "date";
        private String navThread = "thread";
        private String navSubject = "subject";
        private String navAuthor = "author";
        private String footerSource = "Source:";
        private String footerMailingList = "mailing list";
        private String footerViewMarkdown = "View Markdown";
        private String footerViewOriginal = "View Original";

        public String getBack() { return back; }
        public void setBack(String back) { this.back = back; }
        public String getErrorLabel() { return errorLabel; }
        public void setErrorLabel(String errorLabel) { this.errorLabel = errorLabel; }
        public String getErrorGoback() { return errorGoback; }
        public void setErrorGoback(String errorGoback) { this.errorGoback = errorGoback; }
        public String getMetaFrom() { return metaFrom; }
        public void setMetaFrom(String metaFrom) { this.metaFrom = metaFrom; }
        public String getMetaDate() { return metaDate; }
        public void setMetaDate(String metaDate) { this.metaDate = metaDate; }
        public String getMetaList() { return metaList; }
        public void setMetaList(String metaList) { this.metaList = metaList; }
        public String getMetaOriginal() { return metaOriginal; }
        public void setMetaOriginal(String metaOriginal) { this.metaOriginal = metaOriginal; }
        public String getNavNoPrevious() { return navNoPrevious; }
        public void setNavNoPrevious(String navNoPrevious) { this.navNoPrevious = navNoPrevious; }
        public String getNavNoNext() { return navNoNext; }
        public void setNavNoNext(String navNoNext) { this.navNoNext = navNoNext; }
        public String getNavSortBy() { return navSortBy; }
        public void setNavSortBy(String navSortBy) { this.navSortBy = navSortBy; }
        public String getNavDate() { return navDate; }
        public void setNavDate(String navDate) { this.navDate = navDate; }
        public String getNavThread() { return navThread; }
        public void setNavThread(String navThread) { this.navThread = navThread; }
        public String getNavSubject() { return navSubject; }
        public void setNavSubject(String navSubject) { this.navSubject = navSubject; }
        public String getNavAuthor() { return navAuthor; }
        public void setNavAuthor(String navAuthor) { this.navAuthor = navAuthor; }
        public String getFooterSource() { return footerSource; }
        public void setFooterSource(String footerSource) { this.footerSource = footerSource; }
        public String getFooterMailingList() { return footerMailingList; }
        public void setFooterMailingList(String footerMailingList) { this.footerMailingList = footerMailingList; }
        public String getFooterViewMarkdown() { return footerViewMarkdown; }
        public void setFooterViewMarkdown(String footerViewMarkdown) { this.footerViewMarkdown = footerViewMarkdown; }
        public String getFooterViewOriginal() { return footerViewOriginal; }
        public void setFooterViewOriginal(String footerViewOriginal) { this.footerViewOriginal = footerViewOriginal; }

        // Template-friendly accessors
        public String back() { return back; }
        public String errorLabel() { return errorLabel; }
        public String errorGoback() { return errorGoback; }
        public String metaFrom() { return metaFrom; }
        public String metaDate() { return metaDate; }
        public String metaList() { return metaList; }
        public String metaOriginal() { return metaOriginal; }
        public String navNoPrevious() { return navNoPrevious; }
        public String navNoNext() { return navNoNext; }
        public String navSortBy() { return navSortBy; }
        public String navDate() { return navDate; }
        public String navThread() { return navThread; }
        public String navSubject() { return navSubject; }
        public String navAuthor() { return navAuthor; }
        public String footerSource() { return footerSource; }
        public String footerMailingList() { return footerMailingList; }
        public String footerViewMarkdown() { return footerViewMarkdown; }
        public String footerViewOriginal() { return footerViewOriginal; }
    }
}
