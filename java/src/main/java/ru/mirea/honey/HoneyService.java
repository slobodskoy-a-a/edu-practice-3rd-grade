package ru.mirea.honey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HoneyService {
    private final List<Product> products = new ArrayList<>();
    private final List<Sale> sales = new ArrayList<>();
    private final Map<Long, BigDecimal> packedGramsByBatch = new HashMap<>();

    public Product packageHoney(long productId, HoneyBatch batch, int volumeGrams, int quantity, BigDecimal price) {
        if (products.stream().anyMatch(product -> product.id() == productId)) throw new IllegalArgumentException("Идентификатор товара уже занят");
        BigDecimal requestedGrams = BigDecimal.valueOf((long) volumeGrams * quantity);
        BigDecimal alreadyPacked = packedGramsByBatch.getOrDefault(batch.id(), BigDecimal.ZERO);
        if (alreadyPacked.add(requestedGrams).compareTo(batch.availableGrams()) > 0) {
            throw new IllegalArgumentException("Недостаточно мёда в партии");
        }
        Product product = new Product(productId, batch, volumeGrams, quantity, price);
        products.add(product);
        packedGramsByBatch.put(batch.id(), alreadyPacked.add(requestedGrams));
        return product;
    }

    public Sale sell(long saleId, long productId, int quantity, String customer) {
        Product product = products.stream().filter(item -> item.id() == productId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));
        Sale sale = new Sale(saleId, product, quantity, customer, LocalDate.now());
        product.reserve(quantity);
        sales.add(sale);
        return sale;
    }

    public List<Product> products() { return List.copyOf(products); }
    public List<Sale> sales() { return List.copyOf(sales); }
}
