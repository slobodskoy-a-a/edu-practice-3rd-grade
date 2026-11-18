package ru.mirea.honey;

import java.math.BigDecimal;

public final class Product extends InventoryItem implements Reservable {
    private final HoneyBatch batch;
    private final int volumeGrams;
    private final BigDecimal unitPrice;
    private int quantity;

    public Product(long id, HoneyBatch batch, int volumeGrams, int quantity, BigDecimal unitPrice) {
        super(id);
        if (!batch.isAccepted()) throw new IllegalArgumentException("Партия не прошла контроль качества");
        if (volumeGrams <= 0 || quantity <= 0 || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Некорректные параметры фасовки");
        }
        this.batch = batch;
        this.volumeGrams = volumeGrams;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    @Override
    public void reserve(int requestedQuantity) {
        if (requestedQuantity <= 0 || requestedQuantity > quantity) {
            throw new IllegalArgumentException("Недостаточный остаток товара");
        }
        quantity -= requestedQuantity;
    }

    @Override
    public int quantity() { return quantity; }
    public BigDecimal unitPrice() { return unitPrice; }
    public String description() { return batch.variety() + ", " + volumeGrams + " г"; }
}
