"""Процедурная консольная система учета производства, фасовки и продаж мёда."""

from __future__ import annotations

import sqlite3
from datetime import date
from pathlib import Path

DATABASE = Path(__file__).with_name("honey.db")
SCHEMA = Path(__file__).parents[1] / "database" / "schema.sql"


def connect() -> sqlite3.Connection:
    connection = sqlite3.connect(DATABASE)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA foreign_keys = ON")
    return connection


def initialize_database(connection: sqlite3.Connection) -> None:
    connection.executescript(SCHEMA.read_text(encoding="utf-8"))
    connection.execute(
        "INSERT OR IGNORE INTO apiary(apiary_id, name, location) VALUES (1, ?, ?)",
        ("Пасека Северная", "Московская область"),
    )
    connection.commit()


def add_batch(connection: sqlite3.Connection, variety: str, weight: float, moisture: float) -> None:
    if not variety.strip() or weight <= 0 or not 0 <= moisture <= 30:
        raise ValueError("Проверьте сорт, массу и влажность партии.")
    status = "accepted" if moisture <= 20 else "rejected"
    connection.execute(
        """INSERT INTO honey_batch(apiary_id, harvest_date, variety, gross_weight_kg,
           moisture_percent, quality_status) VALUES (1, ?, ?, ?, ?, ?)""",
        (date.today().isoformat(), variety.strip(), weight, moisture, status),
    )
    connection.commit()


def package_batch(connection: sqlite3.Connection, batch_id: int, volume_g: int, quantity: int, price: float) -> None:
    batch = connection.execute(
        "SELECT gross_weight_kg, quality_status FROM honey_batch WHERE batch_id = ?", (batch_id,)
    ).fetchone()
    if batch is None:
        raise ValueError("Партия не найдена.")
    if batch["quality_status"] != "accepted":
        raise ValueError("Нельзя фасовать партию, не прошедшую контроль качества.")
    if volume_g <= 0 or quantity <= 0 or price < 0:
        raise ValueError("Объем, количество и цена должны быть корректными.")
    packed_grams = connection.execute(
        "SELECT COALESCE(SUM(container_volume_g * quantity), 0) FROM packaging WHERE batch_id = ?", (batch_id,)
    ).fetchone()[0]
    if packed_grams + volume_g * quantity > batch["gross_weight_kg"] * 1000:
        raise ValueError("Для фасовки недостаточно мёда в партии.")
    connection.execute(
        """INSERT INTO packaging(batch_id, packaging_date, container_volume_g, quantity, unit_price)
           VALUES (?, ?, ?, ?, ?)""",
        (batch_id, date.today().isoformat(), volume_g, quantity, price),
    )
    connection.commit()


def register_sale(connection: sqlite3.Connection, packaging_id: int, quantity: int, customer: str) -> None:
    product = connection.execute(
        "SELECT available_quantity, unit_price FROM v_product_balance WHERE packaging_id = ?", (packaging_id,)
    ).fetchone()
    if product is None or product["available_quantity"] < quantity or quantity <= 0:
        raise ValueError("Недостаточно товара на складе.")
    with connection:
        cursor = connection.execute(
            "INSERT INTO customer_order(order_date, customer_name, status) VALUES (?, ?, 'paid')",
            (date.today().isoformat(), customer.strip() or "Розничный покупатель"),
        )
        connection.execute(
            "INSERT INTO sale_item(order_id, packaging_id, quantity, sale_price) VALUES (?, ?, ?, ?)",
            (cursor.lastrowid, packaging_id, quantity, product["unit_price"]),
        )


def show_balance(connection: sqlite3.Connection) -> None:
    rows = connection.execute(
        "SELECT packaging_id, variety, container_volume_g, available_quantity, unit_price FROM v_product_balance"
    ).fetchall()
    if not rows:
        print("На складе пока нет фасованной продукции.")
        return
    print("\nID | Сорт | Объем, г | Остаток, шт. | Цена, руб.")
    for row in rows:
        print(f"{row['packaging_id']:>2} | {row['variety']} | {row['container_volume_g']:>8} | {row['available_quantity']:>12} | {row['unit_price']:.2f}")


def show_batches(connection: sqlite3.Connection) -> None:
    rows = connection.execute(
        "SELECT batch_id, harvest_date, variety, gross_weight_kg, moisture_percent, quality_status FROM honey_batch"
    ).fetchall()
    for row in rows:
        print(dict(row))


def main() -> None:
    connection = connect()
    initialize_database(connection)
    actions = {
        "1": lambda: add_batch(connection, input("Сорт: "), float(input("Масса, кг: ")), float(input("Влажность, %: "))),
        "2": lambda: package_batch(connection, int(input("ID партии: ")), int(input("Объем банки, г: ")), int(input("Количество: ")), float(input("Цена, руб.: "))),
        "3": lambda: register_sale(connection, int(input("ID фасовки: ")), int(input("Количество: ")), input("Покупатель: ")),
        "4": lambda: show_batches(connection),
        "5": lambda: show_balance(connection),
    }
    try:
        while True:
            print("\n1 - партия; 2 - фасовка; 3 - продажа; 4 - партии; 5 - остатки; 0 - выход")
            choice = input("> ").strip()
            if choice == "0":
                return
            try:
                actions[choice]()
            except KeyError:
                print("Выберите пункт меню от 0 до 5.")
            except ValueError as error:
                print(f"Ошибка: {error}")
    finally:
        connection.close()


if __name__ == "__main__":
    main()
