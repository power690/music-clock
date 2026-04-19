package com.xiaowei.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

 
public class MusicNotificationService extends NotificationListenerService {

    
    public static final String ACTION_MUSIC_UPDATE = "com.xiaowei.music.MUSIC_UPDATE";
    public static final String ACTION_REFRESH_REQUEST = "com.xiaowei.music.REFRESH_REQUEST";
    public static final String ACTION_SERVICE_RESTART = "com.xiaowei.music.SERVICE_RESTART";
    public static final String ACTION_APP_FOREGROUND = "com.xiaowei.music.APP_FOREGROUND";
    public static final String ACTION_APP_BACKGROUND = "com.xiaowei.music.APP_BACKGROUND";

    
    public static final String ACTION_SHOW_LOCAL = "com.xiaowei.music.SHOW_LOCAL";
    public static final String ACTION_LOCAL_PLAY = "com.xiaowei.music.LOCAL_PLAY";
    public static final String ACTION_LOCAL_PAUSE = "com.xiaowei.music.LOCAL_PAUSE";
    public static final String ACTION_LOCAL_NEXT = "com.xiaowei.music.LOCAL_NEXT";
    public static final String ACTION_LOCAL_PREV = "com.xiaowei.music.LOCAL_PREV";
    public static final String ACTION_LOCAL_SEEK = "com.xiaowei.music.LOCAL_SEEK";
    
    
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_IS_PLAYING = "isPlaying";
    public static final String EXTRA_ALBUM = "album";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_POSITION = "position";
    
    
    public static Bitmap currentAlbumBitmap = null;
    private static final int MAX_BITMAP_SIZE = 512;

    private static final String TAG = "MusicNotifService";
    private static final String NOTIFICATION_CHANNEL_ID = "music_listener_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1001;
    private static final String KUGOU_LITE_PACKAGE = "com.kugou.android.lite";

    private Handler handler;
    private BroadcastReceiver refreshReceiver;
    private BroadcastReceiver restartReceiver;
    private BroadcastReceiver localControlReceiver;
    private BroadcastReceiver appStateReceiver;
    
    
    private String mLastTitle = "";
    private String mLastArtist = "";
    private boolean mLastIsPlaying = false;
    private Bitmap mLastBitmap = null; 
    private long mLastDuration = 0;
    private long mLastPosition = 0;

    
    private ExecutorService networkExecutor;
    private String lastFetchedSongKey = "";
    private boolean isFetching = false;

    
    private MediaSessionManager mediaSessionManager;
    private Handler mediaSessionHandler;
    private Runnable mediaSessionRunnable;
    private static final long MEDIA_SESSION_UPDATE_INTERVAL = 1000;
    private MediaSessionCompat mLocalSession;

    
    private boolean isLocalMode = false; 
    private boolean isAppInForeground = true; 

    
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "音乐通知服务已创建");
        networkExecutor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
        initLocalMediaSession();
        
        handler = new Handler();
        mediaSessionHandler = new Handler();

        
        initWakeLock();

        setupReceivers();
        startMediaSessionListening();
    }

     
    private void initWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicClock:ServiceWakeLock");
                mWakeLock.setReferenceCounted(false); 
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化唤醒锁失败", e);
        }
    }

     
    private void acquireWakeLock() {
        try {
            if (mWakeLock != null && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
                Log.d(TAG, "Service WakeLock acquired");
            }
        } catch (Exception e) {
            Log.e(TAG, "获取Service唤醒锁失败", e);
        }
    }

     
    private void releaseWakeLock() {
        try {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
                Log.d(TAG, "Service WakeLock released");
            }
        } catch (Exception e) {
            Log.e(TAG, "释放Service唤醒锁失败", e);
        }
    }

     
    private Notification createDefaultNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("音乐服务")
                .setContentText("正在运行...")
                .setSmallIcon(R.drawable.ic_music_note)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true);
        return builder.build();
    }

    private Bitmap smartCropBitmap(Bitmap original) {
        if (original == null) return null;
        int width = original.getWidth();
        int height = original.getHeight();
        int[] pixels = new int[width * height];
        original.getPixels(pixels, 0, width, 0, 0, width, height);

        int top = 0;
        int bottom = height;
        int left = 0;
        int right = width;
        
        final int THRESHOLD = 30;

        for (int y = 0; y < height; y++) {
            boolean rowHasData = false;
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                if (Color.red(pixel) > THRESHOLD || Color.green(pixel) > THRESHOLD || Color.blue(pixel) > THRESHOLD) {
                    rowHasData = true;
                    break;
                }
            }
            if (rowHasData) {
                top = y;
                break;
            }
        }

        for (int y = height - 1; y >= 0; y--) {
            boolean rowHasData = false;
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                if (Color.red(pixel) > THRESHOLD || Color.green(pixel) > THRESHOLD || Color.blue(pixel) > THRESHOLD) {
                    rowHasData = true;
                    break;
                }
            }
            if (rowHasData) {
                bottom = y + 1;
                break;
            }
        }

        for (int x = 0; x < width; x++) {
            boolean colHasData = false;
            for (int y = top; y < bottom; y++) {
                int pixel = pixels[y * width + x];
                if (Color.red(pixel) > THRESHOLD || Color.green(pixel) > THRESHOLD || Color.blue(pixel) > THRESHOLD) {
                    colHasData = true;
                    break;
                }
            }
            if (colHasData) {
                left = x;
                break;
            }
        }

        for (int x = width - 1; x >= 0; x--) {
            boolean colHasData = false;
            for (int y = top; y < bottom; y++) {
                int pixel = pixels[y * width + x];
                if (Color.red(pixel) > THRESHOLD || Color.green(pixel) > THRESHOLD || Color.blue(pixel) > THRESHOLD) {
                    colHasData = true;
                    break;
                }
            }
            if (colHasData) {
                right = x + 1;
                break;
            }
        }

        int newWidth = right - left;
        int newHeight = bottom - top;

        if (newWidth <= 0 || newHeight <= 0) return original;
        if (newWidth > width - 5 && newHeight > height - 5) {
            return original;
        }

        try {
            return Bitmap.createBitmap(original, left, top, newWidth, newHeight);
        } catch (Exception e) {
            return original;
        }
    }

    private Bitmap optimizeBitmapSize(Bitmap original) {
        try {
            if (original == null) return null;
            int width = original.getWidth();
            int height = original.getHeight();
            
            if (width > MAX_BITMAP_SIZE || height > MAX_BITMAP_SIZE) {
                float scaleFactor = Math.min(
                    (float) MAX_BITMAP_SIZE / width,
                    (float) MAX_BITMAP_SIZE / height
                );
                int newWidth = (int) (width * scaleFactor);
                int newHeight = (int) (height * scaleFactor);
                
                return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
            }
            
            return original;
        } catch (Exception e) {
            return original;
        }
    }

    private void initLocalMediaSession() {
        ComponentName receiverComponent = new ComponentName(this, MusicNotificationService.class);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(receiverComponent);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent mbrIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, flags);
        mLocalSession = new MediaSessionCompat(this, "LocalMusicSession", receiverComponent, mbrIntent);
        
        mLocalSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | 
                               MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
                               
        mLocalSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() { sendBroadcastToActivity(ACTION_LOCAL_PLAY); }
            @Override
            public void onPause() { sendBroadcastToActivity(ACTION_LOCAL_PAUSE); }
            @Override
            public void onSkipToNext() { sendBroadcastToActivity(ACTION_LOCAL_NEXT); }
            @Override
            public void onSkipToPrevious() { sendBroadcastToActivity(ACTION_LOCAL_PREV); }
            @Override
            public void onSeekTo(long pos) {
                Intent intent = new Intent(ACTION_LOCAL_SEEK);
                intent.putExtra(EXTRA_POSITION, pos);
                LocalBroadcastManager.getInstance(MusicNotificationService.this).sendBroadcast(intent);
            }
        });
        mLocalSession.setActive(true);
    }

    private void sendBroadcastToActivity(String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "音乐控制",
                NotificationManager.IMPORTANCE_LOW 
            );
            channel.setDescription("音乐播放控制通知");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

     
    private void updateLocalNotification(String title, String artist, Bitmap cover, boolean isPlaying, long duration, long position) {
        mLastTitle = title;
        mLastArtist = artist;
        mLastIsPlaying = isPlaying;
        mLastBitmap = cover;
        mLastDuration = duration;
        mLastPosition = position;

        
        if (isPlaying) {
            acquireWakeLock();
        } else {
            releaseWakeLock();
        }

        if (isAppInForeground) {
            stopForeground(true);
            return;
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        Intent playIntent = new Intent(this, MusicNotificationService.class);
        playIntent.setAction(isPlaying ? ACTION_LOCAL_PAUSE : ACTION_LOCAL_PLAY);
        PendingIntent pPlay = PendingIntent.getService(this, 0, playIntent, flags);

        Intent nextIntent = new Intent(this, MusicNotificationService.class);
        nextIntent.setAction(ACTION_LOCAL_NEXT);
        PendingIntent pNext = PendingIntent.getService(this, 1, nextIntent, flags);

        Intent prevIntent = new Intent(this, MusicNotificationService.class);
        prevIntent.setAction(ACTION_LOCAL_PREV);
        PendingIntent pPrev = PendingIntent.getService(this, 2, prevIntent, flags);
        
        Intent contentIntent = new Intent(this, MusicPlayerActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pContent = PendingIntent.getActivity(this, 0, contentIntent, flags);

        updateMediaSession(title, artist, cover, isPlaying, duration, position);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(cover != null ? cover : BitmapFactory.decodeResource(getResources(), R.drawable.ic_music_note))
                .setContentIntent(pContent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(isPlaying) 
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_skip_previous, "Previous", pPrev)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, isPlaying ? "Pause" : "Play", pPlay)
                .addAction(R.drawable.ic_skip_next, "Next", pNext)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mLocalSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(FOREGROUND_NOTIFICATION_ID, builder.build(), 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMediaSession(String title, String artist, Bitmap cover, boolean isPlaying, long duration, long position) {
        if (mLocalSession == null) return;
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
        if (cover != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover);
        }
        mLocalSession.setMetadata(metadataBuilder.build());
        
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | 
                            PlaybackStateCompat.ACTION_PAUSE | 
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT | 
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                            PlaybackStateCompat.ACTION_SEEK_TO);
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        stateBuilder.setState(state, position, isPlaying ? 1.0f : 0.0f);
        mLocalSession.setPlaybackState(stateBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Notification notification = createDefaultNotification();
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(FOREGROUND_NOTIFICATION_ID, notification, 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                } else if (Build.VERSION.SDK_INT >= 29) {
                    startForeground(FOREGROUND_NOTIFICATION_ID, notification, 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                } else {
                    startForeground(FOREGROUND_NOTIFICATION_ID, notification);
                }
                
                if (isAppInForeground) {
                    stopForeground(true);
                }
            } catch (Exception e) {
                Log.e(TAG, "启动前台服务失败: " + e.getMessage());
            }
        }

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_LOCAL_PLAY.equals(action) || 
                ACTION_LOCAL_PAUSE.equals(action) || 
                ACTION_LOCAL_NEXT.equals(action) || 
                ACTION_LOCAL_PREV.equals(action)) {
                
                Intent broadcastIntent = new Intent(action);
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
            }
            
            if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                if (mLocalSession != null) {
                    android.view.KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (keyEvent != null) {
                        mLocalSession.getController().dispatchMediaButtonEvent(keyEvent);
                    }
                }
            }
        }
        
        if (isLocalMode && !isAppInForeground) {
            updateLocalNotification(mLastTitle, mLastArtist, mLastBitmap, mLastIsPlaying, mLastDuration, mLastPosition);
        }
        
        return START_STICKY;
    }

    private void setupReceivers() {
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isLocalMode) {
                    updateLocalNotification(mLastTitle, mLastArtist, mLastBitmap, mLastIsPlaying, mLastDuration, mLastPosition);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(refreshReceiver, new IntentFilter(ACTION_REFRESH_REQUEST));

        restartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                restartListening();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(restartReceiver, new IntentFilter(ACTION_SERVICE_RESTART));
        
        localControlReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_SHOW_LOCAL.equals(intent.getAction())) {
                    String title = intent.getStringExtra(EXTRA_TITLE);
                    String artist = intent.getStringExtra(EXTRA_ARTIST);
                    boolean isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false);
                    long duration = intent.getLongExtra(EXTRA_DURATION, 0);
                    long position = intent.getLongExtra(EXTRA_POSITION, 0);
                    
                    isLocalMode = true;
                    updateLocalNotification(title, artist, currentAlbumBitmap, isPlaying, duration, position);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(localControlReceiver, new IntentFilter(ACTION_SHOW_LOCAL));

        appStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_APP_FOREGROUND.equals(intent.getAction())) {
                    isAppInForeground = true;
                    if (isLocalMode) {
                        stopForeground(true);
                        
                        
                        
                    }
                    
                } else if (ACTION_APP_BACKGROUND.equals(intent.getAction())) {
                    isAppInForeground = false;
                    if (isLocalMode) {
                        updateLocalNotification(mLastTitle, mLastArtist, mLastBitmap, mLastIsPlaying, mLastDuration, mLastPosition);
                    }
                }
            }
        };
        IntentFilter appStateFilter = new IntentFilter();
        appStateFilter.addAction(ACTION_APP_FOREGROUND);
        appStateFilter.addAction(ACTION_APP_BACKGROUND);
        LocalBroadcastManager.getInstance(this).registerReceiver(appStateReceiver, appStateFilter);
    }

    private void restartListening() {
        stopMediaSessionListening();
        if (mediaSessionHandler == null) mediaSessionHandler = new Handler();
        startMediaSessionListening();
    }

    private void startMediaSessionListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager != null) {
                mediaSessionRunnable = new Runnable() {
                    @Override
                    public void run() {
                        checkMediaSession(); 
                        mediaSessionHandler.postDelayed(this, MEDIA_SESSION_UPDATE_INTERVAL);
                    }
                };
                mediaSessionHandler.post(mediaSessionRunnable);
            }
        }
    }

    private void stopMediaSessionListening() {
        if (mediaSessionHandler != null && mediaSessionRunnable != null) {
            mediaSessionHandler.removeCallbacks(mediaSessionRunnable);
        }
    }

    private void checkMediaSession() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSessionManager != null) {
                ComponentName componentName = new ComponentName(this, getClass());
                List<MediaController> activeSessions = mediaSessionManager.getActiveSessions(componentName);
                
                if (activeSessions == null || activeSessions.isEmpty()) {
                    if (!isLocalMode) {
                        stopForeground(true);
                    }
                    return;
                }
                
                MediaController playingController = null;
                MediaMetadata playingMetadata = null;
                PlaybackState playingState = null;
                
                for (MediaController controller : activeSessions) {
                    if (controller.getPackageName().equals(getPackageName())) {
                        continue;
                    }

                    PlaybackState pbState = controller.getPlaybackState();
                    if (pbState != null && pbState.getState() == PlaybackState.STATE_PLAYING) {
                        playingController = controller;
                        playingMetadata = controller.getMetadata();
                        playingState = pbState;
                        break;
                    }
                }
                
                if (playingController != null && playingMetadata != null) {
                    isLocalMode = false;
                    releaseWakeLock(); 
                    stopForeground(true);
                    
                    String title = playingMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                    String artist = playingMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                    String album = playingMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
                    boolean isPlaying = true;
                    
                    if (TextUtils.equals(title, mLastTitle) && TextUtils.equals(artist, mLastArtist) && currentAlbumBitmap != null) {
                        sendBroadcastToUI(title, artist, album, currentAlbumBitmap, isPlaying);
                        return; 
                    }
                    
                    mLastTitle = title;
                    mLastArtist = artist;

                    boolean isKugouLite = KUGOU_LITE_PACKAGE.equals(playingController.getPackageName());
                    Bitmap albumBitmap = null;
                    
                    if (isKugouLite) {
                        String currentSongKey = title + "-" + artist;
                        if (currentSongKey.equals(lastFetchedSongKey) && currentAlbumBitmap != null) {
                            albumBitmap = currentAlbumBitmap;
                        } else {
                            albumBitmap = currentAlbumBitmap; 
                            if (!isFetching) {
                                fetchiTunesCover(title, artist, album, isPlaying);
                            }
                        }
                    } else {
                        albumBitmap = playingMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                        if (albumBitmap == null) {
                            albumBitmap = playingMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                        }
                    }
                    
                    if (albumBitmap != null) {
                        albumBitmap = optimizeBitmapSize(albumBitmap);
                        currentAlbumBitmap = albumBitmap; 
                    }
                    
                    sendBroadcastToUI(title, artist, album, currentAlbumBitmap, isPlaying);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendBroadcastToUI(String title, String artist, String album, Bitmap bitmap, boolean isPlaying) {
        Intent intent = new Intent(ACTION_MUSIC_UPDATE);
        intent.putExtra(EXTRA_TITLE, title != null ? title : "");
        intent.putExtra(EXTRA_ARTIST, artist != null ? artist : "");
        intent.putExtra(EXTRA_ALBUM, album != null ? album : "");
        intent.putExtra(EXTRA_IS_PLAYING, isPlaying);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void fetchiTunesCover(final String title, final String artist, final String album, final boolean isPlaying) {
        if (TextUtils.isEmpty(title)) return;
        isFetching = true;
        final String songKey = title + "-" + artist;
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String term = title;
                    if (!TextUtils.isEmpty(artist) && !"未知艺术家".equals(artist)) {
                        term += " " + artist;
                    }
                    term = term.replaceAll("\\([^)]*\\)", "");
                    String encodedTerm = URLEncoder.encode(term, "UTF-8");
                    String urlStr = "https://itunes.apple.com/search?term=" + encodedTerm + "&entity=song&limit=1";
                    
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    
                    if (conn.getResponseCode() == 200) {
                        InputStream in = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();
                        
                        JSONObject root = new JSONObject(sb.toString());
                        if (root.optInt("resultCount") > 0) {
                            JSONArray results = root.getJSONArray("results");
                            JSONObject track = results.getJSONObject(0);
                            String artworkUrl = track.optString("artworkUrl100");
                            
                            if (!TextUtils.isEmpty(artworkUrl)) {
                                artworkUrl = artworkUrl.replace("100x100bb", "600x600bb");
                                Bitmap bitmap = downloadBitmap(artworkUrl);
                                if (bitmap != null) {
                                    final Bitmap finalBitmap = smartCropBitmap(bitmap);
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            lastFetchedSongKey = songKey;
                                            currentAlbumBitmap = finalBitmap;
                                            sendBroadcastToUI(title, artist, album, finalBitmap, isPlaying);
                                        }
                                    });
                                }
                            }
                        }
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "iTunes API error", e);
                } finally {
                    isFetching = false;
                }
            }
        });
    }

    private Bitmap downloadBitmap(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 200) {
                InputStream in = conn.getInputStream();
                return BitmapFactory.decodeStream(in);
            }
        } catch (Exception e) {}
        return null;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        if (!isLocalMode) {
            checkMediaSession();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupResources();
        stopForeground(true);
        if (mLocalSession != null) {
            mLocalSession.release();
            mLocalSession = null;
        }
        if (networkExecutor != null) {
            networkExecutor.shutdown();
        }
    }

    private void cleanupResources() {
        try {
            stopMediaSessionListening();
            if (refreshReceiver != null) LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshReceiver);
            if (restartReceiver != null) LocalBroadcastManager.getInstance(this).unregisterReceiver(restartReceiver);
            if (localControlReceiver != null) LocalBroadcastManager.getInstance(this).unregisterReceiver(localControlReceiver);
            if (appStateReceiver != null) LocalBroadcastManager.getInstance(this).unregisterReceiver(appStateReceiver);
            
            
            releaseWakeLock();
            
            currentAlbumBitmap = null;
            if (handler != null) handler.removeCallbacksAndMessages(null);
            if (mediaSessionHandler != null) mediaSessionHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {}
    }
}