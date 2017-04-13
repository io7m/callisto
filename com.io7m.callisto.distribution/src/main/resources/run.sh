#!/bin/sh

exec java \
  -Dlogback.configurationFile=etc/logback.xml \
  -jar lib-boot/com.io7m.callisto.container-0.0.1.jar
