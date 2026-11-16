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

You need:

- An Android TV with Android 9 or newer.
- A computer with `adb` on the same network. Turn on **wireless debugging** on the TV first.
- A webhook address that starts with `https://`, for example from Home Assistant.

### 1. Put Lookout on the TV

1. Download the `.apk` file from [Releases](../../releases/latest).
2. On your computer, connect to the TV. Use the address and port the TV shows under wireless debugging:

   ```sh
   adb connect <tv-ip>:<port>
   ```

3. Install it:

   ```sh
   ./scripts/install.sh lookout-<version>.apk
   ```

That is all. It is safe to run again, for example after a factory reset.

### 2. Tell Lookout where to send things

Open **Lookout** on the TV, then:

1. Type your webhook address.
2. Press **Test webhook**. Wait until it says "Test event delivered". If it says something else, check the address.
3. Press **Save**.

Done. Lookout now sends a message whenever something changes.

Typing with the remote is slow. To fill the address in from your computer instead, run this, then press **Test webhook** and **Save** on the TV:

```sh
adb shell am start --activity-clear-top -n dev.aman.lookout/.SettingsActivity \
  --es url "https://example.com/hooks/lookout"
```

### Things you can do on the TV

- **Enabled** (under the status on the left) turns all reports off and on. Your settings stay.
- **Ignored packages** is a list of apps never to report, such as a launcher, separated by commas. For example: `com.spocky.projengmenu,com.google.android.apps.tv.dreamx`. You can also send it from your computer with `--es ignored "..."` in the command above.
- **Deliveries** shows the last 10 messages and whether they arrived. Press OK on one to see exactly what was sent. **Filter** at the top also shows heartbeats.
- **Updates** installs the newest version. Tick **Include pre-releases** to get test versions too. Lookout only talks to GitHub while this tab is open.

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

- `event` names the kind of message.
- `activity` is the window Android reported when the app came to the front, often its main activity but sometimes a dialog or splash window. Later screens inside the app do not update it. It is left out when unknown.
- `device` is the TV's name from its system settings, so one webhook can serve several TVs. It is only sent when **Send this TV's name** is ticked.
- `timestamp` is Unix seconds.

Lookout also reports the TV itself, with the same `event`, `device` and `timestamp` fields:

- `screen_on` and `screen_off`. The screen state is also sent when the service starts.
- `screensaver_started` and `screensaver_stopped`. While the screensaver runs, app changes are not reported, so the app you were using stays as the current one.

Besides window changes, Lookout only watches these screen and screensaver changes, and playback if you allow it below. It reads nothing else about the TV.

### Playback (optional)

Lookout can also report what an app is playing:

```json
{
  "event": "playback_changed",
  "package": "org.smarttube.stable",
  "state": "playing",
  "title": "Video title",
  "artist": "Channel",
  "album": "Playlist",
  "duration": 215,
  "position": 42,
  "artwork": "https://example.com/cover.jpg",
  "timestamp": 1731600000
}
```

- `state` is `playing`, `paused` or `stopped`.
- Only `event`, `package`, `state` and `timestamp` are always there. The rest is left out when the app does not give it: `title`, `artist`, `album`, `album_artist`, `subtitle`, `description`, `genre`, `year`, `duration`, `position` and `artwork`.
- `duration` and `position` are in seconds. `position` is how far in the app was when the message was sent. It is not sent again as it moves, only with a change of state or of what is playing.
- `artwork` is a web address. Text is cut at 200 characters.
- `device` works as above.

It is off until you allow it once from your computer:

```sh
adb shell cmd notification allow_listener dev.aman.lookout/dev.aman.lookout.playback.MediaAccessService
```

This gives Lookout Android's notification access, which is the only way an app may read media sessions. Lookout never reads notifications; the service it registers for this does nothing else.

Only apps that mark their media session as active are seen. SmartTube does so unless **Settings → Player → Developer options → Disable playback notifications** is ticked.

### Delivery

```json
{
  "event": "app_changed",
  "package": "org.smarttube.stable",
  "timestamp": 1731600000,
  "id": "c0ffee00-1234-4abc-8def-0123456789ab",
  "delayed": true
}
```

- Any 2xx counts as delivered. Network errors, 408, 429 and 5xx are retried; other responses are not.
- Retries happen at once when the network comes back, otherwise after waits that grow to 15 minutes. They stop when the message is delivered or a newer one of its kind replaces it.
- Waiting messages survive a restart, an update or a reboot.
- Redirects are not followed, so use the final URL.
- `id` is also sent as the `webhook-id` header and stays the same on each attempt, so a receiver can drop a repeat.
- `delayed` is `true` on a message sent a minute or more after it happened, so automations can skip stale changes. It is left out otherwise.

### Heartbeat (optional)

Pick an interval under **Heartbeat** on the Settings tab and Lookout also sends a short message regularly, so your smart home can tell Lookout is running and the TV is reachable:

```json
{
  "event": "heartbeat",
  "interval": 300,
  "timestamp": 1731600000
}
```

- It carries nothing about the TV's state: use the events above for that.
- `interval` is the seconds until the next one, so a receiver can treat the TV as gone once a few are overdue.
- A heartbeat that fails is not resent, since a late one would wrongly say the TV is reachable. **Deliveries** lists them once you turn them on under **Filter**.
- A missing one means the TV is off or offline, or Lookout is not running. A TV that deep-sleeps in standby may stop sending too.

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

| Package      | Contents                                                                                      |
| ------------ | --------------------------------------------------------------------------------------------- |
| `event`      | `Event` and the JSON builder                                                                  |
| `foreground` | `ForegroundTracker` turns window events into app changes; `AppEvent`                          |
| `device`     | `DeviceEvent` and `DeviceStateReceiver`: screen and screensaver changes                       |
| `playback`   | `PlaybackTracker` and `PlaybackEvent`; `MediaSessionWatcher` reads the sessions               |
| `delivery`   | `Outbox`, `OutboxSender` and `WebhookSink`; `DeliveryLog`                                     |
| `heartbeat`  | `Heartbeat` sends `HeartbeatEvent` at the chosen interval                                     |
| `navigation` | `RailFocus` decides where the D-pad goes between a page and the side rail; `ExitConfirmation` |
| `config`     | `Config`, validated settings; `SaveGate`; `PrefsConfigStore` persists them                    |
| `update`     | `GitHubReleases`, `UpdateChecker`, `ApkFetcher`, `UpdateInstaller`, `RelaunchAfterUpdate`     |
| `ui`         | One panel class per tab                                                                       |
| (root)       | `LookoutService` and `SettingsActivity`; their names are stored on the TV                     |

To send events somewhere other than a webhook, implement `EventSink`.

**Releases.** Pushing a tag like `v1.2.3` builds, signs and publishes the APK with its SHA-256 (`v1.2.3-rc.1` makes a pre-release, which the app only offers when **Include pre-releases** is ticked). The release notes come from the matching `CHANGELOG.md` section, and the release fails without one. The version code is `major*10000 + minor*100 + patch`. Gradle builds without the signing key; a separate job signs with `apksigner` using the `release` environment's secrets. Each APK also gets a build provenance attestation; check a download with `gh attestation verify lookout-v1.2.3.apk --repo technoamaniac/lookout`.

**Pins.** Actions, the Gradle distribution and every dependency are pinned (by SHA, `app/gradle.lockfile` and `gradle/verification-metadata.xml`). After changing a dependency:

```sh
./gradlew :app:dependencies --write-locks
./gradlew --write-verification-metadata sha256 test assembleDebug assembleRelease
```

After an AGP upgrade, also add the Linux and Windows `aapt2` hashes by hand from Google Maven's `.sha256` files; Gradle only fetches the current machine's.

Issues and pull requests are welcome; new logic should come with a test. Lookout is developed with [Claude Code](https://claude.com/claude-code) under the maintainer's review; the conventions are in [CLAUDE.md](CLAUDE.md).

## License

Apache License 2.0. See [LICENSE](LICENSE). Copyright 2026 Aman Singh.
