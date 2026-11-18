package ru.mirea.honey;

import java.math.BigDecimal;

/** Проекция представления v_product_balance из SQLite. */
public record ProductBalance(
        long packagingId,
        String variety,
        int containerVolumeGrams,
        int availableQuantity,
        BigDecimal unitPrice
) { }
