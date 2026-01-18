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
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.views.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Global error handler that returns HTML error pages for browser requests.
 */
@Controller
public class ErrorController {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorController.class);

    private final DevModeConfig devModeConfig;

    public ErrorController(DevModeConfig devModeConfig) {
        this.devModeConfig = devModeConfig;
    }

    @Error(global = true, status = HttpStatus.NOT_FOUND)
    public HttpResponse<?> notFound(HttpRequest<?> request) {
        return handleError(request, HttpStatus.NOT_FOUND, "Page Not Found",
                "The page you're looking for doesn't exist.", null);
    }

    @Error(global = true, status = HttpStatus.BAD_REQUEST)
    public HttpResponse<?> badRequest(HttpRequest<?> request) {
        return handleError(request, HttpStatus.BAD_REQUEST, "Bad Request",
                "The request could not be understood.", null);
    }

    @Error(global = true, status = HttpStatus.INTERNAL_SERVER_ERROR)
    public HttpResponse<?> internalError(HttpRequest<?> request) {
        return handleError(request, HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                "An unexpected error occurred.", null);
    }

    @Error(global = true)
    public HttpResponse<?> handleException(HttpRequest<?> request, Throwable throwable) {
        LOG.error("Unhandled exception for request: {}", request.getPath(), throwable);

        String userMessage = devModeConfig.enabled()
                ? throwable.getMessage()
                : "An unexpected error occurred. Please try again later.";

        return handleError(request, HttpStatus.INTERNAL_SERVER_ERROR, "Error",
                userMessage, throwable);
    }

    private HttpResponse<?> handleError(HttpRequest<?> request, HttpStatus status,
                                        String title, String message, Throwable throwable) {
        if (acceptsHtml(request)) {
            return htmlErrorResponse(request, status, title, message, throwable);
        } else {
            return jsonErrorResponse(request, status, message);
        }
    }

    private boolean acceptsHtml(HttpRequest<?> request) {
        return request.getHeaders()
                .accept()
                .stream()
                .anyMatch(mt -> mt.matches(MediaType.TEXT_HTML_TYPE));
    }

    private HttpResponse<?> htmlErrorResponse(HttpRequest<?> request, HttpStatus status,
                                              String title, String message, Throwable throwable) {
        Map<String, Object> model = new HashMap<>();
        model.put("title", title);
        model.put("status", status.getCode());
        model.put("message", message);
        model.put("devMode", devModeConfig.enabled());

        if (devModeConfig.enabled()) {
            model.put("path", request.getPath());
            if (throwable != null) {
                StringWriter sw = new StringWriter();
                throwable.printStackTrace(new PrintWriter(sw));
                model.put("stackTrace", sw.toString());
            }
        }

        return HttpResponse.status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(new ModelAndView<>("error", model));
    }

    private HttpResponse<?> jsonErrorResponse(HttpRequest<?> request, HttpStatus status, String message) {
        String jsonMessage = devModeConfig.enabled()
                ? message
                : getGenericMessage(status);

        JsonError error = new JsonError(jsonMessage);
        if (devModeConfig.enabled()) {
            error.path(request.getPath());
        }

        return HttpResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    private String getGenericMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "The requested resource was not found.";
            case BAD_REQUEST -> "Invalid request.";
            case INTERNAL_SERVER_ERROR -> "An error occurred.";
            default -> "An error occurred.";
        };
    }
}
