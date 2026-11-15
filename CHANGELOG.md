# Changelog

All notable changes to Lookout. The format follows [Keep a Changelog](https://keepachangelog.com/),
and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Each message now carries `event` (`app_changed`) and, when known, `activity`, the window that
  brought the app to the front.
- **Send this TV's name** option adds `device` to each message so one webhook can serve several
  TVs. It is off by default.

### Fixed

- Changing a setting no longer reports the app already in front as a new app change.
