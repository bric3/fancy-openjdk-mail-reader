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
import dev.brice.fancymail.service.MailService;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.runtime.Micronaut;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * CLI command for the Fancy Mail application.
 */
@Command(
        name = "fancymail",
        description = "Beautify OpenJDK mailing list emails",
        mixinStandardHelpOptions = true,
        version = "0.1.0"
)
public class FancyMailCommand implements Callable<Integer> {

    @Option(names = {"-u", "--url"}, description = "The OpenJDK mailing list URL to convert")
    private String url;

    @Option(names = {"-o", "--output"}, description = "Output file path (default: stdout)")
    private Path outputPath;

    @Option(names = {"-s", "--server"}, description = "Start in server mode")
    private boolean serverMode;

    @Option(names = {"-p", "--port"}, description = "Server port (default: 8080)", defaultValue = "8080")
    private int port;

    @Option(names = {"--open"}, description = "Open browser automatically in server mode")
    private boolean openBrowser;

    @Inject
    private MailService mailService;

    @Override
    public Integer call() throws Exception {
        if (serverMode) {
            return runServer();
        } else if (url != null && !url.isBlank()) {
            // If URL is provided without --server, default to CLI mode (markdown output)
            return convertUrl();
        } else {
            System.err.println("Error: Either --url or --server must be specified");
            System.err.println("Use --help for usage information");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  CLI mode:    fancymail --url <mail-url>");
            System.err.println("  Server mode: fancymail --server");
            System.err.println("  Server+URL:  fancymail --server --url <mail-url>");
            return 1;
        }
    }

    private Integer convertUrl() {
        try {
            System.err.println("Fetching: " + url);
            String markdown = mailService.getMailAsMarkdown(url);

            if (outputPath != null) {
                Files.writeString(outputPath, markdown);
                System.err.println("Written to: " + outputPath);
            } else {
                System.out.println(markdown);
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private Integer runServer() {
        System.err.println("Starting server on port " + port + "...");

        // Set the port via system property before Micronaut starts
        System.setProperty("micronaut.server.port", String.valueOf(port));

        // Determine the URL to open
        String browserUrl = "http://localhost:" + port;
        if (url != null && !url.isBlank()) {
            try {
                // Convert the mail URL to the rendered path
                MailPath mailPath = MailPath.fromUrl(url);
                browserUrl = "http://localhost:" + port + mailPath.toRenderedPath();
                System.err.println("Will open: " + browserUrl);
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Could not parse URL, opening home page instead: " + e.getMessage());
            }
        }

        // Open browser after a short delay
        if (openBrowser && Desktop.isDesktopSupported()) {
            final String urlToOpen = browserUrl;
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Wait for server to start
                    Desktop.getDesktop().browse(URI.create(urlToOpen));
                } catch (Exception e) {
                    System.err.println("Could not open browser: " + e.getMessage());
                }
            }).start();
        }

        // This will block and run the server
        // Note: The server is started by Micronaut's embedded server in the Application context
        System.err.println("Server running at http://localhost:" + port);
        System.err.println("Press Ctrl+C to stop");

        // Keep the main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return 0;
    }

    public static void main(String[] args) {
        // Check if server mode is requested
        boolean isServerMode = false;
        for (String arg : args) {
            if (arg.equals("-s") || arg.equals("--server")) {
                isServerMode = true;
                break;
            }
        }

        if (isServerMode) {
            // In server mode, run as a full Micronaut application
            Micronaut.run(Application.class, args);
        } else {
            // In CLI mode, use Picocli runner
            int exitCode = PicocliRunner.execute(FancyMailCommand.class, args);
            System.exit(exitCode);
        }
    }
}
