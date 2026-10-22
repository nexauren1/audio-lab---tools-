# Audio Lab — Tools

An Android-first, bilingual toolbox for audio editing, effects, voice workflows, conversion and sound experiments.

**Languages:** English and Portuguese  
**Platform:** Android (Jetpack Compose)  
**Status:** UI foundation / early prototype

## What is in this first scaffold?

- A dark, advanced UI with an original Audio Lab waveform identity.
- A 38-tool catalogue organized into five task-focused categories.
- Global search over localized tool names and descriptions.
- Tool cards with category accents and saved/favorite state.
- A saved-tools screen and settings screen.
- An in-app language selector for English and Portuguese.
- An Android document picker for selecting audio or video files from a tool workspace.
- An adaptive launcher icon and a separate SVG icon source.

**Important:** the interface, category browsing, language switch, saved tools and file picker are wired. Audio DSP/AI processing is not yet connected. A picked file is selected for the workspace but is not modified or exported by this scaffold.

## Product structure

- **Effects** — equalization, spatial effects, reverb, bass and time-based effects.
- **Voice & AI** — vocal tuning, voice changes, cleanup, stem splitting and speech tools.
- **Edit & convert** — trimming, joining, pitch, speed, metadata and format conversion.
- **Create & analyze** — beat maker, BPM detection, tone generator, visualizer and soundboard.
- **The lab** — experimental time stretching and noise-based sound generation.

The category system is inspired by task-oriented audio tool catalogues and guides. This repository has its own UI, icon and descriptions.

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- AppCompat locale APIs for language switching
- Android document picker for file selection
- Material Icons

## Open in Android Studio

1. Open this repository as a project.
2. Use JDK 17.
3. Let Android Studio sync the Gradle project.
4. Run the app configuration on an Android 8.0+ emulator or device.

The first implementation targets API 35 and has minimum SDK 26.

## Next implementation milestones

1. Add a tested audio import/decode layer and file metadata.
2. Build non-destructive trim, join, reverse and format-conversion engines.
3. Add an export pipeline and audio quality checks.
4. Integrate real-time effects and then higher-complexity voice/AI tools.
5. Add tests for signal-processing math, permission handling, recovery and localization.

See docs/PRODUCT_GUIDE.md and docs/DESIGN_SYSTEM.md.
