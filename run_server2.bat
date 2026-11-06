@echo off
title RMI Server 2 (Port 1100) - Lab 5
cd /d "%~dp0"
echo ==========================================================================
echo          KHOI DONG RMI SERVER 2 (PORT 1100 - PARTNER 1099)
echo ==========================================================================
echo.
echo Goi y: Khi chay tren 2 may vat ly qua mang LAN:
echo Sua dong duoi: thay 127.0.0.1 bang IP that cua may ban (vd: 192.168.1.101)
echo.
java "-Dfile.encoding=UTF-8" -cp "bin" server.ServerMain 1100 1099 127.0.0.1 127.0.0.1 "Server 2"
pause
