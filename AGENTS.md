# AGENTS.md

Guidance for agents working in this repository.

## What this app is

**Gallery** (`com.gallery.app`) is a private, offline Android photo/video gallery. It has **no internet
permission**, no ads, no tracking and no account. Keep it that way: never add network permissions,
analytics, crash reporting or telemetry.

The UI is **Samsung One UI styling only** and **English only**. The upstream project shipped four design
languages (Samsung One UI, Google Gemini, iOS Liquid Glass, Xiaomi HyperOS) and English/German strings;
this fork intentionally keeps only One UI and English.

## Repository layout

```
.
├── app/
│   ├── build.gradle.kts          # namespace/applicationId = com.gallery.app
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # one launcher alias: .LauncherOneUi
│   │   ├── java/com/gallery/app/
│   │   │   ├── GalleryApplication.kt   # Application, graphics-quality tiers, Coil setup
│   │   │   ├── MainActivity.kt         # app shell, navigation, theme
│   │   │   ├── data/                   # MediaStore repository, thumbnails, duplicate finder
│   │   │   ├── model/                  # MediaItem, AlbumItem, sorts
│   │   │   ├── service/                # live wallpaper, move-media service
│   │   │   ├── ui/
│   │   │   │   ├── components/         # shared composables (bars, sheets, grid items)
│   │   │   │   ├── design/             # design-system primitives, motion, overlays
│   │   │   │   ├── editor/             # photo/video editor
│   │   │   │   ├── i18n/Strings.kt     # English strings (only language)
│   │   │   │   ├── screens/            # feature screens + per-design settings pages
│   │   │   │   ├── theme/              # palettes, shapes, ThemeState
│   │   │   │   └── viewmodel/          # GalleryViewModel
│   │   │   └── util/                   # launcher icon, wallpaper accents, helpers
│   │   └── res/                        # strings, themes, launcher icons
│   └── proguard-rules.pro
├── build.gradle.kts              # pins R8 8.8.34 for reproducible builds
├── settings.gradle.kts           # rootProject.name = "Gallery"
├── gradle/wrapper/               # Gradle 8.9
├── fastlane/metadata/android/en-US/   # store listing (English only)
└── .github/workflows/build-release.yml
```

## Build and verification

There is no local JDK/Android SDK guarantee in every environment. Prefer CI as the build check:

```sh
./gradlew assembleRelease --no-daemon
```

- CI runs **only** via manual `workflow_dispatch` in `.github/workflows/build-release.yml`
  (Ubuntu 24.04, Temurin JDK 21). A normal push does **not** start a build.
- The workflow uploads `app-release-unsigned.apk` as `Gallery-unsigned-<ref>`.
- The release APK is unsigned unless a git-ignored `keystore.properties` exists.
- Keep the R8 `8.8.34` pin in the root `build.gradle.kts`; changing it breaks the reproducible build.
- Kotlin/JVM target is 17, `compileSdk`/`targetSdk` 34, `minSdk` 29.

## Current styling/translation state (important)

- `DesignLanguage` still contains all four enum values because per-design composables are shared and
  branching on the enum throughout `ui/`. **Do not add UI to select a design.** `ThemeState.load` and
  `ThemeState.apply` force `SAMSUNG_ONEUI`, so only the One UI paths are reachable.
- `AppLanguage` is reduced to `ENGLISH`; `LanguageState.language`/`locale` are constants. `strings`
  always returns `EnglishStrings`. Do not reintroduce a language picker or `GermanStrings`.
- The manifest has a single launcher alias, `.LauncherOneUi`. `LauncherIcon.apply` also disables the old
  Gemini/iOS/HyperOS aliases from previous builds.
- Legacy `amoled_gallery_*` SharedPreferences names are kept intentionally so existing settings survive.
- Internal names `AmoledBlack` and `AmoledVideoPlayer` remain for historical reasons; renaming them is
  optional churn.

## Editing conventions

- Match the surrounding Compose style (4-space indent, trailing commas, one composable per concern).
- Read user-facing text from `strings` / `AppStrings`; do not hardcode UI strings in composables.
- Keep the One UI look: AMOLED black background, large centred titles, floating frosted bars, grouped
  cards with coloured circle icons.
- All Kotlin files live under `app/src/main/java/com/gallery/app/` and their `package` line must match
  the directory.
- Do not commit `keystore.properties`, signing keys, `local.properties` or build output.

## Testing notes

- There are no unit/instrumentation tests in this fork; CI compilation is the primary gate.
- When changing resources, verify `R.*` references still resolve and that removed drawables/mipmaps are
  not referenced from the manifest or Kotlin.
