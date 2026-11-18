"""Проверка операций и ограничений SQLite на изолированной базе в памяти."""
import sqlite3
import unittest

import honey_accounting as app


class IntegrationTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.row_factory = sqlite3.Row
        self.db.execute("PRAGMA foreign_keys=ON")
        app.initialize_database(self.db)
        app.add_batch(self.db, "Липовый", 1, 18)

    def tearDown(self):
        self.db.close()

    def test_sale_and_rejected_sale_keep_balance(self):
        app.package_batch(self.db, 1, 500, 2, 100)
        app.register_sale(self.db, 1, 1, "Покупатель")
        with self.assertRaises(ValueError):
            app.register_sale(self.db, 1, 2, "Покупатель")
        self.assertEqual(self.db.execute("SELECT available_quantity FROM v_product_balance").fetchone()[0], 1)

    def test_direct_insert_and_update_cannot_exceed_stock_or_weight(self):
        app.package_batch(self.db, 1, 500, 2, 100)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE packaging SET quantity=3 WHERE packaging_id=1")
        app.register_sale(self.db, 1, 1, "Покупатель")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE sale_item SET quantity=3 WHERE sale_item_id=1")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE honey_batch SET gross_weight_kg=0.5 WHERE batch_id=1")

    def test_rejected_batch_cannot_be_packaged_directly(self):
        app.add_batch(self.db, "Луговой", 1, 22)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO packaging(batch_id, packaging_date, container_volume_g, quantity, unit_price) VALUES(2,'2026-01-01',500,1,100)")


if __name__ == "__main__":
    unittest.main()
