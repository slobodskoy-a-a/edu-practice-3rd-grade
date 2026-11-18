package ru.mirea.honey;

import java.math.BigDecimal;

public final class HoneyBatch {
    private final long id;
    private final String variety;
    private final BigDecimal weightKg;
    private final BigDecimal moisturePercent;

    public HoneyBatch(long id, String variety, BigDecimal weightKg, BigDecimal moisturePercent) {
        if (variety == null || variety.isBlank() || weightKg == null || weightKg.signum() <= 0) {
            throw new IllegalArgumentException("Сорт и масса партии обязательны");
        }
        if (moisturePercent == null || moisturePercent.signum() < 0 || moisturePercent.compareTo(new BigDecimal("30")) > 0) {
            throw new IllegalArgumentException("Влажность должна быть от 0 до 30 процентов");
        }
        this.id = id;
        this.variety = variety;
        this.weightKg = weightKg;
        this.moisturePercent = moisturePercent;
    }

    public boolean isAccepted() {
        return moisturePercent.compareTo(new BigDecimal("20")) <= 0;
    }

    public long id() { return id; }
    public String variety() { return variety; }
    public BigDecimal availableGrams() { return weightKg.multiply(new BigDecimal("1000")); }
}
