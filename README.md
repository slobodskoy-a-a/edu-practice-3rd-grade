# Информационная система производства, фасовки и реализации мёда

Учебный проект для производственной практики по теме «Производство, фасовка и реализация мёда».

Проект показывает автоматизацию трех связанных операций:

1. учет партий мёда, полученных на пасеке;
2. фасовка партий в товарные единицы;
3. оформление продаж и контроль остатка готовой продукции.

## Структура

| Путь | Назначение |
| --- | --- |
| `database/schema.sql` | физическая модель данных SQLite |
| `database/seed.sql` | демонстрационные данные для БД |
| `docs/bpmn/honey-production-sales.bpmn` | BPMN 2.0 модель основного бизнес-процесса |
| `python/honey_accounting.py` | процедурная консольная реализация на Python |
| `java/src/main/java/ru/mirea/honey` | объектно-ориентированная реализация на Java |
| `docs/report-content.md` | материалы для основной части отчета |
| `docs/english-abstract.md` | аннотация, глоссарий и перевод на английский язык |
| `docs/test-cases.md` | тестовые сценарии |
| `TODO.md` | список завершения работы перед сдачей |

## Быстрый запуск Python-версии

Требуется Python 3.10 или новее. В папке проекта выполните:

```powershell
python python/honey_accounting.py
```

Программа создаёт файл `honey.db` рядом со скриптом. Через меню можно добавить партию, выполнить фасовку, оформить продажу и посмотреть остатки.

## Демонстрационные данные

После создания структуры БД выполните содержимое `database/seed.sql` в SQLite-клиенте. Скрипт добавит две пасеки, три партии, три фасовки и три заказа. Одна партия намеренно имеет статус `rejected`, чтобы показать запрет её фасовки; отменённый заказ не уменьшает доступный остаток.

## Запуск Java-версии

Корневой `pom.xml` подключает каталог `java` как Maven-модуль, чтобы VS Code распознавал и основной код, и `java/src/test/java`. После первого открытия проекта дождитесь импорта Maven; если редактор продолжает показывать предупреждение о classpath, выполните команду VS Code **Java: Clean Java Language Server Workspace** и дождитесь переимпорта.

Требуется JDK 17 или новее:

```powershell
cd java
javac -encoding UTF-8 -d out src/main/java/ru/mirea/honey/*.java
java -cp out ru.mirea.honey.Main
```

Java-версия демонстрирует те же правила предметной области через классы `HoneyBatch`, `Product`, `Sale` и `HoneyService`.

### Подключение Java к SQLite

В `java/pom.xml` добавлен драйвер SQLite JDBC, а `SqliteHoneyRepository` читает представление `v_product_balance`. После создания и заполнения базы данных запустите проект через Maven:

```powershell
cd java
..\tools\mvn.ps1 compile exec:java -Dexec.mainClass=ru.mirea.honey.Main -Dexec.args="--initialize-database ..\python\honey.db"
..\tools\mvn.ps1 compile exec:java -Dexec.mainClass=ru.mirea.honey.Main -Dexec.args="--database ..\python\honey.db"
```

Режим `--database` выводит остатки напрямую из SQLite. Обычный запуск без аргументов остаётся объектной демонстрацией фасовки и продажи в памяти.

Для интерактивной фасовки и продажи в той же базе из папки `java`:

```powershell
..\tools\mvn.ps1 compile exec:java -Dexec.mainClass=ru.mirea.honey.Main -Dexec.args="--interactive ..\python\honey.db"
```

При первом подключении Java устанавливает ограничения из `database/triggers.sql` в существующую базу. Фасовка и продажа записываются в SQLite; продажа сохраняется в одной транзакции.
