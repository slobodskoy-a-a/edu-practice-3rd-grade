"""Проверка операций и ограничений SQLite на изолированной базе в памяти."""
import sqlite3
import sys
import textwrap
import unittest

import honey_accounting as app


class IntegrationTest(unittest.TestCase):
    def setUp(self):
        self.actual = 'Результат не получен'
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
        balance = self.db.execute("SELECT available_quantity FROM v_product_balance").fetchone()[0]
        self.actual = f'Повторная продажа отклонена; остаток: {balance} банка'
        self.assertEqual(balance, 1)

    def test_direct_insert_and_update_cannot_exceed_stock_or_weight(self):
        app.package_batch(self.db, 1, 500, 2, 100)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE packaging SET quantity=3 WHERE packaging_id=1")
        app.register_sale(self.db, 1, 1, "Покупатель")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE sale_item SET quantity=3 WHERE sale_item_id=1")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE honey_batch SET gross_weight_kg=0.5 WHERE batch_id=1")
        self.actual = 'Все три изменения отклонены БД'

    def test_rejected_batch_cannot_be_packaged_directly(self):
        app.add_batch(self.db, "Луговой", 1, 22)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT INTO packaging(batch_id, packaging_date, container_volume_g, quantity, unit_price) VALUES(2,'2026-01-01',500,1,100)")
        self.actual = 'Запись отклонена БД'

    def test_packaging_exact_weight_then_reject_excess(self):
        app.package_batch(self.db, 1, 500, 2, 100)
        with self.assertRaises(ValueError):
            app.package_batch(self.db, 1, 500, 1, 100)
        count = self.db.execute("SELECT COUNT(*) FROM packaging").fetchone()[0]
        self.actual = f'Лишняя фасовка отклонена; записей: {count}'
        self.assertEqual(count, 1)

    def test_insufficient_sale_does_not_create_order(self):
        app.package_batch(self.db, 1, 500, 2, 100)
        with self.assertRaises(ValueError):
            app.register_sale(self.db, 1, 3, "Покупатель")
        orders = self.db.execute("SELECT COUNT(*) FROM customer_order").fetchone()[0]
        balance = self.db.execute("SELECT available_quantity FROM v_product_balance").fetchone()[0]
        self.actual = f'Продажа отклонена; заказов: {orders}; остаток: {balance}'
        self.assertEqual(orders, 0)
        self.assertEqual(balance, 2)

    def test_invalid_batch_and_packaging_values(self):
        for variety, weight, moisture in [('', 1, 18), ('Гречишный', -1, 18), ('Гречишный', 1, 31)]:
            with self.subTest(variety=variety, weight=weight, moisture=moisture):
                with self.assertRaises(ValueError):
                    app.add_batch(self.db, variety, weight, moisture)
        for grams, quantity, price in [(0, 1, 100), (500, 0, 100), (500, 1, -1)]:
            with self.subTest(grams=grams, quantity=quantity, price=price):
                with self.assertRaises(ValueError):
                    app.package_batch(self.db, 1, grams, quantity, price)
        self.actual = 'Все 6 некорректных вариантов отклонены'

    def test_cancelled_order_returns_stock(self):
        app.package_batch(self.db, 1, 500, 2, 100)
        app.register_sale(self.db, 1, 1, "Покупатель")
        self.db.execute("UPDATE customer_order SET status='cancelled' WHERE order_id=1")
        balance = self.db.execute("SELECT available_quantity FROM v_product_balance").fetchone()[0]
        self.actual = f'Остаток после отмены: {balance} банки'
        self.assertEqual(balance, 2)


CASES = {
    'test_sale_and_rejected_sale_keep_balance': ('2 банки; продать 1, затем 2', 'Повторная продажа отклонена; остаток: 1 банка'),
    'test_direct_insert_and_update_cannot_exceed_stock_or_weight': ('Изменить фасовку, продажу и массу сверх допустимого', 'Все три изменения отклонены БД'),
    'test_rejected_batch_cannot_be_packaged_directly': ('Партия отклонена; создать фасовку через SQL', 'Запись отклонена БД'),
    'test_packaging_exact_weight_then_reject_excess': ('1 кг; фасовать 2 банки по 500 г, затем ещё 1', 'Лишняя фасовка отклонена; записей: 1'),
    'test_insufficient_sale_does_not_create_order': ('Остаток 2 банки; продать 3', 'Продажа отклонена; заказов: 0; остаток: 2'),
    'test_invalid_batch_and_packaging_values': ('6 вариантов некорректных данных партии и фасовки', 'Все 6 некорректных вариантов отклонены'),
    'test_cancelled_order_returns_stock': ('2 банки; продать 1 и отменить заказ', 'Остаток после отмены: 2 банки'),
}


class TableResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.rows = []

    def _record(self, test, status, detail=None):
        inputs, expected = CASES.get(test._testMethodName, (test._testMethodName, 'Успешное выполнение'))
        actual = test.actual if detail is None else f'{test.actual}; ошибка: {detail}'
        self.rows.append((inputs, expected, actual, status))

    def addSuccess(self, test):
        super().addSuccess(test)
        self._record(test, 'OK')

    def addFailure(self, test, err):
        super().addFailure(test, err)
        self._record(test, 'FAIL', str(err[1]))

    def addError(self, test, err):
        super().addError(test, err)
        self._record(test, 'ERROR', str(err[1]))

    def addSubTest(self, test, subtest, err):
        super().addSubTest(test, subtest, err)
        if err is not None:
            self._record(test, 'FAIL', str(err[1]))


def print_table(rows):
    widths = (20, 20, 20, 6)
    border = '+' + '+'.join('-' * (width + 2) for width in widths) + '+'
    print('\n' + border)
    for values in [('Входные данные', 'Ожидаемый результат', 'Фактический результат', 'Статус')] + rows:
        columns = [textwrap.wrap(str(value), width=width, break_long_words=False, break_on_hyphens=False) or ['']
                   for value, width in zip(values, widths)]
        for i in range(max(map(len, columns))):
            print('| ' + ' | '.join((column[i] if i < len(column) else '').ljust(width)
                                    for column, width in zip(columns, widths)) + ' |')
        print(border)


if __name__ == "__main__":
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(IntegrationTest)
    result = unittest.TextTestRunner(verbosity=0, resultclass=TableResult).run(suite)
    print_table(result.rows)
    sys.exit(0 if result.wasSuccessful() else 1)
