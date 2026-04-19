package com.xiaowei.music;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class MusicDetectorService extends Service {
    private AudioManager audioManager;
    private Handler handler;
    private Runnable musicCheckRunnable;
    private boolean isMusicPlaying = false;
    private MusicDetectorListener listener;
    
    public interface MusicDetectorListener {
        void onMusicPlayingStateChanged(boolean isPlaying);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        handler = new Handler();
        
        musicCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkMusicStatus();
                handler.postDelayed(this, 500); 
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.post(musicCheckRunnable);
        return START_STICKY;
    }
    
    private void checkMusicStatus() {
        boolean currentlyPlaying = audioManager.isMusicActive();
        
        if (currentlyPlaying != isMusicPlaying) {
            isMusicPlaying = currentlyPlaying;
            if (listener != null) {
                listener.onMusicPlayingStateChanged(isMusicPlaying);
            }
        }
    }
    
    public void setListener(MusicDetectorListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(musicCheckRunnable);
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
