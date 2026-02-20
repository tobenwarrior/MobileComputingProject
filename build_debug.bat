@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
call gradlew.bat assembleDebug > build_output.txt 2>&1
echo Exit code: %ERRORLEVEL% >> build_output.txt
