@ECHO OFF
SETLOCAL
SET DIRNAME=%~dp0
IF "%DIRNAME%"=="" SET DIRNAME=.
SET APP_HOME=%DIRNAME%
SET DEFAULT_JVM_OPTS=
SET JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
IF %ERRORLEVEL% EQU 0 GOTO execute
IF NOT "%JAVA_HOME%"=="" SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
IF EXIST "%JAVA_EXE%" GOTO execute
ECHO ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
EXIT /B 1

:execute
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%~n0" -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
ENDLOCAL
