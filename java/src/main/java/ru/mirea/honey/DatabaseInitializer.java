package ru.mirea.honey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Создаёт SQLite-базу и загружает демонстрационные данные из SQL-скриптов. */
public final class DatabaseInitializer {
    private DatabaseInitializer() { }

    public static void initialize(String databasePath, Path scriptsDirectory) throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            runScript(connection, Files.readString(scriptsDirectory.resolve("schema.sql")));
            runScript(connection, Files.readString(scriptsDirectory.resolve("seed.sql")));
        }
    }

    private static void runScript(Connection connection, String script) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String command : script.split(";")) {
                if (!command.trim().isEmpty()) statement.execute(command);
            }
        }
    }
}
