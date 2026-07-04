# Tackcall

A relative tactical compass for sailboat racing, built as a single-file HTML/JavaScript web app.

**Live app:** hosted via GitHub Pages
**License:** MIT

---

## What is Tackcall?

Tackcall is not a magnetic compass in the traditional sense. Instead of tracking absolute heading against magnetic north, it tracks **session-calibrated mean headings per tack** (starboard / port) and detects **oscillations** (lifts and headers) relative to those means. This relative architecture is the core idea behind the app: because the mean is set fresh each session, absolute magnetometer offset errors cancel out at the moment the mean is set. What matters for tactics is the *change* relative to your own baseline, not the absolute number.

The app is designed to help with the classic racing decision: *am I lifted or headed, and should I tack?*

## Features

- **Per-tack mean heading tracking** (STB / BB) with right-of-way color coding
- **Lift/header detection** relative to session means
- **MountFlow onboarding** — guided calibration flow covering:
  - Platform self-test (iOS vs. Android compass behavior differs; iOS `webkitCompassHeading` is pre-fused by CoreMotion and follows portrait-top as a fixed reference, while Android corrects for `screen.orientation.angle`)
  - Heading offset adjustment (−180°…180°, 5° steps)
  - Tilt quality assessment (3-tier verdict based on tilt angle, sampled at 5 Hz into 5° bins)
- **Portrait/landscape support** with orientation locking (portrait / landscape / auto)
- **Polar performance system**: persisted per-tack means with timestamps, an append-only raw observation log, kernel regression-based P90 polar estimation, a live PERF percentage, and a polar diagram in the debug view
- **iOS Safari compatibility fixes**: capped device pixel ratio, `visualViewport`-aware layout for toolbar collapse, and touch handling tuned to avoid double-tap zoom

## Project structure

| File | Description |
|---|---|
| `index.html` | Classic view (frozen at v11) |
| `mittari.html` | LED-style meter view — actively developed |
| `CHANGELOG.md` | Version history, updated with every change |

Active development happens on the `polar-dev-v1` branch, primarily around polar performance and sensor work.

## Design philosophy

**"Yksinkertainen on kaunista"** — simple is beautiful. Tackcall is deliberately minimal:

- No external libraries, no build pipeline — a single HTML file per view
- Features are added only when they carry clear tactical value; ideas like COG-based mean setting, boat-type presets, or a VMG display have been evaluated and set aside when they added complexity without a clear payoff
- The magnetometer is kept as the primary heading source — the relative architecture is what makes it trustworthy, not extra sensor fusion

## Known constraints

- **Magnetic dip**: at high latitudes (e.g. ~73° in Helsinki), boats heeling past roughly 17° push tilt-compensation errors into a range where heel itself can look like a wind shift on a single phone.
- **Mounting trade-off**: the most magnetically stable phone position (flat, screen up, top toward the bow) is also the least readable one. Any tilt added for readability degrades accuracy — the motivation behind the two-phone architecture explored below.
- **iOS Safari storage limit**: local storage is capped around 5MB, so long-term data relies on exporting via the Web Share API to the Files app rather than accumulating everything in `localStorage`.

## Roadmap

- **Phase 2c — COG + leeway fusion**: correct mean drift over time using GPS course-over-ground with a 60-second window and a `LEEWAY_DEG` constant, with confidence shown as a progressive-opacity gauge. Spec is locked; awaiting test data logged with COG.
- **Two-phone architecture**: one phone mounted flat as a dedicated sensor, streaming raw magnetometer/accelerometer/gyroscope data (via the open-source Sensor Server Android app) to a second phone acting as the display. Blocked today because GitHub Pages serves over HTTPS and browsers won't open a local `ws://` connection from a secure origin (mixed-content policy). The likely path is a thin Expo/React Native WebView wrapper (iOS + Android from one codebase), using an `NSAllowsLocalNetworking` ATS exception on iOS.
- Continued polar system maturation, including Files-app export as the primary long-term storage strategy.

## Contributing / workflow notes

- Development happens mobile-first — commits are made directly from a phone via GitHub's mobile app, so contributions should arrive as complete, ready-to-commit files rather than partial diffs.
- Code comments and project discussion are in Finnish; all user-facing UI strings are in English.
- Version numbers are bumped and `CHANGELOG.md` is updated with every change.
- Run `node --check` on any changed file before committing.

## Credits

Built by Vesa, with UI/meter design input from Joonas Sandholm.

## License

MIT
