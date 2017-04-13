#!/bin/sh

exec java \
  -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=y \
  -Dlogback.configurationFile=etc/logback.xml \
  -jar lib-boot/com.io7m.callisto.container-0.0.1.jar
