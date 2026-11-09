# Gallery

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

### Bundled Gradle distribution (CI)

The Gradle wrapper downloads `gradle-8.9-bin.zip` from `services.gradle.org`, which redirects to
`github.com/gradle/gradle-distributions`. That GitHub download occasionally answers HTTP 504, and the
CI build then fails before compilation starts. To keep the manual CI build working during those
outages, the Gradle 8.9 distribution is stored in the repository as three parts:

```
gradle/wrapper/gradle-8.9-bin.zip.part-00
gradle/wrapper/gradle-8.9-bin.zip.part-01
gradle/wrapper/gradle-8.9-bin.zip.part-02
```

They are the unmodified `gradle-8.9-bin.zip`, split with `split -b 45M`. GitHub rejects any single
file larger than 100 MB, hence the split. The workflow concatenates the parts, verifies the SHA-256
(`d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab`), unpacks the distribution and
runs that `gradle` directly, so CI never downloads it. Local `./gradlew` builds are unchanged and
still download Gradle normally.

To rebuild the archive by hand:

```sh
cat gradle/wrapper/gradle-8.9-bin.zip.part-* > gradle-8.9-bin.zip
sha256sum gradle-8.9-bin.zip   # must match the hash above
unzip gradle-8.9-bin.zip
./gradle-8.9/bin/gradle assembleRelease --no-daemon
```

**Removing it again:** once the Gradle download is reliable, delete the three
`gradle/wrapper/gradle-8.9-bin.zip.part-*` files and remove the "Restore the bundled Gradle
distribution" step from `.github/workflows/build-release.yml`, switching the build step back to
`./gradlew`. Deleting the files in a new commit does not reclaim space, because the 130 MB stay in
the repository history; only a history rewrite (`git filter-repo` or BFG) actually shrinks the clone.
Do that only if the repository size matters.

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
