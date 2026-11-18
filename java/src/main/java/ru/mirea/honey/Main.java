package ru.mirea.honey;

import java.math.BigDecimal;

public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--database".equals(args[0])) {
            showDatabaseBalances(args[1]);
            return;
        }

        HoneyBatch batch = new HoneyBatch(1, "Липовый", new BigDecimal("25.0"), new BigDecimal("18.2"));
        HoneyService service = new HoneyService();
        Product product = service.packageHoney(1, batch, 500, 40, new BigDecimal("650.00"));
        Sale sale = service.sell(1, product.id(), 2, "Иван Петров");

        System.out.println("Оформлена продажа: " + sale.total() + " руб.");
        System.out.println("Остаток " + product.description() + ": " + product.quantity() + " шт.");
    }

    private static void showDatabaseBalances(String databasePath) throws Exception {
        try (SqliteHoneyRepository repository = new SqliteHoneyRepository(databasePath)) {
            System.out.println("Остатки из SQLite:");
            for (ProductBalance balance : repository.findProductBalances()) {
                System.out.printf("%d | %s | %d г | %d шт. | %s руб.%n",
                        balance.packagingId(), balance.variety(), balance.containerVolumeGrams(),
                        balance.availableQuantity(), balance.unitPrice());
            }
        }
    }
}
