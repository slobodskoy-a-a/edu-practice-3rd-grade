# Учёт производства, фасовки и продажи мёда

Учебный проект демонстрирует регистрацию партий мёда, контроль допуска к фасовке, учёт фасованной продукции и оформление продаж. Доступный остаток рассчитывается с учётом неотменённых заказов.

В проекте представлены процедурное консольное приложение на Python, объектно-ориентированная реализация на Java и база данных SQLite. Описание предметной области, модели, алгоритмы и результаты тестирования приведены в отчёте:

- [Отчёт в Word](Отчет_Учебная_практика_дополненный.docx)
- [Отчёт в PDF](Отчет_Учебная_практика_дополненный.pdf)

## Структура проекта

| Путь | Содержимое |
| --- | --- |
| `python/honey_accounting.py` | Консольное приложение на Python |
| `python/test_integration.py` | Семь сценариев проверки Python и SQLite |
| `java/src/main/java/ru/mirea/honey/` | Классы Java-приложения |
| `java/src/test/java/ru/mirea/honey/HoneyServiceTest.java` | Шесть проверок объектной модели Java |
| `database/schema.sql` | Таблицы и представление остатков SQLite |
| `database/triggers.sql` | Ограничения операций с партиями, фасовками и продажами |
| `database/seed.sql` | Демонстрационные данные |
| `tools/python_demo.py` | Запуск Python с временной базой для демонстрации |
| `tools/mvn.ps1` | Запуск локального Maven, если он установлен в `.tools` |
| `test-java.cmd` | Компиляция и запуск Java-тестов с табличным выводом |

## Python

Требуется Python 3.9 или новее. Из корня проекта:

```powershell
py -3.9 python/honey_accounting.py
```

При первом запуске создаётся `python/honey.db`. Через меню доступны добавление партии, фасовка, продажа, просмотр партий и остатков. Для работы с той же базой через DBeaver выберите подключение SQLite и укажите путь к этому файлу.

Демонстрация с временной базой, которая удаляется после выхода:

```powershell
py -3.9 tools/python_demo.py
```

Запуск тестов:

```powershell
cd python
py -3.9 test_integration.py
```

Тесты используют независимую базу в памяти и выводят таблицу входных данных и результатов.

## Java

Требуется JDK 16 или новее. Для демонстрации объектов в PowerShell из корня проекта:

```powershell
cd java
javac -encoding UTF-8 -d out -sourcepath src/main/java src/main/java/ru/mirea/honey/Main.java
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new()
java "-Dfile.encoding=UTF-8" -cp out ru.mirea.honey.Main
```

Обычный запуск показывает фасовку и продажу в памяти; данные этого примера не записываются в базу. Для запуска проверок из папки `java`:

```powershell
..\test-java.cmd
```

## Java и SQLite

Для работы с SQLite нужен Maven и доступ к зависимости SQLite JDBC, указанной в `java/pom.xml`. Выполняйте команды из папки `java`:

```powershell
# Создать схему и загрузить примеры, если таблица партий пуста
mvn compile exec:java "-Dexec.mainClass=ru.mirea.honey.Main" "-Dexec.args=--initialize-database ../python/honey.db"

# Показать остатки
mvn compile exec:java "-Dexec.mainClass=ru.mirea.honey.Main" "-Dexec.args=--database ../python/honey.db"

# Открыть интерактивное меню фасовки и продажи
mvn compile exec:java "-Dexec.mainClass=ru.mirea.honey.Main" "-Dexec.args=--interactive ../python/honey.db"
```

При работе в Windows с локальным Maven из `.tools` вместо `mvn` можно использовать `powershell -NoProfile -ExecutionPolicy Bypass -File ..\tools\mvn.ps1`. Каталог `.tools` в Git не входит; на другом компьютере потребуется установить Maven или подготовить локальную копию.

Если русский текст в терминале отображается некорректно, перед запуском Maven выполните:

```powershell
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new()
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
```

Инициализация создаёт схему и добавляет демонстрационные данные только при отсутствии партий. Учебная проверка влажности не заменяет лабораторный контроль качества мёда.