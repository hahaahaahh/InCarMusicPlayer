package com.example.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.adapter.MusicAdapter;
import com.example.musicplayer.model.MusicItem;
import com.example.musicplayer.utils.MetadataExtractor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MusicPlayerService.OnPlaybackListener {
    private RecyclerView rvPlaylist;
    private MusicAdapter musicAdapter;
    private List<MusicItem> musicList;
    private ImageButton btnPlay, btnNext, btnPrev, btnVolumeUp, btnVolumeDown;
    private SeekBar seekBar;
    private TextView tvCurrentTitle, tvCurrentArtist, tvCurrentAlbum, tvCurrentTime, tvTotalTime;
    private boolean isPlaying = false;
    private int currentSongIndex = 0; // Track current song position
    
    // Service binding
    private MusicPlayerService musicPlayerService;
    private boolean bound = false;
    
    // UI state
    private boolean isSeekBarUserTouching = false;
    
    // Volume control variables
    private AudioManager audioManager;
    private int maxVolume;
    private int currentVolume;
    private Handler volumeToastHandler;
    private Runnable volumeToastRunnable;

    // Service connection
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            try {
                MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
                musicPlayerService = binder.getService();
                bound = true;
                
                // Set up the service with our music list and listener
                if (musicList != null) {
                    musicPlayerService.setMusicList(musicList);
                }
                musicPlayerService.setOnPlaybackListener(MainActivity.this);
                
                // Update UI with current service state
                isPlaying = musicPlayerService.isPlaying();
                currentSongIndex = musicPlayerService.getCurrentSongIndex();
                updatePlayButton();
                updateCurrentSongDisplay();
                if (musicAdapter != null) {
                    musicAdapter.updateCurrentSongIndex(currentSongIndex);
                }
                
                Log.d("MainActivity", "Service connected successfully");
            } catch (Exception e) {
                Log.e("MainActivity", "Error in onServiceConnected: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
            musicPlayerService = null;
            Log.d("MainActivity", "Service disconnected");
        }
    };

    // Helper method to check which drawable resource is being used
    private void logDrawableResource(int resourceId, String resourceName) {
        try {
            android.graphics.drawable.Drawable drawable = getResources().getDrawable(resourceId);
            if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                android.graphics.Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                Log.d("DrawableDebug", resourceName + " - Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                
                // Get the resource entry name to help identify which density folder
                String entryName = getResources().getResourceEntryName(resourceId);
                Log.d("DrawableDebug", resourceName + " - Resource entry: " + entryName);
                
                // Log the density bucket being used
                float densityDpi = getResources().getDisplayMetrics().densityDpi;
                String densityBucket;
                if (densityDpi <= 120) densityBucket = "ldpi";
                else if (densityDpi <= 160) densityBucket = "mdpi";
                else if (densityDpi <= 240) densityBucket = "hdpi";
                else if (densityDpi <= 320) densityBucket = "xhdpi";
                else if (densityDpi <= 480) densityBucket = "xxhdpi";
                else densityBucket = "xxxhdpi";
                
                Log.d("DrawableDebug", resourceName + " - Likely loaded from: drawable-" + densityBucket + "/" + entryName + ".png");
            }
        } catch (Exception e) {
            Log.e("DrawableDebug", "Error loading " + resourceName + ": " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Debug: Log device density information
        float density = getResources().getDisplayMetrics().density;
        float densityDpi = getResources().getDisplayMetrics().densityDpi;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        
        Log.d("DensityDebug", "Screen density: " + density);
        Log.d("DensityDebug", "Screen density DPI: " + densityDpi);
        Log.d("DensityDebug", "Screen resolution: " + screenWidth + "x" + screenHeight);
        
        // Determine which density bucket this device falls into
        String densityBucket;
        if (densityDpi <= 120) densityBucket = "ldpi";
        else if (densityDpi <= 160) densityBucket = "mdpi";
        else if (densityDpi <= 240) densityBucket = "hdpi";
        else if (densityDpi <= 320) densityBucket = "xhdpi";
        else if (densityDpi <= 480) densityBucket = "xxhdpi";
        else densityBucket = "xxxhdpi";
        
        Log.d("DensityDebug", "Density bucket: " + densityBucket);
        Log.d("DensityDebug", "Android will use drawable-" + densityBucket + " resources");

        // Log which drawable resources are being used
        logDrawableResource(R.drawable.background, "background");
        logDrawableResource(R.drawable.left_cover, "left_cover");
        logDrawableResource(R.drawable.right_cover, "right_cover");
        logDrawableResource(R.drawable.volume_increase, "volume_increase");
        logDrawableResource(R.drawable.volume_decrease, "volume_decrease");
        logDrawableResource(R.drawable.thumb, "thumb");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Initialize volume control
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        
        // Initialize volume toast handler
        volumeToastHandler = new Handler(Looper.getMainLooper());
        volumeToastRunnable = new Runnable() {
            @Override
            public void run() {
                // Hide volume toast after 2 seconds
                // This will be used to show volume percentage
            }
        };
        
        // Sync current volume state
        syncVolumeState();

        // Initialize views
        rvPlaylist = findViewById(R.id.rvPlaylist);
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnVolumeUp = findViewById(R.id.btnVolumeUp);
        btnVolumeDown = findViewById(R.id.btnVolumeDown);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTitle = findViewById(R.id.tv_current_title);
        tvCurrentArtist = findViewById(R.id.tv_current_artist);
        tvCurrentAlbum = findViewById(R.id.tv_current_album);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);

        // Bind to MusicPlayerService
        try {
            Intent intent = new Intent(this, MusicPlayerService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e("MainActivity", "Error binding to service: " + e.getMessage());
            e.printStackTrace();
        }

        // Setup SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bound && musicPlayerService != null) {
                    tvCurrentTime.setText(formatTime(progress / 1000));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekBarUserTouching = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekBarUserTouching = false;
                if (bound && musicPlayerService != null) {
                    musicPlayerService.seekTo(seekBar.getProgress());
                }
            }
        });

        // Setup RecyclerView
        try {
            musicList = getMusicListFromAssets();
            Log.d("MusicPlayer", "Playlist created with " + musicList.size() + " songs");
            musicAdapter = new MusicAdapter(this, musicList);
            rvPlaylist.setLayoutManager(new LinearLayoutManager(this));
            rvPlaylist.setAdapter(musicAdapter);
            musicAdapter.updateCurrentSongIndex(currentSongIndex); // Set initial highlighting
        } catch (Exception e) {
            Log.e("MainActivity", "Error setting up RecyclerView: " + e.getMessage());
            e.printStackTrace();
            // Create empty list to prevent crashes
            musicList = new ArrayList<>();
            musicAdapter = new MusicAdapter(this, musicList);
            rvPlaylist.setLayoutManager(new LinearLayoutManager(this));
            rvPlaylist.setAdapter(musicAdapter);
        }

        // Make playlist items clickable
        musicAdapter.setOnItemClickListener(new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                try {
                    if (position != currentSongIndex && bound && musicPlayerService != null) {
                        currentSongIndex = position;
                        musicPlayerService.playSongAtIndex(currentSongIndex);
                        musicAdapter.updateCurrentSongIndex(currentSongIndex);
                        updateCurrentSongDisplay();
                        scrollToCurrentSong();
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in playlist item click: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        // Set initial song display (first song)
        updateCurrentSongDisplay();
        
        // Set initial time display
        updateTimeDisplay();

        // Setup button listeners
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (bound && musicPlayerService != null) {
                        musicPlayerService.play();
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in play button click: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (bound && musicPlayerService != null) {
                        musicPlayerService.nextTrack();
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in next button click: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (bound && musicPlayerService != null) {
                        musicPlayerService.prevTrack();
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in prev button click: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        btnVolumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseVolume();
            }
        });
        btnVolumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseVolume();
            }
        });


    }

    // Service callback methods
    @Override
    public void onPlaybackProgress(int currentPosition, int duration) {
        try {
            if (!isSeekBarUserTouching) {
                seekBar.setMax(duration);
                seekBar.setProgress(currentPosition);
                tvCurrentTime.setText(formatTime(currentPosition / 1000));
                tvTotalTime.setText(formatTime(duration / 1000));
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onPlaybackProgress: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean playing) {
        try {
            isPlaying = playing;
            updatePlayButton();
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onPlaybackStateChanged: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onSongChanged(int songIndex) {
        try {
            currentSongIndex = songIndex;
            updateCurrentSongDisplay();
            if (musicAdapter != null) {
                musicAdapter.updateCurrentSongIndex(currentSongIndex);
            }
            scrollToCurrentSong();
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onSongChanged: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePlayButton() {
        if (isPlaying) {
            btnPlay.setImageResource(R.drawable.ic_pause_circle);
        } else {
            btnPlay.setImageResource(R.drawable.ic_play_circle);
        }
    }

    // Update the current song display on the left panel
    private void updateCurrentSongDisplay() {
        try {
            if (musicList != null && !musicList.isEmpty() && currentSongIndex < musicList.size()) {
                MusicItem currentSong = musicList.get(currentSongIndex);
                tvCurrentTitle.setText(currentSong.getTitle());
                tvCurrentArtist.setText(currentSong.getArtist());
                tvCurrentAlbum.setText(currentSong.getAlbum());
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error in updateCurrentSongDisplay: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Generate playlist based on available audio files in assets
    private List<MusicItem> getMusicListFromAssets() {
        List<MusicItem> list = new ArrayList<>();
        try {
            String[] files = getAssets().list("");
            if (files != null) {
                for (String file : files) {
                    // Only support well-supported audio formats
                    if (isAudioFormatSupported(file)) {
                        // Extract metadata from the audio file
                        MetadataExtractor.AudioMetadata metadata = MetadataExtractor.extractMetadata(this, file);
                        
                        // Use extracted metadata or fallback to defaults
                        String title = metadata.title != null ? metadata.title : "Unknown Title";
                        String artist = metadata.artist != null ? metadata.artist : "Unknown Artist";
                        String album = metadata.album != null ? metadata.album : "Unknown Album";
                        String duration = getAudioDurationStringFromAsset(file);
                        
                        list.add(new MusicItem(title, artist, album, duration, file));
                        Log.d("MusicPlayer", "Found supported audio file: " + file + 
                              " - Title: " + title + ", Artist: " + artist + ", Album: " + album);
                    } else if (file.toLowerCase().endsWith(".flac") || file.toLowerCase().endsWith(".ogg") || 
                               file.toLowerCase().endsWith(".wma")) {
                        Log.w("MusicPlayer", "Skipping unsupported audio format: " + file);
                    }
                }
            }
            Log.d("MusicPlayer", "Total supported audio files found: " + list.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Helper to get duration string from asset file
    private String getAudioDurationStringFromAsset(String assetFileName) {
        int durationMs = 0;
        try {
            AssetFileDescriptor afd = getAssets().openFd(assetFileName);
            android.media.MediaPlayer tempPlayer = new android.media.MediaPlayer();
            tempPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            tempPlayer.prepare();
            durationMs = tempPlayer.getDuration();
            tempPlayer.release();
            afd.close();
            Log.d("MusicPlayer", "Duration for " + assetFileName + ": " + durationMs + "ms");
        } catch (IOException e) {
            Log.e("MusicPlayer", "Error getting duration for " + assetFileName + ": " + e.getMessage());
            e.printStackTrace();
            return "00:00"; // Return default duration for unsupported formats
        } catch (Exception e) {
            Log.e("MusicPlayer", "Unexpected error getting duration for " + assetFileName + ": " + e.getMessage());
            e.printStackTrace();
            return "00:00"; // Return default duration for unsupported formats
        }
        int durationSec = durationMs / 1000;
        return MusicItem.formatDuration(durationSec);
    }



    // Check if audio format is supported by MediaPlayer
    private boolean isAudioFormatSupported(String fileName) {
        if (fileName == null) return false;
        String lowerFile = fileName.toLowerCase();
        return lowerFile.endsWith(".mp3") || lowerFile.endsWith(".m4a") || 
               lowerFile.endsWith(".aac") || lowerFile.endsWith(".wav");
    }



    // Scroll to current song with smooth animation
    private void scrollToCurrentSong() {
        if (rvPlaylist != null && currentSongIndex < musicList.size()) {
            rvPlaylist.smoothScrollToPosition(currentSongIndex);
        }
    }

    // Increase volume and show percentage
    private void increaseVolume() {
        if (currentVolume < maxVolume) {
            currentVolume++;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            showVolumePercentage();
        }
        Log.d("MusicPlayer", "Volume up: " + currentVolume + "/" + maxVolume);
    }

    // Decrease volume and show percentage
    private void decreaseVolume() {
        if (currentVolume > 0) {
            currentVolume--;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            showVolumePercentage();
        }
        Log.d("MusicPlayer", "Volume down: " + currentVolume + "/" + maxVolume);
    }
    
    // Show volume percentage as toast
    private void showVolumePercentage() {
        int percentage = (int) ((float) currentVolume / maxVolume * 100);
        String volumeText = "Volume: " + percentage + "%";
        
        // Cancel any existing toast
        volumeToastHandler.removeCallbacks(volumeToastRunnable);
        
        // Show new toast
        Toast.makeText(this, volumeText, Toast.LENGTH_SHORT).show();
        
        // Auto-hide after 2 seconds
        volumeToastHandler.postDelayed(volumeToastRunnable, 2000);
    }
    
    // Sync current volume state with system
    private void syncVolumeState() {
        if (audioManager != null) {
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncVolumeState();
    }

    // Update time display
    private void updateTimeDisplay() {
        if (bound && musicPlayerService != null) {
            tvCurrentTime.setText(formatTime(musicPlayerService.getCurrentPosition() / 1000));
            tvTotalTime.setText(formatTime(musicPlayerService.getDuration() / 1000));
        }
    }

    // Update seek bar progress
    private void updateSeekBarProgress() {
        if (!isSeekBarUserTouching && bound && musicPlayerService != null) {
            seekBar.setProgress(musicPlayerService.getCurrentPosition());
        }
    }

    // Format seconds to mm:ss
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    @Override
    protected void onDestroy() {
        if (bound) {
            unbindService(connection);
            bound = false;
        }
        if (volumeToastHandler != null) {
            volumeToastHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
}

// Create a RecyclerView Adapter in Java that displays a playlist.

// Each item includes a default album image (right_cover.png), a placeholder song title, artist name, and a duration of "00:00".

// Use a ViewHolder class named MusicViewHolder.

// The item layout is item_music.xml.

// Do not implement playback logic, just display static content for now.

// Update MainActivity.java to wire up the playback control buttons: // - btnPlay should toggle between pause and resume using existing playback code // - btnNext and btnPrev should trigger next and previous song actions // - btnVolumeUp and btnVolumeDown control volume levels // Use drawables: ic_pause.png, ic_play.png, ic_next.png, ic_prev.png, ic_volume_increase.png, ic_volume_decrease.png // Use existing methods: play(), pause(), nextTrack(), prevTrack(), increaseVolume(), decreaseVolume() // Also set appropriate contentDescription for accessibility

// Add click listeners to btnPlay, btnNext, and btnPrev // Log a message or show a Toast for each action as a placeholder // e.g., "Playing track", "Next track", "Previous track"

// Create a layout file activity_main.xml: // - Left side: playback controls (buttons for play/pause, next, previous, volume up/down) // - Right side: playlist RecyclerView // - Background image: background.png // - SeekBar with thumb icon

// Create a data class MusicItem.java: // - Fields: title (String), artist (String), duration (String) // - Constructor: initialize all fields // - Add a method to format duration as mm:ss

// Create a layout file item_music.xml: // - Default album image (right_cover.png) // - TextViews for title, artist, and duration // - Horizontal layout

// Create a RecyclerView Adapter MusicAdapter.java: // - Bind MusicItem data to item_music.xml // - Use MusicViewHolder to hold views // - Populate RecyclerView with dummy data

// Add dummy data to MainActivity.java: // - Create a list of MusicItem objects // - Populate RecyclerView with MusicAdapter // - Use placeholder data for title, artist, and duration

// Add accessibility features: // - Set contentDescription for all buttons and images // - Ensure RecyclerView items are focusable

// Add testing logic: // - Log actions for button clicks // - Display Toast messages for user interactions

// Optimize layout for large desktop screens: // - Use ConstraintLayout for activity_main.xml // - Adjust margins and paddings for better spacing // - Ensure RecyclerView scrolls smoothly

// Add comments to explain each step and method

// Test the app on an emulator or physical device // - Verify layout appearance // - Check button click responses // - Ensure RecyclerView displays all items correctly

// Refactor code for readability and maintainability // - Use meaningful variable names // - Add comments for clarity // - Organize methods logically

// Prepare for future playback implementation: // - Add placeholders for play(), pause(), nextTrack(), prevTrack(), increaseVolume(), decreaseVolume() // - Ensure methods are ready to integrate with audio playback logic

// Document the app structure and functionality: // - Create a README file // - Include setup instructions // - Describe app features and limitations // - Provide screenshots of the app interface

// Resource Files: // - Playlist cover (each item): right_cover.png // - Currently playing cover: left_cover.png // - Volume control: volume_increase.png, volume_decrease.png // - Background image: background.png // - Play/pause, nav: ic_play.png, ic_pause.png, ic_next.png, ic_prev.png // - SeekBar thumb: thumb.png // - Stored in res/drawable/

// Adjust layout orientation: // - Ensure all layouts are horizontal // - Update activity_main.xml to use horizontal orientation // - Modify item_music.xml to align elements horizontally // - Test layout appearance after changes