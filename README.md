# Lookout

An Android TV service that reports the foreground app and the screen state to a webhook.

TV remote APIs (Sony Bravia, Fire TV and others) often can't tell you which app is open, especially sideloaded ones like SmartTube. Lookout runs on the TV and POSTs each app change to an endpoint you control, such as Home Assistant, n8n or any HTTPS server.

## How it works

An accessibility service listens for window changes only. It never reads window content, so it sees which app is in front, not what is on screen. System UI, keyboards and Lookout itself are ignored. Each change is sent as:

```
POST <webhook url>
Content-Type: application/json

{"event": "app_changed", "package": "org.smarttube.stable",
 "activity": "org.smarttube.stable.MainActivity", "device": "Living room",
 "timestamp": 1731600000}
```

`event` names the kind of message. `activity` is the window Android reported when the app came to the front, often its main activity but sometimes a dialog or splash window; later screens inside the app do not update it. `device` is the TV's name from its system settings, so one webhook can serve several TVs; it is only sent when **Send this TV's name** is ticked. Both are left out when unknown. `timestamp` is Unix seconds.

Lookout also reports the TV itself, with the same `event`, `device` and `timestamp` fields: `screen_on`, `screen_off`, `screensaver_started` and `screensaver_stopped`. The screen state is also sent when the service starts. Besides window changes, Lookout only watches these screen and screensaver changes; it reads nothing else about the TV. While the screensaver runs, app changes are not reported, so the app you were using stays as the current one. `screen_off` is best effort: a TV that loses its network as it goes to standby may not manage to send it. These messages have no `package`, so an automation that reads `trigger.json.package` needs the condition `{{ trigger.json.package is defined }}`, or it will blank its state on every screen change.

Any 2xx counts as delivered. Network errors, 408, 429 and 5xx are retried; other responses are not. Redirects are not followed, so use the final URL.

## Install

Requires Android 9+ and `adb` access to the TV (wireless debugging is enough). Download the APK from [Releases](../../releases/latest), then:

```sh
adb connect <tv-ip>:<port>
./scripts/install.sh lookout-v0.1.0.apk
```

The script installs the APK and enables the service, keeping other enabled services. It is safe to re-run, for example after a factory reset.

Open **Lookout** on the TV, enter your webhook URL, press **Save**, then **Send test**. The URL must be HTTPS. **Ignored packages** optionally lists apps never to report, such as a launcher, separated by commas.

To avoid typing with the remote, send either field from your computer and press Save on the TV:

```sh
adb shell am start --activity-clear-top -n dev.aman.lookout/.SettingsActivity \
  --es url "https://example.com/hooks/lookout" \
  --es ignored "com.spocky.projengmenu,com.google.android.apps.tv.dreamx"
```

## Using the app

- **Deliveries** shows the last 10 attempts until Lookout restarts. Press OK on one to see the exact payload.
- **Updates** installs the latest stable release from this repository after checking its SHA-256. Android only accepts it if it is signed with the same key as the installed app. Lookout contacts GitHub only while this screen is open.

## Home Assistant example

```yaml
automation:
  - alias: TV foreground app
    triggers:
      - trigger: webhook
        webhook_id: !secret lookout_webhook_id
        local_only: false
    actions:
      - action: input_text.set_value
        target: { entity_id: input_text.tv_foreground_app }
        data: { value: "{{ trigger.json.package }}" }
```

## Limitations

- Reports app changes, not playback.
- After a restart, Lookout reports the next app change. Android only names the app in front when the service may read window content, which Lookout deliberately does not.
- In-app updates need the "install unknown apps" permission; some TVs have no screen to grant it, so use `scripts/install.sh` there.

## Development

Needs JDK 17 and the Android SDK (platform 37).

```sh
./gradlew test assembleDebug lintDebug
```

Logic lives in plain-Java packages with JVM unit tests; Android classes stay thin.

| Package      | Contents                                                                  |
| ------------ | ------------------------------------------------------------------------- |
| `event`      | `Event`, the JSON builder and `LatestEvents`, which drops superseded ones |
| `foreground` | `ForegroundTracker` turns window events into app changes; `AppEvent`      |
| `device`     | `DeviceEvent` and `DeviceStateReceiver`: screen and screensaver changes   |
| `delivery`   | `EventSink` and `WebhookSink`, `RetryingDispatcher`, `DeliveryLog`        |
| `config`     | `Config`, validated settings; `PrefsConfigStore` persists them            |
| `update`     | `GitHubReleases`, `UpdateChecker`, `ApkFetcher`, `UpdateInstaller`        |
| `ui`         | One panel class per tab                                                   |
| (root)       | `LookoutService` and `SettingsActivity`; their names are stored on the TV |

To send events somewhere other than a webhook, implement `EventSink`.

**Releases.** Pushing a tag like `v1.2.3` builds, signs and publishes the APK with its SHA-256 (`v1.2.3-rc.1` makes a pre-release, which the app never offers). The release notes come from the matching `CHANGELOG.md` section, and the release fails without one. The version code is `major*10000 + minor*100 + patch`. Gradle builds without the signing key; a separate job signs with `apksigner` using the `release` environment's secrets.

**Pins.** Actions, the Gradle distribution and every dependency are pinned (by SHA, `app/gradle.lockfile` and `gradle/verification-metadata.xml`). After changing a dependency:

```sh
./gradlew :app:dependencies --write-locks
./gradlew --write-verification-metadata sha256 test assembleDebug assembleRelease
```

After an AGP upgrade, also add the Linux and Windows `aapt2` hashes by hand from Google Maven's `.sha256` files; Gradle only fetches the current machine's.

Issues and pull requests are welcome; new logic should come with a test. Lookout is developed with [Claude Code](https://claude.com/claude-code) under the maintainer's review; the conventions are in [CLAUDE.md](CLAUDE.md).

## License

Apache License 2.0. See [LICENSE](LICENSE). Copyright 2026 Aman Singh.
