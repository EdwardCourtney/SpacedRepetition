package com.morrello.spacedrepetition.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

public final class Database {
    private static final String DATABASE_FILE_NAME = "spaced_repetition.db";
    private static final Path LEGACY_DATABASE_PATH = Path.of("data", DATABASE_FILE_NAME);
    private static final Path DATABASE_DIRECTORY = appDataDirectory().resolve("data");
    private static final Path DATABASE_PATH = DATABASE_DIRECTORY.resolve(DATABASE_FILE_NAME);
    private static final Path BACKUP_DIRECTORY = appDataDirectory().resolve("backups");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_DIRECTORY.resolve("spaced_repetition.db");
    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int MAX_BACKUPS = 10;

    private Database() {
    }

    public static void initialize() {
        try {
            Files.createDirectories(DATABASE_DIRECTORY);
            migrateLegacyDatabase();

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS review_items (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            subject TEXT NOT NULL,
                            lesson TEXT NOT NULL,
                            day TEXT NOT NULL,
                            repeated INTEGER NOT NULL DEFAULT 0,
                            status TEXT NOT NULL DEFAULT 'TODAY',
                            last_reviewed_day TEXT,
                            next_review_day TEXT NOT NULL DEFAULT ''
                        )
                        """);
                addColumnIfMissing(statement, "review_items", "status", "TEXT NOT NULL DEFAULT 'TODAY'");
                addColumnIfMissing(statement, "review_items", "last_reviewed_day", "TEXT");
                addColumnIfMissing(statement, "review_items", "next_review_day", "TEXT NOT NULL DEFAULT ''");
                statement.execute("""
                        UPDATE review_items
                        SET next_review_day = day
                        WHERE next_review_day = ''
                        """);
                addUniqueReviewItemIndexIfPossible(statement);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS app_note (
                            id INTEGER PRIMARY KEY CHECK (id = 1),
                            content TEXT NOT NULL DEFAULT '',
                            updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                statement.execute("""
                        INSERT OR IGNORE INTO app_note (id, content)
                        VALUES (1, '')
                        """);
            }
            backupDatabase();
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Failed to initialize SQLite database.", exception);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    public static Path getDatabasePath() {
        return DATABASE_PATH;
    }

    private static void addColumnIfMissing(Statement statement, String table, String column, String definition)
            throws SQLException {
        try {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException exception) {
            if (!exception.getMessage().contains("duplicate column name")) {
                throw exception;
            }
        }
    }

    private static void addUniqueReviewItemIndexIfPossible(Statement statement) throws SQLException {
        try {
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_review_items_subject_lesson_unique
                    ON review_items (lower(trim(subject)), lower(trim(lesson)))
                    """);
        } catch (SQLException exception) {
            if (!exception.getMessage().contains("UNIQUE constraint failed")) {
                throw exception;
            }
        }
    }

    private static Path appDataDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "SpacedRepetition");
        }

        return Path.of(System.getProperty("user.home"), ".spaced-repetition");
    }

    private static void migrateLegacyDatabase() throws IOException {
        if (Files.exists(LEGACY_DATABASE_PATH) && Files.notExists(DATABASE_PATH)) {
            Files.copy(LEGACY_DATABASE_PATH, DATABASE_PATH);
        }
    }

    private static void backupDatabase() throws IOException {
        if (Files.notExists(DATABASE_PATH)) {
            return;
        }

        Files.createDirectories(BACKUP_DIRECTORY);
        String backupFileName = "spaced_repetition_" + LocalDateTime.now().format(BACKUP_TIMESTAMP) + ".db";
        Files.copy(DATABASE_PATH, BACKUP_DIRECTORY.resolve(backupFileName), StandardCopyOption.REPLACE_EXISTING);
        pruneOldBackups();
    }

    private static void pruneOldBackups() throws IOException {
        try (Stream<Path> backups = Files.list(BACKUP_DIRECTORY)) {
            Path[] sortedBackups = backups
                    .filter(path -> path.getFileName().toString().endsWith(".db"))
                    .sorted(Comparator.comparing(Database::lastModifiedTime).reversed())
                    .toArray(Path[]::new);

            for (int index = MAX_BACKUPS; index < sortedBackups.length; index++) {
                Files.deleteIfExists(sortedBackups[index]);
            }
        }
    }

    private static long lastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect backup file.", exception);
        }
    }
}
