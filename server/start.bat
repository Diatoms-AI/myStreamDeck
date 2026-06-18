@echo off
title myStreamDeck Server
echo.
echo  Starting myStreamDeck server...
echo  Android button URL: http://10.158.12.5:8765/button/^<id^>
echo.
node "%~dp0server.js"
pause
