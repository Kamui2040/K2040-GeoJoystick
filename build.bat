@echo off
setlocal
where py >nul 2>nul
if %errorlevel%==0 (
    py -3 tools\build.py %*
) else (
    python tools\build.py %*
)
set "exit_code=%errorlevel%"
endlocal & exit /b %exit_code%
