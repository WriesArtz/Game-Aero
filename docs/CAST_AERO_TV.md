# Google Cast / Aero TV integration

## Current sender-side groundwork

The `frutiger-dashboard` branch includes the Google Cast Android Sender SDK, a Cast options provider, and a Cast device-picker control in the Aero dashboard.

At this stage, the sender uses Google's Default Media Receiver (`CC1AD845`). This verifies the sender integration path and device picker, but it **does not mirror Game-Aero's custom dashboard to the TV**.

## Next phase: custom Aero receiver

A dashboard receiver requires a registered Custom Web Receiver application in the Google Cast SDK Developer Console and its application ID. The receiver should be a hosted HTML/JavaScript app that renders a TV-sized Aero interface. Once registered and hosted, replace the default receiver ID in `AeroCastOptionsProvider` and add the custom Cast message channel for dashboard state/game selection.

Phone-to-TV dashboard casting and full phone/game screen mirroring are separate features. This integration is for a custom TV dashboard, not game video mirroring.
