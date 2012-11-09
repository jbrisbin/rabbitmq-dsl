@echo off
setlocal enabledelayedexpansion

set CP=%~dp0..\conf;
rem Add all jars in "lib" to CLASSPATH
for %%j in ("%~dp0..\lib\*.jar") do set CP=!CP!%%~fj;
rem Add packaged jar
for %%j in ("%~dp0..\target\*.jar") do set CP=!CP!%%~fj;

set JAVA_OPTS=-Xmx128m %JAVA_OPTS%

rem Logging
for %%? in ("%~dp0..\log") do set LOG_DIR=%%~f?
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

java -cp "%CP%" %JAVA_OPTS% -Dlog.dir="%LOG_DIR%" com.jbrisbin.groovy.mqdsl.RabbitMQDsl %*
