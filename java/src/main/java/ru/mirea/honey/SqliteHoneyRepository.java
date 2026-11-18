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
