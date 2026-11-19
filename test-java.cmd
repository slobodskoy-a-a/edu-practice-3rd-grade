@echo off
chcp 65001 >nul
setlocal
set "PROJECT=%~dp0"
if not exist "%PROJECT%java\out" mkdir "%PROJECT%java\out"
javac -encoding UTF-8 -d "%PROJECT%java\out" -sourcepath "%PROJECT%java\src\main\java;%PROJECT%java\src\test\java" "%PROJECT%java\src\test\java\ru\mirea\honey\HoneyServiceTest.java"
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)
java -Dfile.encoding=UTF-8 -cp "%PROJECT%java\out" ru.mirea.honey.HoneyServiceTest
exit /b %errorlevel%