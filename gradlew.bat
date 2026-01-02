@echo off
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set GRADLE_WRAPPER_JAR=%DIRNAME%\gradle\wrapper\gradle-wrapper.jar

if not defined JAVA_HOME goto findJavaFromPath
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" goto init

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
exit /b 1

:findJavaFromPath
set JAVA_EXE=java.exe
for %%i in (%JAVA_EXE%) do set JAVA_EXE=%%~$PATH:i
if not defined JAVA_EXE (
    echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
    exit /b 1
)

:init
set CLASSPATH=%GRADLE_WRAPPER_JAR%
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.appname=%APP_BASE_NAME%
"%JAVA_EXE%" %GRADLE_OPTS% -classpath %CLASSPATH% org.gradle.wrapper.GradleWrapperMain %*
