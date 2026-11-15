# Changelog

All notable changes to Lookout. The format follows [Keep a Changelog](https://keepachangelog.com/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Each message now carries `event` (`app_changed`) and, when known, `activity`, the window that brought the app to the front.
- New messages for the TV itself: `screen_on`, `screen_off`, `screensaver_started` and `screensaver_stopped`. The screen state is also sent when the service starts. `screen_off` is best effort on TVs that lose their network in standby. These messages have no `package`: an automation that reads `trigger.json.package` should first check `{{ trigger.json.package is defined }}`.
- **Send this TV's name** option adds `device` to each message so one webhook can serve several TVs. It is off by default.

### Fixed

- Changing a setting no longer reports the app already in front as a new app change.

## [0.1.0] - 2026-11-14

Initial release.
