#!/usr/bin/env bash

declare LOCATION
declare COMMAND
declare JAVACMD
declare LOCALCLASSPATH
declare LIBDIR

LOCATION=$(dirname $0)

if [ -z "$JAVACMD" ] ; then 
  if [ -n "$JAVA_HOME"  ] ; then
    JAVACMD=$JAVA_HOME/bin/java
  else
    JAVACMD=$(which java)
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

if [ -n "$CLASSPATH" ] ; then
  LOCALCLASSPATH=$CLASSPATH
fi

if [ -z "$IDP_BASE_URL" ] ; then
  IDP_BASE_URL="http://localhost/idp"
fi

# add in the dependency .jar files

LOCALCLASSPATH="$LOCATION/../dist/webapp/WEB-INF/lib/*":$LOCALCLASSPATH
if [ -z "$NO_PLUGIN_WEBAPP" ] ; then
  LOCALCLASSPATH="$LOCATION/../dist/plugin-webapp/WEB-INF/lib/*":$LOCALCLASSPATH
fi
LOCALCLASSPATH="$LOCATION/../edit-webapp/WEB-INF/lib/*":$LOCALCLASSPATH
LOCALCLASSPATH="$LOCATION/../dist/binlib/*":$LOCALCLASSPATH

if [ -n "$JAVA_HOME" ] ; then
  if [ -f "$JAVA_HOME/lib/tools.jar" ] ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/tools.jar
  fi

  if [ -f "$JAVA_HOME/lib/classes.zip" ] ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/classes.zip
  fi
fi

"$JAVACMD" '-classpath' "$LOCALCLASSPATH" $JAVA_OPTS -Dnet.shibboleth.idp.cli.baseURL=$IDP_BASE_URL "$@" $SHIB_OPTS
