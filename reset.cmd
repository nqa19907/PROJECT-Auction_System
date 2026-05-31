@echo off
chcp 65001 >nul
setlocal

set "DATA_DIR=%~dp0AUCTION_SYSTEM\auction\data"
set "IMAGE_DIR=%DATA_DIR%\item-images"

if /I "%~1"=="--help" goto :usage_success
if /I "%~1"=="-h" goto :usage_success

if not exist "%DATA_DIR%\" (
    echo Data directory does not exist: %DATA_DIR%
    exit /b 0
)

if "%~1"=="" (
    call :delete_pattern "%DATA_DIR%\*.ser"
    call :delete_pattern "%DATA_DIR%\*.bak"
    echo Deleted all serialized data, including users, in: %DATA_DIR%
    exit /b 0
)

if /I "%~1"=="--with-images" (
    call :delete_pattern "%DATA_DIR%\*.ser"
    call :delete_pattern "%DATA_DIR%\*.bak"
    if exist "%IMAGE_DIR%\" (
        rmdir /s /q "%IMAGE_DIR%"
    )
    echo Deleted all serialized data and item images in: %DATA_DIR%
    exit /b 0
)

goto :usage_error

:delete_pattern
del /q "%~1" 2>nul
exit /b 0

:usage_success
call :usage
exit /b 0

:usage_error
call :usage
exit /b 1

:usage
echo Usage: reset.cmd [--with-images]
echo.
echo Deletes data files in: %DATA_DIR%
echo   default       delete all .ser and .bak files, including users
echo   --with-images delete all .ser/.bak files and data\item-images
exit /b 0
