# BlackBeard for Android

BlackBeard is a protected three-site Android WebView browser for:

- `https://cinejoy.to/`
- `https://aether.bar/`
- `https://popcornmovies.io/`

It starts on the BlackBeard site-selection screen. The Home button in the
bottom browser bar returns to that screen.

## Build

1. Open this folder in Android Studio.
2. Allow the Gradle project to sync.
3. Select the `app` configuration.
4. Choose an Android device running Android 7.0 or newer.
5. Press **Run**.

From a terminal, run `./gradlew assembleDebug`. The APK is written to
`app/build/outputs/apk/debug/app-debug.apk`.

The packaged device-test APK is one folder above this project:
`../BlackBeard-Android-Test.apk`. Its application ID is
`earth.degenerate.blackbeard`.

## Protection behavior

- Blocks new browser windows and hostile tabs.
- Restricts top-level navigation to the currently selected site's root domain
  and subdomains.
- Blocks non-HTTPS app-opening URLs at the top level.
- Replaces JavaScript `window.open()` and removes `_blank` link targets.
- Blocks known ad-network hosts used by the supported movie sites.
- Leaves embedded frames and media requests available for video playback.

## Android test checklist

- Confirm the BlackBeard icon, name, and supplied loading artwork.
- Confirm Cinejoy, Aether, and Popcorn open the correct destinations.
- Browse normal internal links on all three sites.
- Confirm popups, hostile tabs, redirects, and app-opening links are blocked.
- Start a video, enter fullscreen, rotate to landscape, leave fullscreen, and
  rotate back to portrait.
- Confirm the BlackBeard toolbar disappears in fullscreen and returns afterward.
- Confirm the Home button returns to the site-selection screen.
- Try Android's system Cast or screen-mirroring control with a compatible TV.

## Limitations

The websites and advertising providers can change without notice. New ad hosts
may require rule updates. Direct Chromecast media casting is not included;
Android system screen mirroring remains available when supported by the phone,
TV, video player, and content protection.
