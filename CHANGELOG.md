# Changelog

All notable changes to Lookout. The format follows [Keep a Changelog](https://keepachangelog.com/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- An **Enabled** switch under the status in the side rail pauses all reports at once, without changing your settings. While it is off, failed deliveries stop retrying too. Turning it back on sends the latest app, screen and playback state that changed while it was off, plus anything that had not been delivered yet, so your smart home catches up straight away.
- Reports that could not be delivered are kept, even across a restart, an update or a reboot, and retried until they arrive or a newer report of the same kind replaces them, at once when the network comes back. After an outage your smart home gets the current state, not a backlog.
- Every message has an `id`, also sent as the `webhook-id` header, that stays the same on each attempt, so a receiver can drop a repeat. A message sent a minute or more after it happened has `"delayed": true`.
- An optional **Heartbeat** on the Settings tab sends a short message every 1, 5 or 15 minutes, with the `interval` in seconds, so your smart home can tell Lookout is running and the TV is reachable. It carries nothing else: no app, screen or playback. It is off by default. A heartbeat that fails is not resent. **Deliveries** hides heartbeats until you turn them on under the new **Filter** button at the top, and keeps their last 10 apart from your other deliveries, so they never push those out. Only failed heartbeats are kept across a restart.
- Playback messages carry more of what the app shares, when it does: `album`, `album_artist`, `subtitle`, `description`, `genre`, `year`, `duration` and `position` in seconds, and an `artwork` web address. Text is cut at 200 characters. Apps that only give a display title are now shown with it as the `title`. A change of position alone is not a new message.

### Changed

- Moving around with the remote is more predictable. Left from a page always goes to the side menu's selected tab, instead of sometimes jumping to **Updates** or the **Enabled** switch. Up and Down no longer slip out of a page into the side menu. Back moves from a page to the side menu, and Back on the side menu asks you to press it again before leaving.
- A heartbeat that fails is labelled "Failed, not resent" on **Deliveries**, since Lookout does not retry it.
- **Deliveries** keeps its last 10 attempts across restarts.
- After an update installed from the **Updates** tab, Lookout opens again on that tab.
- A new webhook URL can only be saved after **Test webhook** reaches it: Save stays off until then, and a note beside the buttons says so. The form is locked while a test runs. Changing only the other settings needs no test. With the URL field empty, both buttons are off. **Send test** is now called **Test webhook**.
- The webhook URL field starts with `https://` filled in, so you only type the rest.
- Saving a webhook URL, your first or a new one, sends it the app in front, the screen state and any playback straight away, instead of waiting for the next change.
- The app's side rail shows just the name, without the old "Foreground app → webhook" tagline, which no longer described everything Lookout reports. The description in the TV's accessibility settings now lists what it reports.

### Fixed

- Pressing Down on the **Updates** tab now moves to the status card below it, instead of jumping into the page.
- Up and Down leave the webhook URL and ignored packages fields with one press, instead of first moving the cursor to the start or end of the text.
- In the delivery details, the payload box now stops above the Close button and its text scrolls inside the box, with a scroll bar, instead of the whole box scrolling and running behind the button.
- Opening another delivery's details now shows its payload from the top, instead of keeping the scroll position of the last one you looked at.

## [0.3.0] - 2026-11-14

### Added

- **Include pre-releases** on the Updates tab: tick it to be offered release candidates as well as stable releases. It is off by default.
- Optional playback messages: `playback_changed` with `package`, `state` (`playing`, `paused` or `stopped`) and, when the app gives them, `title` and `artist`. They stay off until you allow Lookout's notification access with adb, as described in the README. Automations that track the foreground app should check `trigger.json.event == 'app_changed'`.

### Fixed

- The whole page now scrolls together, so the title and description no longer end up behind the content when a tab is taller than the screen.
- The delivery details sheet has a smaller Close button, and a long payload scrolls instead of running behind it.

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
