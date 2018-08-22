@echo off
set OUT_DIR=bin

if exist %OUT_DIR% rd /s /q %OUT_DIR%

call mvn clean package -Dmaven.test.skip=true

rem copy 
xcopy /e /q target\lib %OUT_DIR%\lib\
xcopy /e /q target\classes %OUT_DIR%\

pause