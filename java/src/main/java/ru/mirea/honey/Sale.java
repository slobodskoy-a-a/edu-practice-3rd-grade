package ru.mirea.honey;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Sale(long id, Product product, int quantity, String customer, LocalDate date) {
    public Sale {
        if (quantity <= 0 || customer == null || customer.isBlank()) {
            throw new IllegalArgumentException("Количество и покупатель обязательны");
        }
    }
    public BigDecimal total() { return product.unitPrice().multiply(BigDecimal.valueOf(quantity)); }
}
