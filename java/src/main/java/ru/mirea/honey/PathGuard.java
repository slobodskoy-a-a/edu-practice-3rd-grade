package ru.mirea.honey;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/** Устанавливает ограничения при работе с существующей учебной базой. */
final class PathGuard {
    static void install(Connection connection) throws SQLException {
        Path path = Path.of("..", "database", "triggers.sql");
        try {
            String sql = Files.readString(path);
            for (String trigger : sql.split("(?i)END;\\s*(?=CREATE TRIGGER|$)")) {
                if (!trigger.isBlank()) try (var statement = connection.createStatement()) { statement.execute(trigger + "END;"); }
            }
        } catch (java.io.IOException e) { throw new SQLException("Не найден файл database/triggers.sql", e); }
    }
}
