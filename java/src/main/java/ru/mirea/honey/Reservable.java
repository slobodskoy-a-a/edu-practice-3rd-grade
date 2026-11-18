package ru.mirea.honey;

/** Контракт товарной позиции, которую можно резервировать для продажи. */
public interface Reservable {
    void reserve(int requestedQuantity);
}
