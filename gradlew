#!/bin/sh

APP_HOME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)
APP_NAME="Gradle"
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

if [ ! -f "$JAR" ]; then
  echo "Gradle wrapper jar not found: $JAR" >&2
  exit 1
fi

exec "$JAVACMD" $JAVA_OPTS $GRADLE_OPTS \
  -Dorg.gradle.appname="$APP_NAME" \
  -classpath "$JAR" \
  org.gradle.wrapper.GradleWrapperMain "$@"
