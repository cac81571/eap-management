@echo off
setlocal
cd /d "%~dp0"
if not exist "target\eap-manager.jar" (
  echo 初回起動のためビルドしています...
  call mvn -q -DskipTests package
  if errorlevel 1 exit /b 1
)
java -jar "target\eap-manager.jar" %*
