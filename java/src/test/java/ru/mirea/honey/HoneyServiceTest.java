package ru.mirea.honey;

import java.math.BigDecimal;

/** Самодостаточные проверочные тесты без сторонних библиотек. */
public final class HoneyServiceTest {
    public static void main(String[] args) {
        acceptsBatchByMoisture();
        rejectsPackagingForRejectedBatch();
        preventsOverPackagingAcrossProducts();
        preventsSaleBeyondStock();
        rejectsEmptyCustomerWithoutReducingStock();
        registersSaleAndReducesStock();
        System.out.println("All HoneyService tests passed.");
    }

    private static void acceptsBatchByMoisture() {
        HoneyBatch batch = batch("18.0", "25.0");
        check(batch.isAccepted(), "Партия с влажностью 18 % должна быть принята");
    }

    private static void rejectsPackagingForRejectedBatch() {
        HoneyBatch batch = batch("22.0", "10.0");
        HoneyService service = new HoneyService();
        expectIllegalArgument(() -> service.packageHoney(1, batch, 500, 1, BigDecimal.ONE));
    }

    private static void preventsOverPackagingAcrossProducts() {
        HoneyBatch batch = batch("18.0", "1.0");
        HoneyService service = new HoneyService();
        service.packageHoney(1, batch, 500, 2, BigDecimal.ONE);
        expectIllegalArgument(() -> service.packageHoney(2, batch, 500, 1, BigDecimal.ONE));
    }

    private static void preventsSaleBeyondStock() {
        HoneyService service = new HoneyService();
        Product product = service.packageHoney(1, batch("18.0", "1.0"), 500, 2, BigDecimal.ONE);
        expectIllegalArgument(() -> service.sell(1, product.id(), 3, "Покупатель"));
    }

    private static void registersSaleAndReducesStock() {
        HoneyService service = new HoneyService();
        Product product = service.packageHoney(1, batch("18.0", "1.0"), 500, 2, new BigDecimal("650"));
        Sale sale = service.sell(1, product.id(), 1, "Покупатель");
        check(product.quantity() == 1, "После продажи должна остаться одна банка");
        check(sale.total().compareTo(new BigDecimal("650")) == 0, "Сумма продажи должна быть 650");
    }

    private static void rejectsEmptyCustomerWithoutReducingStock() {
        HoneyService service = new HoneyService();
        Product product = service.packageHoney(1, batch("18.0", "1.0"), 500, 2, BigDecimal.ONE);
        expectIllegalArgument(() -> service.sell(1, product.id(), 1, " "));
        check(product.quantity() == 2, "Неуспешная продажа не должна менять остаток");
    }

    private static HoneyBatch batch(String moisture, String weight) {
        return new HoneyBatch(1, "Липовый", new BigDecimal(weight), new BigDecimal(moisture));
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Ожидалось IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Проверяемая бизнес-ошибка получена.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
