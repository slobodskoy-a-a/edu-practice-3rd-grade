package ru.mirea.honey;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Самодостаточные проверки с таблицей результатов. */
public final class HoneyServiceTest {
    private record Case(String input, String expected, Scenario scenario) { }
    @FunctionalInterface private interface Scenario { String run(); }

    public static void main(String[] args) {
        List<Case> cases = List.of(
            new Case("Влажность партии: 18 %", "Партия принята", () ->
                batch("18.0", "25.0").isAccepted() ? "Партия принята" : "Партия отклонена"),
            new Case("Влажность: 22 %; фасовать 1 банку", "Партия не прошла контроль качества", () -> {
                HoneyService service = new HoneyService();
                return errorMessage(() -> service.packageHoney(1, batch("22.0", "10.0"), 500, 1, BigDecimal.ONE));
            }),
            new Case("1 кг; фасовать 2 банки по 500 г, затем ещё 1", "Недостаточно мёда в партии", () -> {
                HoneyBatch batch = batch("18.0", "1.0");
                HoneyService service = new HoneyService();
                service.packageHoney(1, batch, 500, 2, BigDecimal.ONE);
                return errorMessage(() -> service.packageHoney(2, batch, 500, 1, BigDecimal.ONE));
            }),
            new Case("Остаток: 2 банки; продать 3", "Недостаточный остаток товара; остаток: 2", () -> {
                HoneyService service = new HoneyService();
                Product product = service.packageHoney(1, batch("18.0", "1.0"), 500, 2, BigDecimal.ONE);
                String result = errorMessage(() -> service.sell(1, product.id(), 3, "Покупатель"));
                return result + "; остаток: " + product.quantity();
            }),
            new Case("Остаток: 2 банки; покупатель не указан", "Количество и покупатель обязательны; остаток: 2", () -> {
                HoneyService service = new HoneyService();
                Product product = service.packageHoney(1, batch("18.0", "1.0"), 500, 2, BigDecimal.ONE);
                String result = errorMessage(() -> service.sell(1, product.id(), 1, " "));
                return result + "; остаток: " + product.quantity();
            }),
            new Case("2 банки по 650 руб.; продать 1", "Сумма: 650 руб.; остаток: 1", () -> {
                HoneyService service = new HoneyService();
                Product product = service.packageHoney(1, batch("18.0", "1.0"), 500, 2, new BigDecimal("650"));
                Sale sale = service.sell(1, product.id(), 1, "Покупатель");
                return "Сумма: " + sale.total().stripTrailingZeros().toPlainString()
                    + " руб.; остаток: " + product.quantity();
            })
        );

        int failed = 0;
        printBorder();
        printRow("Входные данные", "Ожидаемый результат", "Фактический результат", "Статус");
        printBorder();
        for (Case test : cases) {
            String actual;
            String status;
            try {
                actual = test.scenario().run();
                status = test.expected().equals(actual) ? "OK" : "FAIL";
            } catch (Throwable error) {
                actual = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
                status = "ERROR";
            }
            if (!status.equals("OK")) failed++;
            printRow(test.input(), test.expected(), actual, status);
            printBorder();
        }
        System.out.printf("Всего: %d; успешно: %d; ошибок: %d%n", cases.size(), cases.size() - failed, failed);
        if (failed > 0) throw new AssertionError("Не все проверки пройдены: " + failed);
        System.out.println("All HoneyService tests passed.");
    }

    private static HoneyBatch batch(String moisture, String weight) {
        return new HoneyBatch(1, "Липовый", new BigDecimal(weight), new BigDecimal(moisture));
    }

    private static String errorMessage(Runnable action) {
        try {
            action.run();
            return "Операция выполнена без ошибки";
        } catch (IllegalArgumentException error) {
            return error.getMessage();
        }
    }

    private static final int[] WIDTHS = {22, 22, 22, 6};

    private static void printBorder() {
        StringBuilder line = new StringBuilder("+");
        for (int width : WIDTHS) line.append("-".repeat(width + 2)).append('+');
        System.out.println(line);
    }

    private static void printRow(String... values) {
        List<List<String>> columns = new ArrayList<>();
        int lines = 0;
        for (int i = 0; i < values.length; i++) {
            List<String> wrapped = wrap(values[i], WIDTHS[i]);
            columns.add(wrapped);
            lines = Math.max(lines, wrapped.size());
        }
        for (int line = 0; line < lines; line++) {
            StringBuilder row = new StringBuilder("|");
            for (int i = 0; i < WIDTHS.length; i++) {
                String part = line < columns.get(i).size() ? columns.get(i).get(line) : "";
                row.append(' ').append(String.format("%-" + WIDTHS[i] + "s", part)).append(" |");
            }
            System.out.println(row);
        }
    }

    private static List<String> wrap(String value, int width) {
        List<String> lines = new ArrayList<>();
        String remaining = value;
        while (remaining.length() > width) {
            int split = remaining.lastIndexOf(' ', width);
            if (split <= 0) split = width;
            lines.add(remaining.substring(0, split));
            remaining = remaining.substring(split).stripLeading();
        }
        lines.add(remaining);
        return lines;
    }
}