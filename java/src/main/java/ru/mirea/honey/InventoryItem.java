package ru.mirea.honey;

/** Базовый тип позиции, которая имеет идентификатор и доступное количество. */
public abstract class InventoryItem {
    private final long id;

    protected InventoryItem(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    public abstract int quantity();
}
