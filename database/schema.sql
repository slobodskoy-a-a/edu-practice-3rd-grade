PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS apiary (
    apiary_id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    location TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS honey_batch (
    batch_id INTEGER PRIMARY KEY,
    apiary_id INTEGER NOT NULL REFERENCES apiary(apiary_id),
    harvest_date TEXT NOT NULL,
    variety TEXT NOT NULL,
    gross_weight_kg REAL NOT NULL CHECK (gross_weight_kg > 0),
    moisture_percent REAL NOT NULL CHECK (moisture_percent >= 0 AND moisture_percent <= 30),
    quality_status TEXT NOT NULL CHECK (quality_status IN ('accepted', 'rejected')),
    UNIQUE (apiary_id, harvest_date, variety)
);

CREATE TABLE IF NOT EXISTS packaging (
    packaging_id INTEGER PRIMARY KEY,
    batch_id INTEGER NOT NULL REFERENCES honey_batch(batch_id),
    packaging_date TEXT NOT NULL,
    container_volume_g INTEGER NOT NULL CHECK (container_volume_g > 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price REAL NOT NULL CHECK (unit_price >= 0)
);

CREATE TABLE IF NOT EXISTS customer_order (
    order_id INTEGER PRIMARY KEY,
    order_date TEXT NOT NULL,
    customer_name TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('new', 'paid', 'shipped', 'cancelled'))
);

CREATE TABLE IF NOT EXISTS sale_item (
    sale_item_id INTEGER PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES customer_order(order_id),
    packaging_id INTEGER NOT NULL REFERENCES packaging(packaging_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    sale_price REAL NOT NULL CHECK (sale_price >= 0),
    UNIQUE (order_id, packaging_id)
);

CREATE VIEW IF NOT EXISTS v_product_balance AS
SELECT
    p.packaging_id,
    b.variety,
    p.container_volume_g,
    p.quantity - COALESCE(SUM(CASE WHEN o.status <> 'cancelled' THEN s.quantity ELSE 0 END), 0) AS available_quantity,
    p.unit_price
FROM packaging p
JOIN honey_batch b ON b.batch_id = p.batch_id
LEFT JOIN sale_item s ON s.packaging_id = p.packaging_id
LEFT JOIN customer_order o ON o.order_id = s.order_id
GROUP BY p.packaging_id, b.variety, p.container_volume_g, p.quantity, p.unit_price;
