# standard (Android Kotlin music app)

`standard` is a Kotlin + Jetpack Compose Android music app starter with features typically expected in a modern demo player:

- Library view with curated tracks
- Stream playback with ExoPlayer
- Search by title, artist, or mood
- Favorites section
- Bottom now-playing bar with play/pause

## Run

1. Open in Android Studio / Android IDE.
2. Let Gradle sync.
3. Build with the included wrapper: `./gradlew assembleDebug` (or run the `app` configuration).

> If you see `./gradlew: No such file or directory`, pull the latest code so `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` are present.
>
> Use JDK 17 for Android Gradle Plugin compatibility.

The sample tracks stream from SoundHelix over the network.

## Repository note

This repository intentionally keeps only text files. The binary `gradle-wrapper.jar` is excluded by request.

