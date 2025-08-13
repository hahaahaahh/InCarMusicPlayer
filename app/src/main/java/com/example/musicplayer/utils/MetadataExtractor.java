package com.example.musicplayer.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import java.io.IOException;
import java.io.InputStream;

public class MetadataExtractor {
    
    public static class AudioMetadata {
        public String title;
        public String artist;
        public String album;
        
        public AudioMetadata(String title, String artist, String album) {
            this.title = title != null ? title.trim() : null;
            this.artist = artist != null ? artist.trim() : null;
            this.album = album != null ? album.trim() : null;
        }
    }
    
    public static AudioMetadata extractMetadata(Context context, String assetFileName) {
        String lowerFileName = assetFileName.toLowerCase();
        
        if (lowerFileName.endsWith(".mp3")) {
            return extractMp3Metadata(context, assetFileName);
        } else {
            return extractGenericMetadata(context, assetFileName);
        }
    }
    
    private static AudioMetadata extractMp3Metadata(Context context, String assetFileName) {
        try {
            // Create a temporary file from the asset
            java.io.File tempFile = java.io.File.createTempFile("temp_mp3", ".mp3", context.getCacheDir());
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
            
            // Copy asset to temporary file
            InputStream inputStream = context.getAssets().open(assetFileName);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
            
            // Create Mp3File from temporary file
            Mp3File mp3File = new Mp3File(tempFile.getAbsolutePath());
            
            String title = null;
            String artist = null;
            String album = null;
            
            if (mp3File.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3File.getId3v2Tag();
                title = id3v2Tag.getTitle();
                artist = id3v2Tag.getArtist();
                album = id3v2Tag.getAlbum();
            } else if (mp3File.hasId3v1Tag()) {
                ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                title = id3v1Tag.getTitle();
                artist = id3v1Tag.getArtist();
                album = id3v1Tag.getAlbum();
            }
            
            // Clean up temporary file
            tempFile.delete();
            
            // If no metadata found, fallback to filename
            if (isEmpty(title)) {
                title = getTitleFromFileName(assetFileName);
            }
            
            Log.d("MetadataExtractor", "MP3 metadata for " + assetFileName + 
                  " - Title: " + title + ", Artist: " + artist + ", Album: " + album);
            
            return new AudioMetadata(title, artist, album);
            
        } catch (IOException | UnsupportedTagException | InvalidDataException e) {
            Log.w("MetadataExtractor", "Error extracting MP3 metadata for " + assetFileName + ": " + e.getMessage());
            // Fallback to generic method
            return extractGenericMetadata(context, assetFileName);
        }
    }
    
    private static AudioMetadata extractGenericMetadata(Context context, String assetFileName) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context.getAssets().openFd(assetFileName).getFileDescriptor());
            
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            
            retriever.release();
            
            // If no metadata found, fallback to filename
            if (isEmpty(title)) {
                title = getTitleFromFileName(assetFileName);
            }
            
            Log.d("MetadataExtractor", "Generic metadata for " + assetFileName + 
                  " - Title: " + title + ", Artist: " + artist + ", Album: " + album);
            
            return new AudioMetadata(title, artist, album);
            
        } catch (Exception e) {
            Log.w("MetadataExtractor", "Error extracting generic metadata for " + assetFileName + ": " + e.getMessage());
            // Final fallback - use filename as title
            String title = getTitleFromFileName(assetFileName);
            return new AudioMetadata(title, null, null);
        }
    }
    
    public static String getTitleFromFileName(String fileName) {
        if (fileName == null) return "Unknown Title";
        int dot = fileName.lastIndexOf('.');
        String name = (dot > 0) ? fileName.substring(0, dot) : fileName;
        return name.replace('_', ' ').replace('-', ' ');
    }
    
    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
} 