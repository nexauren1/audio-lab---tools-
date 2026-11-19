# Audio Lab – Tools

A native Android audio toolbox for editing, converting, cleaning and analysing audio files. It is not a voice recorder, singing-effects app or vocal studio.

## Product direction

Audio Lab is a premium, offline-first toolbox. Its interface uses a dark workspace with violet and cyan accents, individual tool screens and a persistent background-processing queue.

## Supported audio formats

The import flow uses Android's Storage Access Framework and the `audio/*` MIME filter. The FFmpeg audio engine is configured for:

- MP3
- WAV
- FLAC
- AAC
- M4A
- OGG
- OPUS

The selected FFmpeg audio build includes LAME/MP3, Vorbis and Opus encoders; AAC and FLAC encoders are provided by FFmpeg itself. Actual decoding still depends on the codecs built into the package. Audio files are processed on-device; source media is not uploaded.

## Implemented operations

- **Inspect audio:** FFprobe format, codec, duration, sample rate, channels, bit depth, size and bitrate, with an Android MediaMetadataRetriever fallback.
- **Convert format:** MP3, WAV, FLAC, AAC, M4A, OGG and OPUS output choices. Bitrate is adjustable for MP3/AAC/M4A/OPUS.
- **Compress file size:** AAC/M4A encoding with a 64–256 kbps quality/size setting.
- **Trim audio:** select start and duration and save a new output file.
- **Join audio:** select 2–8 files; inputs are resampled to a common stereo format before concatenation.
- **Normalize volume:** EBU R128 loudness normalization for supported audio formats.
- **Gain:** increase or reduce volume with a bounded dB setting.
- **Noise reduction:** FFmpeg FFT denoising with adjustable reduction strength and noise floor. Very aggressive settings can remove wanted audio detail.
- **Dynamic compressor:** separate dynamics processing with adjustable ratio and threshold. This is not the same as reducing the encoded file size.
- **Speed:** change playback speed while preserving pitch.
- **Pitch:** adjust pitch by semitones while compensating duration.
- **Metadata:** write title and artist tags when supported by the output container.
- **Audio analysis:** codec/file inspection, peak/RMS levels and spectral centroid, rolloff and flatness. Spectral metrics are sampled from the first 30 seconds.

The following remain planned: split into many files, sample-rate-only conversion, batch processing, expanded metadata fields and an interactive spectrogram.

## Processing reliability

- FFmpeg runs asynchronously and reports its own statistics to WorkManager for progress updates.
- Cancelling a WorkManager job cancels the corresponding native FFmpeg session.
- Files are imported to bounded local temporary files and copied to a new result. Source files are not overwritten.
- Unsupported or invalid audio inputs return an error instead of publishing a partial result.
- Temporary input/output files are removed after success, failure or cancellation.
- Audio inspection is performed with FFprobe where the native engine is supported, with Android metadata extraction as a fallback.

## Device compatibility and size

The maintained FFmpeg audio package currently used by the app is built for `arm64-v8a` and `x86_64`. Advanced FFmpeg operations are therefore enabled only on supported 64-bit devices. The existing streaming WAV PCM engine remains as a fallback for integer PCM WAV normalization and gain on devices without a supported FFmpeg ABI.

The app uses the audio-only artifact `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.9`, rather than the full video package, to avoid bundling video-specific components. It still adds a substantial native-library payload to the APK; the release APK size must be checked before publication.

The FFmpeg audio artifact is distributed under LGPL-3.0. Keep the upstream licence and third-party notices available in release builds, and verify the applicable LGPL packaging/source obligations before publishing. This note is not legal advice.

## Technical stack

- Kotlin, native Android, Jetpack Compose and Material 3.
- WorkManager for persistent background work.
- Maintained FFmpeg audio library plus FFprobe.
- Android MediaMetadataRetriever as an inspection fallback.
- A streaming PCM WAV processor (8/16/24/32-bit integer PCM).
- Storage Access Framework and MediaStore for safe file access.

## Build

Open the repository in a recent Android Studio version with JDK 17 and Android SDK Platform 37 installed. The project uses Android Gradle Plugin 9.3.1, Gradle 9.5.0 and Kotlin 2.4.21. Keep `compileSdk` at 37; `targetSdk` remains 36 until Android 17 behaviour changes are tested. Let Gradle sync, then run the `app` configuration.

The Gradle wrapper distribution is configured under `gradle/wrapper/gradle-wrapper.properties`. If the wrapper scripts are not present in a local checkout, generate them with `gradle wrapper --gradle-version 9.5.0`.

## Validation status

GitHub Actions runs `assembleDebug` and `testDebugUnitTest` for each commit on `main`. A passing CI build validates compilation and unit tests; it does not replace real-device testing across supported Android versions, file providers, codecs, long files and audio edge cases.
