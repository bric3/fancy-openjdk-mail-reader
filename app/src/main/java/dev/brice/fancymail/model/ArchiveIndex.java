/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.model;

import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

/**
 * Represents the archive index for a mailing list, containing all available months.
 *
 * @param list            the mailing list name
 * @param availableMonths set of available year-months in OpenJDK format (e.g., "2026-January")
 */
public record ArchiveIndex(
        String list,
        Set<String> availableMonths
) {
    private static final DateTimeFormatter YEAR_MONTH_PARSER = DateTimeFormatter.ofPattern("yyyy-MMMM", Locale.ENGLISH);

    /**
     * Checks if a given year-month is available in the archive.
     *
     * @param yearMonth the year-month in OpenJDK format (e.g., "2026-January")
     * @return true if the month is available
     */
    public boolean isAvailable(String yearMonth) {
        return availableMonths.contains(yearMonth);
    }

    /**
     * Checks if a given year and month are available.
     *
     * @param year  the year
     * @param month the month (1-12)
     * @return true if the month is available
     */
    public boolean isAvailable(int year, int month) {
        return availableMonths.contains(formatYearMonth(year, month));
    }

    /**
     * Returns the minimum year in the archive.
     */
    public int minYear() {
        return availableMonths.stream()
                .map(this::parseYear)
                .min(Comparator.naturalOrder())
                .orElse(YearMonth.now().getYear());
    }

    /**
     * Returns the maximum year in the archive.
     */
    public int maxYear() {
        return availableMonths.stream()
                .map(this::parseYear)
                .max(Comparator.naturalOrder())
                .orElse(YearMonth.now().getYear());
    }

    /**
     * Returns all years that have at least one available month.
     */
    public List<Integer> years() {
        return IntStream.rangeClosed(minYear(), maxYear())
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /**
     * Formats a year and month to OpenJDK format (e.g., "2026-January").
     */
    public static String formatYearMonth(int year, int month) {
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return year + "-" + monthName;
    }

    /**
     * Returns the short month name for display (e.g., "Jan", "Feb").
     */
    public static String shortMonthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private int parseYear(String yearMonth) {
        return YearMonth.parse(yearMonth, YEAR_MONTH_PARSER).getYear();
    }

    /**
     * Creates an ArchiveIndex from a collection of year-month strings.
     */
    public static ArchiveIndex of(String list, java.util.Collection<String> months) {
        return new ArchiveIndex(list, new TreeSet<>(months));
    }
}
