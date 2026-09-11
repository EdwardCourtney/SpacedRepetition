package com.morrello.spacedrepetition.model;

public record ReviewItem(
        long id,
        String subject,
        String lesson,
        String day,
        int repeated,
        ReviewStatus status,
        String lastReviewedDay,
        String nextReviewDay
) {
}
