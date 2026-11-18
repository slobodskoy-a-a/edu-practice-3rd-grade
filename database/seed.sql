-- Демонстрационные данные для показа работы системы.
-- Выполняется после database/schema.sql.

INSERT OR IGNORE INTO apiary (apiary_id, name, location) VALUES
    (1, 'Пасека Северная', 'Московская область'),
    (2, 'Пасека Луговая', 'Тверская область');

INSERT OR IGNORE INTO honey_batch
    (batch_id, apiary_id, harvest_date, variety, gross_weight_kg, moisture_percent, quality_status)
VALUES
    (1, 1, '2026-08-15', 'Липовый', 25.0, 18.2, 'accepted'),
    (2, 2, '2026-08-18', 'Гречишный', 18.0, 19.1, 'accepted'),
    (3, 1, '2026-08-20', 'Луговой', 12.0, 22.4, 'rejected');

INSERT OR IGNORE INTO packaging
    (packaging_id, batch_id, packaging_date, container_volume_g, quantity, unit_price)
VALUES
    (1, 1, '2026-08-16', 500, 40, 650.00),
    (2, 1, '2026-08-16', 250, 10, 380.00),
    (3, 2, '2026-08-19', 1000, 15, 1150.00);

INSERT OR IGNORE INTO customer_order (order_id, order_date, customer_name, status) VALUES
    (1, '2026-08-20', 'Иван Петров', 'paid'),
    (2, '2026-08-21', 'ООО Вкус мёда', 'shipped'),
    (3, '2026-08-22', 'Мария Соколова', 'cancelled');

INSERT OR IGNORE INTO sale_item
    (sale_item_id, order_id, packaging_id, quantity, sale_price)
VALUES
    (1, 1, 1, 2, 650.00),
    (2, 2, 3, 3, 1150.00),
    (3, 3, 2, 1, 380.00);

-- Ожидаемый остаток в представлении v_product_balance:
-- липовый 500 г: 38 банок; липовый 250 г: 10 банок;
-- гречишный 1000 г: 12 банок.
