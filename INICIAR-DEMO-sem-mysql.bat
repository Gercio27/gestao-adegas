@echo off
chcp 65001 >nul
title Gestao de Adegas - MODO DEMONSTRACAO (sem base de dados)
echo ============================================================
echo   MODO DEMONSTRACAO - dados apagados ao fechar
echo   (nao precisa do XAMPP/MySQL)
echo.
echo   Abrir em:  http://localhost:8080
echo   Utilizador: admin   Palavra-passe: admin123
echo ============================================================
echo.

set "JAVA_EXE=%~dp0java\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"

"%JAVA_EXE%" -jar "%~dp0target\gestao-adegas-0.1.0.jar" --spring.profiles.active=dev
pause
