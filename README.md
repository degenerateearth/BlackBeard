# BlackBeard

BlackBeard is an open-source browser utility for iPhone and Android. It opens
three supported websites in a protected, app-contained web view:

- [Cineby](https://cineby.at)
- [Aether](https://aether.bar/)
- [Popcorn](https://popcornmovies.io/)

The black landing screen lets the user choose a site and return to that menu
at any time. The supplied BlackBeard artwork is used for the app icon and
loading screen on both platforms.

## Project contents

- `ios/BlackBeard` — complete Swift/Xcode source for iPhone.
- `android/BlackBeard` — complete Java/Gradle source for Android.
- `android/BlackBeard-Android-Test.apk` — installable Android debug build.

## Protection behavior

BlackBeard preserves normal navigation and embedded media while:

- preventing unwanted new windows, tabs, and JavaScript popups;
- suppressing embedded ad boxes observed on Aether and Popcorn;
- blocking top-level redirects away from the selected website;
- preventing top-level links from unexpectedly opening other apps;
- blocking the known ad and pop-under hosts inherited from the Cineby build;
- keeping third-party media frames available for video playback; and
- hiding the BlackBeard navigation bar during fullscreen and active inline video.

Each selected site is restricted to its root domain and subdomains at the top
level. The user can return to the BlackBeard site-selection screen from the
browser toolbar.

## Build the iPhone app

1. Open `ios/BlackBeard/BlackBeard.xcodeproj` in Xcode.
2. Select the **BlackBeard** target.
3. Under **Signing & Capabilities**, choose your own Apple development team.
4. Connect an iPhone, select it as the run destination, and press **Run**.

See `ios/BlackBeard/README.md` for the complete iPhone testing checklist and
personal-device signing notes.

## Build the Android app

Android 7.0 or newer is required.

1. Open `android/BlackBeard` in Android Studio and allow Gradle to sync.
2. Choose an Android device and run the `app` configuration.

Alternatively, from `android/BlackBeard`, run:

```sh
./gradlew assembleDebug
```

The resulting APK is written to
`app/build/outputs/apk/debug/app-debug.apk`. A prebuilt debug APK for device
testing is included at `android/BlackBeard-Android-Test.apk`.

See `android/BlackBeard/README.md` for the full Android test checklist.

## Known limitations

- Supported websites, embedded video players, redirects, and advertising hosts
  can change without notice and may require future rule updates.
- Some video providers may prevent playback or mirroring because of their own
  player behavior or content-protection rules.
- AirPlay behavior depends on the website and its player.
- Direct Chromecast media casting is not implemented. Android system screen
  mirroring may work when supported by the phone, television, player, and
  content.
- The included Android APK is a debug build intended for testing. Production
  distribution requires a separately managed release signing key.

## Disclaimer

BlackBeard is an independent open-source browser utility. It does not own,
operate, host, provide, endorse, or control the third-party websites available
through the app or any content those websites make available. Each website and
its content remain subject to its owners' terms, policies, availability, and
applicable law.
