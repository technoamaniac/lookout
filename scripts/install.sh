#!/bin/sh
# Installs the APK and enables the accessibility service, keeping any services already enabled.
# Select a device with ANDROID_SERIAL=<host:port> if more than one is connected.
set -eu

SERVICE="dev.aman.lookout/dev.aman.lookout.LookoutService"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"

if [ ! -f "$APK" ]; then
  echo "APK not found: $APK (build it with ./gradlew assembleDebug)" >&2
  exit 1
fi

adb install -r "$APK"

# adb shell re-parses its arguments in the device shell, so single-quote the value: other
# services' class names may contain '$' (inner classes), which would otherwise be expanded
enable() {
  adb shell settings put secure enabled_accessibility_services "'$1'"
}

current=$(adb shell settings get secure enabled_accessibility_services | tr -d '\r')
case ":$current:" in
  *":$SERVICE:"*) ;;
  ":null:" | "::") enable "$SERVICE" ;;
  *) enable "$current:$SERVICE" ;;
esac
adb shell settings put secure accessibility_enabled 1
echo "Lookout enabled."
