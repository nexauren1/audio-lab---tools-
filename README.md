# Audio Lab – Tools

A native Android audio toolbox for editing, converting, cleaning, inspecting and organizing audio files — not singing, vocal recording, or voice effects. The catalogue contains 37 tools across six specialist categories.

## Product direction

Audio Lab is being built as a premium, offline-first toolbox for managing and processing audio files. The interface is inspired by the approved dark, neon-blue and violet concept.

### Tool groups

- **Edit:** trim, split, join, reverse, fades, speed and channel operations.
- **Convert:** format, sample-rate, bit-depth and channel conversion, plus local video audio extraction.
- **Clean:** normalization, gain, silence removal, noise reduction, compression and limiting.
- **Analyze:** file properties, waveform, spectrogram, frequency, loudness, clipping and silence analysis.
- **Metadata:** tags, cover artwork, batch rename and metadata cleanup.
- **Batch:** convert, normalize, rename and export collections.

### Available in this first foundation

- Audio file inspection using Android media metadata.
- WAV PCM trimming by start/end time (8/16/24/32-bit integer PCM).
- WAV PCM splitting into 2–20 frame-aligned parts, exported as a ZIP archive.
- Joining 2–20 WAV PCM files with matching sample rate, channels and bit depth.
- Reversing WAV PCM in frame order, preserving multi-channel frames.
- WAV PCM normalization.
- WAV PCM gain adjustment.
- Persistent background work queue with real progress, cancellation and result links.
- Portuguese and English interface, selected from the device language.
- Android Storage Access Framework import; no broad storage permission is required to choose a file.

Other tools are visible in the catalogue but clearly marked as planned until their processing engines are implemented.

The current trim, split, join, reverse, normalize and gain processors accept uncompressed integer-PCM WAV at 8, 16, 24 or 32 bits. They do not silently convert unsupported encodings.

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

Open the repository in a recent Android Studio version with JDK 17 and Android SDK 37 installed. Let Gradle sync, then run the `app` configuration.

The Gradle wrapper distribution is configured under `gradle/wrapper/gradle-wrapper.properties`. If the wrapper scripts are not present in a local checkout, generate them with `gradle wrapper --gradle-version 9.3.1`.

## Current stage

The premium toolbox dashboard and 37-tool catalogue are in place. Nine operations currently have processing engines; the remaining catalogue entries are clearly marked as in development. The dashboard also supports saved favourites. Next milestones are more tested editing and conversion engines, deeper file-library workflows, accessibility polish and release validation on real Android devices.
