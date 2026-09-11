package com.morrello.spacedrepetition.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class NoteRepository {
    public String load() {
        String sql = """
                SELECT content
                FROM app_note
                WHERE id = 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString("content");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load note.", exception);
        }

        return "";
    }

    public void save(String content) {
        String sql = """
                UPDATE app_note
                SET content = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = 1
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, content);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save note.", exception);
        }
    }
}
