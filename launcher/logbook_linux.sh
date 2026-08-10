#!/bin/sh

APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec "$APP_DIR/runtime/bin/java" -jar "$APP_DIR/logbook.jar" "$@"
