package ru.mirea.honey;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Слой доступа к SQLite для чтения текущих товарных остатков. */
public final class SqliteHoneyRepository implements AutoCloseable {
    private final Connection connection;

    public SqliteHoneyRepository(String databasePath) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (java.sql.Statement s = connection.createStatement()) { s.execute("PRAGMA foreign_keys=ON"); }
        PathGuard.install(connection);
    }

    public void packageBatch(long batchId, int grams, int quantity, BigDecimal price) throws SQLException {
        if (grams <= 0 || quantity <= 0 || price.signum() < 0) throw new IllegalArgumentException("Неверные параметры фасовки");
        try (PreparedStatement s = connection.prepareStatement("INSERT INTO packaging(batch_id, packaging_date, container_volume_g, quantity, unit_price) VALUES (?,date('now','localtime'),?,?,?)")) {
            s.setLong(1, batchId); s.setInt(2, grams); s.setInt(3, quantity); s.setBigDecimal(4, price); s.executeUpdate();
        }
    }

    public void sell(long packagingId, int quantity, String customer) throws SQLException {
        if (quantity <= 0 || customer == null || customer.isBlank()) throw new IllegalArgumentException("Укажите покупателя и количество");
        connection.setAutoCommit(false);
        try {
            long orderId;
            try (PreparedStatement s = connection.prepareStatement("INSERT INTO customer_order(order_date, customer_name, status) VALUES(date('now','localtime'), ?, 'paid')", java.sql.Statement.RETURN_GENERATED_KEYS)) {
                s.setString(1, customer); s.executeUpdate();
                try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Нет идентификатора заказа"); orderId = keys.getLong(1); }
            }
            try (PreparedStatement s = connection.prepareStatement("INSERT INTO sale_item(order_id, packaging_id, quantity, sale_price) SELECT ?, packaging_id, ?, unit_price FROM packaging WHERE packaging_id=?")) {
                s.setLong(1, orderId); s.setInt(2, quantity); s.setLong(3, packagingId);
                if (s.executeUpdate() != 1) throw new SQLException("Товар не найден");
            }
            connection.commit();
        } catch (SQLException | RuntimeException e) { connection.rollback(); throw e; }
        finally { connection.setAutoCommit(true); }
    }

    public List<ProductBalance> findProductBalances() throws SQLException {
        String sql = """
                SELECT packaging_id, variety, container_volume_g, available_quantity, unit_price
                FROM v_product_balance
                ORDER BY packaging_id
                """;
        List<ProductBalance> balances = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                balances.add(new ProductBalance(
                        result.getLong("packaging_id"),
                        result.getString("variety"),
                        result.getInt("container_volume_g"),
                        result.getInt("available_quantity"),
                        result.getBigDecimal("unit_price")
                ));
            }
        }
        return balances;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
