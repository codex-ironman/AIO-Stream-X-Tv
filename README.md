# AIO Stream X TV

Dedicated Android TV edition of AIO Stream X, based on AIO Stream X v1.1.2 / AIOStreams stable v2.32.1.

## TV edition

- Separate package: `com.codexironman.aiostremiotv` (installs beside the mobile app)
- Android TV / Google TV Leanback launcher support
- Normal launcher fallback for generic Android 10 TV boxes
- Fixed landscape orientation and immersive full-screen mode
- No touchscreen requirement
- 320×180 TV banner support
- Larger TV-friendly WebView text
- Enhanced remote/D-pad focus navigation with visible focus rings and auto-scroll
- Android 10 TV (API 29) supported; project minSdk remains 24
- ARM64 (`arm64-v8a`) runtime, matching the supplied AIO Stream X source

The `Android-App` directory contains the TV-specific Android wrapper source. The complete modified source bundle also includes the AIOStreams upstream/local source used by AIO Stream X v1.1.2.

See `TV-EDITION.md` and `Android-App/README.md` for build details.
