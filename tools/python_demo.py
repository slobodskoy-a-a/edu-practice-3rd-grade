"""Запуск демонстрации Python с временной базой данных."""
from pathlib import Path
from tempfile import TemporaryDirectory
import sys

root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root / "python"))
import honey_accounting as app

with TemporaryDirectory(prefix="honey-demo-") as directory:
    app.DATABASE = Path(directory) / "honey-demo.db"
    print("Демонстрация: данные сохраняются во временную базу до выхода.")
    app.main()