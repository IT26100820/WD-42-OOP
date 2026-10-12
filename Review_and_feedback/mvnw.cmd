@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Batch Script for Windows
@REM ----------------------------------------------------------------------------

@IF "%DEBUG%" == "" @ECHO OFF
@SETLOCAL EnableExtensions EnableDelayedExpansion

SET "DIRNAME=%~dp0"
IF "%DIRNAME%" == "" SET "DIRNAME=."
IF "%DIRNAME:~-1%" == "\" SET "DIRNAME=%DIRNAME:~0,-1%"

IF NOT "%JAVA_HOME%" == "" (
    SET "JAVACMD=%JAVA_HOME%\bin\java.exe"
) ELSE (
    SET "JAVACMD=java.exe"
)

SET "WRAPPER_JAR=%DIRNAME%\.mvn\wrapper\maven-wrapper.jar"

IF NOT EXIST "%WRAPPER_JAR%" (
    ECHO Error: %WRAPPER_JAR% not found.
    EXIT /B 1
)

"%JAVACMD%" "-Dmaven.multiModuleProjectDirectory=%DIRNAME%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
EXIT /B %ERRORLEVEL%
