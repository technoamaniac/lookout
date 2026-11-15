# <img src="docs/logo.svg" alt="" width="40" height="40" align="top"> Lookout

Lets your smart home know what your Android TV is doing: which app is open, whether the screen is on, and what's playing.

Your TV remote API can tell Home Assistant the TV is on. It usually can't tell it that you just opened SmartTube, paused an episode or let the screensaver kick in, and sideloaded apps are invisible to it. Lookout runs on the TV itself, so it sees every app, and sends each change to your webhook the moment it happens.

## What you can do with it

- Dim the lights when playback starts and bring them back up when you pause.
- Switch the soundbar to its movie mode for Plex or Netflix, and to music for Spotify.
- Pause the kitchen speaker when the TV wakes up.
- Turn the receiver off when the screensaver starts, instead of waiting for a timer.
- Get a notification when YouTube opens after bedtime.
- Show what's on the TV on your dashboard, with the title when the app shares it.

## Why Lookout

- **Works with any app**, including sideloaded ones. There is nothing to set up per app.
- **Private by design.** It sees which app is in front, never what is on screen. Playback reporting is off until you switch it on.
- **Instant, not polled.** Each change is pushed as it happens and retried if the network blips.
- **Any receiver.** Home Assistant, Node-RED, n8n or your own HTTPS server: it is a plain JSON POST.
- **Keeps itself current.** It updates from this repository's releases after checking each download's SHA-256.
- **Free and open source** under the Apache 2.0 license.

## Install

Requires Android 9+ and `adb` access to the TV (wireless debugging is enough). Download the APK from [Releases](../../releases/latest), then:

```sh
adb connect <tv-ip>:<port>
./scripts/install.sh lookout-<version>.apk
```

The script installs the APK and enables the service, keeping other enabled services. It is safe to re-run, for example after a factory reset.

Open **Lookout** on the TV, enter your webhook URL, press **Test webhook**, then **Save**. The URL must be HTTPS, and Save stays off until a test event reaches it. **Ignored packages** optionally lists apps never to report, such as a launcher, separated by commas. To stop reports for a while, turn off **Send reports** under the status in the side rail; it applies at once and keeps your settings.

To avoid typing with the remote, send either field from your computer, then press **Save** on the TV, after **Test webhook** for a new URL:

```sh
adb shell am start --activity-clear-top -n dev.aman.lookout/.SettingsActivity \
  --es url "https://example.com/hooks/lookout" \
  --es ignored "com.spocky.projengmenu,com.google.android.apps.tv.dreamx"
```

On the TV, **Deliveries** shows the last 10 attempts, kept across restarts; press OK on one to see the exact payload. **Updates** installs the latest stable release (tick **Include pre-releases** to be offered release candidates too). Android only accepts an update signed with the same key as the installed app, and Lookout contacts GitHub only while that screen is open.

## Home Assistant example

```yaml
automation:
  - alias: TV foreground app
    triggers:
      - trigger: webhook
        webhook_id: !secret lookout_webhook_id
        local_only: false
    conditions:
      - condition: template
        value_template: "{{ trigger.json.event == 'app_changed' }}"
    actions:
      - action: input_text.set_value
        target: { entity_id: input_text.tv_foreground_app }
        data: { value: "{{ trigger.json.package }}" }
```

The condition matters: screen and playback messages arrive on the same webhook.

## What it sends

An accessibility service listens for window changes only. It never reads window content, so it sees which app is in front, not what is on screen. System UI, keyboards and Lookout itself are ignored. Each app change is sent as:

```json
POST <webhook url>
Content-Type: application/json

{
  "event": "app_changed",
  "package": "org.smarttube.stable",
  "activity": "org.smarttube.stable.MainActivity",
  "device": "Living room",
  "timestamp": 1731600000
}
```

`event` names the kind of message. `activity` is the window Android reported when the app came to the front, often its main activity but sometimes a dialog or splash window; later screens inside the app do not update it. `device` is the TV's name from its system settings, so one webhook can serve several TVs; it is only sent when **Send this TV's name** is ticked. Both are left out when unknown. `timestamp` is Unix seconds.

Lookout also reports the TV itself, with the same `event`, `device` and `timestamp` fields: `screen_on`, `screen_off`, `screensaver_started` and `screensaver_stopped`. The screen state is also sent when the service starts. Besides window changes, Lookout only watches these screen and screensaver changes, and playback if you allow it below; it reads nothing else about the TV. While the screensaver runs, app changes are not reported, so the app you were using stays as the current one. `screen_off` is best effort: a TV that loses its network as it goes to standby may not manage to send it.

### Playback (optional)

Lookout can also report what an app is playing: `{"event": "playback_changed", "package": "org.jellyfin.androidtv", "state": "playing", "title": "Song", "artist": "Band", "timestamp": 1731600000}`. `state` is `playing`, `paused` or `stopped`; `title` and `artist` are left out when the app does not give them, and `device` works as above. It is off until you allow it once from your computer:

```sh
adb shell cmd notification allow_listener dev.aman.lookout/dev.aman.lookout.playback.MediaAccessService
```

This gives Lookout Android's notification access, which is the only way an app may read media sessions. Lookout never reads notifications; the service it registers for this does nothing else.

Only apps that mark their media session as active are seen. SmartTube does so unless **Settings → Player → Developer options → Disable playback notifications** is ticked.

Any 2xx counts as delivered. Network errors, 408, 429 and 5xx are retried, at once when the network comes back and otherwise with waits that grow to 15 minutes, until the message is delivered or a newer one of its kind replaces it; other responses are not retried. Messages waiting to be sent survive a restart, an update or a reboot. Redirects are not followed, so use the final URL.

Every message carries an `id`, also sent as the `webhook-id` header, that stays the same on each attempt, so a receiver can drop a repeat. A message sent a minute or more after it happened also has `"delayed": true`, so automations can skip stale changes.

## Limitations

- Playback is only reported for apps with an active media session, and only after the one-time grant above. Jellyfin's default video player has none yet, so its episodes and movies are not reported.
- After a restart, Lookout reports the next app change. Android only names the app in front when the service may read window content, which Lookout deliberately does not.
- In-app updates need the "install unknown apps" permission; some TVs have no screen to grant it, so use `scripts/install.sh` there.

## Development

Needs JDK 17 and the Android SDK (platform 37).

```sh
./gradlew test assembleDebug lintDebug
```

Logic lives in plain-Java packages with JVM unit tests; Android classes stay thin.

| Package      | Contents                                                                            |
| ------------ | ----------------------------------------------------------------------------------- |
| `event`      | `Event` and the JSON builder                                                        |
| `foreground` | `ForegroundTracker` turns window events into app changes; `AppEvent`                |
| `device`     | `DeviceEvent` and `DeviceStateReceiver`: screen and screensaver changes             |
| `playback`   | `PlaybackTracker` and `PlaybackEvent`; `MediaSessionWatcher` reads the sessions     |
| `delivery`   | `Outbox`, `OutboxSender` and `WebhookSink`; `DeliveryLog`                           |
| `config`     | `Config`, validated settings; `SaveGate`; `PrefsConfigStore` persists them          |
| `update`     | `GitHubReleases`, `UpdateChecker`, `ApkFetcher`, `UpdateInstaller`                  |
| `ui`         | One panel class per tab                                                             |
| (root)       | `LookoutService` and `SettingsActivity`; their names are stored on the TV           |

To send events somewhere other than a webhook, implement `EventSink`.

**Releases.** Pushing a tag like `v1.2.3` builds, signs and publishes the APK with its SHA-256 (`v1.2.3-rc.1` makes a pre-release, which the app only offers when **Include pre-releases** is ticked). The release notes come from the matching `CHANGELOG.md` section, and the release fails without one. The version code is `major*10000 + minor*100 + patch`. Gradle builds without the signing key; a separate job signs with `apksigner` using the `release` environment's secrets.

**Pins.** Actions, the Gradle distribution and every dependency are pinned (by SHA, `app/gradle.lockfile` and `gradle/verification-metadata.xml`). After changing a dependency:

```sh
./gradlew :app:dependencies --write-locks
./gradlew --write-verification-metadata sha256 test assembleDebug assembleRelease
```

After an AGP upgrade, also add the Linux and Windows `aapt2` hashes by hand from Google Maven's `.sha256` files; Gradle only fetches the current machine's.

Issues and pull requests are welcome; new logic should come with a test. Lookout is developed with [Claude Code](https://claude.com/claude-code) under the maintainer's review; the conventions are in [CLAUDE.md](CLAUDE.md).

## License

Apache License 2.0. See [LICENSE](LICENSE). Copyright 2026 Aman Singh.
