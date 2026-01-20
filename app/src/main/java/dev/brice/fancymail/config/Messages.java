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
 * All values must be defined in the YAML configuration.
 */
@ConfigurationProperties("messages")
public class Messages {

    // Index page messages
    @ConfigurationProperties("index")
    public static class Index {
        private String title;
        private String subtitle;
        private String formLabel;
        private String formPlaceholder;
        private String formSubmit;
        private String featureCleanTitle;
        private String featureCleanDesc;
        private String featureCodeTitle;
        private String featureCodeDesc;
        private String featureLinksTitle;
        private String featureLinksDesc;

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
        private String back;
        private String errorLabel;
        private String errorGoback;
        private String metaFrom;
        private String metaDate;
        private String metaList;
        private String metaOriginal;
        private String navNoPrevious;
        private String navNoNext;
        private String navSortBy;
        private String navDate;
        private String navThread;
        private String navSubject;
        private String navAuthor;
        private String footerSource;
        private String footerMailingList;
        private String footerViewMarkdown;
        private String footerViewOriginal;
        private String threadTitle;
        private String threadToggle;
        private String threadCurrent;
        private String threadMerkleRoot;

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
        public String getThreadTitle() { return threadTitle; }
        public void setThreadTitle(String threadTitle) { this.threadTitle = threadTitle; }
        public String getThreadToggle() { return threadToggle; }
        public void setThreadToggle(String threadToggle) { this.threadToggle = threadToggle; }
        public String getThreadCurrent() { return threadCurrent; }
        public void setThreadCurrent(String threadCurrent) { this.threadCurrent = threadCurrent; }
        public String getThreadMerkleRoot() { return threadMerkleRoot; }
        public void setThreadMerkleRoot(String threadMerkleRoot) { this.threadMerkleRoot = threadMerkleRoot; }

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
        public String threadTitle() { return threadTitle; }
        public String threadToggle() { return threadToggle; }
        public String threadCurrent() { return threadCurrent; }
        public String threadMerkleRoot() { return threadMerkleRoot; }
    }

    // Threads page messages
    @ConfigurationProperties("threads")
    public static class Threads {
        private String back;
        private String errorLabel;
        private String errorGoback;
        private String threadsTitle;
        private String totalMessages;
        private String noThreads;
        private String prevMonth;
        private String nextMonth;

        public String getBack() { return back; }
        public void setBack(String back) { this.back = back; }
        public String getErrorLabel() { return errorLabel; }
        public void setErrorLabel(String errorLabel) { this.errorLabel = errorLabel; }
        public String getErrorGoback() { return errorGoback; }
        public void setErrorGoback(String errorGoback) { this.errorGoback = errorGoback; }
        public String getThreadsTitle() { return threadsTitle; }
        public void setThreadsTitle(String threadsTitle) { this.threadsTitle = threadsTitle; }
        public String getTotalMessages() { return totalMessages; }
        public void setTotalMessages(String totalMessages) { this.totalMessages = totalMessages; }
        public String getNoThreads() { return noThreads; }
        public void setNoThreads(String noThreads) { this.noThreads = noThreads; }
        public String getPrevMonth() { return prevMonth; }
        public void setPrevMonth(String prevMonth) { this.prevMonth = prevMonth; }
        public String getNextMonth() { return nextMonth; }
        public void setNextMonth(String nextMonth) { this.nextMonth = nextMonth; }

        // Template-friendly accessors
        public String back() { return back; }
        public String errorLabel() { return errorLabel; }
        public String errorGoback() { return errorGoback; }
        public String threadsTitle() { return threadsTitle; }
        public String totalMessages() { return totalMessages; }
        public String noThreads() { return noThreads; }
        public String prevMonth() { return prevMonth; }
        public String nextMonth() { return nextMonth; }
    }
}
