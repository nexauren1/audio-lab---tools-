# Audio Lab – Tools

A native Android audio toolbox focused on practical file operations — not singing, vocal recording, or voice effects.

## Product direction

Audio Lab is being built as a premium, offline-first toolbox for managing and processing audio files. The interface is inspired by the approved dark, neon-blue and violet concept.

### Tool groups

- **Edit:** trim, split, join and batch operations.
- **Convert:** audio format and sample-rate conversion.
- **Clean:** normalization, gain adjustment and silence removal.
- **Analyze:** technical audio inspection and audio properties.
- **Metadata:** inspect and manage file tags.
- **Batch:** apply supported operations to several files.

### Available in this first foundation

- Audio file inspection using Android media metadata.
- WAV PCM normalization.
- WAV PCM gain adjustment.
- Persistent background work queue.
- Portuguese and English interface, selected from the device language.
- Android Storage Access Framework import; no broad storage permission is required to choose a file.

Other tools are visible in the catalogue but clearly marked as planned until their processing engines are implemented.

## Processing rules

1. Opening a tool prepares its interface; it does not silently start modifying a file.
2. Processing begins only after the user chooses a source and taps **Process**.
3. Work is submitted to WorkManager and continues if the user leaves the tool or closes the screen.
4. Returning to a tool or opening the queue reads the existing job state. It never starts the same operation again automatically.
5. Cancellation is explicit. The source file is never overwritten.
6. Results are written as new files. Temporary files are removed after success or failure.
7. Unsupported WAV encodings are rejected with an understandable error instead of producing a corrupt file.

## Technical stack

- Kotlin, Android native, Jetpack Compose and Material 3.
- WorkManager for persistent background work.
- Android MediaMetadataRetriever for file inspection.
- A streaming PCM WAV processor (8/16/24/32-bit integer PCM).
- Storage Access Framework and MediaStore for safe file access.

## Build

Open the repository in a recent Android Studio version with JDK 17 and Android SDK Platform 37 installed. The project uses Android Gradle Plugin 9.3.1, Gradle 9.5.0 and Kotlin 2.4.21. Keep `compileSdk` at 37; `targetSdk` remains 36 until Android 17 behaviour changes are tested. Let Gradle sync, then run the `app` configuration.

The Gradle wrapper distribution is configured under `gradle/wrapper/gradle-wrapper.properties`. If the wrapper scripts are not present in a local checkout, generate them with `gradle wrapper --gradle-version 9.5.0`.

## Current stage

This is the first application foundation, not a release build. The next milestones are additional editing and conversion engines, automated tests for audio edge cases, accessibility polish, and release validation on real Android devices.
