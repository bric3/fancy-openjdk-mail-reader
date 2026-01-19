/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.controller;

import dev.brice.fancymail.config.DevModeConfig;
import dev.brice.fancymail.config.MailingListsConfig;
import dev.brice.fancymail.config.Messages.Index;
import dev.brice.fancymail.config.Messages.Rendered;
import dev.brice.fancymail.config.Messages.Threads;
import dev.brice.fancymail.config.PathsConfig;
import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import dev.brice.fancymail.model.ThreadContext;
import dev.brice.fancymail.model.ThreadTree;
import dev.brice.fancymail.service.MailService;
import dev.brice.fancymail.service.ThreadService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.views.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for the web UI and rendered mail views.
 */
@Controller
@ExecuteOn(TaskExecutors.BLOCKING)
public class MailController {

    private static final Logger LOG = LoggerFactory.getLogger(MailController.class);

    private final MailService mailService;
    private final ThreadService threadService;
    private final Index indexMessages;
    private final Rendered renderedMessages;
    private final Threads threadsMessages;
    private final DevModeConfig devModeConfig;
    private final PathsConfig pathsConfig;
    private final List<MailingListsConfig> mailingLists;

    public MailController(MailService mailService, ThreadService threadService, Index indexMessages, Rendered renderedMessages, Threads threadsMessages, DevModeConfig devModeConfig, PathsConfig pathsConfig, List<MailingListsConfig> mailingLists) {
        this.mailService = mailService;
        this.threadService = threadService;
        this.indexMessages = indexMessages;
        this.renderedMessages = renderedMessages;
        this.threadsMessages = threadsMessages;
        this.devModeConfig = devModeConfig;
        this.pathsConfig = pathsConfig;
        this.mailingLists = mailingLists;
    }

    /**
     * Home page with URL input form.
     */
    @Get("/")
    @View("index")
    public Map<String, Object> index() {
        return Map.of(
                "title", "Fancy Mail - OpenJDK Mailing List Beautifier",
                "msg", indexMessages,
                "devMode", devModeConfig.enabled(),
                "mailingLists", mailingLists,
                "currentMonth", getCurrentYearMonth(),
                "paths", pathsConfig
        );
    }

    /**
     * Returns the current year-month in OpenJDK format (e.g., "2026-January").
     */
    private String getCurrentYearMonth() {
        LocalDate now = LocalDate.now();
        String month = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return now.getYear() + "-" + month;
    }

    /**
     * Handle form submission - redirect to rendered view.
     */
    @Post("/go")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> go(@Body Map<String, String> form) {
        String url = form.get("url");
        if (url == null || url.isBlank()) {
            return HttpResponse.badRequest("URL is required");
        }

        try {
            MailPath mailPath = MailPath.fromUrl(url.trim());
            return HttpResponse.redirect(URI.create(pathsConfig.toRenderedPath(mailPath.list(), mailPath.yearMonth(), mailPath.id())));
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid URL submitted: {}", url);
            return HttpResponse.badRequest("Invalid OpenJDK mailing list URL");
        }
    }

    /**
     * Render a mail by its path components.
     */
    @Get("/${fancymail.paths.rendered:rendered}/{list}/{yearMonth}/{id}.html")
    @View("rendered")
    public Map<String, Object> rendered(
            @PathVariable String list,
            @PathVariable String yearMonth,
            @PathVariable String id,
            @QueryValue(value = "thread", defaultValue = "") String threadParam) {

        LOG.info("Rendering mail: {}/{}/{}", list, yearMonth, id);
        boolean threadOpen = "open".equals(threadParam);

        try {
            ParsedMail mail = mailService.getMail(list, yearMonth, id);

            // Fetch thread context (non-critical - don't fail if thread unavailable)
            ThreadContext threadContext = null;
            try {
                threadContext = threadService.getThreadContext(list, yearMonth, id);
            } catch (Exception e) {
                LOG.warn("Could not fetch thread context for {}/{}/{}: {}", list, yearMonth, id, e.getMessage());
            }

            Map<String, Object> model = new HashMap<>();
            model.put("title", mail.subject());
            model.put("mail", mail);
            model.put("list", list);
            model.put("yearMonth", yearMonth);
            model.put("id", id);
            model.put("msg", renderedMessages);
            model.put("threadContext", threadContext);
            model.put("threadOpen", threadOpen);
            model.put("devMode", devModeConfig.enabled());
            model.put("paths", pathsConfig);
            return model;
        } catch (Exception e) {
            LOG.error("Error rendering mail: {}/{}/{}", list, yearMonth, id, e);
            String errorMessage = "Failed to fetch or parse mail: " + e.getMessage();
            String stackTrace = null;
            if (devModeConfig.enabled()) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                stackTrace = sw.toString();
            }
            Map<String, Object> errorModel = new HashMap<>();
            errorModel.put("title", "Error");
            errorModel.put("error", errorMessage);
            errorModel.put("stackTrace", stackTrace);
            errorModel.put("msg", renderedMessages);
            errorModel.put("devMode", devModeConfig.enabled());
            errorModel.put("paths", pathsConfig);
            return errorModel;
        }
    }

    /**
     * Get raw markdown for a mail.
     */
    @Get("/${fancymail.paths.markdown:markdown}/{list}/{yearMonth}/{id}.md")
    @Produces("text/plain; charset=utf-8")
    public String markdown(
            @PathVariable String list,
            @PathVariable String yearMonth,
            @PathVariable String id) {

        MailPath mailPath = new MailPath(list, yearMonth, id);
        return mailService.getMailAsMarkdown(mailPath);
    }

    /**
     * Show thread list for a mailing list and month.
     */
    @Get("/${fancymail.paths.threads:threads}/{list}/{yearMonth}")
    @View("threads")
    public Map<String, Object> threads(
            @PathVariable String list,
            @PathVariable String yearMonth,
            @QueryValue(value = "hideBot", defaultValue = "true") boolean hideBot) {

        LOG.info("Showing threads for {}/{} (hideBot={})", list, yearMonth, hideBot);

        try {
            ThreadTree threadTree = threadService.getThreadTree(list, yearMonth);

            Map<String, Object> model = new HashMap<>();
            model.put("title", list + " - " + yearMonth);
            model.put("list", list);
            model.put("yearMonth", yearMonth);
            model.put("threadTree", threadTree);
            model.put("hideBot", hideBot);
            model.put("msg", threadsMessages);
            model.put("devMode", devModeConfig.enabled());
            model.put("paths", pathsConfig);
            return model;
        } catch (Exception e) {
            LOG.error("Error fetching threads for {}/{}", list, yearMonth, e);
            String errorMessage = "Failed to fetch threads: " + e.getMessage();
            String stackTrace = null;
            if (devModeConfig.enabled()) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                stackTrace = sw.toString();
            }
            Map<String, Object> errorModel = new HashMap<>();
            errorModel.put("title", "Error");
            errorModel.put("list", list);
            errorModel.put("yearMonth", yearMonth);
            errorModel.put("error", errorMessage);
            errorModel.put("stackTrace", stackTrace);
            errorModel.put("msg", threadsMessages);
            errorModel.put("devMode", devModeConfig.enabled());
            errorModel.put("paths", pathsConfig);
            return errorModel;
        }
    }
}
