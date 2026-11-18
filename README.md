# Audio Lab – Tools

A native Android audio toolbox for editing, converting, cleaning and analysing audio files. It is not a voice recorder, singing-effects app or vocal studio.

## Product direction

Audio Lab is a premium, offline-first toolbox. Its interface uses a dark workspace with violet and cyan accents, individual tool screens and an explicit background-processing queue.

## Supported audio formats

The import flow uses Android's Storage Access Framework and the `audio/*` MIME filter. The advanced engine is configured for:

- MP3
- WAV
- FLAC
- AAC
- M4A
- OGG
- OPUS

Actual decoding and encoding still depends on the codecs included in the selected FFmpeg audio build. Files are processed locally; no audio is uploaded to a server.

## Implemented operations

The current codebase includes these processing paths:

- **Inspect audio:** FFprobe-based format, codec, duration, sample rate, channels, bit depth, size and bitrate details on supported 64-bit devices, with an Android MediaMetadataRetriever fallback.
- **Convert format:** MP3, WAV, FLAC, AAC, M4A, OGG and OPUS output choices.
- **Compress audio:** AAC/M4A output with a user-selected bitrate.
- **Trim audio:** select start and duration and save a new output file.
- **Join audio:** select 2–8 files; inputs are resampled to a common stereo format before concatenation.
- **Normalize volume:** loudness normalization for supported input formats.
- **Gain:** increase or reduce volume with a bounded dB setting.
- **Noise reduction:** FFmpeg frequency-domain denoising filter.
- **Speed:** change playback speed while preserving pitch.
- **Pitch:** adjust pitch by semitones while compensating duration.
- **Metadata:** write title and artist tags when supported by the output container.
- **Audio analysis:** codec and file inspection, peak/RMS levels and spectral metrics such as centroid, rolloff and flatness. Spectral metrics are sampled from the first 30 seconds.

The following remain planned and are not presented as ready: split into many files, sample-rate-only conversion, batch processing, advanced metadata fields and an interactive spectrum/spectrogram visualiser.

## Device compatibility

The maintained FFmpeg audio package currently used by the app is built for `arm64-v8a` and `x86_64`. Its advanced operations are therefore enabled only on supported 64-bit devices. The existing streaming WAV PCM engine remains as a fallback for integer PCM WAV normalization and gain on devices without a supported FFmpeg ABI.

The current FFmpeg dependency is the community-maintained audio-only package `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.9`, under LGPL-3.0. The app should retain the corresponding third-party notices and licence information in release builds. The audio-only variant is selected to avoid bundling the complete video feature set.

## Processing rules

1. Opening a tool prepares its screen; it does not start changing a file.
2. Processing starts only after the user selects a source and taps the action button.
3. Jobs are submitted to WorkManager and can continue when the user leaves a screen.
4. Returning to the app reads the existing job state; it does not restart a task.
5. Cancellation is explicit.
6. Original source files are not overwritten. Results are saved as new files in the Audio Lab music folder.
7. Temporary copies are deleted after success, failure or cancellation.
8. Unsupported formats and encodings should return a readable error rather than a corrupt output.

## Technical stack

- Kotlin, Android native, Jetpack Compose and Material 3.
- WorkManager for persistent background work.
- Maintained FFmpeg audio library plus FFprobe for multiformat operations.
- Android MediaMetadataRetriever as an inspection fallback.
- A streaming PCM WAV processor (8/16/24/32-bit integer PCM).
- Storage Access Framework and MediaStore for safe file access.

## Build

Open the repository in a recent Android Studio version with JDK 17 and Android SDK Platform 37 installed. The project uses Android Gradle Plugin 9.3.1, Gradle 9.5.0 and Kotlin 2.4.21. Keep `compileSdk` at 37; `targetSdk` remains 36 until Android 17 behaviour changes are tested. Let Gradle sync, then run the `app` configuration.

The Gradle wrapper distribution is configured under `gradle/wrapper/gradle-wrapper.properties`. If the wrapper scripts are not present in a local checkout, generate them with `gradle wrapper --gradle-version 9.5.0`.

## Validation status

The GitHub Actions workflow runs `assembleDebug` and `testDebugUnitTest` for each commit on `main`. A passing CI build validates compilation and unit tests; it does not replace real-device testing across supported Android versions, file providers, codecs and audio edge cases.
