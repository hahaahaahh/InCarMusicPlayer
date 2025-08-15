# MusicPlayer

A simple Android music player app built with Java. Plays audio files from the app's assets, displays metadata, and provides a modern, accessible UI with playlist and playback controls.

## Features

- Play, pause, seek, next/previous track controls
- Playlist view with current track highlighting (RecyclerView)
- Real-time seek bar and time display
- Volume controls using AudioManager
- Extracts and displays audio metadata (title, artist, album) using MediaMetadataRetriever and mp3agic
- Landscape-optimized, accessible UI with multi-density drawable support
- Unit and instrumentation tests (JUnit, AndroidX Test, Espresso, Mockito)

## Tech Stack

**Languages:**  
Java, XML

**Frameworks/Tools:**  
Android Studio (IDE); Android SDK APIs (MediaPlayer, MediaMetadataRetriever); AndroidX (AppCompat, Material, ConstraintLayout, RecyclerView); Gradle + AGP (Version Catalog), ProGuard/R8; JUnit4, AndroidX Test, Espresso, Mockito; mp3agic; Android SDK 24–35; Java 11

## Getting Started

### Prerequisites

- Android Studio (latest recommended)
- Android SDK 24 or higher
- Java 11

### Build & Run

1. Clone the repository:
    ```sh
    git clone https://github.com/yourusername/MusicPlayer.git
    cd MusicPlayer
    ```
2. Open the project in Android Studio.
3. Build the project (Gradle sync will download dependencies).
4. Run on an emulator or physical device (landscape orientation recommended).

### Project Structure
app/
  src/
    main/
      java/com/example/musicplayer/
        MainActivity.java
        MusicPlayerService.java
        adapter/
        model/
        utils/
      res/
        layout/
        drawable/
        values/
      assets/
      AndroidManifest.xml
    androidTest/
    test/
  build.gradle

### Testing

- Unit tests:  
  Run with `./gradlew test`
- Instrumentation tests:  
  Run with `./gradlew connectedAndroidTest` or via Android Studio's test runner

## License

MIT License. See [LICENSE](LICENSE) for details.

## Credits

- [mp3agic](https://github.com/mpatric/mp3agic) for MP3 metadata extraction
- AndroidX, Material Components

---

*Feel free to fork, contribute, or open issues!*
