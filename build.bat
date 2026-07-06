@echo off
setlocal
where py >nul 2>nul
if %errorlevel%==0 (
    py -3 tools\build.py
) else (
    python tools\build.py
)
if errorlevel 1 pause
endlocal
