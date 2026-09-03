# AIO Stream X TV v1.1.2-tv1

This is the dedicated Android TV edition of AIO Stream X, based on the provided AIO Stream X v1.1.2 source and AIOStreams stable v2.32.1.

## TV edition changes

- Separate application package: `com.codexironman.aiostremiotv`.
- App name: **AIO Stream X TV**.
- Android TV / Google TV Leanback launcher entry plus normal launcher fallback for generic Android TV boxes.
- Fixed landscape orientation.
- No touchscreen or fake-touch requirement.
- TV banner support retained.
- Full-screen immersive TV window.
- Larger WebView text sizing for televisions.
- Hardware-accelerated WebView.
- D-pad focus helper injected into the AIOStreams configuration page with visible focus ring, directional navigation and auto-scroll.
- Larger focusable Retry control.
- TV-specific foreground-service notification branding.
- Output APK name: `AIO-Stream-X-TV-v1.1.2.apk`.

## Android 10 TV support

The project keeps `minSdk 24`, so Android 10 TV (API 29) is supported. Leanback is not marked required because some generic Android 10 TV boxes omit that feature flag even when they are used as televisions.

The bundled native runtime from the supplied AIO Stream X source is ARM64-only, so the final APK requires a 64-bit ARM (`arm64-v8a`) Android TV box.

## Build note

The supplied source archive intentionally omits generated runtime archives, native Node/cloudflared binaries, and the signing keystore. Those build-time artifacts must be restored before producing a fully self-contained APK.
