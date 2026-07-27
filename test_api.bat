@echo off
setlocal enabledelayedexpansion

set BASE=http://localhost:3000/api

echo ================================================
echo PHASE 1: Admin Login
echo ================================================
curl.exe -s -X POST "%BASE%/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"admin@gmail.com\",\"password\":\"admin123\"}"
echo.

echo ================================================
echo PHASE 1b: Leader Login
echo ================================================
curl.exe -s -X POST "%BASE%/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"leader@manga.com\",\"password\":\"password123\"}"
echo.

echo ================================================
echo PHASE 1c: Tantou Login
echo ================================================
curl.exe -s -X POST "%BASE%/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"tantou@manga.com\",\"password\":\"password123\"}"
echo.

echo ================================================
echo PHASE 1d: Board Login
echo ================================================
curl.exe -s -X POST "%BASE%/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"board1@manga.com\",\"password\":\"password123\"}"
echo.

echo ================================================
echo PHASE 1e: Mangaka Login
echo ================================================
curl.exe -s -X POST "%BASE%/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"mangaka@manga.com\",\"password\":\"password123\"}"
echo.

echo ================================================
echo PHASE 1f: Assistant Login
echo ================================================
curl.exe -s -X POST "%BASE%/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"assistant1@manga.com\",\"password\":\"password123\"}"
echo.

echo ================================================
echo DONE
echo ================================================
