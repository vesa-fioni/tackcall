# Tackcall

A relative tactical compass for sailboat racing, built as a single-file HTML/JavaScript web app.

**Live app:** [tackcall.app](https://tackcall.app)
**License:** MIT

---

## What is Tackcall?

Tackcall is not a magnetic compass in the traditional sense. Instead of tracking absolute heading against magnetic north, it tracks **session-calibrated mean headings per tack** (starboard / port) and detects **oscillations** (lifts and headers) relative to those means. This relative architecture is the core idea behind the app: because the mean is set fresh each session, absolute magnetometer offset errors cancel out at the moment the mean is set. What matters for tactics is the *change* relative to your own baseline, not the absolute number.

The app is designed to help with the classic racing decision: *am I lifted or headed, and should I tack?*

It runs straight in the browser — nothing to install, nothing to log in to, and all data stays on the device.

## Features

- **Per-tack mean heading tracking** (STB / BB) with right-of-way colour coding, and **lift/header detection** relative to the session means.
- **TACK alert** with a settled-latch behaviour, so it fires on a real, stable shift rather than momentary noise.
- **Offshore mode** — heading read from GPS course over ground (COG) instead of the magnetometer, so the phone can be held in hand with no fixed mounting. Below ~3 kn the GPS heading is unreliable, so the display shows `WAITING FOR SPEED`; the main number turns pink/violet so the heading source is obvious at a glance.
- **VMG** — the portion of boat speed actually made good directly to/from the wind, computed from wind angle and GPS speed with no separate wind instrument required.
- **PERF %** — a learned per-boat performance baseline. As you sail, the app compares current speed to the boat's own best in similar conditions and shows a live percentage (values above 100% are normal and expected).
- **Post-sail summary** — speed across wind angles, comparison to your previous sail and full history, a build-up animation of the session, and a heuristic guess of whether a difference was due to wind or to trim/technique. A single tap excludes a bad session from history.
- **Tack & leg analysis** — for each beat leg, the angle actually sailed vs. the session's own best-VMG target (pinching vs. footing) plus VMG efficiency; per-tack cards show how quickly speed recovered. Unrecovered tacks are shown honestly as `—` rather than a guessed number.
- **Auto-mean trust-arc (AMR)** — a confidence indicator around automatic mean handling.
- **Polar performance system** — persisted per-tack means with timestamps, an append-only raw observation log, kernel-regression P90 polar estimation, and a polar diagram in the debug view.
- **MountFlow onboarding** — guided calibration covering:
  - Platform self-test (iOS vs. Android compass behaviour differs; iOS `webkitCompassHeading` is pre-fused by CoreMotion and follows portrait-top as a fixed reference, while Android corrects for `screen.orientation.angle`)
  - Heading offset adjustment (−180°…180°, 5° steps)
  - Heel/tilt quality assessment (3-tier verdict, with advice on whether to relocate the mount)
- **Sunlight-readable duotone UI** — dark, high-contrast, large bright numbers, tacks distinguished by colour (green / red), nothing extra on screen.
- **Portrait/landscape support** with orientation locking (portrait / landscape / auto).
- **Offline-first PWA** — manifest and installable, designed to work on the water with no network.
- **In-app Terms of Use and Privacy Policy** as offline sheets, with a one-time consent overlay and a safety disclaimer.
- **iOS Safari compatibility fixes** — capped device pixel ratio, `visualViewport`-aware layout for toolbar collapse, and touch handling tuned to avoid double-tap zoom.

## Project structure

| File | Description |
|---|---|
| `mittari.html` | The app — LED-style meter view, actively developed on `main` |
| `index.html` | A direct copy of `mittari.html`, served by GitHub Pages |
| `CHANGELOG.md` | Version history, updated with every change |

Development happens on `main`. `index.html` carries no independent logic — it exists only so GitHub Pages has an entry point, and is kept in sync with `mittari.html`.

## Design philosophy

**"Yksinkertainen on kaunista"** — simple is beautiful. Tackcall is deliberately minimal:

- No external libraries, no build pipeline — a single HTML file.
- Changes are **additive and surgical**: new features never break existing ones, and scope stays tight.
- Thresholds are **field-calibrated, not desk-estimated** — provisional values are confirmed against real on-water data before they're trusted.
- The **raw log is append-only** and treated as the source of truth; display-layer fixes never rewrite logged data.
- Features are added only when they carry clear tactical value. Ideas such as COG-based mean setting (circular reasoning, leeway contamination) and boat-type polar presets have been evaluated and set aside when they added complexity without a clear payoff.
- The magnetometer is kept as the primary heading source — the relative architecture is what makes it trustworthy, not extra sensor fusion.

## Privacy & data

- **All data stays on the device.** There is no backend and no account.
- The raw observation log is stored in **IndexedDB** (chosen for capacity and write performance); per-tack means are stored in `localStorage`.
- Any data sharing is strictly **opt-in and user-initiated** — the app never transmits sailing data on its own.

## Known constraints

- **Magnetic dip**: at high latitudes (magnetic inclination ~73° in Helsinki), a boat heeling past roughly 17° pushes tilt-compensation errors into a range where heel itself can look like a wind shift on a single phone.
- **Mounting trade-off**: the most magnetically stable phone position (flat, screen up, top toward the bow) is also the least readable. Any tilt added for readability degrades accuracy — the motivation behind the two-phone architecture below. Offshore mode is the mounting-free alternative.
- **Browser storage eviction**: Safari's 7-day ITP can evict browser storage (IndexedDB and `localStorage` alike), so long-term retention relies on opt-in export rather than assuming stored data persists indefinitely.

## Roadmap

- **Data export / sharing UI** — a voluntary, user-initiated local export, consistent with the on-device privacy model.
- **Two-phone architecture** — one phone mounted flat as a dedicated sensor, streaming raw magnetometer/accelerometer/gyroscope data (via the open-source Sensor Server Android app) to a second phone acting as the display. Blocked today because GitHub Pages serves over HTTPS and browsers won't open a local `ws://` connection from a secure origin (mixed-content policy). The likely path is a thin Expo/React Native WebView wrapper (iOS + Android from one codebase), using an `NSAllowsLocalNetworking` ATS exception on iOS.
- **Continued polar and summary maturation**, with thresholds calibrated against accumulated field data.

## Contributing / workflow notes

- Development is **mobile-first** — commits are made directly from a phone via GitHub's mobile app, so contributions should arrive as complete, ready-to-commit files rather than partial diffs.
- Code comments and project discussion are in Finnish; all user-facing UI strings are in English.
- Version numbers are bumped and `CHANGELOG.md` is updated with every change.
- Run `node --check` on the inline script of any changed file before committing.

## Credits

Built by Vesa ([vesa.lindqvist@fioni.fi](mailto:vesa.lindqvist@fioni.fi)), with UI/meter design input from Joonas Sandholm. Published personally under the MIT Licence.

## License

MIT
