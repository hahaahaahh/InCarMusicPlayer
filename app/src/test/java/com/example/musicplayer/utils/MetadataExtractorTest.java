package com.example.musicplayer.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataExtractorTest {
    @Test
    public void testGetTitleFromFileName() {
        assertEquals("test song", MetadataExtractor.getTitleFromFileName("test_song.mp3"));
        assertEquals("不要说话 陈奕迅", MetadataExtractor.getTitleFromFileName("不要说话-陈奕迅.mp3"));
        assertEquals("Unknown Title", MetadataExtractor.getTitleFromFileName(null));
    }
} 