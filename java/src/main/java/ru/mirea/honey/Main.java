package ru.mirea.honey;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--initialize-database".equals(args[0])) {
            DatabaseInitializer.initialize(args[1], Path.of("..", "database"));
            System.out.println("SQLite-база создана и заполнена демонстрационными данными.");
            return;
        }
        if (args.length == 2 && "--database".equals(args[0])) {
            showDatabaseBalances(args[1]);
            return;
        }
        if (args.length == 2 && "--interactive".equals(args[0])) {
            try (SqliteHoneyRepository repository = new SqliteHoneyRepository(args[1]); Scanner input = new Scanner(System.in)) {
                while (true) {
                    System.out.println("1 — остатки, 2 — фасовка, 3 — продажа, 0 — выход");
                    String choice = input.nextLine().trim();
                    if (choice.equals("0")) break;
                    try {
                        if (choice.equals("1")) {
                            for (ProductBalance b : repository.findProductBalances()) System.out.println(b);
                        } else if (choice.equals("2")) {
                            System.out.println("ID партии, масса банки (г), количество, цена — каждое значение с новой строки:");
                            repository.packageBatch(Long.parseLong(input.nextLine()), Integer.parseInt(input.nextLine()), Integer.parseInt(input.nextLine()), new BigDecimal(input.nextLine()));
                            System.out.println("Фасовка сохранена");
                        } else if (choice.equals("3")) {
                            System.out.println("ID фасовки, количество, покупатель — каждое значение с новой строки:");
                            repository.sell(Long.parseLong(input.nextLine()), Integer.parseInt(input.nextLine()), input.nextLine());
                            System.out.println("Продажа сохранена");
                        } else System.out.println("Неизвестная команда");
                    } catch (Exception e) { System.out.println("Ошибка: " + e.getMessage()); }
                }
            }
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
