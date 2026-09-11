package com.morrello.spacedrepetition.database;

import com.morrello.spacedrepetition.model.ReviewItem;
import com.morrello.spacedrepetition.model.ReviewStatus;
import com.morrello.spacedrepetition.util.DateCalculator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ReviewItemRepository {
    public long create(String subject, String lesson, String day) {
        String sql = """
                INSERT INTO review_items (subject, lesson, day, status, next_review_day)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, subject);
            statement.setString(2, lesson);
            statement.setString(3, day);
            statement.setString(4, ReviewStatus.TODAY.name());
            statement.setString(5, day);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create review item.", exception);
        }

        throw new IllegalStateException("SQLite did not return a generated review item ID.");
    }

    public void markRepeated(long id) {
        ReviewItem item = findById(id);
        int repeated = item.repeated() + 1;
        String today = DateCalculator.today();
        String nextReviewDay = DateCalculator.nextReviewDay(repeated);
        String sql = """
                UPDATE review_items
                SET repeated = ?,
                    status = ?,
                    last_reviewed_day = ?,
                    next_review_day = ?
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, repeated);
            statement.setString(2, ReviewStatus.COMPLETED.name());
            statement.setString(3, today);
            statement.setString(4, nextReviewDay);
            statement.setLong(5, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark review item as repeated.", exception);
        }
    }

    public void undoCompletion(long id) {
        ReviewItem item = findById(id);
        int repeated = Math.max(0, item.repeated() - 1);
        String sql = """
                UPDATE review_items
                SET repeated = ?,
                    status = ?,
                    last_reviewed_day = NULL,
                    next_review_day = ?
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, repeated);
            statement.setString(2, ReviewStatus.TODAY.name());
            statement.setString(3, DateCalculator.today());
            statement.setLong(4, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to undo review item completion.", exception);
        }
    }

    public void delayOneDay(long id) {
        String sql = """
                UPDATE review_items
                SET status = ?,
                    last_reviewed_day = NULL,
                    next_review_day = ?
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ReviewStatus.WAITING.name());
            statement.setString(2, DateCalculator.tomorrow());
            statement.setLong(3, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delay review item.", exception);
        }
    }

    public void delete(long id) {
        String sql = """
                DELETE FROM review_items
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete review item.", exception);
        }
    }

    public boolean existsBySubjectAndLesson(String subject, String lesson) {
        String sql = """
                SELECT 1
                FROM review_items
                WHERE lower(trim(subject)) = lower(trim(?))
                  AND lower(trim(lesson)) = lower(trim(?))
                LIMIT 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, subject);
            statement.setString(2, lesson);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check duplicate review item.", exception);
        }
    }

    public void refreshDueItems() {
        String delayExpiredSql = """
                UPDATE review_items
                SET status = ?,
                    next_review_day = ?
                WHERE next_review_day < ?
                """;
        String dueTodaySql = """
                UPDATE review_items
                SET status = ?
                WHERE next_review_day = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement delayExpiredStatement = connection.prepareStatement(delayExpiredSql);
             PreparedStatement dueTodayStatement = connection.prepareStatement(dueTodaySql)) {
            String today = DateCalculator.today();

            delayExpiredStatement.setString(1, ReviewStatus.TODAY.name());
            delayExpiredStatement.setString(2, today);
            delayExpiredStatement.setString(3, today);
            delayExpiredStatement.executeUpdate();

            dueTodayStatement.setString(1, ReviewStatus.TODAY.name());
            dueTodayStatement.setString(2, today);
            dueTodayStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to refresh due review items.", exception);
        }
    }

    public List<ReviewItem> findAll() {
        String sql = """
                SELECT id, subject, lesson, day, repeated, status, last_reviewed_day, next_review_day
                FROM review_items
                ORDER BY next_review_day ASC, id DESC
                """;

        List<ReviewItem> items = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(mapReviewItem(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load review items.", exception);
        }

        return items;
    }

    public ReviewItem findById(long id) {
        String sql = """
                SELECT id, subject, lesson, day, repeated, status, last_reviewed_day, next_review_day
                FROM review_items
                WHERE id = ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapReviewItem(resultSet);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load review item.", exception);
        }

        throw new IllegalArgumentException("Review item does not exist: " + id);
    }

    public List<ReviewItem> findTodayWork() {
        return findByStatus("""
                SELECT id, subject, lesson, day, repeated, status, last_reviewed_day, next_review_day
                FROM review_items
                WHERE status = ?
                ORDER BY id DESC
                """, ReviewStatus.TODAY.name());
    }

    public List<ReviewItem> findCompletedToday() {
        String sql = """
                SELECT id, subject, lesson, day, repeated, status, last_reviewed_day, next_review_day
                FROM review_items
                WHERE status = ?
                  AND last_reviewed_day = ?
                ORDER BY id DESC
                """;

        List<ReviewItem> items = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ReviewStatus.COMPLETED.name());
            statement.setString(2, DateCalculator.today());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapReviewItem(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load completed review items.", exception);
        }

        return items;
    }

    private List<ReviewItem> findByStatus(String sql, String status) {
        List<ReviewItem> items = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapReviewItem(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load review items by status.", exception);
        }

        return items;
    }

    private ReviewItem mapReviewItem(ResultSet resultSet) throws SQLException {
        return new ReviewItem(
                resultSet.getLong("id"),
                resultSet.getString("subject"),
                resultSet.getString("lesson"),
                resultSet.getString("day"),
                resultSet.getInt("repeated"),
                ReviewStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("last_reviewed_day"),
                resultSet.getString("next_review_day")
        );
    }
}
