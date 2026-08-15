# BlackBeard for iPhone

BlackBeard is a protected three-site browser for:

- `https://cinejoy.to/`
- `https://aether.bar/`
- `https://popcornmovies.io/`

It starts on a black site-selection screen. The Main Menu button in the bottom
browser bar returns to that screen from any selected site.

## What it blocks

- New browser windows and tabs
- Top-level redirects away from the currently selected site
- Links that try to open another app through a non-HTTPS URL scheme
- JavaScript calls to `window.open()`
- The ad-script domains observed on the supported movie sites during development
- Several common pop-under advertising networks on all three sites

Embedded frames and media requests remain available so third-party video
players can work. The app cannot guarantee that every future advertising domain
will be blocked because these sites and their ad providers can change.

## Build and install

1. Open `BlackBeard.xcodeproj` in Xcode on a Mac.
2. Select the **BlackBeard** project and **BlackBeard** target.
3. Open **Signing & Capabilities** and select your Apple ID team.
4. Connect your iPhone and choose it as the run destination.
5. Press **Run**.
6. If prompted, enable Developer Mode on the iPhone and trust the developer
   profile.

A free Apple ID can sign the app for personal testing, but the installation may
need periodic renewal. A paid Apple Developer account provides longer-lived
signing.

## Test checklist

- Confirm the supplied BlackBeard artwork appears during launch.
- Confirm the app icon and the name beneath it are BlackBeard.
- Open Cinejoy, Aether, and Popcorn and confirm each button reaches the correct
  destination.
- On each site, browse through normal internal links.
- Trigger or encounter a popup and confirm it does not create a new window.
- Confirm an external redirect or app-opening link is blocked.
- Start a video, enter fullscreen, rotate to landscape, then leave fullscreen
  and rotate back to portrait. Confirm the BlackBeard bottom bar hides while
  fullscreen is active and returns after fullscreen closes.
- Tap the grid button in the bottom bar and confirm it returns to the BlackBeard
  site-selection screen.

## Notes

- The app intentionally has no address bar.
- Top-level navigation is restricted to the selected site's root domain and its
  subdomains.
- Embedded video hosts are allowed inside frames, but cannot replace the
  top-level page.
- Site or video-player changes may require small rule adjustments.
