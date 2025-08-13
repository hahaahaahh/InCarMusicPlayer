package com.example.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.content.res.AssetFileDescriptor;
import java.io.IOException;
import java.util.List;
import com.example.musicplayer.model.MusicItem;

public class MusicPlayerService extends Service {
    private static final String TAG = "MusicPlayerService";
    
    private final IBinder binder = new MusicPlayerBinder();
    private MediaPlayer mediaPlayer;
    private Handler progressHandler;
    private Runnable progressRunnable;
    private boolean isPlaying = false;
    private int currentSongIndex = 0;
    private List<MusicItem> musicList;
    private OnPlaybackListener playbackListener;
    
    // Interface for communication with MainActivity
    public interface OnPlaybackListener {
        void onPlaybackProgress(int currentPosition, int duration);
        void onPlaybackStateChanged(boolean isPlaying);
        void onSongChanged(int songIndex);
    }
    
    public class MusicPlayerBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        mediaPlayer = new MediaPlayer();
        progressHandler = new Handler(Looper.getMainLooper());
        
        // Setup progress tracking
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    int current = mediaPlayer.getCurrentPosition();
                    int total = mediaPlayer.getDuration();
                    if (playbackListener != null) {
                        playbackListener.onPlaybackProgress(current, total);
                    }
                }
                progressHandler.postDelayed(this, 500);
            }
        };
        
        // Setup completion listener
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextTrack();
            }
        });
    }
    
    public void setMusicList(List<MusicItem> musicList) {
        this.musicList = musicList;
    }
    
    public void setOnPlaybackListener(OnPlaybackListener listener) {
        this.playbackListener = listener;
    }
    
    public void play() {
        if (musicList == null || musicList.isEmpty()) {
            Log.e(TAG, "No music list available");
            return;
        }

        if (mediaPlayer != null && !isPlaying && mediaPlayer.getCurrentPosition() > 0) {
            // Resume from paused position
            mediaPlayer.start();
            isPlaying = true;
            progressHandler.post(progressRunnable);
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(true);
            }
            Log.d(TAG, "Resuming playback");
            return;
        }

        if (isPlaying) {
            pause();
            return;
        }

        if (currentSongIndex >= musicList.size()) {
            currentSongIndex = 0;
        }

        playSong(musicList.get(currentSongIndex).getAssetFileName());
    }
    
    public void playSong(String assetFileName) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            
            mediaPlayer.reset();
            AssetFileDescriptor afd = getAssets().openFd(assetFileName);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            
            // Start progress tracking
            progressHandler.post(progressRunnable);
            
            // Notify activity
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(true);
                playbackListener.onSongChanged(currentSongIndex);
            }
            
            Log.d(TAG, "Playing: " + assetFileName);
            
        } catch (IOException e) {
            Log.e(TAG, "Error playing song: " + assetFileName, e);
            // Try next song if current one fails
            nextTrack();
        }
    }
    
    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            
            // Stop progress tracking
            progressHandler.removeCallbacks(progressRunnable);
            
            if (playbackListener != null) {
                playbackListener.onPlaybackStateChanged(false);
            }
        }
    }
    
    public void nextTrack() {
        if (musicList == null || musicList.isEmpty()) return;
        
        currentSongIndex = (currentSongIndex + 1) % musicList.size();
        playSong(musicList.get(currentSongIndex).getAssetFileName());
    }
    
    public void prevTrack() {
        if (musicList == null || musicList.isEmpty()) return;
        
        currentSongIndex = (currentSongIndex - 1 + musicList.size()) % musicList.size();
        playSong(musicList.get(currentSongIndex).getAssetFileName());
    }
    
    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }
    
    public void playSongAtIndex(int index) {
        if (musicList == null || index < 0 || index >= musicList.size()) return;
        
        currentSongIndex = index;
        playSong(musicList.get(currentSongIndex).getAssetFileName());
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public int getCurrentSongIndex() {
        return currentSongIndex;
    }
    
    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }
    
    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        
        if (progressHandler != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
        
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
} 