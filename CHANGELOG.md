# Changelog

All notable changes to Lookout. The format follows [Keep a Changelog](https://keepachangelog.com/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Optional playback messages: `playback_changed` with `package`, `state` (`playing`, `paused` or `stopped`) and, when the app gives them, `title` and `artist`. They stay off until you allow Lookout's notification access with adb, as described in the README. Automations that track the foreground app should check `trigger.json.event == 'app_changed'`.

## [0.2.0] - 2026-11-14

### Added

- Each message now carries `event` (`app_changed`) and, when known, `activity`, the window that brought the app to the front.
- New messages for the TV itself: `screen_on`, `screen_off`, `screensaver_started` and `screensaver_stopped`. The screen state is also sent when the service starts. `screen_off` is best effort on TVs that lose their network in standby. These messages have no `package`: an automation that reads `trigger.json.package` should first check `{{ trigger.json.package is defined }}`.
- **Send this TV's name** option adds `device` to each message so one webhook can serve several TVs. It is off by default.

### Fixed

- Changing a setting no longer reports the app already in front as a new app change.
- The screensaver is no longer reported as the app in front. App changes wait until it ends, and the app that was showing before it is not reported again.

## [0.1.0] - 2026-11-14

Initial release.
