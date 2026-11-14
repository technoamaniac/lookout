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

`timestamp` is Unix epoch seconds. Events are sent only when the app changes.

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

## Releases

Pushing a tag like `v1.0.0` builds a signed APK and publishes it, with a SHA-256 file, as a GitHub
release. Maintainers need four repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS` and `KEY_PASSWORD`. CI runs the tests and a debug build on every push and PR.

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

The core is plain Java with no Android dependencies, so it is unit-tested on the JVM:

| Class                | Responsibility                                                      |
| -------------------- | ------------------------------------------------------------------- |
| `ForegroundTracker`  | Turns window notifications into app _changes_; owns current state   |
| `PackageFilter`      | Which packages count as user-facing apps                            |
| `AppEvent`           | Immutable event and its JSON form                                   |
| `EventSink`          | Delivery abstraction; `WebhookSink` is the HTTP implementation      |
| `RetryingDispatcher` | Off-thread delivery with backoff; drops events a newer one replaced |
| `LookoutService`     | The only Android-aware class; wires the above together              |

To send events somewhere other than a webhook (MQTT, a local socket), implement `EventSink`.

## Limitations

- Reports app changes, not playback state. Pair it with your TV's power state to know when the
  screen is actually on.
- The enabled-keyboard ignore list is read when the service starts; restart it after installing a
  new keyboard.
- Some TVs reset secure settings on factory reset, so re-run `scripts/install.sh` afterwards.

## Contributing

Issues and pull requests are welcome. Run `./gradlew test` before sending a change, and keep new
logic in the Android-free core with a test.

## License

Apache License 2.0. See [LICENSE](LICENSE). Copyright 2026 Aman Singh.
