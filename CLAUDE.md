# Lookout

An Android TV accessibility service that reports the foreground app to an HTTPS webhook. Read `README.md` for behaviour and the class overview; this file holds the working rules.

## Code principles

- Write every change to the standard of a principal Java engineer: nitpick every detail (names, comments, structure, edge cases, tests) and do not stop until it looks perfect.
- Follow good object-oriented design: small classes with one responsibility, dependencies passed in (constructor injection), behaviour behind small interfaces such as `EventSink` and `ReleaseSource`, immutable value objects. Do not hack things together.
- The code must be maintainable and very readable. Prefer clear names and small methods over clever ones. Match the surrounding comment density: comment the _why_ of non-obvious decisions, not what the code says.
- Keep logic in the plain-Java packages (`event`, `foreground`, `device`, `playback`, `delivery`, `heartbeat`, `config`, `update`) with unit tests. Android classes stay thin: `ui` panels, the service, the activity and the few adapters (`LookoutApplication`, `PrefsConfigStore`, `PrefsUpdateChannel`, `DeviceStateReceiver`, `MediaSessionWatcher`, `MediaAccessService`, `NetworkWatcher`, `WebhookOutbox`, `InstalledVersion`, `UpdateInstaller`, `InstallResultReceiver`).
- Dependencies point one way: the root classes and `ui` use the feature packages, never the reverse. One class per tab in `ui`; `SettingsActivity` only builds the panels and tabs.
- Keep changes local: each behaviour or rule is decided in exactly one place, so changing it means editing one class (and its test) or one layout. Split a class or layout as soon as it does more than one job or grows hard to scan; as a guide, a class past about 200 lines or a layout holding more than one screen or tab. Remove dead code and comments that no longer hold in the same change that makes them so.
- Keep `LookoutService` and `SettingsActivity` in the root package under their current names: the TV stores the service's name and `scripts/install.sh` uses it.
- Fix root causes and add a regression test with each bug fix. Prove a new test fails on the old code when practical.

## New features

- Research a new feature thoroughly before building it, including online sources such as the Android developer documentation, the AOSP source, release notes and how comparable projects solve it. Compare the realistic approaches and their trade-offs (permissions, privacy, API levels, TV vendor quirks, reliability) and pick the best one, not the first that comes to mind. Record the reasoning in the commit message or a code comment when the choice is not obvious.
- When approaches differ in what they ask of the user or what they read from the device, decide with Aman before building.

## Documentation

- The repository is public. Keep `README.md` short and user-first: what Lookout does, how to install and use it, then one compact Development section. Say each thing once, in plain words; leave implementation detail to the code and its comments, and maintainer rules to this file.
- Write prose in `CLAUDE.md`, `README.md` and `CHANGELOG.md` as one line per paragraph or list item, never hard-wrapped at a column width: editors soft-wrap it, and GitHub shows single newlines in release notes as line breaks. Code blocks, tables and headings keep their own line layout.
- Update the README in the same change as any behaviour it describes.
- Record every user-visible change in `CHANGELOG.md` under `[Unreleased]`, in the same change. Releasing means renaming that section to `[x.y.z]` before tagging: the release job takes its notes from the section named after the tag and fails without one. Pre-releases fall back to `[Unreleased]`. Write for users of the TV app, not for maintainers.

## Platform constraints

- `minSdk` is 28. Do not use newer APIs without a version check (for example `Optional.isEmpty` needs API 33; use `!isPresent()`).
- Webhook URLs must be HTTPS. Redirects are never followed for deliveries, because a followed redirect turns the POST into a GET that looks delivered.
- Retry only network errors, 408, 429 and 5xx; any other response is a permanent failure.
- The updater reads the public GitHub releases of this repository, verifies the SHA-256, and relies on Android's signature check. It offers pre-releases only while the user ticks **Include pre-releases** on the Updates tab, which is off by default.

## Build and verify

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # JDK 17
./gradlew test assembleDebug lintDebug             # CI runs test and assembleDebug
```

A Homebrew JDK is not registered with `java_home`, so there use `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.

Lint, tests and the release build (`assembleRelease`, which runs R8) should all pass before anything is committed. There is no emulator in CI, so say plainly when something was not tried on a real TV.

- Always have every change reviewed before it is committed: run the `code-review` skill with `--fix` and let it fix what it finds without asking, then rebuild and re-run the tests. Say what the review found and fixed.

## Dependencies and pins

Everything is pinned: actions by commit SHA, the Gradle distribution by checksum, `app/gradle.lockfile` and `gradle/verification-metadata.xml`. After changing a dependency, refresh the pins as described in the README, including the Linux and Windows `aapt2` hashes that Gradle does not download on macOS. A new AGP major is upgraded by hand together with Gradle; Dependabot is configured to skip it.

## Git workflow

- Commit and push only when asked. When asked to push, push straight to `main`; this is a solo project with a linear history, so no feature branch or pull request.
- Keep commits focused: for example, fix a broken CI on its own before unrelated work.
- Never commit signing keys, keystores or secrets, or anything private such as a real webhook URL or a home IP address.

## Releases

Pushing a tag like `v1.2.3` builds a signed APK and publishes it with its `.sha256`. The tag sets the version name; the version code is `major*10000 + minor*100 + patch`, so minor and patch stay below 100.

The `build` job runs Gradle with a read-only token and no secrets. Only the `publish` job, which checks out no code and runs no Gradle, gets the signing key (the `release` environment's secrets, usable only from `v*` tags) and a token that can write. Keep it that way: never give Gradle the keystore. Signing must keep using the same key, or installed copies cannot update.
