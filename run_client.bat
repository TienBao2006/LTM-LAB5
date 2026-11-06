@echo off
title RMI Client - Quan ly Sinh vien (Lab 5)
cd /d "%~dp0"
echo ==========================================================================
echo          KHOI DONG RMI CLIENT GIAO DIEN JAVA SWING
echo ==========================================================================
java "-Dfile.encoding=UTF-8" -cp "bin" client.ClientForm
pause
