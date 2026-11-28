# Game-Aero screen streaming to Chromecast

## Goal

Cast the live Game-Aero app window to a Chromecast/Google Cast receiver. The TV receives a video stream of the UI that is currently shown by the phone; this is not a second dashboard or a remote copy of the library database.

## Implemented sender pipeline

1. The Cast picker is provided by the Google Cast Sender SDK. The sender uses the Google Default Media Receiver.
2. The user starts a capture session through Android's MediaProjection consent UI.
3. `AeroScreenStreamService` runs as a foreground media-projection service and creates one virtual display for the session.
4. `AeroVideoEncoder` encodes the virtual-display surface as H.264 using MediaCodec.
5. `AeroHlsSegmenter` and `AeroTsMuxer` create a rolling, video-only MPEG-TS HLS stream.
6. `AeroHlsHttpServer` serves the HLS playlists and segments over the phone's local network. A random per-session token is required for every stream path.
7. Once the first segment is available, the sender loads the live HLS master playlist into the active Cast session.

The phone and Chromecast must be on the same reachable local network. There is no cloud relay. The HTTP server binds directly to the selected local IPv4 address (preferring Wi-Fi), rather than listening on every interface. The stream URL is shared only with the active Cast session. The token is discarded when the service stops.

## Preferences

- Resolution: 480p, 720p, or 1080p.
- Aspect ratio: Original, 16:9, or 21:9. The old 4:3 option was removed.
- The selected stream dimensions are applied to the capture surface only; they do not change the phone's normal display resolution or the library's layout.
- 16:9 uses standard HD dimensions for the selected preset. 21:9 is bounded to the corresponding preset's 16:9 maximum width to avoid unusually wide decoder surfaces. Original follows the app's current display ratio.

## Privacy and lifecycle

- Android requests permission before every screen-capture session. The service uses the projection token once and creates one virtual display for that session.
- On Android 14 and later, the system picker allows the user to choose a single app window. The user should select Game-Aero. Earlier Android versions may only offer full-display sharing.
- A persistent foreground notification shows while capture is active and includes a Stop action.
- The virtual display, encoder, HTTP server, and projection are released when the user stops the stream, Android revokes the projection, or the Cast session ends.
- Audio: Game-Aero's own playback is captured with AudioPlaybackCapture (API 29+, own UID only), encoded as AAC-LC 48 kHz stereo and muxed as ADTS into the same MPEG-TS. It needs the RECORD_AUDIO permission; without it, or if the user switches audio off, the stream is video only.
- The HTTP origin uses a random session token and is intended only for the local Wi-Fi/LAN connection.

## Known limitations / validation still required

- The initial HLS implementation has segment buffering and is not intended to match the latency of a direct HDMI connection. End-to-end latency has not yet been measured on a physical Chromecast.
- Actual playback compatibility depends on the Chromecast model and its Default Media Receiver support for the encoded H.264/HLS stream.
- The source changes have not yet been built or exercised on a phone/TV pair. The next validation step is a modern-flavor Android build, followed by tests at 480p/720p/1080p and Original/16:9/21:9 on the user's device.
- If this app later targets Android 17/API 37 or higher, the LAN server path must be revisited for Android's new local-network permission requirements. The current modern flavor targets API 36.

## Relevant source

- `AeroScreenStreamSettings.kt`: saved preferences and output-size calculation.
- `AeroScreenStreamService.kt`: projection consent result, foreground service, capture lifecycle, and stream-ready broadcasts.
- `AeroVideoEncoder.kt`: H.264 MediaCodec encoder.
- `AeroHlsSegmenter.kt` and `AeroTsMuxer.kt`: HLS segment and MPEG-TS construction.
- `AeroHlsHttpServer.kt`: token-protected local HTTP delivery.
- `AeroScreenStreamSettingsDialog.kt` and `AeroHomeScreen.kt`: Cast picker, permission flow, and start/stop controls.
