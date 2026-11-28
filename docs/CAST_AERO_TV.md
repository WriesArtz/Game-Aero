# Screen mirroring to TV

## Goal

Game-Aero should mirror the app's visible UI to a Chromecast/Google Cast receiver, rather than render a separate dashboard. The stream should be a live video image of the current Game-Aero screen.

## Preferences added

The Aero dashboard now opens a screen-streaming settings dialog from the Cast icon. It stores these preferences locally:

- Stream resolution: 480p, 720p, or 1080p (default 720p)
- Aspect ratio: Original, 16:9, 4:3, or 21:9 (default Original)

These settings are groundwork only: the capture/encoding/transport pipeline has not yet been integrated, so the app cannot currently start a screen stream from this button. Preferences are not applied to the app's regular display size or game rendering.

## Implementation direction

Use Android MediaProjection with user consent for capture, and create a virtual display at the requested output dimensions. Maintain the app image without distortion by default; explicit ratio modes should letterbox/crop only in the stream output, not change the phone UI. Chromecast needs a receiver-compatible live-video transport. The existing Cast Sender SDK media button is not screen mirroring and was replaced in the Aero dashboard with the settings entry point.

Screen capture must use Android's consent flow and stop when the user ends the session. Stream access should be limited to the local Wi-Fi path and protected by a per-session token.
