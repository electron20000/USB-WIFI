#!/usr/bin/env sh

# Gradle start up script for UN*X

DIRNAME=$(cd "$(dirname "$0")" && pwd)
APP_BASE_NAME=$(basename "$0")
GRADLE_WRAPPER_JAR="$DIRNAME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] ; then
    JAVA_HOME_BIN="$JAVA_HOME/bin"
    JAVACMD="$JAVA_HOME_BIN/java"
else
    JAVACMD="java"
fi

if [ ! -x "$JAVACMD" ] ; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
fi

CLASSPATH="$GRADLE_WRAPPER_JAR"
GRADLE_OPTS="$GRADLE_OPTS -Dorg.gradle.appname=$APP_BASE_NAME"

exec "$JAVACMD" $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
