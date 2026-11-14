#!/bin/sh
# Installs the APK and enables the accessibility service, keeping any services already enabled.
# Select a device with ANDROID_SERIAL=<host:port> if more than one is connected.
set -eu

SERVICE="dev.aman.lookout/dev.aman.lookout.LookoutService"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"

adb install -r "$APK"

current=$(adb shell settings get secure enabled_accessibility_services | tr -d '\r')
case "$current" in
  *"$SERVICE"*) ;;
  null|"") adb shell settings put secure enabled_accessibility_services "$SERVICE" ;;
  *) adb shell settings put secure enabled_accessibility_services "$current:$SERVICE" ;;
esac
adb shell settings put secure accessibility_enabled 1
echo "Lookout enabled."
