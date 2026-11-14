# Lookout

A tiny Android TV service that reports which app is in the foreground to a webhook.

TV vendors' remote APIs (Sony Bravia REST, Fire TV, and others) often don't expose the
current app, especially for sideloaded ones like SmartTube. Lookout runs on the TV itself, sees
every app that takes the foreground, and pushes a JSON event to an endpoint you control, such as
Home Assistant, n8n, or a plain HTTP server.

## How it works

An `AccessibilityService` subscribes to window-state changes only. It never requests window
content, so it can't read what's on screen, only which package owns the window. System UI, this
app and enabled keyboards are filtered out so they don't mask the real foreground app. Each change
is POSTed to your webhook with retry and backoff.

```
POST <webhook.url>
Content-Type: application/json

{"package": "org.smarttube.stable", "timestamp": 1731600000}
```

`timestamp` is Unix epoch seconds. Events are sent only when the app changes. Any 2xx response
counts as delivered. Network errors, 408, 429 and 5xx responses are retried; other responses are
not, and redirects are not followed, so point the URL at the final endpoint.

## Requirements

- Android 9+ (API 28), tested on Android TV
- JDK 17 and the Android SDK (platform 34) to build
- `adb` access to the TV (wireless debugging is enough)

## Build

```sh
./gradlew test assembleDebug
```

The APK has no configuration baked in, so one build works for everyone and is safe to share.

## Install and configure

TVs usually have no UI for enabling accessibility services, so enable it over ADB:

```sh
adb connect <tv-ip>:<port>
./scripts/install.sh
```

The script installs the APK and adds Lookout to `enabled_accessibility_services` without
removing services that are already enabled. It is safe to re-run.

Then open **Lookout** from the TV's app list, enter your webhook URL, press **Save**, and use
**Send test** to confirm it arrives. Settings persist across reboots and apply immediately.

Typing a long URL with a remote is tedious. Seed the field from your computer instead, then press
Save on the TV:

```sh
adb shell am start -n dev.aman.lookout/.SettingsActivity --es url "https://example.com/hooks/lookout"
```

| Setting            | Meaning                                                             |
| ------------------ | ------------------------------------------------------------------- |
| Webhook URL        | Endpoint that receives the POST. HTTPS only; cleartext is rejected. |
| Ignored packages   | Optional comma-separated packages never to report (launchers etc.). |

## Deliveries and updates

The app has three tabs. **Deliveries** lists the last 50 delivery attempts (kept in memory, so
they clear when the app restarts). **Updates** checks this repository's latest stable GitHub
release and installs it in place; pre-releases are never offered. A corner badge appears when a
newer version exists. The app contacts github.com only when you open it, never in the background.

Updating needs the repository to be public. The app downloads the release APK, checks it against
the published SHA-256, then hands it to Android's installer, which only accepts it if it is signed
with the same key as the installed app. A debug build therefore cannot update to a release build.
The first time, Android asks you to allow Lookout to install apps. Expect a confirmation screen
for each update.

## Releases

Versions follow [Semantic Versioning](https://semver.org). Pushing a tag like `v0.1.0` (or a
pre-release such as `v1.1.0-rc.1`, published as a GitHub pre-release) builds a signed APK and publishes it, with a SHA-256 file, as a GitHub
release. Maintainers need four repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS` and `KEY_PASSWORD`. The tag sets the app's version name; the version code is
`major*10000 + minor*100 + patch`, so keep minor and patch below 100. CI runs the tests and a debug build on every push and PR.

## Pinned dependencies

Everything is pinned for reproducible, auditable builds: GitHub Actions by commit SHA, the Gradle
distribution by checksum, dependency versions in `app/gradle.lockfile` and artifact hashes in
`gradle/verification-metadata.xml`, plus an exact JDK and runner image. Dependabot proposes
updates weekly. After changing a dependency, refresh the pins with:

```sh
./gradlew :app:dependencies --write-locks
./gradlew --write-verification-metadata sha256 test assembleDebug assembleRelease
```

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

## Design

The code is split by feature. Apart from `ui`, the root classes, `PrefsConfigStore`,
`UpdateInstaller` and `InstallResultReceiver`, everything is plain Java with no Android
dependencies, so it is unit-tested on the JVM. Dependencies point one way:
`ui` and the entry points use the feature packages, and the feature packages never use them.

| Package      | Class                | Responsibility                                                      |
| ------------ | -------------------- | ------------------------------------------------------------------- |
| `foreground` | `ForegroundTracker`  | Turns window notifications into app _changes_; owns current state   |
|              | `PackageFilter`      | Which packages count as user-facing apps                            |
|              | `AppEvent`           | Immutable event and its JSON form                                   |
| `delivery`   | `EventSink`          | Delivery abstraction; `WebhookSink` is the HTTP implementation      |
|              | `DeliveryResult`     | Whether a send succeeded, may succeed on retry, or never will       |
|              | `RetryingDispatcher` | Off-thread delivery with backoff; drops events a newer one replaced |
|              | `DeliveryLog`        | Bounded, thread-safe history of attempts; `RecordingSink` fills it  |
| `config`     | `Config`             | Validated, immutable settings; `PrefsConfigStore` persists them     |
| `update`     | `Version`            | Semantic version ordering, pre-releases included                    |
|              | `ReleaseSource`      | Where the newest release comes from; `GitHubReleases` implements it |
|              | `UpdateChecker`      | Whether a published release is newer than the installed version     |
|              | `ApkFetcher`         | Downloads an APK and verifies its SHA-256                           |
|              | `UpdateInstaller`    | Hands the APK to Android's package installer                        |
| `ui`         | `*Panel`             | One class per tab (settings, deliveries, updates) plus the service status |
| (root)       | `LookoutService`     | Accessibility service; wires the delivery classes together          |
|              | `SettingsActivity`   | The screen; builds the panels and the tabs                          |

The two root classes stay at the root because their names are stored on the TV (the enabled
accessibility service) and used by `scripts/install.sh`; renaming them would break existing installs.

To send events somewhere other than a webhook (MQTT, a local socket), implement `EventSink`.

## Limitations

- Reports app changes, not playback state. Pair it with your TV's power state to know when the
  screen is actually on.
- The enabled-keyboard ignore list is read when the service starts or settings are saved; press
  Save again after installing a new keyboard.
- Update installs need Android's "install unknown apps" permission, and some TV builds have no
  screen to grant it; use `scripts/install.sh` there.
- Some TVs reset secure settings on factory reset, so re-run `scripts/install.sh` afterwards.

## Contributing

Issues and pull requests are welcome. Run `./gradlew test` before sending a change, and keep new
logic in the Android-free core with a test.

## License

Apache License 2.0. See [LICENSE](LICENSE). Copyright 2026 Aman Singh.
