package com.example.musicplayer.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class MusicItemTest {
    @Test
    public void testFormatDuration() {
        assertEquals("01:30", MusicItem.formatDuration(90));
        assertEquals("00:00", MusicItem.formatDuration(0));
        assertEquals("10:05", MusicItem.formatDuration(605));
    }

    @Test
    public void testGetters() {
        MusicItem item = new MusicItem("Song", "Artist", "Album", "03:20", "song.mp3");
        assertEquals("Song", item.getTitle());
        assertEquals("Artist", item.getArtist());
        assertEquals("Album", item.getAlbum());
        assertEquals("03:20", item.getDuration());
        assertEquals("song.mp3", item.getAssetFileName());
    }
} 