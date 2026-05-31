@echo off
chcp 65001 >nul
setlocal
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

set "AUCTION_SERVER_HOST=%~1"
if not defined AUCTION_SERVER_HOST set "AUCTION_SERVER_HOST=127.0.0.1"

set "AUCTION_SERVER_PORT=%~2"
if not defined AUCTION_SERVER_PORT set "AUCTION_SERVER_PORT=8080"

call "%~dp0mvnw.cmd" -f "%~dp0AUCTION_SYSTEM\auction\pom.xml" javafx:run
