# Audio Lab – Tools

Audio Lab is a native Android audio toolbox for editing, converting, shaping, cleaning and analysing audio files. It is not a recorder or a singing-effects app.

## Product experience

The app follows a premium dark workspace with violet, blue and cyan accents. It has a dedicated Home screen, a searchable two-column catalogue, a separate full-screen workspace for each tool, a local audio library, settings and a persistent processing queue.

The catalogue contains **38 tools**. Every catalogue entry is wired to a processing operation; no entry is presented as ready while only showing a prototype message.

## Languages

English is the default language on a fresh installation. The first launch asks the user to choose English or Portuguese. The selection is stored locally and can be changed later in Settings. The app does not depend on the device language after the user makes a choice.

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

## Processing operations

### File operations
- **Inspect audio:** codec, duration, sample rate, channels, bit depth, size and bitrate, with an Android metadata fallback.
- **Convert format:** MP3, WAV, FLAC, AAC, M4A, OGG and OPUS.
- **Compress file size:** AAC/M4A output with an adjustable 64–256 kbps bitrate.
- **Trim audio:** select start and duration and save a new output.
- **Join audio:** combine 2–8 files after resampling them to a common stereo format.
- **Sample-rate conversion:** resample to 44.1, 48 or 96 kHz.
- **Normalize volume:** EBU R128 loudness normalization.
- **Gain:** increase or reduce volume with a bounded dB setting.
- **Noise reduction:** FFmpeg FFT denoising with adjustable reduction strength and noise floor.
- **Dynamic compression:** control dynamics with adjustable ratio and threshold.
- **Speed:** change tempo while preserving pitch.
- **Pitch:** shift pitch by semitones while compensating duration.
- **Nightcore:** combine a faster tempo with a raised pitch in one export.
- **Metadata:** write title and artist tags when supported by the output container.
- **Analysis:** codec/file inspection, peak/RMS levels and spectral centroid, rolloff and flatness. Spectral metrics are sampled from the first 30 seconds.

### Sound effects and shaping
- Audio EQ, 8D stereo modulation, reverb, bass boost, treble boost and echo.
- Chorus, flanger, phaser, tremolo and vibrato.
- Soft distortion, radio and telephone band-limiting effects.
- Mono conversion and stereo widening.
- Reverse audio (limited to source files up to five minutes to protect device memory).
- Fade-in and fade-out.
- Silence trimming.
- Peak limiting.
- Voice pitch shifting. This is a conventional DSP effect, not AI voice cloning.

The app does not claim to provide AI stem separation, voice cloning or speech recognition. Those require different models/services and are not represented as working local tools.

## Processing reliability

- FFmpeg runs asynchronously through WorkManager and reports processing statistics for progress updates.
- Cancelling a WorkManager job cancels its native FFmpeg session.
- Input files are copied to bounded local temporary files. Source files are never overwritten.
- Unsupported or invalid inputs return a processing error instead of publishing a partial result.
- Temporary input/output files are removed after success, failure or cancellation.
- Reverse processing is restricted to five-minute files because FFmpeg's reverse filter buffers audio in memory.
- Audio inspection uses FFprobe where the native engine is supported, with Android metadata extraction as a fallback.

## Device compatibility and size

The maintained FFmpeg audio package is built for `arm64-v8a` and `x86_64`. Advanced FFmpeg operations are enabled only on those 64-bit ABIs. On other devices, the app keeps the compatible WAV PCM normalization and gain fallback, plus Android metadata inspection.

The app uses the audio-only artifact `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.9`, rather than the full video package, to avoid bundling video-specific components. It still adds a substantial native-library payload to the APK; check the release APK size before publication.

The FFmpeg audio artifact is distributed under LGPL-3.0. Keep the upstream licence and third-party notices available in release builds, and verify the applicable LGPL packaging/source obligations before publishing. This note is not legal advice.

## Technical stack

- Kotlin, native Android, Jetpack Compose and Material 3.
- WorkManager for persistent background processing.
- Maintained FFmpeg audio library plus FFprobe.
- Android MediaMetadataRetriever as an inspection fallback.
- A streaming PCM WAV processor (8/16/24/32-bit integer PCM).
- Android Storage Access Framework and MediaStore for file access.

## Build

Open the repository in Android Studio with JDK 17 and Android SDK Platform 37 installed. The project uses Android Gradle Plugin 9.3.1, Gradle 9.5.0 and Kotlin 2.4.21. The application uses minSdk 26 and targetSdk 36.

The Gradle wrapper distribution is configured under `gradle/wrapper/gradle-wrapper.properties`. If the wrapper scripts are absent in a local checkout, generate them with `gradle wrapper --gradle-version 9.5.0`.

## Validation

GitHub Actions runs `:app:testDebugUnitTest`, `:app:assembleDebug`, then validates the configured JKS and builds a signed release APK/AAB. It verifies the APK signature before uploading artifacts. CI compilation and unit tests do not replace real-device testing across supported Android versions, file providers, codecs, long files and audio edge cases.
