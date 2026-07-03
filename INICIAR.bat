@echo off
chcp 65001 >nul
title Gestao de Adegas - ACV Vinhos de Talha
echo ============================================================
echo   GESTAO DE ADEGAS - ACV Vinhos de Talha
echo ============================================================
echo.
echo   CONFIRME que o MySQL do XAMPP esta INICIADO
echo   (Painel do XAMPP - botao Start no MySQL).
echo.
echo   A aplicacao vai abrir em:  http://localhost:8080
echo   Utilizador: admin      Palavra-passe: admin123
echo.
echo   Para PARAR: feche esta janela ou carregue Ctrl+C.
echo ============================================================
echo.

rem Usa o Java embutido na pasta do projeto (nao precisa de instalar Java).
set "JAVA_EXE=%~dp0java\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"

"%JAVA_EXE%" -jar "%~dp0target\gestao-adegas-0.1.0.jar"

echo.
echo A aplicacao terminou.
pause
