package com.xiaowei.music;
import android.Manifest;
import android.app.Dialog;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.qian.lock.AdvancedBlurUtils;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicPlayerActivity extends AppCompatActivity {
    private static final String TAG = "MusicPlayerActivity";
    private static final int REQUEST_CODE_PERMISSION = 1001;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 1002;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1003;
    private static final String PREFS_NAME = "ClockPrefs";
    private static final String PREF_MUSIC_PATH = "localMusicPath";

    
    private ConstraintLayout containerLayout;
    private CardView albumCard;
    private ImageView albumCover;
    private ImageView backgroundImage;
    private TextView songTitle;
    private TextView artistName;
    private ImageView btnPrev, btnPlayPause, btnNext, btnRotate;
    private ImageView btnMenu; 
    private LinearLayout controlsContainer;
    private SeekBar progressBar;
    private TextView timeDisplay;
    private View leftAmbientLight;
    private View rightAmbientLight;

    
    private ConstraintLayout containerLayoutVertical;
    private CardView albumCardVertical;
    private ImageView albumCoverVertical;
    private ImageView backgroundImageVertical;
    private TextView songTitleVertical;
    private TextView artistNameVertical;
    private ImageView btnPrevVertical, btnPlayPauseVertical, btnNextVertical, btnRotateBack;
    private ImageView btnMenuVertical; 
    private SeekBar progressBarVertical;
    private TextView timeCurrentVertical;
    private TextView timeTotalVertical;

    
    private boolean isVerticalMode = false;
    private boolean isAnimating = false;

    
    private Handler ambientLightHandler;
    private Runnable ambientLightRunnable;
    private boolean isAmbientLightAnimating = false;
    private int currentAmbientColorIndex = 0;
    private long lastAmbientLightTime = 0;
    private static final long AMBIENT_LIGHT_MAX_INTERVAL = 500;

    
    private PowerManager.WakeLock wakeLock;
    private Random randomGenerator = new Random();
    private boolean isMusicPlaying = false;
    private boolean hasActiveMediaSession = false;
    private AudioManager audioManager;
    private Handler musicHandler;
    private Runnable musicCheckRunnable;

    
    private GestureDetector gestureDetector;

    
    private MusicReceiver musicReceiver;
    
    private LocalControlReceiver localControlReceiver;
    private boolean hasNotificationPermission = false;

    
    private MediaSessionManager mediaSessionManager;
    private MediaController mediaController;
    private Handler progressHandler;
    private Runnable progressRunnable;
    private long currentMediaDuration = 0;
    private long currentMediaPosition = 0;

    
    private boolean isUserDragging = false;
    private long userDraggingPosition = 0;
    private Handler draggingHandler;
    private Runnable draggingRunnable;

    
    private long tvTargetSeekPosition = -1;
    private Handler tvSeekHandler = new Handler();
    private Runnable tvSeekRunnable;

    
    private boolean isActivityResumed = false;

    
    private String currentTitle = "歌名未知";
    private String currentArtist = "歌手未知";
    private String currentAlbum = "";
    private Bitmap currentAlbumBitmap = null;

    
    private List<LrcEntry> currentLyrics = new ArrayList<>();
    private String lastLyricText = ""; 

    
    private Bitmap lastDisplayedBitmap = null;
    private boolean isUpdatingCover = false;

    
    private ImageView currentCoverView = null;
    
    private int[] coverLocation = new int[2];
    private int[] targetCoverLocation = new int[2];
    private int coverWidth = 0;
    private int coverHeight = 0;

    
    private Bitmap lastBackgroundBitmap = null;
    private boolean isUpdatingBackground = false;

    
    private boolean isDefaultCover = true;

    
    private Handler blurHandler;
    private Bitmap currentBlurredBitmap = null;
    private Bitmap lastProcessedBitmap = null;

    
    private boolean enableTitleMarquee = false;
    private boolean enableArtistMarquee = false;
    private long lastTitleUpdateTime = 0;
    private long lastArtistUpdateTime = 0;
    private static final long MARQUEE_UPDATE_COOLDOWN = 500;

    
    private float titleTextSize = 24f;
    private float artistTextSize = 14f;
    private int maxTitleCharsVertical = 10;
    private int maxArtistCharsVertical = 15;
    private int maxTitleCharsHorizontal = 15;
    private int maxArtistCharsHorizontal = 25;

    
    private boolean isLocalMode = false; 
    private LocalMusicHelper localMusicHelper;
    private String lastSelectedPath; 
    private SharedPreferences preferences;

    
    private static final float LONG_IMAGE_RATIO_THRESHOLD = 1.3f;

    
    private static final int[] AMBIENT_COLORS = {
            Color.parseColor("#FF00FF"),
            Color.parseColor("#00FFFF"),
            Color.parseColor("#FFFF00"),
            Color.parseColor("#FF69B4"),
            Color.parseColor("#00FF00"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#9400D3"),
            Color.parseColor("#FF0000"),
            Color.parseColor("#FF1493"),
            Color.parseColor("#00CED1"),
            Color.parseColor("#FFD700"),
            Color.parseColor("#FF6347"),
            Color.parseColor("#4169E1"),
            Color.parseColor("#32CD32"),
            Color.parseColor("#FF4500"),
            Color.parseColor("#DA70D6"),
            Color.parseColor("#00FA9A"),
            Color.parseColor("#1E90FF"),
            Color.parseColor("#CD5C5C"),
            Color.parseColor("#4682B4"),
            Color.parseColor("#DAA520"),
            Color.parseColor("#FF8C00"),
            Color.parseColor("#8B008B"),
            Color.parseColor("#008080"),
            Color.parseColor("#FF6B6B"),
            Color.parseColor("#4ECDC4"),
            Color.parseColor("#95E1D3"),
            Color.parseColor("#F38181"),
            Color.parseColor("#AA96DA"),
            Color.parseColor("#FCBAD3"),
            Color.parseColor("#FAD02E"),
            Color.parseColor("#F1C40F"),
            Color.parseColor("#E74C3C"),
            Color.parseColor("#3498DB"),
            Color.parseColor("#2ECC71"),
            Color.parseColor("#F39C12"),
            Color.parseColor("#9B59B6"),
            Color.parseColor("#1ABC9C"),
            Color.parseColor("#E67E22"),
            Color.parseColor("#95A5A6"),
            Color.parseColor("#34495E"),
            Color.parseColor("#16A085"),
            Color.parseColor("#27AE60"),
            Color.parseColor("#2980B9"),
            Color.parseColor("#8E44AD"),
            Color.parseColor("#F1C40F"),
            Color.parseColor("#D35400"),
            Color.parseColor("#C0392B"),
            Color.parseColor("#7F8C8D")
    };

     
    private static class LrcEntry implements Comparable<LrcEntry> {
        long time;
        String text;

        public LrcEntry(long time, String text) {
            this.time = time;
            this.text = text;
        }

        @Override
        public int compareTo(@NonNull LrcEntry o) {
            return Long.compare(this.time, o.time);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        try {
            
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            hideSystemUI();
            setContentView(R.layout.activity_music_player);

            preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            
            initWakeLock();

            
            initLocalMusicHelper();

            
            initTvSeekRunnable();

            
            initViews();

            
            ambientLightHandler = new Handler();
            musicHandler = new Handler();
            progressHandler = new Handler();
            blurHandler = new Handler();
            draggingHandler = new Handler();

            setupGesture();

            
            musicReceiver = new MusicReceiver();
            IntentFilter filter = new IntentFilter(MusicNotificationService.ACTION_MUSIC_UPDATE);
            LocalBroadcastManager.getInstance(this).registerReceiver(musicReceiver, filter);

            
            localControlReceiver = new LocalControlReceiver();
            IntentFilter localFilter = new IntentFilter();
            localFilter.addAction(MusicNotificationService.ACTION_LOCAL_PLAY);
            localFilter.addAction(MusicNotificationService.ACTION_LOCAL_PAUSE);
            localFilter.addAction(MusicNotificationService.ACTION_LOCAL_NEXT);
            localFilter.addAction(MusicNotificationService.ACTION_LOCAL_PREV);
            localFilter.addAction(MusicNotificationService.ACTION_LOCAL_SEEK);
            LocalBroadcastManager.getInstance(this).registerReceiver(localControlReceiver, localFilter);

            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            
            initMusicCheck();

            
            startProgressUpdate();

            
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkAndStartNotificationService();
                    
                    checkAndStartKeepAliveService();
                }
            }, 1000);

        } catch (Exception e) {
            Log.e(TAG, "Activity创建失败", e);
            Toast.makeText(this, "音乐播放器启动失败，请返回重试", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

     
    private boolean isTvDevice() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

     
    private void initTvSeekRunnable() {
        tvSeekRunnable = new Runnable() {
            @Override
            public void run() {
                if (tvTargetSeekPosition >= 0) {
                    if (isLocalMode) {
                        localMusicHelper.seekTo(tvTargetSeekPosition);
                        
                        LocalMusicHelper.LocalSong song = localMusicHelper.getCurrentSong();
                        if (song != null) {
                            sendLocalMusicNotification(song.title, song.artist, MusicNotificationService.currentAlbumBitmap, localMusicHelper.isPlaying(), currentMediaDuration, tvTargetSeekPosition);
                        }
                    } else if (hasActiveMediaSession && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaController != null) {
                        mediaController.getTransportControls().seekTo(tvTargetSeekPosition);
                    }
                    currentMediaPosition = tvTargetSeekPosition;
                    tvTargetSeekPosition = -1; 
                    
                    
                    draggingRunnable = new Runnable() {
                        @Override
                        public void run() {
                            isUserDragging = false;
                            userDraggingPosition = 0;
                            if (progressHandler != null && progressRunnable != null) {
                                progressHandler.removeCallbacks(progressRunnable);
                                progressHandler.post(progressRunnable);
                            }
                        }
                    };
                    
                    draggingHandler.postDelayed(draggingRunnable, 300);
                }
            }
        };
    }

     
    private void handleTvSeek(int keyCode, KeyEvent event) {
        if (currentMediaDuration <= 0) return;

        
        isUserDragging = true;
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
        
        
        tvSeekHandler.removeCallbacks(tvSeekRunnable);

        
        if (tvTargetSeekPosition == -1) {
            tvTargetSeekPosition = currentMediaPosition;
        }

        
        long step = 5000;
        if (event.getRepeatCount() > 0) {
            step = 15000; 
        }

        
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            tvTargetSeekPosition -= step;
            if (tvTargetSeekPosition < 0) tvTargetSeekPosition = 0;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            tvTargetSeekPosition += step;
            if (tvTargetSeekPosition > currentMediaDuration) tvTargetSeekPosition = currentMediaDuration;
        }

        
        updateProgressUI(tvTargetSeekPosition, currentMediaDuration);

        
        tvSeekHandler.postDelayed(tvSeekRunnable, 500);
    }

     
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isTvDevice()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                handlePrev();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                handleNext();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                handleTvSeek(keyCode, event);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                handleTvSeek(keyCode, event);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                
                handlePlayPause();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                onMenuClicked();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

     
    private void parseLyrics(String lrcContent) {
        currentLyrics.clear();
        if (TextUtils.isEmpty(lrcContent)) return;
        String[] lines = lrcContent.split("\n");
        
        Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                try {
                    long min = Long.parseLong(matcher.group(1));
                    long sec = Long.parseLong(matcher.group(2));
                    String milStr = matcher.group(3);
                    long mil = Long.parseLong(milStr);
                    
                    if (milStr.length() == 2) {
                        mil = mil * 10;
                    }
                    long time = min * 60 * 1000 + sec * 1000 + mil;
                    String text = matcher.group(4);
                    if (text != null) {
                        text = text.trim();
                    } else {
                        text = "";
                    }
                    currentLyrics.add(new LrcEntry(time, text));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Collections.sort(currentLyrics);
    }

     
    private void checkAndStartKeepAliveService() {
        if (isTvDevice()) {
            startKeepAliveService();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                
                showKeepAlivePermissionDialog();
            } else {
                
                startKeepAliveService();
            }
        } else {
            
            startKeepAliveService();
        }
    }

     
    private void startKeepAliveService() {
        try {
            Intent intent = new Intent(this, KeepAliveService.class);
            startService(intent);
            Log.d(TAG, "尝试启动保活服务");
        } catch (Exception e) {
            Log.e(TAG, "启动保活服务失败", e);
        }
    }

     
    private void showKeepAlivePermissionDialog() {
        final Dialog dialog = new Dialog(this, R.style.AppTheme_Dialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_permission_request, null);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                requestOverlayPermission();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(MusicPlayerActivity.this, "未开启保活，后台播放可能会暂停", Toast.LENGTH_LONG).show();
            }
        });
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        dialog.setCancelable(false);
        dialog.show();
    }

     
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            } catch (Exception e) {
                Log.e(TAG, "跳转权限设置失败", e);
                Toast.makeText(this, "无法打开设置页面，请手动开启悬浮窗权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    
    private void initLocalMusicHelper() {
        
        localMusicHelper = LocalMusicHelper.getInstance(this);
        
        localMusicHelper.setListener(new LocalMusicHelper.OnMusicPlayerListener() {
            @Override
            public void onTrackStart(final LocalMusicHelper.LocalSong song, final Bitmap cover) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isLocalMode = true; 
                        currentTitle = song.title;
                        currentArtist = song.artist;
                        currentAlbum = song.album != null ? song.album : "";
                        currentMediaDuration = song.duration;
                        if (currentAlbumBitmap != null && !currentAlbumBitmap.isRecycled() && currentAlbumBitmap != MusicNotificationService.currentAlbumBitmap) {
                            
                        }
                        currentAlbumBitmap = cover;

                        
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String lrc = localMusicHelper.getLyricsContent(song.path);
                                if (lrc != null) {
                                    parseLyrics(lrc);
                                } else {
                                    currentLyrics.clear();
                                }
                                lastLyricText = ""; 
                            }
                        }).start();

                        
                        if (isVerticalMode) {
                            updateVerticalUI();
                        } else {
                            updateHorizontalUI();
                        }

                        
                        isMusicPlaying = localMusicHelper.isPlaying();
                        updatePlayPauseIcon();

                        
                        if (isMusicPlaying && !isVerticalMode) {
                            showAmbientLights();
                            startAmbientLightAnimation();
                        } else if (!isMusicPlaying && !isVerticalMode) {
                            hideAmbientLights();
                            stopAmbientLightAnimation();
                        }

                        
                        sendLocalMusicNotification(song.title, song.artist, cover, isMusicPlaying, song.duration, localMusicHelper.getCurrentPosition());

                        
                        checkAndStartKeepAliveService();
                    }
                });
            }

            @Override
            public void onPlaybackComplete() {
                
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MusicPlayerActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        
        if (localMusicHelper.getCurrentSong() != null) {
            isLocalMode = true;
            localMusicHelper.refreshCurrentSongInfo();
        }
    }

    
    private void sendLocalMusicNotification(String title, String artist, Bitmap cover, boolean isPlaying, long duration, long position) {
        MusicNotificationService.currentAlbumBitmap = cover;
        Intent intent = new Intent(MusicNotificationService.ACTION_SHOW_LOCAL);
        intent.putExtra(MusicNotificationService.EXTRA_TITLE, title);
        intent.putExtra(MusicNotificationService.EXTRA_ARTIST, artist);
        intent.putExtra(MusicNotificationService.EXTRA_IS_PLAYING, isPlaying);
        intent.putExtra(MusicNotificationService.EXTRA_DURATION, duration);
        intent.putExtra(MusicNotificationService.EXTRA_POSITION, position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void onMenuClicked() {
        checkPermissionsAndHandleMenu();
    }

    private void checkPermissionsAndHandleMenu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                handleMusicMenuLogic();
            } else {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                    Toast.makeText(this, "请授予文件管理权限以读取音乐", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
            } else {
                handleMusicMenuLogic();
            }
        }
    }

    private void handleMusicMenuLogic() {
        String savedPath = preferences.getString(PREF_MUSIC_PATH, null);
        if (savedPath != null) {
            File dir = new File(savedPath);
            if (dir.exists() && dir.isDirectory()) {
                scanAndShowMusicList(savedPath);
            } else {
                lastSelectedPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                showPathPickerDialog();
            }
        } else {
            lastSelectedPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            showPathPickerDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleMusicMenuLogic();
            } else {
                Toast.makeText(this, "需要存储权限才能播放本地音乐", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    handleMusicMenuLogic();
                }
            }
        } else if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startKeepAliveService();
                    Toast.makeText(this, "保活服务已启动", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "权限未授予，后台播放可能受限", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void setupDialogWindow(Dialog dialog, View rootView) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            if (isLandscape) {
                window.setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                );
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    window.setAttributes(lp);
                }
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                window.getDecorView().setPadding(0, 0, 0, 0);
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
                if (rootView != null) {
                    LinearLayout rootLayout = rootView.findViewById(R.id.dialogRootLayout);
                    if (rootLayout != null) {
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rootLayout.getLayoutParams();
                        params.setMargins(0, 0, 0, 0);
                        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                        rootLayout.setLayoutParams(params);
                        rootLayout.setBackgroundColor(Color.parseColor("#FF222222"));
                    }
                }
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(400));
            }
        }
    }

    private void showPathPickerDialog() {
        final File currentDir = new File(lastSelectedPath);
        if (!currentDir.exists()) {
            lastSelectedPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        File dir = new File(lastSelectedPath);
        final File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !pathname.isHidden();
            }
        });
        if (files == null) {
            Toast.makeText(this, "无法读取目录: " + lastSelectedPath, Toast.LENGTH_SHORT).show();
            return;
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        List<String> names = new ArrayList<>();
        names.add("..");
        names.add("选择当前目录"); 
        for (File f : files) {
            names.add(f.getName());
        }
        
        final int[] selectedPos = {-1}; 
        final Dialog dialog = new Dialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_music_list, null);
        final ListView listView = view.findViewById(R.id.lvMusicList);
        TextView title = view.findViewById(R.id.tvListTitle);
        title.setText("选择文件夹:\n" + dir.getName());
        
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.item_local_music, names) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_local_music, parent, false);
                }
                TextView tvTitle = convertView.findViewById(R.id.tvItemTitle);
                TextView tvArtist = convertView.findViewById(R.id.tvItemArtist);
                ImageView ivIcon = convertView.findViewById(R.id.ivItemIcon);
                tvTitle.setText(getItem(position));
                tvArtist.setVisibility(View.GONE);
                
                
                if (position == 0) {
                    ivIcon.setImageResource(R.drawable.ic_folder);
                    ivIcon.setVisibility(View.VISIBLE); 
                } else if (position == 1) {
                    ivIcon.setVisibility(View.GONE);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_folder);
                    ivIcon.setVisibility(View.VISIBLE);
                }

                
                convertView.setFocusable(false);
                convertView.setClickable(false);

                
                boolean isSelected = (position == selectedPos[0]);
                if (isSelected) {
                    convertView.setBackgroundColor(Color.WHITE);
                    tvTitle.setTextColor(Color.BLACK);
                    if (ivIcon.getVisibility() == View.VISIBLE) ivIcon.setColorFilter(Color.BLACK);
                } else {
                    convertView.setBackgroundColor(Color.TRANSPARENT);
                    tvTitle.setTextColor(Color.WHITE);
                    if (ivIcon.getVisibility() == View.VISIBLE) ivIcon.setColorFilter(Color.WHITE);
                }

                return convertView;
            }
        };
        listView.setAdapter(adapter);

        
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPos[0] = position;
                adapter.notifyDataSetChanged(); 
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedPos[0] = -1;
                adapter.notifyDataSetChanged();
            }
        });

        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    File parentDir = new File(lastSelectedPath).getParentFile();
                    if (parentDir != null && parentDir.canRead()) {
                        lastSelectedPath = parentDir.getAbsolutePath();
                        dialog.dismiss();
                        showPathPickerDialog();
                    } else {
                        Toast.makeText(MusicPlayerActivity.this, "已是根目录", Toast.LENGTH_SHORT).show();
                    }
                } else if (position == 1) {
                    preferences.edit().putString(PREF_MUSIC_PATH, lastSelectedPath).apply();
                    dialog.dismiss();
                    scanAndShowMusicList(lastSelectedPath);
                } else {
                    lastSelectedPath = files[position - 2].getAbsolutePath();
                    dialog.dismiss();
                    showPathPickerDialog();
                }
            }
        });

        
        listView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    int pos = listView.getSelectedItemPosition();
                    if (pos != AdapterView.INVALID_POSITION) {
                        listView.performItemClick(listView.getSelectedView(), pos, adapter.getItemId(pos));
                        return true;
                    }
                }
                return false;
            }
        });
        
        
        dialog.setOnShowListener(d -> listView.post(() -> {
            listView.requestFocus();
            listView.setSelection(0);
        }));

        dialog.setContentView(view);
        setupDialogWindow(dialog, view);
        dialog.show();
    }

    private void scanAndShowMusicList(String path) {
        final List<LocalMusicHelper.LocalSong> songs = localMusicHelper.scanMusicFiles(path);
        if (songs.isEmpty()) {
            Toast.makeText(this, "该目录下没有找到音乐文件", Toast.LENGTH_SHORT).show();
            lastSelectedPath = path;
            showPathPickerDialog();
            return;
        }
        
        final int[] selectedPos = {-1}; 
        final Dialog dialog = new Dialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_music_list, null);
        final ListView listView = view.findViewById(R.id.lvMusicList);
        TextView title = view.findViewById(R.id.tvListTitle);
        title.setText("播放列表 (" + songs.size() + "首)");

        final ArrayAdapter<LocalMusicHelper.LocalSong> adapter = new ArrayAdapter<LocalMusicHelper.LocalSong>(this, R.layout.item_local_music, songs) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_local_music, parent, false);
                }
                final LocalMusicHelper.LocalSong song = getItem(position);
                TextView tvTitle = convertView.findViewById(R.id.tvItemTitle);
                TextView tvArtist = convertView.findViewById(R.id.tvItemArtist);
                ImageView ivIcon = convertView.findViewById(R.id.ivItemIcon);
                
                tvTitle.setText(song.title);
                tvArtist.setText(song.artist);
                tvArtist.setVisibility(View.VISIBLE);
                
                ivIcon.setImageResource(R.drawable.ic_music_note);
                ivIcon.setVisibility(View.VISIBLE);

                final boolean isCurrentPlaying = isLocalMode && localMusicHelper.getCurrentSong() != null && localMusicHelper.getCurrentSong().path.equals(song.path);

                
                convertView.setFocusable(false);
                convertView.setClickable(false);

                
                boolean isSelected = (position == selectedPos[0]);
                if (isSelected) {
                    convertView.setBackgroundColor(Color.WHITE);
                    tvTitle.setTextColor(Color.BLACK);
                    tvArtist.setTextColor(Color.DKGRAY);
                    ivIcon.setColorFilter(Color.BLACK);
                } else {
                    convertView.setBackgroundColor(Color.TRANSPARENT);
                    if (isCurrentPlaying) {
                        tvTitle.setTextColor(Color.YELLOW);
                        ivIcon.setColorFilter(Color.YELLOW);
                    } else {
                        tvTitle.setTextColor(Color.WHITE);
                        ivIcon.setColorFilter(Color.WHITE);
                    }
                    tvArtist.setTextColor(Color.LTGRAY);
                }

                return convertView;
            }
        };
        listView.setAdapter(adapter);

        
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPos[0] = position;
                adapter.notifyDataSetChanged(); 
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedPos[0] = -1;
                adapter.notifyDataSetChanged();
            }
        });

        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                isLocalMode = true;
                if (audioManager != null) {
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                }
                localMusicHelper.playSong(position);
                dialog.dismiss();
            }
        });

        
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                lastSelectedPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                showPathPickerDialog();
                return true;
            }
        });

        
        listView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    int pos = listView.getSelectedItemPosition();
                    if (pos != AdapterView.INVALID_POSITION) {
                        listView.performItemClick(listView.getSelectedView(), pos, adapter.getItemId(pos));
                        return true;
                    }
                }
                return false;
            }
        });

        
        dialog.setOnShowListener(d -> {
            listView.requestFocus();
            if (isLocalMode && localMusicHelper.getCurrentSong() != null) {
                String currentPath = localMusicHelper.getCurrentSong().path;
                for (int i = 0; i < songs.size(); i++) {
                    if (songs.get(i).path.equals(currentPath)) {
                        final int selection = i;
                        listView.post(() -> listView.setSelection(selection));
                        return;
                    }
                }
            }
            
            listView.post(() -> listView.setSelection(0));
        });

        dialog.setContentView(view);
        setupDialogWindow(dialog, view);
        dialog.show();
    }

    private void initWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "MusicPlayer:WakeLock"
                );
                wakeLock.setReferenceCounted(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化WakeLock失败", e);
        }
    }

    private void acquireWakeLock() {
        try {
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取WakeLock失败", e);
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "释放WakeLock失败", e);
        }
    }

    private void initMusicCheck() {
        musicCheckRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isFinishing()) {
                        checkMusicStatus();
                        musicHandler.postDelayed(this, 500);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "音乐状态检查异常", e);
                }
            }
        };
        musicHandler.post(musicCheckRunnable);
    }

    private void startProgressUpdate() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isFinishing()) {
                        if (!isUserDragging) {
                            if (isLocalMode) {
                                if (localMusicHelper != null && localMusicHelper.isPlaying()) {
                                    currentMediaPosition = localMusicHelper.getCurrentPosition();
                                    currentMediaDuration = localMusicHelper.getDuration();
                                    updateProgressUI(currentMediaPosition, currentMediaDuration);
                                }
                            } else {
                                updateProgressFromMediaSession();
                            }
                        }
                        progressHandler.postDelayed(this, 1000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "进度更新异常", e);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void updateProgressFromMediaSession() {
        if (isLocalMode) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mediaSessionManager == null) {
                    mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
                }
                if (mediaSessionManager != null) {
                    ComponentName componentName = new ComponentName(MusicPlayerActivity.this, MusicNotificationService.class);
                    List<MediaController> activeSessions = mediaSessionManager.getActiveSessions(componentName);
                    hasActiveMediaSession = false;
                    for (MediaController controller : activeSessions) {
                        mediaController = controller;
                        MediaMetadata metadata = controller.getMetadata();
                        PlaybackState playbackState = controller.getPlaybackState();
                        if (metadata != null && playbackState != null) {
                            hasActiveMediaSession = true;
                            if (metadata.containsKey(MediaMetadata.METADATA_KEY_DURATION)) {
                                currentMediaDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                            }
                            currentMediaPosition = playbackState.getPosition();
                            int state = playbackState.getState();
                            boolean wasMusicPlaying = isMusicPlaying;
                            isMusicPlaying = (state == PlaybackState.STATE_PLAYING);
                            if (isMusicPlaying != wasMusicPlaying) {
                                updatePlayPauseIcon();
                                if (isMusicPlaying) {
                                    if (!isVerticalMode) {
                                        showAmbientLights();
                                        startAmbientLightAnimation();
                                    }
                                } else {
                                    if (!isVerticalMode) {
                                        hideAmbientLights();
                                        stopAmbientLightAnimation();
                                    }
                                }
                            }
                            if (!isUserDragging && currentMediaDuration > 0) {
                                updateProgressUI(currentMediaPosition, currentMediaDuration);
                            }
                            return;
                        }
                    }
                    if (!hasActiveMediaSession) {
                        isMusicPlaying = false;
                        updatePlayPauseIcon();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "从MediaSession更新进度失败", e);
        }
    }

    private void updateProgressUI(long position, long duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    
                    String textToDisplay = currentTitle; 
                    if (currentLyrics != null && !currentLyrics.isEmpty()) {
                        for (int i = 0; i < currentLyrics.size(); i++) {
                            if (position >= currentLyrics.get(i).time) {
                                textToDisplay = currentLyrics.get(i).text;
                            } else {
                                break;
                            }
                        }
                    }
                    if (!textToDisplay.equals(lastLyricText)) {
                        lastLyricText = textToDisplay;
                        if (isVerticalMode) {
                            if (songTitleVertical != null) {
                                boolean shouldMarquee = shouldEnableMarquee(songTitleVertical, textToDisplay, maxTitleCharsVertical);
                                enableTitleMarquee = shouldMarquee;
                                safeUpdateTextView(songTitleVertical, textToDisplay, enableTitleMarquee);
                            }
                        } else {
                            if (songTitle != null) {
                                boolean shouldMarquee = shouldEnableMarquee(songTitle, textToDisplay, maxTitleCharsHorizontal);
                                enableTitleMarquee = shouldMarquee;
                                safeUpdateTextView(songTitle, textToDisplay, enableTitleMarquee);
                            }
                        }
                    }

                    if (isVerticalMode) {
                        if (progressBarVertical != null && duration > 0) {
                            int progress = (int) ((position * 1000) / duration);
                            progressBarVertical.setProgress(progress);
                        }
                        if (timeCurrentVertical != null) timeCurrentVertical.setText(formatTime(position));
                        if (timeTotalVertical != null) timeTotalVertical.setText(formatTime(duration));
                    } else {
                        if (progressBar != null && duration > 0) {
                            int progress = (int) ((position * 1000) / duration);
                            progressBar.setProgress(progress);
                        }
                        if (timeDisplay != null) timeDisplay.setText(formatTime(position) + " / " + formatTime(duration));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "更新进度UI异常", e);
                }
            }
        });
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "00:00";
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private void checkAndStartNotificationService() {
        try {
            if (isTvDevice()) {
                
                startNotificationService();
                refreshNotificationService();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                
                String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
                hasNotificationPermission = listeners != null && listeners.contains(getPackageName());
                if (!hasNotificationPermission) {
                    
                    showNotificationPermissionDialog();
                } else {
                    startNotificationService();
                    refreshNotificationService();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "检查通知权限失败", e);
        }
    }

    private void startNotificationService() {
        try {
            Intent serviceIntent = new Intent(this, MusicNotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "启动通知服务失败", e);
        }
    }

    private void refreshNotificationService() {
        try {
            Intent refreshIntent = new Intent(MusicNotificationService.ACTION_REFRESH_REQUEST);
            LocalBroadcastManager.getInstance(this).sendBroadcast(refreshIntent);
        } catch (Exception e) {
            Log.e(TAG, "刷新通知服务失败", e);
        }
    }

     
    private void showNotificationPermissionDialog() {
        
        final Dialog dialog = new Dialog(this, R.style.AppTheme_Dialog);
        
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_permission_request, null);
        
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            
            if (group.getChildCount() >= 2) {
                View titleView = group.getChildAt(0);
                if (titleView instanceof TextView) {
                    ((TextView) titleView).setText("需通知权限");
                }
                View msgView = group.getChildAt(1);
                if (msgView instanceof TextView) {
                    ((TextView) msgView).setText("为了获取音乐播放信息，需要您授予通知访问权限。\n\n请在接下来的设置页面中启用本应用的通知使用权。");
                }
            }
        }
        Button btnConfirm = view.findViewById(R.id.btnConfirm);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    
                    intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                }
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
        dialog.setCancelable(false);
        dialog.show();
    }

    
    private void initViews() {
        try {
            clearVerticalViews();
            containerLayout = findViewById(R.id.containerLayout);
            albumCard = findViewById(R.id.albumCard); 
            albumCover = findViewById(R.id.albumCover);
            backgroundImage = findViewById(R.id.backgroundImage);
            songTitle = findViewById(R.id.songTitle);
            artistName = findViewById(R.id.artistName);
            progressBar = findViewById(R.id.progressBar);
            timeDisplay = findViewById(R.id.timeDisplay);
            btnPrev = findViewById(R.id.btnPrev);
            btnPlayPause = findViewById(R.id.btnPlayPause);
            btnNext = findViewById(R.id.btnNext);
            btnRotate = findViewById(R.id.btnRotate);
            btnMenu = findViewById(R.id.btnMenu);
            controlsContainer = findViewById(R.id.controlsContainer);
            leftAmbientLight = findViewById(R.id.leftAmbientLight);
            rightAmbientLight = findViewById(R.id.rightAmbientLight);

            
            if (isTvDevice()) {
                if (progressBar != null) progressBar.setFocusable(false);
                if (btnPrev != null) btnPrev.setFocusable(false);
                if (btnPlayPause != null) btnPlayPause.setFocusable(false);
                if (btnNext != null) btnNext.setFocusable(false);
                if (btnRotate != null) btnRotate.setFocusable(false);
                if (btnMenu != null) btnMenu.setFocusable(false);
            }

            if (btnPrev != null) btnPrev.setOnClickListener(v -> handlePrev());
            if (btnPlayPause != null) btnPlayPause.setOnClickListener(v -> handlePlayPause());
            if (btnNext != null) btnNext.setOnClickListener(v -> handleNext());
            if (btnRotate != null) btnRotate.setOnClickListener(v -> switchToVerticalMode());
            if (btnMenu != null) btnMenu.setOnClickListener(v -> onMenuClicked());

            if (progressBar != null) {
                progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && currentMediaDuration > 0) {
                            userDraggingPosition = (progress * currentMediaDuration) / 1000;
                            if (timeDisplay != null) {
                                timeDisplay.setText(formatTime(userDraggingPosition) + " / " + formatTime(currentMediaDuration));
                            }
                            if (isUserDragging && progressBar != null) {
                                progressBar.setProgress(progress);
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        isUserDragging = true;
                        if (progressHandler != null && progressRunnable != null) {
                            progressHandler.removeCallbacks(progressRunnable);
                        }
                        if (draggingRunnable != null) {
                            draggingHandler.removeCallbacks(draggingRunnable);
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (isLocalMode) {
                            int progress = seekBar.getProgress();
                            long newPosition = (progress * currentMediaDuration) / 1000;
                            localMusicHelper.seekTo(newPosition);
                            LocalMusicHelper.LocalSong song = localMusicHelper.getCurrentSong();
                            if (song != null) {
                                sendLocalMusicNotification(song.title, song.artist, MusicNotificationService.currentAlbumBitmap, localMusicHelper.isPlaying(), currentMediaDuration, newPosition);
                            }
                        } else if (hasActiveMediaSession) {
                            int progress = seekBar.getProgress();
                            long newPosition = (progress * currentMediaDuration) / 1000;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaController != null) {
                                mediaController.getTransportControls().seekTo(newPosition);
                            }
                            currentMediaPosition = newPosition;
                        }
                        draggingRunnable = new Runnable() {
                            @Override
                            public void run() {
                                isUserDragging = false;
                                userDraggingPosition = 0;
                                if (progressHandler != null && progressRunnable != null) {
                                    progressHandler.removeCallbacks(progressRunnable);
                                    progressHandler.post(progressRunnable);
                                }
                            }
                        };
                        draggingHandler.postDelayed(draggingRunnable, 500);
                    }
                });
            }
            setupAlbumCover();
            updateHorizontalUI();
            currentCoverView = albumCover;
        } catch (Exception e) {
            Log.e(TAG, "初始化横屏界面失败", e);
        }
    }

    
    private void handlePrev() {
        if (isLocalMode) {
            localMusicHelper.playPrev();
            LocalMusicHelper.LocalSong song = localMusicHelper.getCurrentSong();
            if (song != null) {
                sendLocalMusicNotification(song.title, song.artist, MusicNotificationService.currentAlbumBitmap, true, song.duration, localMusicHelper.getCurrentPosition());
            }
        } else {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        }
    }

    private void handleNext() {
        if (isLocalMode) {
            localMusicHelper.playNext();
            LocalMusicHelper.LocalSong song = localMusicHelper.getCurrentSong();
            if (song != null) {
                sendLocalMusicNotification(song.title, song.artist, MusicNotificationService.currentAlbumBitmap, true, song.duration, localMusicHelper.getCurrentPosition());
            }
        } else {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
        }
    }

    private void handlePlayPause() {
        if (isLocalMode) {
            localMusicHelper.playPause();
            isMusicPlaying = localMusicHelper.isPlaying();
            updatePlayPauseIcon();
            LocalMusicHelper.LocalSong song = localMusicHelper.getCurrentSong();
            if (song != null) {
                sendLocalMusicNotification(song.title, song.artist, MusicNotificationService.currentAlbumBitmap, isMusicPlaying, song.duration, localMusicHelper.getCurrentPosition());
            }
            if (isMusicPlaying) {
                if (!isVerticalMode) {
                    showAmbientLights();
                    startAmbientLightAnimation();
                }
            } else {
                if (!isVerticalMode) {
                    hideAmbientLights();
                    stopAmbientLightAnimation();
                }
            }
        } else {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        }
    }

    
    private void initVerticalViews() {
        try {
            clearHorizontalViews();
            containerLayoutVertical = findViewById(R.id.containerLayoutVertical);
            albumCardVertical = findViewById(R.id.albumCardVertical); 
            albumCoverVertical = findViewById(R.id.albumCoverVertical);
            backgroundImageVertical = findViewById(R.id.backgroundImageVertical);
            songTitleVertical = findViewById(R.id.songTitleVertical);
            artistNameVertical = findViewById(R.id.artistNameVertical);
            progressBarVertical = findViewById(R.id.progressBarVertical);
            timeCurrentVertical = findViewById(R.id.timeCurrentVertical);
            timeTotalVertical = findViewById(R.id.timeTotalVertical);
            btnPrevVertical = findViewById(R.id.btnPrevVertical);
            btnPlayPauseVertical = findViewById(R.id.btnPlayPauseVertical);
            btnNextVertical = findViewById(R.id.btnNextVertical);
            btnRotateBack = findViewById(R.id.btnRotateBack);
            btnMenuVertical = findViewById(R.id.btnMenuVertical);

            
            if (isTvDevice()) {
                if (progressBarVertical != null) progressBarVertical.setFocusable(false);
                if (btnPrevVertical != null) btnPrevVertical.setFocusable(false);
                if (btnPlayPauseVertical != null) btnPlayPauseVertical.setFocusable(false);
                if (btnNextVertical != null) btnNextVertical.setFocusable(false);
                if (btnRotateBack != null) btnRotateBack.setFocusable(false);
                if (btnMenuVertical != null) btnMenuVertical.setFocusable(false);
            }

            if (btnPrevVertical != null) btnPrevVertical.setOnClickListener(v -> handlePrev());
            if (btnPlayPauseVertical != null) btnPlayPauseVertical.setOnClickListener(v -> handlePlayPause());
            if (btnNextVertical != null) btnNextVertical.setOnClickListener(v -> handleNext());
            if (btnRotateBack != null) btnRotateBack.setOnClickListener(v -> switchToHorizontalMode());
            if (btnMenuVertical != null) btnMenuVertical.setOnClickListener(v -> onMenuClicked());

            if (progressBarVertical != null) {
                progressBarVertical.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && currentMediaDuration > 0) {
                            userDraggingPosition = (progress * currentMediaDuration) / 1000;
                            if (timeCurrentVertical != null) {
                                timeCurrentVertical.setText(formatTime(userDraggingPosition));
                            }
                            if (isUserDragging && progressBarVertical != null) {
                                progressBarVertical.setProgress(progress);
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        isUserDragging = true;
                        if (progressHandler != null && progressRunnable != null) {
                            progressHandler.removeCallbacks(progressRunnable);
                        }
                        if (draggingRunnable != null) {
                            draggingHandler.removeCallbacks(draggingRunnable);
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (isLocalMode) {
                            int progress = seekBar.getProgress();
                            long newPosition = (progress * currentMediaDuration) / 1000;
                            localMusicHelper.seekTo(newPosition);
                            LocalMusicHelper.LocalSong song = localMusicHelper.getCurrentSong();
                            if (song != null) {
                                sendLocalMusicNotification(song.title, song.artist, MusicNotificationService.currentAlbumBitmap, localMusicHelper.isPlaying(), currentMediaDuration, newPosition);
                            }
                        } else if (hasActiveMediaSession) {
                            int progress = seekBar.getProgress();
                            long newPosition = (progress * currentMediaDuration) / 1000;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaController != null) {
                                mediaController.getTransportControls().seekTo(newPosition);
                            }
                            currentMediaPosition = newPosition;
                        }
                        draggingRunnable = new Runnable() {
                            @Override
                            public void run() {
                                isUserDragging = false;
                                userDraggingPosition = 0;
                                if (progressHandler != null && progressRunnable != null) {
                                    progressHandler.removeCallbacks(progressRunnable);
                                    progressHandler.post(progressRunnable);
                                }
                            }
                        };
                        draggingHandler.postDelayed(draggingRunnable, 500);
                    }
                });
            }
            setupVerticalAlbumCover();
            updateVerticalUI();
            currentCoverView = albumCoverVertical;
        } catch (Exception e) {
            Log.e(TAG, "初始化竖屏界面失败", e);
        }
    }

    private void setupVerticalAlbumCover() {
        try {
            if (albumCoverVertical != null) {
                albumCoverVertical.setScaleType(ImageView.ScaleType.CENTER_CROP);
                albumCoverVertical.setPadding(0, 0, 0, 0);
                albumCoverVertical.setAdjustViewBounds(false);
                albumCoverVertical.setBackgroundColor(Color.parseColor("#FF444444"));
            }
        } catch (Exception e) {
            Log.e(TAG, "设置竖屏专辑封面失败", e);
        }
    }

    private void clearHorizontalViews() {
        containerLayout = null;
        albumCard = null;
        albumCover = null;
        backgroundImage = null;
        songTitle = null;
        artistName = null;
        progressBar = null;
        timeDisplay = null;
        btnPrev = null;
        btnPlayPause = null;
        btnNext = null;
        btnRotate = null;
        btnMenu = null;
        controlsContainer = null;
        leftAmbientLight = null;
        rightAmbientLight = null;
    }

    private void clearVerticalViews() {
        containerLayoutVertical = null;
        albumCardVertical = null;
        albumCoverVertical = null;
        backgroundImageVertical = null;
        songTitleVertical = null;
        artistNameVertical = null;
        progressBarVertical = null;
        timeCurrentVertical = null;
        timeTotalVertical = null;
        btnPrevVertical = null;
        btnPlayPauseVertical = null;
        btnNextVertical = null;
        btnRotateBack = null;
        btnMenuVertical = null;
    }

    
    private boolean shouldEnableMarquee(TextView textView, String text, int maxChars) {
        if (text == null || textView == null) return false;
        boolean byCharCount = text.length() > maxChars;
        boolean byWidth = false;
        try {
            int availableWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
            if (availableWidth > 0) {
                Paint paint = new Paint();
                paint.setTextSize(textView.getTextSize());
                float textWidth = paint.measureText(text);
                byWidth = textWidth > availableWidth;
            }
        } catch (Exception e) {
            Log.e(TAG, "测量文本宽度失败", e);
        }
        return byCharCount || byWidth;
    }

    private void safeUpdateTextView(TextView textView, String text, boolean enableMarquee) {
        if (textView == null) return;
        try {
            textView.setText(text);
            if (enableMarquee) {
                textView.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
                textView.setMarqueeRepeatLimit(-1);
                textView.setSingleLine(true);
                textView.setSelected(true);
                
                if (!isTvDevice()) {
                    textView.setFocusable(true);
                    textView.setFocusableInTouchMode(true);
                    textView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (textView != null) {
                                textView.requestFocus();
                            }
                        }
                    }, 200);
                }
            } else {
                textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                textView.setFocusable(false);
                textView.setFocusableInTouchMode(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新TextView失败", e);
        }
    }

    private void updateVerticalUI() {
        try {
            long currentTime = System.currentTimeMillis();
            if (songTitleVertical != null) {
                String textToDisplay = TextUtils.isEmpty(lastLyricText) ? currentTitle : lastLyricText;
                String currentText = songTitleVertical.getText().toString();
                boolean textChanged = !currentText.equals(textToDisplay);
                boolean shouldMarquee = shouldEnableMarquee(songTitleVertical, textToDisplay, maxTitleCharsVertical);
                if (textChanged || shouldMarquee != enableTitleMarquee) {
                    enableTitleMarquee = shouldMarquee;
                    safeUpdateTextView(songTitleVertical, textToDisplay, enableTitleMarquee);
                    lastTitleUpdateTime = currentTime;
                }
            }
            if (artistNameVertical != null) {
                String currentText = artistNameVertical.getText().toString();
                boolean textChanged = !currentText.equals(currentArtist);
                boolean shouldMarquee = shouldEnableMarquee(artistNameVertical, currentArtist, maxArtistCharsVertical);
                if (textChanged || shouldMarquee != enableArtistMarquee) {
                    enableArtistMarquee = shouldEnableMarquee(artistNameVertical, currentArtist, maxArtistCharsVertical);
                    safeUpdateTextView(artistNameVertical, currentArtist, enableArtistMarquee);
                    lastArtistUpdateTime = currentTime;
                }
            }
            updateAlbumCoverSafe(albumCoverVertical, currentAlbumBitmap);
            if (backgroundImageVertical != null) {
                if (currentAlbumBitmap != null && !currentAlbumBitmap.isRecycled() && !isDefaultCover) {
                    updateBackgroundImageWithBlur(backgroundImageVertical, currentAlbumBitmap, 0.9f);
                } else {
                    backgroundImageVertical.setVisibility(View.GONE);
                }
            }
            updatePlayPauseIcon();
            if (!isUserDragging && currentMediaDuration > 0 && progressBarVertical != null) {
                int progress = (int) ((currentMediaPosition * 1000) / currentMediaDuration);
                progressBarVertical.setProgress(progress);
                if (timeCurrentVertical != null) timeCurrentVertical.setText(formatTime(currentMediaPosition));
                if (timeTotalVertical != null) timeTotalVertical.setText(formatTime(currentMediaDuration));
            }
        } catch (Exception e) {
            Log.e(TAG, "更新竖屏UI失败", e);
        }
    }

    private void setupAlbumCover() {
        try {
            if (albumCover != null) {
                albumCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
                albumCover.setPadding(0, 0, 0, 0);
                albumCover.setAdjustViewBounds(false);
                albumCover.setBackgroundColor(Color.parseColor("#FF444444"));
            }
        } catch (Exception e) {
            Log.e(TAG, "设置专辑封面失败", e);
        }
    }

    private void applyLongImageScaling(ImageView imageView, Bitmap bitmap) {
        if (imageView == null || bitmap == null || bitmap.isRecycled()) {
            return;
        }
        try {
            int vWidth = imageView.getWidth();
            int vHeight = imageView.getHeight();
            int bWidth = bitmap.getWidth();
            int bHeight = bitmap.getHeight();
            if (vWidth == 0 || vHeight == 0) {
                imageView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        applyLongImageScaling(imageView, bitmap);
                    }
                }, 100);
                return;
            }
            if ((float) bHeight / bWidth > LONG_IMAGE_RATIO_THRESHOLD) {
                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                Matrix matrix = new Matrix();
                float scale = (float) vWidth / bWidth;
                if (scale * bHeight < vHeight) {
                    scale = (float) vHeight / bHeight;
                }
                matrix.setScale(scale, scale);
                float dx = (vWidth - bWidth * scale) * 0.5f;
                float dy = 0;
                matrix.postTranslate(dx, dy);
                imageView.setImageMatrix(matrix);
            } else {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        } catch (Exception e) {
            Log.e(TAG, "应用长图缩放失败", e);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }

    private void updateAlbumCoverSafe(ImageView imageView, Bitmap newBitmap) {
        if (imageView == null) return;
        try {
            if (isUpdatingCover) return;
            isUpdatingCover = true;
            if (newBitmap == null || newBitmap.isRecycled()) {
                if (lastDisplayedBitmap == null) {
                    imageView.setImageResource(R.drawable.ic_music_note);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    isDefaultCover = true;
                }
                isUpdatingCover = false;
                return;
            }
            runOnUiThread(() -> {
                try {
                    if (imageView != null) {
                        applyLongImageScaling(imageView, newBitmap);
                        imageView.setImageBitmap(newBitmap);
                        lastDisplayedBitmap = newBitmap;
                        isDefaultCover = false;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "设置封面失败", e);
                    if (imageView != null) {
                        imageView.setImageResource(R.drawable.ic_music_note);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                    isDefaultCover = true;
                } finally {
                    isUpdatingCover = false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "更新封面安全方法异常", e);
            isUpdatingCover = false;
        }
    }

    private void updateBackgroundImageWithBlur(final ImageView imageView, final Bitmap originalBitmap, final float alpha) {
        if (imageView == null || originalBitmap == null || originalBitmap.isRecycled()) {
            return;
        }
        try {
            if (isUpdatingBackground) return;
            isUpdatingBackground = true;
            if (lastProcessedBitmap != null && lastProcessedBitmap == originalBitmap) {
                if (currentBlurredBitmap != null && !currentBlurredBitmap.isRecycled()) {
                    runOnUiThread(() -> {
                        try {
                            if (imageView != null) {
                                imageView.setImageBitmap(currentBlurredBitmap);
                                imageView.setAlpha(alpha);
                                imageView.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "设置缓存背景失败", e);
                        } finally {
                            isUpdatingBackground = false;
                        }
                    });
                    return;
                }
            }
            new Thread(() -> {
                try {
                    if (isFinishing() || isDestroyed()) {
                        isUpdatingBackground = false;
                        return;
                    }
                    Bitmap blurredBitmap = AdvancedBlurUtils.advancedBlur(originalBitmap, 25);
                    if (blurredBitmap != null && !blurredBitmap.isRecycled()) {
                        currentBlurredBitmap = blurredBitmap;
                        lastProcessedBitmap = originalBitmap;
                        runOnUiThread(() -> {
                            try {
                                if (!isFinishing() && !isDestroyed() && imageView != null) {
                                    imageView.setImageBitmap(blurredBitmap);
                                    imageView.setAlpha(alpha);
                                    imageView.setVisibility(View.VISIBLE);
                                    lastBackgroundBitmap = blurredBitmap;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "设置模糊背景失败", e);
                            } finally {
                                isUpdatingBackground = false;
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            if (imageView != null) imageView.setVisibility(View.GONE);
                            isUpdatingBackground = false;
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "模糊处理异常", e);
                    isUpdatingBackground = false;
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "更新背景图方法异常", e);
            isUpdatingBackground = false;
        }
    }

    private void setCoverDirectly(ImageView imageView) {
        if (imageView == null) return;
        try {
            if (currentAlbumBitmap != null && !currentAlbumBitmap.isRecycled()) {
                applyLongImageScaling(imageView, currentAlbumBitmap);
                imageView.setImageBitmap(currentAlbumBitmap);
                isDefaultCover = false;
            } else {
                imageView.setImageResource(R.drawable.ic_music_note);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                isDefaultCover = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "直接设置封面失败", e);
        }
    }

    private void setBackgroundDirectly(ImageView imageView, float alpha) {
        if (imageView == null) return;
        try {
            if (currentBlurredBitmap != null && !currentBlurredBitmap.isRecycled() && !isDefaultCover) {
                imageView.setImageBitmap(currentBlurredBitmap);
                imageView.setAlpha(alpha);
                imageView.setVisibility(View.VISIBLE);
            } else if (currentAlbumBitmap != null && !currentAlbumBitmap.isRecycled() && !isDefaultCover) {
                imageView.setImageBitmap(currentAlbumBitmap);
                imageView.setAlpha(alpha);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "直接设置背景图失败", e);
        }
    }

    private void sendMediaKeyEvent(int keyCode) {
        if (audioManager == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            long eventTime = System.currentTimeMillis();
            KeyEvent keyEventDown = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);
            KeyEvent keyEventUp = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0);
            try {
                audioManager.dispatchMediaKeyEvent(keyEventDown);
                audioManager.dispatchMediaKeyEvent(keyEventUp);
            } catch (Exception e) {
                Log.e(TAG, "Error sending media key", e);
            }
        } else {
            
            if (!isLocalMode) {
                Toast.makeText(this, "当前系统版本仅支持本地音乐控制", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updatePlayPauseIcon() {
        runOnUiThread(() -> {
            try {
                if (isMusicPlaying) {
                    if (isVerticalMode && btnPlayPauseVertical != null) {
                        btnPlayPauseVertical.setImageResource(R.drawable.ic_pause);
                    } else if (!isVerticalMode && btnPlayPause != null) {
                        btnPlayPause.setImageResource(R.drawable.ic_pause);
                    }
                } else {
                    if (isVerticalMode && btnPlayPauseVertical != null) {
                        btnPlayPauseVertical.setImageResource(R.drawable.ic_play);
                    } else if (!isVerticalMode && btnPlayPause != null) {
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "更新播放图标失败", e);
            }
        });
    }

    private void setupGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    if (!isVerticalMode && e1 != null && e2 != null && e1.getX() - e2.getX() > 200 && Math.abs(velocityX) > 200) {
                        finish();
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        return true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "手势检测异常", e);
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                try {
                    if (!isClickOnControls(e)) {
                        handlePlayPause();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "单击事件异常", ex);
                }
                return true;
            }
        });
        if (containerLayout != null) {
            containerLayout.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        }
    }

    private boolean isClickOnControls(MotionEvent e) {
        try {
            if (isVerticalMode) {
                if (btnPlayPauseVertical != null) {
                    int[] location = new int[2];
                    btnPlayPauseVertical.getLocationOnScreen(location);
                    if (e.getRawX() >= location[0] && e.getRawX() <= location[0] + btnPlayPauseVertical.getWidth() &&
                            e.getRawY() >= location[1] && e.getRawY() <= location[1] + btnPlayPauseVertical.getHeight()) {
                        return true;
                    }
                }
                if (btnMenuVertical != null) {
                    int[] location = new int[2];
                    btnMenuVertical.getLocationOnScreen(location);
                    if (e.getRawX() >= location[0] && e.getRawX() <= location[0] + btnMenuVertical.getWidth() &&
                            e.getRawY() >= location[1] && e.getRawY() <= location[1] + btnMenuVertical.getHeight()) {
                        return true;
                    }
                }
                return false;
            } else {
                if (controlsContainer != null) {
                    int[] location = new int[2];
                    controlsContainer.getLocationOnScreen(location);
                    if (e.getRawX() >= location[0] && e.getRawX() <= location[0] + controlsContainer.getWidth() &&
                            e.getRawY() >= location[1] && e.getRawY() <= location[1] + controlsContainer.getHeight()) {
                        return true;
                    }
                }
                if (btnMenu != null) {
                    int[] location = new int[2];
                    btnMenu.getLocationOnScreen(location);
                    if (e.getRawX() >= location[0] && e.getRawX() <= location[0] + btnMenu.getWidth() &&
                            e.getRawY() >= location[1] && e.getRawY() <= location[1] + btnMenu.getHeight()) {
                        return true;
                    }
                }
                return false;
            }
        } catch (Exception ex) {
            Log.e(TAG, "点击位置检测异常", ex);
            return false;
        }
    }

    private void switchToVerticalMode() {
        if (isAnimating || isVerticalMode) return;
        isAnimating = true;
        isVerticalMode = true;
        stopAmbientLightAnimation();
        hideAmbientLights();
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        showImmersiveSystemUI();
        playRotateToVerticalAnimation();
    }

    private void switchToHorizontalMode() {
        if (isAnimating || !isVerticalMode) return;
        isAnimating = true;
        isVerticalMode = false;
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hideSystemUI();
        playRotateToHorizontalAnimation();
    }

    private void playRotateToVerticalAnimation() {
        try {
            if (currentCoverView != null) {
                currentCoverView.getLocationOnScreen(coverLocation);
                coverWidth = currentCoverView.getWidth();
                coverHeight = currentCoverView.getHeight();
                if (currentCoverView.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                    android.graphics.drawable.BitmapDrawable drawable = (android.graphics.drawable.BitmapDrawable) currentCoverView.getDrawable();
                    Bitmap currentBitmap = drawable.getBitmap();
                    if (currentBitmap != null && !currentBitmap.isRecycled()) {
                        currentAlbumBitmap = currentBitmap;
                        isDefaultCover = false;
                    } else {
                        isDefaultCover = true;
                    }
                }
            }
            setContentView(R.layout.activity_music_player_vertical);
            initVerticalViews();
            if (albumCoverVertical != null) {
                setCoverDirectly(albumCoverVertical);
            }
            if (backgroundImageVertical != null) {
                setBackgroundDirectly(backgroundImageVertical, 0.9f);
            }
            if (albumCoverVertical != null) {
                albumCoverVertical.getLocationOnScreen(targetCoverLocation);
                AnimationSet albumAnimationSet = new AnimationSet(true);
                float translateX = targetCoverLocation[0] - coverLocation[0];
                float translateY = targetCoverLocation[1] - coverLocation[1];
                TranslateAnimation albumTranslate = new TranslateAnimation(
                        Animation.ABSOLUTE, -translateX, Animation.ABSOLUTE, 0,
                        Animation.ABSOLUTE, -translateY, Animation.ABSOLUTE, 0
                );
                RotateAnimation albumRotate = new RotateAnimation(
                        -90, 0,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                albumAnimationSet.addAnimation(albumTranslate);
                albumAnimationSet.addAnimation(albumRotate);
                albumAnimationSet.setDuration(600);
                albumAnimationSet.setFillAfter(true);
                albumAnimationSet.setFillEnabled(true);
                albumAnimationSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        try {
                            if (albumCoverVertical != null) {
                                albumCoverVertical.clearAnimation();
                                if (currentAlbumBitmap != null && !currentAlbumBitmap.isRecycled()) {
                                    applyLongImageScaling(albumCoverVertical, currentAlbumBitmap);
                                }
                            }
                            albumAnimationSet.setFillAfter(false);
                            isAnimating = false;
                            updateVerticalUI();
                        } catch (Exception e) {
                            Log.e(TAG, "动画结束处理异常", e);
                            isAnimating = false;
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                albumCoverVertical.startAnimation(albumAnimationSet);
            } else {
                isAnimating = false;
                updateVerticalUI();
            }
        } catch (Exception e) {
            Log.e(TAG, "播放旋转到竖屏动画失败", e);
            setContentView(R.layout.activity_music_player_vertical);
            initVerticalViews();
            isAnimating = false;
            updateVerticalUI();
        }
    }

    private void playRotateToHorizontalAnimation() {
        try {
            if (currentCoverView != null) {
                currentCoverView.getLocationOnScreen(coverLocation);
                coverWidth = currentCoverView.getWidth();
                coverHeight = currentCoverView.getHeight();
                if (currentCoverView.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                    android.graphics.drawable.BitmapDrawable drawable = (android.graphics.drawable.BitmapDrawable) currentCoverView.getDrawable();
                    Bitmap currentBitmap = drawable.getBitmap();
                    if (currentBitmap != null && !currentBitmap.isRecycled()) {
                        currentAlbumBitmap = currentBitmap;
                        isDefaultCover = false;
                    } else {
                        isDefaultCover = true;
                    }
                }
            }
            setContentView(R.layout.activity_music_player);
            initViews();
            setupGesture();
            if (albumCover != null) {
                setCoverDirectly(albumCover);
            }
            if (backgroundImage != null) {
                setBackgroundDirectly(backgroundImage, 0.9f);
            }
            if (albumCover != null) {
                albumCover.getLocationOnScreen(targetCoverLocation);
                AnimationSet albumAnimationSet = new AnimationSet(true);
                float translateX = targetCoverLocation[0] - coverLocation[0];
                float translateY = targetCoverLocation[1] - coverLocation[1];
                TranslateAnimation albumTranslate = new TranslateAnimation(
                        Animation.ABSOLUTE, -translateX, Animation.ABSOLUTE, 0,
                        Animation.ABSOLUTE, -translateY, Animation.ABSOLUTE, 0
                );
                RotateAnimation albumRotate = new RotateAnimation(
                        90, 0,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                albumAnimationSet.addAnimation(albumTranslate);
                albumAnimationSet.addAnimation(albumRotate);
                albumAnimationSet.setDuration(600);
                albumAnimationSet.setFillAfter(true);
                albumAnimationSet.setFillEnabled(true);
                albumAnimationSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        try {
                            if (albumCover != null) {
                                albumCover.clearAnimation();
                                if (currentAlbumBitmap != null && !currentAlbumBitmap.isRecycled()) {
                                    applyLongImageScaling(albumCover, currentAlbumBitmap);
                                }
                            }
                            albumAnimationSet.setFillAfter(false);
                            isAnimating = false;
                            updateHorizontalUI();
                            if (isMusicPlaying) {
                                showAmbientLights();
                                startAmbientLightAnimation();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "动画结束处理异常", e);
                            isAnimating = false;
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                albumCover.startAnimation(albumAnimationSet);
            } else {
                isAnimating = false;
                updateHorizontalUI();
                if (isMusicPlaying) {
                    showAmbientLights();
                    startAmbientLightAnimation();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "播放旋转到横屏动画失败", e);
            setContentView(R.layout.activity_music_player);
            initViews();
            setupGesture();
            isAnimating = false;
            updateHorizontalUI();
            if (isMusicPlaying) {
                showAmbientLights();
                startAmbientLightAnimation();
            }
        }
    }

    private void updateHorizontalUI() {
        try {
            long currentTime = System.currentTimeMillis();
            if (songTitle != null) {
                String textToDisplay = TextUtils.isEmpty(lastLyricText) ? currentTitle : lastLyricText;
                String currentText = songTitle.getText().toString();
                boolean textChanged = !currentText.equals(textToDisplay);
                boolean shouldMarquee = shouldEnableMarquee(songTitle, textToDisplay, maxTitleCharsHorizontal);
                if (textChanged || shouldMarquee != enableTitleMarquee) {
                    enableTitleMarquee = shouldMarquee;
                    safeUpdateTextView(songTitle, textToDisplay, enableTitleMarquee);
                    lastTitleUpdateTime = currentTime;
                }
            }
            if (artistName != null) {
                String currentText = artistName.getText().toString();
                boolean textChanged = !currentText.equals(currentArtist);
                boolean shouldMarquee = shouldEnableMarquee(artistName, currentArtist, maxArtistCharsHorizontal);
                if (textChanged || shouldMarquee != enableArtistMarquee) {
                    enableArtistMarquee = shouldMarquee;
                    safeUpdateTextView(artistName, currentArtist, enableArtistMarquee);
                    lastArtistUpdateTime = currentTime;
                }
            }
            updateAlbumCoverSafe(albumCover, currentAlbumBitmap);
            if (backgroundImage != null) {
                if (currentAlbumBitmap != null && !currentAlbumBitmap.isRecycled() && !isDefaultCover) {
                    updateBackgroundImageWithBlur(backgroundImage, currentAlbumBitmap, 0.9f);
                } else {
                    backgroundImage.setVisibility(View.GONE);
                }
            }
            updatePlayPauseIcon();
            if (!isUserDragging && currentMediaDuration > 0 && progressBar != null) {
                int progress = (int) ((currentMediaPosition * 1000) / currentMediaDuration);
                progressBar.setProgress(progress);
                if (timeDisplay != null)
                    timeDisplay.setText(formatTime(currentMediaPosition) + " / " + formatTime(currentMediaDuration));
            }
        } catch (Exception e) {
            Log.e(TAG, "更新横屏UI失败", e);
        }
    }

    private void checkMusicStatus() {
        if (isLocalMode) {
            if (localMusicHelper != null) {
                boolean active = localMusicHelper.isPlaying();
                if (isMusicPlaying != active) {
                    isMusicPlaying = active;
                    updatePlayPauseIcon();
                    LocalMusicHelper.LocalSong song = localMusicHelper.getCurrentSong();
                    if (song != null) {
                        sendLocalMusicNotification(song.title, song.artist, MusicNotificationService.currentAlbumBitmap, isMusicPlaying, song.duration, localMusicHelper.getCurrentPosition());
                    }
                    if (isMusicPlaying) {
                        if (!isVerticalMode) {
                            showAmbientLights();
                            startAmbientLightAnimation();
                        }
                    } else {
                        if (!isVerticalMode) {
                            hideAmbientLights();
                            stopAmbientLightAnimation();
                        }
                    }
                }
            }
            return;
        }
        try {
            if (audioManager == null) return;
            boolean active = audioManager.isMusicActive();
            boolean wasMusicPlaying = isMusicPlaying;
            isMusicPlaying = active;
            if (isMusicPlaying != wasMusicPlaying) {
                updatePlayPauseIcon();
                if (isMusicPlaying) {
                    if (!isVerticalMode) {
                        showAmbientLights();
                        startAmbientLightAnimation();
                    }
                } else {
                    if (!isVerticalMode) {
                        hideAmbientLights();
                        stopAmbientLightAnimation();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking music status", e);
        }
    }

    private void showAmbientLights() {
        if (isVerticalMode) return;
        runOnUiThread(() -> {
            try {
                if (leftAmbientLight != null) leftAmbientLight.setVisibility(View.VISIBLE);
                if (rightAmbientLight != null) rightAmbientLight.setVisibility(View.VISIBLE);
                updateAmbientLightColor();
            } catch (Exception e) {
                Log.e(TAG, "显示氛围灯失败", e);
            }
        });
    }

    private void hideAmbientLights() {
        runOnUiThread(() -> {
            try {
                if (leftAmbientLight != null) leftAmbientLight.setVisibility(View.GONE);
                if (rightAmbientLight != null) rightAmbientLight.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "隐藏氛围灯失败", e);
            }
        });
    }

    private void startAmbientLightAnimation() {
        if (isVerticalMode) return;
        if (ambientLightHandler != null && !isAmbientLightAnimating) {
            isAmbientLightAnimating = true;
            lastAmbientLightTime = System.currentTimeMillis();
            ambientLightRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (isMusicPlaying && !isVerticalMode && !isFinishing()) {
                            long currentTime = System.currentTimeMillis();
                            long interval = currentTime - lastAmbientLightTime;
                            if (interval > AMBIENT_LIGHT_MAX_INTERVAL) {
                                performBreathingAnimation();
                            }
                            performBreathingAnimation();
                            currentAmbientColorIndex = (currentAmbientColorIndex + 1) % AMBIENT_COLORS.length;
                            lastAmbientLightTime = System.currentTimeMillis();
                            int duration = randomGenerator.nextInt(2200) + 800;
                            ambientLightHandler.postDelayed(this, duration);
                        } else {
                            isAmbientLightAnimating = false;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "氛围灯动画异常", e);
                        isAmbientLightAnimating = false;
                    }
                }
            };
            ambientLightHandler.post(ambientLightRunnable);
        }
    }

    private void performBreathingAnimation() {
        if (isVerticalMode) return;
        try {
            final int color = AMBIENT_COLORS[currentAmbientColorIndex];
            runOnUiThread(() -> {
                try {
                    if (leftAmbientLight == null || rightAmbientLight == null) return;
                    GradientDrawable leftGradient = new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{color, Color.TRANSPARENT});
                    leftGradient.setCornerRadius(0);
                    leftAmbientLight.setBackground(leftGradient);
                    GradientDrawable rightGradient = new GradientDrawable(
                            GradientDrawable.Orientation.RIGHT_LEFT,
                            new int[]{color, Color.TRANSPARENT});
                    rightGradient.setCornerRadius(0);
                    rightAmbientLight.setBackground(rightGradient);
                    createBreathingSequence();
                } catch (Exception e) {
                    Log.e(TAG, "执行呼吸动画失败", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error performing breathing animation", e);
        }
    }

    private void createBreathingSequence() {
        if (isVerticalMode) return;
        try {
            if (leftAmbientLight == null || rightAmbientLight == null) return;
            int speedType = randomGenerator.nextInt(3);
            int fadeInDuration, holdDuration, fadeOutDuration;
            switch (speedType) {
                case 0:
                    fadeInDuration = 800;
                    holdDuration = 600;
                    fadeOutDuration = 800;
                    break;
                case 1:
                    fadeInDuration = 400;
                    holdDuration = 300;
                    fadeOutDuration = 400;
                    break;
                default:
                    fadeInDuration = 200;
                    holdDuration = 150;
                    fadeOutDuration = 200;
                    break;
            }
            leftAmbientLight.setAlpha(0f);
            leftAmbientLight.animate().alpha(1.0f).setDuration(fadeInDuration)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            rightAmbientLight.setAlpha(0f);
            rightAmbientLight.animate().alpha(1.0f).setDuration(fadeInDuration)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            ambientLightHandler.postDelayed(() -> {
                try {
                    if (leftAmbientLight != null)
                        leftAmbientLight.animate().alpha(0f).setDuration(fadeOutDuration)
                                .setInterpolator(new android.view.animation.AccelerateInterpolator()).start();
                    if (rightAmbientLight != null)
                        rightAmbientLight.animate().alpha(0f).setDuration(fadeOutDuration)
                                .setInterpolator(new android.view.animation.AccelerateInterpolator()).start();
                } catch (Exception e) {
                    Log.e(TAG, "淡出动画异常", e);
                }
            }, fadeInDuration + holdDuration);
        } catch (Exception e) {
            Log.e(TAG, "Error creating breathing sequence", e);
        }
    }

    private void stopAmbientLightAnimation() {
        isAmbientLightAnimating = false;
        if (ambientLightHandler != null && ambientLightRunnable != null) {
            ambientLightHandler.removeCallbacks(ambientLightRunnable);
        }
    }

    private void updateAmbientLightColor() {
        performBreathingAnimation();
    }

    private void hideSystemUI() {
        runOnUiThread(() -> {
            try {
                View decorView = getWindow().getDecorView();
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(uiOptions);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } catch (Exception e) {
                Log.e(TAG, "隐藏系统UI失败", e);
            }
        });
    }

    private void showImmersiveSystemUI() {
        runOnUiThread(() -> {
            try {
                View decorView = getWindow().getDecorView();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    uiOptions |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    uiOptions &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                decorView.setSystemUiVisibility(uiOptions);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(Color.TRANSPARENT);
                    getWindow().setNavigationBarColor(Color.TRANSPARENT);
                } else {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                }
            } catch (Exception e) {
                Log.e(TAG, "显示沉浸式系统UI失败", e);
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (!isVerticalMode) switchToVerticalMode();
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (isVerticalMode) switchToHorizontalMode();
        }
    }

    private class MusicReceiver extends BroadcastReceiver {
        private boolean wasMusicPlaying = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isLocalMode) return;
            try {
                if (MusicNotificationService.ACTION_MUSIC_UPDATE.equals(intent.getAction())) {
                    String title = intent.getStringExtra(MusicNotificationService.EXTRA_TITLE);
                    String artist = intent.getStringExtra(MusicNotificationService.EXTRA_ARTIST);
                    String album = intent.getStringExtra(MusicNotificationService.EXTRA_ALBUM);
                    boolean playing = intent.getBooleanExtra(MusicNotificationService.EXTRA_IS_PLAYING, false);
                    final String finalTitle = title != null && !TextUtils.isEmpty(title) ? title : "等待音乐播放...";
                    String displayArtist;
                    if (artist != null && !TextUtils.isEmpty(artist)) {
                        if (album != null && !TextUtils.isEmpty(album) && !album.equals(artist)) {
                            displayArtist = artist + " - " + album;
                        } else {
                            displayArtist = artist;
                        }
                    } else if (album != null && !TextUtils.isEmpty(album)) {
                        displayArtist = album;
                    } else {
                        displayArtist = "歌手未知";
                    }
                    final String finalArtist = displayArtist;
                    final String finalAlbum = album != null ? album : "";
                    Bitmap tempAlbumBitmap = null;
                    try {
                        Bitmap albumBitmap = MusicNotificationService.currentAlbumBitmap;
                        if (albumBitmap != null && !albumBitmap.isRecycled()) {
                            tempAlbumBitmap = albumBitmap;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "从服务获取位图失败", e);
                    }
                    final Bitmap finalAlbumBitmap = tempAlbumBitmap;
                    runOnUiThread(() -> {
                        try {
                            if (isFinishing() || isDestroyed()) return;
                            currentTitle = finalTitle;
                            currentArtist = finalArtist;
                            currentAlbum = finalAlbum;
                            currentAlbumBitmap = finalAlbumBitmap;
                            if (!isVerticalMode) {
                                updateHorizontalUI();
                            } else {
                                updateVerticalUI();
                            }
                            boolean wasPlaying = wasMusicPlaying;
                            isMusicPlaying = playing;
                            hasActiveMediaSession = !TextUtils.isEmpty(title);
                            if (isMusicPlaying != wasPlaying) {
                                updatePlayPauseIcon();
                            }
                            if (isMusicPlaying && !isVerticalMode) {
                                showAmbientLights();
                                startAmbientLightAnimation();
                            } else if (!isMusicPlaying && !isVerticalMode) {
                                hideAmbientLights();
                                stopAmbientLightAnimation();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "UI更新异常", e);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "广播接收异常", e);
            }
        }
    }

    private class LocalControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isLocalMode) return;
            String action = intent.getAction();
            if (MusicNotificationService.ACTION_LOCAL_PLAY.equals(action)) {
                if (!localMusicHelper.isPlaying()) handlePlayPause();
            } else if (MusicNotificationService.ACTION_LOCAL_PAUSE.equals(action)) {
                if (localMusicHelper.isPlaying()) handlePlayPause();
            } else if (MusicNotificationService.ACTION_LOCAL_NEXT.equals(action)) {
                handleNext();
            } else if (MusicNotificationService.ACTION_LOCAL_PREV.equals(action)) {
                handlePrev();
            } else if (MusicNotificationService.ACTION_LOCAL_SEEK.equals(action)) {
                long pos = intent.getLongExtra(MusicNotificationService.EXTRA_POSITION, 0);
                if (isLocalMode && localMusicHelper != null) {
                    localMusicHelper.seekTo(pos);
                    currentMediaPosition = pos;
                    updateProgressUI(currentMediaPosition, currentMediaDuration);
                    LocalMusicHelper.LocalSong song = localMusicHelper.getCurrentSong();
                    if (song != null) {
                        sendLocalMusicNotification(song.title, song.artist, MusicNotificationService.currentAlbumBitmap, localMusicHelper.isPlaying(), currentMediaDuration, currentMediaPosition);
                    }
                }
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (!isVerticalMode) {
                hideSystemUI();
            } else {
                showImmersiveSystemUI();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityResumed = true;
        try {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(MusicNotificationService.ACTION_APP_FOREGROUND));
            acquireWakeLock();
            if (isVerticalMode) {
                showImmersiveSystemUI();
            } else {
                hideSystemUI();
            }
            if (!isLocalMode) {
                new Handler().postDelayed(this::checkAndStartNotificationService, 500);
            }
            checkMusicStatus();
            
            if (isMusicPlaying && !isVerticalMode) {
                showAmbientLights();
                startAmbientLightAnimation();
            }
            if (progressHandler != null) {
                progressHandler.removeCallbacks(progressRunnable);
                progressHandler.post(progressRunnable);
            }
        } catch (Exception e) {
            Log.e(TAG, "onResume异常", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityResumed = false;
        try {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(MusicNotificationService.ACTION_APP_BACKGROUND));
            releaseWakeLock();
            if (!isVerticalMode) stopAmbientLightAnimation();
        } catch (Exception e) {
            Log.e(TAG, "onPause异常", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseWakeLock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupResources();
    }

    private void cleanupResources() {
        try {
            if (musicReceiver != null) LocalBroadcastManager.getInstance(this).unregisterReceiver(musicReceiver);
            if (localControlReceiver != null)
                LocalBroadcastManager.getInstance(this).unregisterReceiver(localControlReceiver);
            stopAmbientLightAnimation();
            if (musicHandler != null) musicHandler.removeCallbacksAndMessages(null);
            if (ambientLightHandler != null) ambientLightHandler.removeCallbacksAndMessages(null);
            if (progressHandler != null) progressHandler.removeCallbacksAndMessages(null);
            if (blurHandler != null) blurHandler.removeCallbacksAndMessages(null);
            if (draggingHandler != null) draggingHandler.removeCallbacksAndMessages(null);
            if (tvSeekHandler != null) tvSeekHandler.removeCallbacksAndMessages(null); 
            audioManager = null;
            releaseWakeLock();
            wakeLock = null;
            lastDisplayedBitmap = null;
            lastBackgroundBitmap = null;
            lastProcessedBitmap = null;
            if (currentBlurredBitmap != null && !currentBlurredBitmap.isRecycled()) {
                currentBlurredBitmap.recycle();
                currentBlurredBitmap = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "清理资源失败", e);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}