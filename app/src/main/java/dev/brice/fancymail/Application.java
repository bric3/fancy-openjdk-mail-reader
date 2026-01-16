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
        boolean openBrowser = !hasFlag(args, "--no-open");

        switch (mode) {
            case CLI -> {
                // CLI mode - use Picocli for --url, --help, --version
                int exitCode = PicocliRunner.execute(FancyMailCommand.class, args);
                System.exit(exitCode);
            }
            case SERVER -> {
                // Server mode - start Micronaut HTTP server
                System.setProperty("micronaut.server.port", String.valueOf(port));

                // Open browser after a delay
                if (openBrowser && Desktop.isDesktopSupported()) {
                    final int finalPort = port;
                    new Thread(() -> {
                        try {
                            Thread.sleep(2500);
                            System.out.println("Opening browser at http://localhost:" + finalPort);
                            Desktop.getDesktop().browse(URI.create("http://localhost:" + finalPort));
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
        // CLI mode triggers
        if (hasFlag(args, "-u", "--url") ||
            hasFlag(args, "-h", "--help") ||
            hasFlag(args, "-V", "--version")) {
            return Mode.CLI;
        }

        // Server mode if --server flag or no relevant args
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
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-p") || args[i].equals("--port")) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 8080;
    }
}
