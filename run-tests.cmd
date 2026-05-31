@echo off
chcp 65001 >nul
setlocal
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

set "PROJECT_POM=%~dp0AUCTION_SYSTEM\auction\pom.xml"
set "JACOCO_REPORT=%~dp0AUCTION_SYSTEM\auction\target\site\jacoco\index.html"

call "%~dp0mvnw.cmd" -f "%PROJECT_POM%" test %*
set "TEST_EXIT_CODE=%ERRORLEVEL%"

if not "%TEST_EXIT_CODE%"=="0" goto tests_failed

echo.
echo Tests passed.
echo JaCoCo report:
echo "%JACOCO_REPORT%"
goto done

:tests_failed
echo.
echo Tests failed with exit code %TEST_EXIT_CODE%.

:done
exit /b %TEST_EXIT_CODE%
