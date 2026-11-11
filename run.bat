@echo off
rem compiles everything into the "out" folder and starts the app
rem usage:  run.bat            (shows login window)
rem         run.bat sachin     (skips login, logs in as sachin)

cd /d "%~dp0"

set SQLITE=lib\sqlite-jdbc-3.54.1.0.jar
set FX=lib\javafx

if not exist out mkdir out

javac -d out --module-path %FX% --add-modules javafx.controls -cp %SQLITE% ^
  src\*.java src\model\*.java src\exception\*.java src\util\*.java ^
  src\service\*.java src\db\*.java src\ui\*.java
if errorlevel 1 exit /b 1

java --module-path %FX% --add-modules javafx.controls ^
  --enable-native-access=javafx.graphics,ALL-UNNAMED ^
  -cp "out;%SQLITE%" Main %*
