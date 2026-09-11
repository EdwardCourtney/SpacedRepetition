package com.morrello.spacedrepetition.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DateCalculator {
    private static final DateTimeFormatter STORAGE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateCalculator() {
    }

    public static String today() {
        return LocalDate.now().format(STORAGE_FORMAT);
    }

    public static String plusDays(int days) {
        return LocalDate.now().plusDays(days).format(STORAGE_FORMAT);
    }

    public static String tomorrow() {
        return plusDays(1);
    }

    public static String nextReviewDay(int repeated) {
        return plusDays(daysUntilNextReview(repeated));
    }

    public static boolean isDue(String day) {
        LocalDate date = LocalDate.parse(day, STORAGE_FORMAT);
        return !date.isAfter(LocalDate.now());
    }

    private static int daysUntilNextReview(int repeated) {
        if (repeated <= 0) {
            return 0;
        }

        return switch (repeated) {
            case 1 -> 3;
            case 2 -> 7;
            case 3 -> 15;
            default -> 15 * (repeated - 2);
        };
    }
}
