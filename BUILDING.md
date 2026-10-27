# Build and provenance / 构建与来源

## Build with the exact published upstream native libraries

Requires JDK21, Android SDK35/Build Tools36.0.0 and Python3.

```sh
python3 tools/prepare_native.py
./gradlew :app:assembleRelease --no-configuration-cache
```

On Windows use gradlew.bat. `prepare_native.py` downloads the unchanged upstream v0.3.5 APK, validates its full SHA-256, extracts and separately checks the two ARM64 libraries, and stages the included SWIG4.3.1 Java source bindings. No phone data or credentials are involved. Without private signing properties the release is unsigned. The exact build configuration is in Gradle files and the native manifest.

## Fully build native libraries from source

The original `sip-pjsua2/native/build-pjsua2.sh` and all configuration/patches are retained. Use Linux/macOS, SWIG4.3.1, Android NDK28.2.13676358 and the script's SHA-pinned pjproject2.17, OpenSSL3.5.9 and Opus1.6.1 sources. Then run Gradle. The upstream notice lists OpenSSL3.5.8 while the v0.3.5 build script pins3.5.9; this fork preserves that notice and records the discrepancy rather than claiming a separately rebuilt native binary. Published native bytes remain exactly upstream.

The Release source archive includes this project, its Java bindings and the script-pinned native source tarballs. The exact versioned upstream source and all retained notices are available alongside the binary.

## Signing

Keep release keys outside Git. Gradle accepts local `vsa.signing.storeFile`, `storePassword`, `keyAlias`, `keyPassword` properties; do not commit them. Public APKs use the same existing local maintainer key. CI's disposable signature is for compilation checks only. If self-signing with another key, update paired-helper certificate pins before using its automatic recovery.
