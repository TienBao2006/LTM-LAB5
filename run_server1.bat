@echo off
title RMI Server 1 (Port 1099) - Lab 5
cd /d "%~dp0"
echo ==========================================================================
echo          KHOI DONG RMI SERVER 1 (PORT 1099 - PARTNER 1100)
echo ==========================================================================
echo.
echo Goi y: Khi chay tren 2 may vat ly qua mang LAN:
echo Sua dong duoi: thay 127.0.0.1 bang IP that cua may ban (vd: 192.168.1.100)
echo.
java "-Dfile.encoding=UTF-8" -cp "bin" server.ServerMain 1099 1100 127.0.0.1 127.0.0.1 "Server 1"
pause
