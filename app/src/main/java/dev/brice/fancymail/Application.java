/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail;

import dev.brice.fancymail.model.MailPath;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.runtime.Micronaut;

import java.awt.Desktop;
import java.net.URI;

/**
 * Main application entry point.
 */
public class Application {

    public static void main(String[] args) {
        // Determine the mode based on arguments
        Mode mode = determineMode(args);
        int port = extractPort(args);
        boolean openBrowser = hasFlag(args, "--open");
        boolean devMode = hasFlag(args, "-d", "--dev");

        switch (mode) {
            case CLI -> {
                // CLI mode - use Picocli for --url, --help, --version
                int exitCode = PicocliRunner.execute(FancyMailCommand.class, args);
                System.exit(exitCode);
            }
            case SERVER -> {
                // Server mode - start Micronaut HTTP server
                System.setProperty("micronaut.server.port", String.valueOf(port));
                System.setProperty("fancymail.dev-mode", String.valueOf(devMode));

                if (devMode) {
                    System.out.println("Development mode enabled - debug info will be shown");
                }

                // Determine the URL to open
                String browserUrl = "http://localhost:" + port;
                String mailUrl = extractValue(args, "-u", "--url");
                if (mailUrl != null) {
                    try {
                        MailPath mailPath = MailPath.fromUrl(mailUrl);
                        browserUrl = "http://localhost:" + port + mailPath.toRenderedPath();
                    } catch (IllegalArgumentException e) {
                        System.err.println("Warning: Could not parse URL, opening home page: " + e.getMessage());
                    }
                }

                // Open browser after a delay
                if (openBrowser && Desktop.isDesktopSupported()) {
                    final String urlToOpen = browserUrl;
                    new Thread(() -> {
                        try {
                            Thread.sleep(2500);
                            System.out.println("Opening browser at " + urlToOpen);
                            Desktop.getDesktop().browse(URI.create(urlToOpen));
                        } catch (Exception e) {
                            System.err.println("Could not open browser: " + e.getMessage());
                        }
                    }).start();
                }

                Micronaut.run(Application.class, args);
            }
        }
    }

    private enum Mode { CLI, SERVER }

    private static Mode determineMode(String[] args) {
        // Explicit server mode
        if (hasFlag(args, "-s", "--server")) {
            return Mode.SERVER;
        }

        // CLI mode triggers (only if not server mode)
        if (hasFlag(args, "-u", "--url") ||
            hasFlag(args, "-h", "--help") ||
            hasFlag(args, "-V", "--version")) {
            return Mode.CLI;
        }

        // Default to server mode if no relevant args
        return Mode.SERVER;
    }

    private static boolean hasFlag(String[] args, String... flags) {
        for (String arg : args) {
            for (String flag : flags) {
                if (arg.equals(flag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int extractPort(String[] args) {
        String value = extractValue(args, "-p", "--port");
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return 8080;
    }

    private static String extractValue(String[] args, String... flags) {
        for (int i = 0; i < args.length - 1; i++) {
            for (String flag : flags) {
                if (args[i].equals(flag)) {
                    return args[i + 1];
                }
            }
        }
        return null;
    }
}
