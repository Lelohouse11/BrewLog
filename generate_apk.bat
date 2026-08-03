@echo off
echo ========================================
echo   BrewLog - Generating New APK
echo ========================================
echo.

call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   SUCCESS!
    echo   Your new APK is ready at:
    echo   app\build\outputs\apk\debug\app-debug.apk
    echo ========================================
) else (
    echo.
    echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    echo   BUILD FAILED. Please check errors above.
    echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
)

pause
