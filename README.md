# YS Gallery

A fast, private photo and video gallery for Android, with a Samsung One UI-inspired design and smooth animations.
It works entirely offline: no internet permission, no ads, no tracking, no account.

Gallery is made for:

- custom ROMs that ship without a usable gallery app,
- phones whose only gallery is a cloud photo app,
- anyone who wants a smooth, good-looking gallery that keeps their photos private.

## Features

- **Samsung One UI-inspired design** with its own layout and animations
- Photos, videos and GIFs from internal storage and the SD card
- Albums: create, rename, hide, sub-albums, move media between albums, move whole albums between internal storage
  and SD card
- Favorites and trash
- Deep zoom, readable even on huge photos and very long scroll screenshots
- Editor: crop, rotate and mirror photos and videos (saved as a copy)
- Storage overview for internal storage and SD card
- Open photos and videos from other apps ("Open with")
- English
- Adjustable graphics quality for older or low-memory devices

## Privacy

Gallery has no internet permission, so it cannot send anything anywhere. It only asks for access to your
photos and videos. Settings stay on your device.

## Requirements

Android 10 (API 29) or newer.

## Download

- **GitHub Releases:** https://github.com/yeasin0423/PrismGallery/releases
- **F-Droid:** coming soon

### Verifying the APK

APKs from GitHub Releases are signed with this certificate (SHA-256):

```
A5:66:D9:9F:38:36:C3:6E:77:BE:C4:EA:44:62:AC:8F:E6:FF:30:9F:67:F1:64:97:F0:87:F8:D0:3D:9E:EF:9F
```

You can check it with `apksigner verify --print-certs app-release.apk`, or let
[AppVerifier](https://github.com/soupslurpr/AppVerifier) or [Obtainium](https://github.com/ImranR98/Obtainium) do it.
The build is reproducible: F-Droid builds the app from source and publishes the APK signed with this same key, so
installs from F-Droid and from GitHub can update each other.

## Building

Requirements: JDK 17 and the Android SDK (compile SDK 34).

```sh
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`. Without a signing key the release APK is unsigned.
To sign it, create a `keystore.properties` file in the project root (it is git-ignored and must never be committed):

```properties
storeFile=/path/to/your-release-key.jks
storePassword=...
keyAlias=...
keyPassword=...
```

### Gradle in CI

There are two manual workflows:

- **Build release APK** — the normal build. It uses the Gradle wrapper (`./gradlew`), which downloads
  Gradle 8.9 from `https://services.gradle.org/distributions/gradle-8.9-bin.zip` and verifies its
  SHA-256 (`distributionSha256Sum` in `gradle/wrapper/gradle-wrapper.properties`).
- **Build release APK (mirror)** — the fallback, for when services.gradle.org or the GitHub mirror it
  redirects to is down. It downloads the identical archive from this repository's `v2.7-b12` release
  (`https://github.com/yeasin0423/YSGallery/releases/download/v2.7-b12/gradle-8.9-bin.zip`), checks
  the same SHA-256 and runs that `gradle` directly.

Both workflows build the APK, upload the unsigned artifact and publish the empty per-version release
the same way; only the Gradle download differs. Local `./gradlew` builds are unchanged.

To build by hand with the mirror:

```sh
curl -fL -o gradle-8.9-bin.zip \
  https://github.com/yeasin0423/YSGallery/releases/download/v2.7-b12/gradle-8.9-bin.zip
sha256sum gradle-8.9-bin.zip   # must match the hash above
unzip gradle-8.9-bin.zip
./gradle-8.9/bin/gradle assembleRelease --no-daemon
```

The archive is not stored in the repository. It was committed as three 45 MB parts for a while; those
parts were purged from the git history so clones stay small.

## Disclaimer

Gallery is an independent project and is not affiliated with, endorsed by or connected to Samsung. The name is
only used to describe the visual style the design is inspired by.

## License

Copyright (C) 2026 Soretake

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [GNU General Public License](LICENSE) for
more details.
