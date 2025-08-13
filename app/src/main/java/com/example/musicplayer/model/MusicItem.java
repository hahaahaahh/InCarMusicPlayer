package com.example.musicplayer.model;

public class MusicItem {
    private String title;
    private String artist;
    private String album;
    private String duration; // Format: mm:ss
    private String assetFileName;

    public MusicItem(String title, String artist, String album, String duration, String assetFileName) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.assetFileName = assetFileName;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getDuration() {
        return duration;
    }

    public String getAssetFileName() {
        return assetFileName;
    }

    // Formats seconds as mm:ss
    public static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
