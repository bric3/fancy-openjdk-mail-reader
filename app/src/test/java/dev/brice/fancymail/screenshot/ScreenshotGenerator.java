/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.screenshot;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ColorScheme;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates screenshots of the application in light and dark modes,
 * then merges them diagonally (light on top-left, dark on bottom-right).
 */
public class ScreenshotGenerator {

    private static final int VIEWPORT_WIDTH = 1280;
    private static final int VIEWPORT_HEIGHT = 800;
    private static final Path OUTPUT_DIR = Path.of("build/screenshots");

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: ScreenshotGenerator <homeUrl> <mailUrl> [--mail-max-height=<pixels>]");
            System.err.println("Example: ScreenshotGenerator http://localhost:8080/ http://localhost:8080/rendered/... --mail-max-height=800");
            System.exit(1);
        }

        String homeUrl = args[0];
        String mailUrl = args[1];
        int mailMaxHeight = parseMaxHeight(args);

        Files.createDirectories(OUTPUT_DIR);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

            System.out.println("Generating home page screenshot...");
            generateMergedScreenshot(browser, homeUrl, "home.png", null, 0, null);

            System.out.println("Generating mail content screenshot" + (mailMaxHeight > 0 ? " (max height: " + mailMaxHeight + "px)" : "") + "...");
            generateMergedScreenshot(browser, mailUrl, "mail-content.png", null, mailMaxHeight, "Dear spec experts,");

            browser.close();
        }

        System.out.println("Screenshots saved to: " + OUTPUT_DIR.toAbsolutePath());
    }

    private static int parseMaxHeight(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--mail-max-height=")) {
                try {
                    return Integer.parseInt(arg.substring("--mail-max-height=".length()));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid max height value: " + arg);
                }
            }
        }
        return 0; // No limit
    }

    private static void generateMergedScreenshot(Browser browser, String url, String outputName, String elementSelector, int maxHeight, String scrollToText) throws IOException {
        byte[] lightScreenshot = takeScreenshot(browser, url, ColorScheme.LIGHT, elementSelector, scrollToText);
        byte[] darkScreenshot = takeScreenshot(browser, url, ColorScheme.DARK, elementSelector, scrollToText);

        BufferedImage lightImage = ImageIO.read(new ByteArrayInputStream(lightScreenshot));
        BufferedImage darkImage = ImageIO.read(new ByteArrayInputStream(darkScreenshot));

        // Clip to max height if specified
        if (maxHeight > 0) {
            lightImage = clipToMaxHeight(lightImage, maxHeight);
            darkImage = clipToMaxHeight(darkImage, maxHeight);
        }

        BufferedImage merged = mergeDiagonally(lightImage, darkImage);

        Path outputPath = OUTPUT_DIR.resolve(outputName);
        ImageIO.write(merged, "PNG", outputPath.toFile());
        System.out.println("  Saved: " + outputPath);
    }

    private static BufferedImage clipToMaxHeight(BufferedImage image, int maxHeight) {
        if (image.getHeight() <= maxHeight) {
            return image;
        }
        return image.getSubimage(0, 0, image.getWidth(), maxHeight);
    }

    private static byte[] takeScreenshot(Browser browser, String url, ColorScheme colorScheme, String elementSelector, String scrollToText) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                .setColorScheme(colorScheme));

        Page page = context.newPage();
        page.navigate(url);

        // Wait for the page to be fully loaded
        page.waitForLoadState();

        // Scroll to specific text if provided
        if (scrollToText != null) {
            var textElement = page.getByText(scrollToText).first();
            // Scroll the element to the top of the viewport
            textElement.evaluate("el => el.scrollIntoView({ block: 'start' })");
        }

        byte[] screenshot;
        if (elementSelector != null) {
            // Screenshot specific element
            var element = page.locator(elementSelector);
            element.scrollIntoViewIfNeeded();
            screenshot = element.screenshot();
        } else {
            // Full page screenshot within viewport
            screenshot = page.screenshot();
        }

        context.close();
        return screenshot;
    }

    /**
     * Merges two images diagonally - light image on top-left triangle,
     * dark image on bottom-right triangle.
     */
    private static BufferedImage mergeDiagonally(BufferedImage lightImage, BufferedImage darkImage) {
        int width = Math.max(lightImage.getWidth(), darkImage.getWidth());
        int height = Math.max(lightImage.getHeight(), darkImage.getHeight());

        BufferedImage merged = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = merged.createGraphics();

        // Draw light image fully first
        g2d.drawImage(lightImage, 0, 0, null);

        // Create diagonal polygon for top-left triangle
        Polygon topLeftTriangle = new Polygon(
                new int[]{0, width, 0},  // x points
                new int[]{0, 0, height}, // y points
                3
        );

        // Create full rectangle area
        Area fullRect = new Area(new Polygon(
                new int[]{0, width, width, 0},
                new int[]{0, 0, height, height},
                4
        ));

        // Subtract top-left triangle to get bottom-right triangle
        Area bottomRightTriangle = fullRect;
        bottomRightTriangle.subtract(new Area(topLeftTriangle));

        // Clip to bottom-right triangle and draw dark image
        g2d.setClip(bottomRightTriangle);
        g2d.drawImage(darkImage, 0, 0, null);

        g2d.dispose();
        return merged;
    }
}
