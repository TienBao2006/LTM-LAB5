@echo off
title Bien dich Du an Java RMI - Lab 5
echo ==========================================================================
echo          BIEN DICH HE THONG JAVA RMI REPLICATION (LAB 5)
echo ==========================================================================

cd /d "%~dp0"
if not exist "bin" mkdir "bin"

echo Dang bien dich ma nguon Java vao thu muc bin/...
javac -encoding UTF-8 -d "bin" src/common/*.java src/server/*.java src/client/*.java src/test/*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Bien dich thanh cong 100%!
    echo Cac tep .class da san sang trong thu muc bin/
) else (
    echo.
    echo [ERROR] Co loi trong qua trinh bien dich.
)

echo ==========================================================================
pause
