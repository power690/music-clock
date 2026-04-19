package com.xiaowei.music;

import android.Manifest;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.res.TypedArray;
import android.content.res.Resources;
import android.media.AudioManager;
import android.content.Context;
import android.util.Log;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.graphics.Typeface;
import android.view.Gravity;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Button;
import android.widget.Toast;
import android.view.KeyEvent;
import android.app.UiModeManager;
import android.content.pm.PackageManager;
import android.app.ActivityManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "ClockPrefs";
    private static final String PREF_TEXT_COLOR_INDEX = "textColorIndex";
    private static final String PREF_BG_COLOR_INDEX = "bgColorIndex";
    private static final String PREF_STYLE_INDEX = "styleIndex";
    private static final String PREF_LAST_WEATHER_UPDATE = "lastWeatherUpdate";
    private static final String PREF_WEATHER_DATA = "weatherData";
    private static final String PREF_PROVINCE = "province";
    private static final String PREF_CITY = "city";
    private static final String PREF_DISTRICT = "district";
    private static final String PREF_LOCATION_SET = "locationSet";
    private static final String PREF_SHOWED_MOBILE_WARNING = "showedMobileWarning";
    private static final String PREF_LAST_WALLPAPER_MINUTE = "lastWallpaperMinute";
    private static final String PREF_OPERATION_GUIDE_SHOWN = "operationGuideShown";
    private static final String TAG = "HorizontalClock";

    
    private static final int LOCATION_SETTING_REQUEST = 1001;
    private static final int OPERATION_GUIDE_REQUEST = 1002;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1004;

    private TextView timeTextView;
    private TextView dateTextView;
    private TextView lunarTextView;
    private TextView festivalTextView;
    private TextView weatherTextView;
    private ImageView weatherIconImageView;
    private ImageView chargingIndicatorImageView;
    private FrameLayout containerLayout;
    private RelativeLayout clockContainer;
    private LinearLayout weatherContainer;
    private Handler handler;
    private Runnable updateTimeRunnable;
    private PowerManager.WakeLock wakeLock;
    private LinearLayout batteryContainer;
    private FrameLayout batteryBody;
    private View batteryTip;
    private View batteryLevel;
    private TextView batteryPercentTextView;

    
    private View leftAmbientLight;
    private View rightAmbientLight;
    private AudioManager audioManager;
    private boolean isMusicPlaying = false;
    private Handler musicHandler;
    private Runnable musicCheckRunnable;
    private int currentAmbientColorIndex = 0;
    private Handler ambientLightHandler;
    private Runnable ambientLightRunnable;

    
    private static final int[] AMBIENT_COLORS = {
            Color.parseColor("#FF00FF"), Color.parseColor("#00FFFF"), Color.parseColor("#FFFF00"),
            Color.parseColor("#FF69B4"), Color.parseColor("#00FF00"), Color.parseColor("#FFA500"),
            Color.parseColor("#9400D3"), Color.parseColor("#FF0000"), Color.parseColor("#FF1493"),
            Color.parseColor("#00CED1"), Color.parseColor("#FFD700"), Color.parseColor("#FF6347"),
            Color.parseColor("#4169E1"), Color.parseColor("#32CD32"), Color.parseColor("#FF4500"),
            Color.parseColor("#DA70D6"), Color.parseColor("#00FA9A"), Color.parseColor("#1E90FF"),
            Color.parseColor("#CD5C5C"), Color.parseColor("#4682B4"), Color.parseColor("#DAA520"),
            Color.parseColor("#FF8C00"), Color.parseColor("#8B008B"), Color.parseColor("#008080"),
            Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"), Color.parseColor("#95E1D3"),
            Color.parseColor("#F38181"), Color.parseColor("#AA96DA"), Color.parseColor("#FCBAD3"),
            Color.parseColor("#FAD02E"), Color.parseColor("#F1C40F"), Color.parseColor("#E74C3C"),
            Color.parseColor("#3498DB"), Color.parseColor("#2ECC71"), Color.parseColor("#F39C12"),
            Color.parseColor("#9B59B6"), Color.parseColor("#1ABC9C"), Color.parseColor("#E67E22"),
            Color.parseColor("#95A5A6"), Color.parseColor("#34495E"), Color.parseColor("#16A085"),
            Color.parseColor("#27AE60"), Color.parseColor("#2980B9"), Color.parseColor("#8E44AD"),
            Color.parseColor("#F1C40F"), Color.parseColor("#D35400"), Color.parseColor("#C0392B"),
            Color.parseColor("#7F8C8D")
    };

    private int currentTextColorIndex = 0;
    private int currentBgColorIndex = 0;
    private int currentStyleIndex = 0;
    private int[] textColors;
    private int[] backgroundColors;

    private String currentLunarDate = "";
    private String currentDateString = "";
    private String currentFestival = "";
    private Random randomGenerator = new Random();
    private SharedPreferences preferences;
    private int lastMinute = -1;
    private int lastSecond = -1;
    private int lastWallpaperMinute = -1;
    private GestureDetector gestureDetector;
    private boolean isDoubleTap = false;
    private boolean isTVMode = false;
    private View currentFocusedView;
    private long backPressedTime = 0;
    private BatteryReceiver batteryReceiver;
    private boolean isBatteryReceiverRegistered = false;
    private int batteryLevelValue = -1;
    private boolean isCharging = false;
    private long lastWeatherUpdateTime = 0;
    private static final long WEATHER_UPDATE_INTERVAL = 10 * 60 * 1000;
    private String currentProvince = "北京";
    private String currentCity = "北京";
    private String currentDistrict = "";
    private String currentLocationName = "北京";
    
    private File wallpaperCacheDir;
    private ImageView wallpaperImageView;
    private ConnectivityManager connectivityManager;
    private boolean isUsingMobileData = false;
    private boolean isNetworkConnected = true;
    private boolean showedMobileWarning = false;
    private NetworkReceiver networkReceiver;
    private static ExecutorService wallpaperExecutor;
    private boolean hasWallpaperDisplayed = false;
    private boolean isLocationSettingOpen = false;
    private boolean isAppInitializing = false;
    private boolean isAmbientLightAnimating = false;
    private long lastAmbientLightTime = 0;
    private static final long AMBIENT_LIGHT_MAX_INTERVAL = 500;
    private long lastOkKeyTime = 0;
    private static final long DOUBLE_TAP_TIMEOUT = 300;
    private boolean needsFullRestart = false;
    private boolean isWallpaperLoading = false;
    private boolean useLowQualityWallpaper = false;
    private static final int LOW_QUALITY_SAMPLE_SIZE = 4;
    private static final int NORMAL_QUALITY_SAMPLE_SIZE = 2;
    private static boolean isExecutorShutdown = false;
    private static MainActivity currentInstance = null;

    private class BatteryReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            if (level >= 0 && scale > 0) {
                batteryLevelValue = (int) (level * 100 / (float) scale);
            }
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            updateBatteryDisplay();
        }
    }

    private class NetworkReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkNetworkType();
        }
    }

    private boolean isDestroyedCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                return isDestroyed();
            } catch (Exception e) {
                return isFinishing();
            }
        }
        return isFinishing();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isAppInitializing = true;
        currentInstance = this;
        try {
            hideNavigationBar();
            setContentView(R.layout.activity_main);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    String appLabel = getApplicationInfo().loadLabel(getPackageManager()).toString();
                    ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(appLabel, null, 0xFF000000);
                    setTaskDescription(taskDescription);
                } catch (Exception e) {}
            }

            if (wallpaperExecutor == null || wallpaperExecutor.isShutdown()) {
                wallpaperExecutor = Executors.newFixedThreadPool(1);
                isExecutorShutdown = false;
            }

            preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
            isTVMode = (getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                    (uiModeManager != null && uiModeManager.getCurrentModeType() == UiModeManager.MODE_NIGHT_YES));

            boolean operationGuideShown = preferences.getBoolean(PREF_OPERATION_GUIDE_SHOWN, false);
            if (!operationGuideShown) {
                showOperationGuide();
                return;
            }

            boolean locationSet = preferences.getBoolean(PREF_LOCATION_SET, false);
            if (!locationSet) {
                showLocationSetting();
                return;
            }

            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            networkReceiver = new NetworkReceiver();
            IntentFilter networkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(networkReceiver, networkFilter);
            checkNetworkType();

            wallpaperCacheDir = new File(getCacheDir(), "wallpapers");
            if (!wallpaperCacheDir.exists()) wallpaperCacheDir.mkdirs();
            lastWallpaperMinute = preferences.getInt(PREF_LAST_WALLPAPER_MINUTE, -1);

            timeTextView = findViewById(R.id.timeTextView);
            dateTextView = findViewById(R.id.dateTextView);
            lunarTextView = findViewById(R.id.lunarTextView);
            festivalTextView = findViewById(R.id.festivalTextView);
            weatherTextView = findViewById(R.id.weatherTextView);
            weatherIconImageView = findViewById(R.id.weatherIconImageView);
            containerLayout = findViewById(R.id.containerLayout);
            clockContainer = findViewById(R.id.clockContainer);
            weatherContainer = findViewById(R.id.weatherContainer);
            wallpaperImageView = findViewById(R.id.wallpaperImageView);
            batteryContainer = findViewById(R.id.batteryContainer);
            if (!isTVMode) createBatteryIcon();
            initAmbientLights();

            batteryReceiver = new BatteryReceiver();
            if (!isTVMode) registerBatteryReceiver();

            loadLocationData();
            setupFocusForTV();

            gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (!isDoubleTap) {
                        if (isClickOnClock(e)) {
                            changeTextColors();
                        } else {
                            forceUpdateWallpaperImmediate();
                        }
                    }
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    isDoubleTap = true;
                    if (isClickOnClock(e)) {
                        changeStyle();
                    }
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isDoubleTap = false;
                        }
                    }, 500);
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 100) {
                        showLocationSetting();
                        return true;
                    }
                    if (e2.getX() - e1.getX() > 100 && Math.abs(velocityX) > 100) {
                        if (isTVMode) {
                            goToMusicPlayer();
                            return true;
                        }
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            Toast.makeText(MainActivity.this, "系统版本不支持", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        checkAndRequestNotificationPermission();
                        return true;
                    }
                    return false;
                }
            });

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "HorizontalClock:ScreenLock");
            }

            TypedArray textColorsTa = getResources().obtainTypedArray(R.array.text_colors);
            int textColorsLength = textColorsTa.length();
            textColors = new int[textColorsLength];
            for (int i = 0; i < textColorsLength; i++) textColors[i] = textColorsTa.getColor(i, 0);
            textColorsTa.recycle();

            TypedArray bgColorsTa = getResources().obtainTypedArray(R.array.background_colors);
            int bgColorsLength = bgColorsTa.length();
            backgroundColors = new int[bgColorsLength];
            for (int i = 0; i < bgColorsLength; i++) backgroundColors[i] = bgColorsTa.getColor(i, 0);
            bgColorsTa.recycle();

            loadColorPreferences();
            showedMobileWarning = preferences.getBoolean(PREF_SHOWED_MOBILE_WARNING, false);
            applyStyle();

            containerLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (isTVMode) return false;
                    gestureDetector.onTouchEvent(event);
                    if (event.getAction() == MotionEvent.ACTION_UP) v.performClick();
                    return true;
                }
            });

            handler = new Handler();
            updateTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    updateTimeAndDate();
                    handler.postDelayed(this, 1000);
                }
            };
            updateTimeAndDate();
            checkAndUpdateWeather();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fetchAndDisplayWallpaper();
                }
            }, 1000);
            handler.post(updateTimeRunnable);
            clockContainer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    centerClock();
                }
            }, 100);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        } finally {
            isAppInitializing = false;
        }
    }

    private void goToMusicPlayer() {
        Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void checkAndRequestNotificationPermission() {
        if (isTVMode) {
            goToMusicPlayer();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String listeners = android.provider.Settings.Secure.getString(
                    getContentResolver(), "enabled_notification_listeners");
            if (listeners != null && listeners.contains(getPackageName())) {
                checkPostNotificationPermissionAndGo();
            } else {
                showNotificationPermissionDialog();
            }
        } else {
            Toast.makeText(this, "系统版本不支持", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPostNotificationPermissionAndGo() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            } else {
                goToMusicPlayer();
            }
        } else {
            goToMusicPlayer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "未授予通知权限，通知栏无法显示控制条", Toast.LENGTH_SHORT).show();
            }
            goToMusicPlayer();
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
                    ((TextView) msgView).setText("为了控制音乐播放和显示通知栏，需要您授予通知权限。\n\n请在设置页面中启用本应用。");
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
                Toast.makeText(MainActivity.this, "功能可能受限", Toast.LENGTH_SHORT).show();
                checkPostNotificationPermissionAndGo();
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

    private void setupFocusForTV() {
        if (!isTVMode) return;
        containerLayout.setFocusable(true);
        containerLayout.setFocusableInTouchMode(true);
        clockContainer.setFocusable(true);
        clockContainer.setFocusableInTouchMode(true);
        clockContainer.requestFocus();
        clockContainer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    currentFocusedView = v;
                    clockContainer.setBackgroundColor(Color.parseColor("#80FFFFFF"));
                } else {
                    clockContainer.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });
        containerLayout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    currentFocusedView = v;
                    containerLayout.setBackgroundColor(Color.parseColor("#80FFFFFF"));
                } else {
                    containerLayout.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });
    }

    private boolean isClickOnClock(MotionEvent e) {
        int[] location = new int[2];
        clockContainer.getLocationOnScreen(location);
        return e.getRawX() >= location[0] &&
                e.getRawX() <= location[0] + clockContainer.getWidth() &&
                e.getRawY() >= location[1] &&
                e.getRawY() <= location[1] + clockContainer.getHeight();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isTVMode) return super.onKeyDown(keyCode, event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                if (isTVMode) {
                    goToMusicPlayer();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastOkKeyTime < DOUBLE_TAP_TIMEOUT) {
                    showLocationSetting();
                    lastOkKeyTime = 0;
                } else {
                    if (currentFocusedView == clockContainer) {
                        changeTextColors();
                    } else {
                        forceUpdateWallpaperImmediate();
                    }
                    lastOkKeyTime = currentTime;
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                changeTextColors();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                changeStyle();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        finish();
                        return true;
                    } else {
                        backPressedTime = System.currentTimeMillis();
                        Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initAmbientLights() {
        try {
            leftAmbientLight = new View(this);
            FrameLayout.LayoutParams leftParams = new FrameLayout.LayoutParams(
                    dpToPx(100), FrameLayout.LayoutParams.MATCH_PARENT);
            leftParams.gravity = Gravity.LEFT;
            leftAmbientLight.setLayoutParams(leftParams);
            containerLayout.addView(leftAmbientLight);

            rightAmbientLight = new View(this);
            FrameLayout.LayoutParams rightParams = new FrameLayout.LayoutParams(
                    dpToPx(100), FrameLayout.LayoutParams.MATCH_PARENT);
            rightParams.gravity = Gravity.RIGHT;
            rightAmbientLight.setLayoutParams(rightParams);
            containerLayout.addView(rightAmbientLight);

            leftAmbientLight.setVisibility(View.GONE);
            rightAmbientLight.setVisibility(View.GONE);

            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            musicHandler = new Handler();
            musicCheckRunnable = new Runnable() {
                @Override
                public void run() {
                    checkMusicStatus();
                    musicHandler.postDelayed(this, 100);
                }
            };

            ambientLightHandler = new Handler();
            startMusicDetection();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ambient lights", e);
        }
    }

    private void startMusicDetection() {
        try {
            if (musicHandler != null && musicCheckRunnable != null) {
                musicHandler.post(musicCheckRunnable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting music detection", e);
        }
    }

    private void stopMusicDetection() {
        try {
            if (musicHandler != null && musicCheckRunnable != null) {
                musicHandler.removeCallbacks(musicCheckRunnable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping music detection", e);
        }
    }

    private void checkMusicStatus() {
        try {
            boolean wasMusicPlaying = isMusicPlaying;
            isMusicPlaying = audioManager.isMusicActive();
            if (isMusicPlaying && !wasMusicPlaying) {
                showAmbientLights();
                startAmbientLightAnimation();
            } else if (!isMusicPlaying && wasMusicPlaying) {
                hideAmbientLights();
                stopAmbientLightAnimation();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking music status", e);
        }
    }

    private void showAmbientLights() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    leftAmbientLight.setVisibility(View.VISIBLE);
                    rightAmbientLight.setVisibility(View.VISIBLE);
                    updateAmbientLightColor();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing ambient lights", e);
        }
    }

    private void hideAmbientLights() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    leftAmbientLight.setVisibility(View.GONE);
                    rightAmbientLight.setVisibility(View.GONE);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error hiding ambient lights", e);
        }
    }

    private void startAmbientLightAnimation() {
        try {
            if (ambientLightHandler != null && !isAmbientLightAnimating) {
                isAmbientLightAnimating = true;
                lastAmbientLightTime = System.currentTimeMillis();
                ambientLightRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (isMusicPlaying) {
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
                    }
                };
                ambientLightHandler.post(ambientLightRunnable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting ambient light animation", e);
        }
    }

    private void performBreathingAnimation() {
        try {
            final int color = AMBIENT_COLORS[currentAmbientColorIndex];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GradientDrawable leftGradient = new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{color, Color.TRANSPARENT}
                    );
                    leftGradient.setCornerRadius(0);
                    leftAmbientLight.setBackground(leftGradient);

                    GradientDrawable rightGradient = new GradientDrawable(
                            GradientDrawable.Orientation.RIGHT_LEFT,
                            new int[]{color, Color.TRANSPARENT}
                    );
                    rightGradient.setCornerRadius(0);
                    rightAmbientLight.setBackground(rightGradient);

                    createBreathingSequence();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error performing breathing animation", e);
        }
    }

    private void createBreathingSequence() {
        try {
            int speedType = randomGenerator.nextInt(3);
            int fadeInDuration, holdDuration, fadeOutDuration;
            switch (speedType) {
                case 0:
                    fadeInDuration = 800; holdDuration = 600; fadeOutDuration = 800; break;
                case 1:
                    fadeInDuration = 400; holdDuration = 300; fadeOutDuration = 400; break;
                case 2:
                default:
                    fadeInDuration = 200; holdDuration = 150; fadeOutDuration = 200; break;
            }

            leftAmbientLight.setAlpha(0f);
            rightAmbientLight.setAlpha(0f);

            leftAmbientLight.animate().alpha(1.0f).setDuration(fadeInDuration).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            rightAmbientLight.animate().alpha(1.0f).setDuration(fadeInDuration).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

            ambientLightHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    leftAmbientLight.animate().alpha(0f).setDuration(fadeOutDuration).setInterpolator(new android.view.animation.AccelerateInterpolator()).start();
                    rightAmbientLight.animate().alpha(0f).setDuration(fadeOutDuration).setInterpolator(new android.view.animation.AccelerateInterpolator()).start();
                }
            }, fadeInDuration + holdDuration);
        } catch (Exception e) {
            Log.e(TAG, "Error creating breathing sequence", e);
        }
    }

    private void stopAmbientLightAnimation() {
        try {
            isAmbientLightAnimating = false;
            if (ambientLightHandler != null && ambientLightRunnable != null) {
                ambientLightHandler.removeCallbacks(ambientLightRunnable);
                ambientLightRunnable = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping ambient light animation", e);
        }
    }

    private void updateAmbientLightColor() {
        try {
            final int color = AMBIENT_COLORS[currentAmbientColorIndex];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GradientDrawable leftGradient = new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{color, Color.TRANSPARENT}
                    );
                    leftGradient.setCornerRadius(0);
                    leftAmbientLight.setBackground(leftGradient);

                    GradientDrawable rightGradient = new GradientDrawable(
                            GradientDrawable.Orientation.RIGHT_LEFT,
                            new int[]{color, Color.TRANSPARENT}
                    );
                    rightGradient.setCornerRadius(0);
                    rightAmbientLight.setBackground(rightGradient);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error updating ambient light color", e);
        }
    }

    private void showOperationGuide() {
        try {
            Intent intent = new Intent(this, OperationGuideActivity.class);
            startActivityForResult(intent, OPERATION_GUIDE_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error showing operation guide", e);
        }
    }

    private void showLocationSetting() {
        try {
            if (isLocationSettingOpen) return;
            isLocationSettingOpen = true;
            cleanupResources();
            needsFullRestart = true;
            Intent intent = new Intent(this, LocationSettingActivity.class);
            startActivityForResult(intent, LOCATION_SETTING_REQUEST);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error showing location setting", e);
            isLocationSettingOpen = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPERATION_GUIDE_REQUEST) {
            boolean locationSet = preferences.getBoolean(PREF_LOCATION_SET, false);
            if (!locationSet) {
                showLocationSetting();
            } else {
                recreate();
            }
        } else if (requestCode == LOCATION_SETTING_REQUEST) {
            isLocationSettingOpen = false;
            if (resultCode == RESULT_OK) {
                needsFullRestart = true;
                cleanupResources();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                boolean locationSet = preferences.getBoolean(PREF_LOCATION_SET, false);
                if (!locationSet) {
                    showLocationSetting();
                } else {
                    recreate();
                }
            }
        }
    }

    private void cleanupResources() {
        try {
            if (handler != null) handler.removeCallbacksAndMessages(null);
            if (musicHandler != null) musicHandler.removeCallbacksAndMessages(null);
            if (ambientLightHandler != null) ambientLightHandler.removeCallbacksAndMessages(null);

            stopMusicDetection();
            stopAmbientLightAnimation();

            if (batteryReceiver != null && isBatteryReceiverRegistered) {
                try {
                    unregisterReceiver(batteryReceiver);
                    isBatteryReceiverRegistered = false;
                } catch (Exception e) {}
            }
            if (networkReceiver != null) {
                try {
                    unregisterReceiver(networkReceiver);
                } catch (Exception e) {}
            }

            if (wakeLock != null && wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (Exception e) {}
            }

            if (wallpaperImageView != null) {
                wallpaperImageView.setImageBitmap(null);
            }

            if (currentInstance == this) {
                currentInstance = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up resources", e);
        }
    }

    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow()
                .setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideNavigationBar();
        }
    }

    private void checkNetworkType() {
        try {
            if (connectivityManager == null) {
                connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager == null) {
                    isUsingMobileData = false;
                    isNetworkConnected = false;
                    return;
                }
            }
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            boolean wasConnected = isNetworkConnected;

            if (activeNetwork != null && activeNetwork.isConnected()) {
                isNetworkConnected = true;
                isUsingMobileData = activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
                if (!wasConnected) {
                    Log.d(TAG, "Network reconnected, instantly refreshing wallpaper.");
                    forceUpdateWallpaperImmediate();
                }
                if (isUsingMobileData && !showedMobileWarning) {
                    showMobileDataWarning();
                    showedMobileWarning = true;
                    preferences.edit().putBoolean(PREF_SHOWED_MOBILE_WARNING, true).apply();
                }
            } else {
                isNetworkConnected = false;
                isUsingMobileData = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network type", e);
            isUsingMobileData = false;
            isNetworkConnected = false;
        }
    }

    private void showMobileDataWarning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(MainActivity.this, "检测到您正在使用移动数据，壁纸加载可能会消耗较多流量。", Toast.LENGTH_LONG).show();
                } catch (Exception e) {}
            }
        });
    }

     
    private void trustAllHostsForOldAndroid(HttpURLConnection connection) {
        if (connection instanceof HttpsURLConnection) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[]{};
                            }
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        }
                };
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                ((HttpsURLConnection) connection).setSSLSocketFactory(sc.getSocketFactory());
                ((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

     
    private void fetchAndDisplayWallpaper() {
        if (isExecutorShutdown || wallpaperExecutor == null || wallpaperExecutor.isShutdown() || isFinishing() || isDestroyedCompat()) {
            return;
        }
        if (isWallpaperLoading) return;
        isWallpaperLoading = true;

        wallpaperExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isFinishing() || isDestroyedCompat()) {
                        isWallpaperLoading = false;
                        return;
                    }

                    if (!isNetworkConnected) {
                        displayOfflineFallback();
                        return;
                    }

                    
                    String targetUrl = (randomGenerator.nextInt(100) < 50) ? 
                            "https://api.eyabc.cn/api/picture/scenery" : 
                            "https://api.eyabc.cn/api/picture/dong_man";

                    File tempFile = new File(wallpaperCacheDir, "wp_" + System.currentTimeMillis() + ".jpg");
                    
                    
                    if (downloadFile(targetUrl, tempFile)) {
                        displayLocalWallpaper(tempFile.getAbsolutePath());
                        cleanupOldWallpapers(); 
                    } else {
                        displayOfflineFallback();
                    }
                } catch (OutOfMemoryError e) {
                    useLowQualityWallpaper = true;
                    System.gc();
                    displayOfflineFallback();
                } catch (Exception e) {
                    displayOfflineFallback();
                }
            }
        });
    }

     
    private boolean downloadFile(String urlStr, File destFile) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            trustAllHostsForOldAndroid(connection); 
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false); 

            int responseCode = connection.getResponseCode();
            int redirectCount = 0;
            
            
            while (redirectCount < 5 && (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                    || responseCode == 307 || responseCode == 308)) {
                
                String location = connection.getHeaderField("Location");
                if (location == null) break;
                
                connection.disconnect();
                url = new URL(location);
                connection = (HttpURLConnection) url.openConnection();
                trustAllHostsForOldAndroid(connection);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(false);
                responseCode = connection.getResponseCode();
                redirectCount++;
            }
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(destFile);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    if (Thread.currentThread().isInterrupted() || isFinishing() || isDestroyedCompat()) {
                        break;
                    }
                }
                outputStream.flush();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Download file failed: " + urlStr, e);
        } finally {
            if (outputStream != null) try { outputStream.close(); } catch (Exception e) {}
            if (inputStream != null) try { inputStream.close(); } catch (Exception e) {}
            if (connection != null) connection.disconnect();
        }
        if (destFile.exists()) destFile.delete();
        return false;
    }

    private void displayLocalWallpaper(final String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = useLowQualityWallpaper ? LOW_QUALITY_SAMPLE_SIZE : NORMAL_QUALITY_SAMPLE_SIZE;
            final Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            
            if (bitmap != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyedCompat()) return;
                        if (wallpaperImageView != null) {
                            wallpaperImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            wallpaperImageView.setImageBitmap(bitmap);
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) wallpaperImageView.getLayoutParams();
                            params.width = FrameLayout.LayoutParams.MATCH_PARENT;
                            params.height = FrameLayout.LayoutParams.MATCH_PARENT;
                            wallpaperImageView.setLayoutParams(params);
                            hasWallpaperDisplayed = true;
                        }
                        isWallpaperLoading = false;
                    }
                });
            } else {
                displayOfflineFallback();
            }
        } catch (Exception e) {
            displayOfflineFallback();
        }
    }

    private void displayOfflineFallback() {
        File[] cached = getCachedWallpapers();
        if (cached != null && cached.length > 0) {
            File fallback = cached[randomGenerator.nextInt(cached.length)];
            displayLocalWallpaper(fallback.getAbsolutePath());
        } else {
            
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (wallpaperImageView != null) {
                        wallpaperImageView.setImageResource(android.R.color.black);
                        hasWallpaperDisplayed = true;
                    }
                    isWallpaperLoading = false;
                }
            });
        }
    }

    private void cleanupOldWallpapers() {
        try {
            File[] files = getCachedWallpapers();
            if (files != null && files.length > 5) {
                
                for (int i = 5; i < files.length; i++) {
                    files[i].delete();
                }
            }
        } catch (Exception e) {}
    }

    private File[] getCachedWallpapers() {
        try {
            File[] files = wallpaperCacheDir.listFiles();
            if (files != null) {
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return Long.compare(f2.lastModified(), f1.lastModified());
                    }
                });
                return files;
            }
        } catch (Exception e) {}
        return new File[0];
    }

    private void forceUpdateWallpaperImmediate() {
        if (isUsingMobileData && !showedMobileWarning) {
            showMobileDataWarning();
            showedMobileWarning = true;
            preferences.edit().putBoolean(PREF_SHOWED_MOBILE_WARNING, true).apply();
            return;
        }
        fetchAndDisplayWallpaper();
    }

    private void checkAndUpdateWallpaperByMinute(int currentMinute) {
        if (currentMinute % 5 == 0) {
            if (currentMinute != lastWallpaperMinute) {
                lastWallpaperMinute = currentMinute;
                preferences.edit().putInt(PREF_LAST_WALLPAPER_MINUTE, lastWallpaperMinute).apply();
                fetchAndDisplayWallpaper();
            }
        }
    }

    private void loadLocationData() {
        try {
            currentProvince = preferences.getString(PREF_PROVINCE, "北京");
            currentCity = preferences.getString(PREF_CITY, "北京");
            currentDistrict = preferences.getString(PREF_DISTRICT, "");
            if (!currentDistrict.isEmpty()) {
                currentLocationName = currentDistrict;
            } else if (currentProvince.equals(currentCity)) {
                currentLocationName = currentCity;
            } else {
                currentLocationName = currentCity;
            }
        } catch (Exception e) {
            currentProvince = "北京"; currentCity = "北京"; currentDistrict = ""; currentLocationName = "北京";
        }
    }

    private void createBatteryIcon() {
        if (isTVMode) return;
        try {
            batteryContainer.setOrientation(LinearLayout.HORIZONTAL);
            batteryContainer.setGravity(Gravity.CENTER_VERTICAL);

            batteryBody = new FrameLayout(this);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(20));
            batteryBody.setLayoutParams(bodyParams);
            GradientDrawable bodyDrawable = new GradientDrawable();
            bodyDrawable.setShape(GradientDrawable.RECTANGLE);
            bodyDrawable.setCornerRadius(dpToPx(2));
            bodyDrawable.setStroke(dpToPx(1), Color.WHITE);
            bodyDrawable.setColor(Color.TRANSPARENT);
            batteryBody.setBackground(bodyDrawable);
            batteryBody.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));

            batteryLevel = new View(this);
            FrameLayout.LayoutParams levelParams = new FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT);
            levelParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            batteryLevel.setLayoutParams(levelParams);
            GradientDrawable levelDrawable = new GradientDrawable();
            levelDrawable.setShape(GradientDrawable.RECTANGLE);
            levelDrawable.setCornerRadius(dpToPx(1));
            levelDrawable.setColor(Color.argb(128, 255, 255, 255));
            batteryLevel.setBackground(levelDrawable);
            batteryBody.addView(batteryLevel);

            batteryTip = new View(this);
            FrameLayout.LayoutParams tipParams = new FrameLayout.LayoutParams(dpToPx(3), dpToPx(10));
            tipParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
            batteryTip.setLayoutParams(tipParams);
            GradientDrawable tipDrawable = new GradientDrawable();
            tipDrawable.setShape(GradientDrawable.RECTANGLE);
            tipDrawable.setCornerRadius(dpToPx(1));
            tipDrawable.setColor(Color.WHITE);
            batteryTip.setBackground(tipDrawable);

            batteryPercentTextView = new TextView(this);
            FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            textParams.gravity = Gravity.CENTER;
            batteryPercentTextView.setLayoutParams(textParams);
            batteryPercentTextView.setTextSize(10);
            batteryPercentTextView.setText("100%");
            batteryPercentTextView.setTextColor(Color.BLACK);
            batteryPercentTextView.setTypeface(Typeface.DEFAULT_BOLD);
            batteryBody.addView(batteryPercentTextView);

            chargingIndicatorImageView = new ImageView(this);
            LinearLayout.LayoutParams lightningParams = new LinearLayout.LayoutParams(dpToPx(16), dpToPx(16));
            lightningParams.gravity = Gravity.CENTER_VERTICAL;
            lightningParams.setMargins(dpToPx(2), 0, 0, 0); 
            chargingIndicatorImageView.setLayoutParams(lightningParams);
            try {
                Drawable lightningDrawable = ContextCompat.getDrawable(this, R.drawable.ic_lightning);
                if (lightningDrawable != null) {
                    lightningDrawable = DrawableCompat.wrap(lightningDrawable);
                    chargingIndicatorImageView.setImageDrawable(lightningDrawable);
                }
            } catch (Exception e) {
                TextView fallbackView = new TextView(this);
                fallbackView.setText("⚡");
                fallbackView.setTextSize(16);
                fallbackView.setTextColor(textColors[currentTextColorIndex]);
                batteryContainer.addView(fallbackView);
            }

            batteryContainer.addView(batteryBody);
            batteryContainer.addView(batteryTip);
            batteryContainer.addView(chargingIndicatorImageView);
        } catch (Exception e) {
            Log.e(TAG, "Error creating battery icon", e);
        }
    }

    private void updateTimeAndDate() {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault());
            Date now = new Date();
            String currentTime = timeFormat.format(now);
            String currentDate = dateFormat.format(now);
            int currentMinute = now.getMinutes();
            int currentSecond = now.getSeconds();

            if (currentMinute != lastMinute) {
                lastMinute = currentMinute;
                moveClockPosition();
                if (currentMinute % 10 == 0) {
                    checkAndUpdateWeather();
                }
                checkAndUpdateWallpaperByMinute(currentMinute);
            }

            if (!currentDate.equals(currentDateString)) {
                currentDateString = currentDate;
                dateTextView.setText(currentDate);
                String lunarDate = LunarUtil.getLunarDate(now);
                currentLunarDate = lunarDate;
                String festival = LunarUtil.getFestival(now);
                currentFestival = festival;
                lunarTextView.setText(currentLunarDate);
                if (festival != null && !festival.isEmpty()) {
                    festivalTextView.setText(festival);
                    festivalTextView.setVisibility(View.VISIBLE);
                } else {
                    festivalTextView.setVisibility(View.GONE);
                }
            }

            if (currentSecond != lastSecond) {
                lastSecond = currentSecond;
                timeTextView.setText(currentTime);
            }
        } catch (Exception e) {
            try {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault());
                Date now = new Date();
                timeTextView.setText(timeFormat.format(now));
                dateTextView.setText(dateFormat.format(now));
                lunarTextView.setText("农历计算失败");
                festivalTextView.setVisibility(View.GONE);
            } catch (Exception ex) {}
        }
    }

    private void forceUpdateWeather() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                fetchWeatherData();
            }
        }).start();
    }

    private void checkAndUpdateWeather() {
        long currentTime = System.currentTimeMillis();
        lastWeatherUpdateTime = preferences.getLong(PREF_LAST_WEATHER_UPDATE, 0);
        String cachedWeatherData = preferences.getString(PREF_WEATHER_DATA, "");
        if (!cachedWeatherData.isEmpty()) {
            try {
                updateWeatherDisplayFromString(cachedWeatherData);
            } catch (Exception e) {}
        }
        if (currentTime - lastWeatherUpdateTime > WEATHER_UPDATE_INTERVAL) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    fetchWeatherData();
                }
            }).start();
        }
    }

    private void fetchWeatherData() {
        HttpURLConnection connection = null;
        try {
            String locationParam = !currentDistrict.isEmpty() ? currentDistrict : currentCity;
            String urlString;
            if (!currentDistrict.isEmpty()) {
                urlString = "https://wis.qq.com/weather/common?source=pc&weather_type=observe&province=" +
                        currentProvince + "&city=" + currentCity + "&county=" + locationParam;
            } else {
                urlString = "https://wis.qq.com/weather/common?source=pc&weather_type=observe&province=" +
                        currentProvince + "&city=" + currentCity;
            }
            urlString = urlString.replace(" ", "%20");
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            trustAllHostsForOldAndroid(connection);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                String weatherDataStr = parseTencentWeatherDataFromResponse(response.toString());
                preferences.edit()
                        .putString(PREF_WEATHER_DATA, weatherDataStr)
                        .putLong(PREF_LAST_WEATHER_UPDATE, System.currentTimeMillis())
                        .apply();
                final String finalWeatherDataStr = weatherDataStr;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateWeatherDisplayFromString(finalWeatherDataStr);
                    }
                });
            } else {
                fetchBackupWeatherData();
            }
        } catch (Exception e) {
            fetchBackupWeatherData();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void fetchBackupWeatherData() {
        HttpURLConnection connection = null;
        try {
            String locationParam = !currentDistrict.isEmpty() ? currentDistrict : currentCity;
            String urlString = "http://wthrcdn.etouch.cn/weather_mini?city=" + locationParam;
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                String weatherDataStr = parseBackupWeatherDataFromResponse(response.toString());
                preferences.edit()
                        .putString(PREF_WEATHER_DATA, weatherDataStr)
                        .putLong(PREF_LAST_WEATHER_UPDATE, System.currentTimeMillis())
                        .apply();
                final String finalWeatherDataStr = weatherDataStr;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateWeatherDisplayFromString(finalWeatherDataStr);
                    }
                });
            }
        } catch (Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String cachedWeatherData = preferences.getString(PREF_WEATHER_DATA, "");
                    if (!cachedWeatherData.isEmpty()) {
                        try {
                            updateWeatherDisplayFromString(cachedWeatherData);
                        } catch (Exception ex) {}
                    }
                }
            });
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String parseTencentWeatherDataFromResponse(String response) {
        try {
            double temperature = 0.0;
            String weatherDescription = "未知";
            if (response.contains("\"degree\"")) {
                int degreeIndex = response.indexOf("\"degree\":");
                int commaIndex = response.indexOf(",", degreeIndex);
                if (commaIndex == -1) commaIndex = response.indexOf("}", degreeIndex);
                if (commaIndex != -1) {
                    String degreeStr = response.substring(degreeIndex + 9, commaIndex).replace("\"", "").trim();
                    try { temperature = Double.parseDouble(degreeStr); } catch (Exception e) {}
                }
            }
            if (response.contains("\"weather\"")) {
                int weatherIndex = response.indexOf("\"weather\":");
                int commaIndex = response.indexOf(",", weatherIndex);
                if (commaIndex == -1) commaIndex = response.indexOf("}", weatherIndex);
                if (commaIndex != -1) {
                    int quoteStart = response.indexOf("\"", weatherIndex + 10);
                    int quoteEnd = response.indexOf("\"", quoteStart + 1);
                    if (quoteStart != -1 && quoteEnd != -1) {
                        weatherDescription = response.substring(quoteStart + 1, quoteEnd);
                    }
                }
            }
            int weatherCode = getWeatherCodeFromDescription(weatherDescription);
            return temperature + "," + weatherDescription + "," + weatherCode;
        } catch (Exception e) {
            return "0,未知,0";
        }
    }

    private String parseBackupWeatherDataFromResponse(String response) {
        try {
            double temperature = 0.0;
            String weatherDescription = "未知";
            if (response.contains("\"wendu\"")) {
                int tempIndex = response.indexOf("\"wendu\":");
                int commaIndex = response.indexOf(",", tempIndex);
                if (commaIndex == -1) commaIndex = response.indexOf("}", tempIndex);
                if (commaIndex != -1) {
                    String tempStr = response.substring(tempIndex + 8, commaIndex).replace("\"", "").trim();
                    try { temperature = Double.parseDouble(tempStr); } catch (Exception e) {}
                }
            }
            if (response.contains("\"type\"")) {
                int typeIndex = response.indexOf("\"type\":");
                int commaIndex = response.indexOf(",", typeIndex);
                if (commaIndex == -1) commaIndex = response.indexOf("}", typeIndex);
                if (commaIndex != -1) {
                    int quoteStart = response.indexOf("\"", typeIndex +7);
                    int quoteEnd = response.indexOf("\"", quoteStart + 1);
                    if (quoteStart != -1 && quoteEnd != -1) {
                        weatherDescription = response.substring(quoteStart + 1, quoteEnd);
                    }
                }
            }
            int weatherCode = getWeatherCodeFromDescription(weatherDescription);
            return temperature + "," + weatherDescription + "," + weatherCode;
        } catch (Exception e) {
            return "0,未知,0";
        }
    }

    private int getWeatherCodeFromDescription(String weatherDescription) {
        if (weatherDescription.contains("晴")) return 0;
        else if (weatherDescription.contains("多云") || weatherDescription.contains("阴")) return 3;
        else if (weatherDescription.contains("雨")) return 61;
        else if (weatherDescription.contains("雪")) return 71;
        else if (weatherDescription.contains("雾")) return 45;
        else if (weatherDescription.contains("雷")) return 95;
        else return 0;
    }

    private void updateWeatherDisplayFromString(String weatherDataStr) {
        try {
            if (weatherContainer == null) return;
            String[] parts = weatherDataStr.split(",");
            if (parts.length >= 2) {
                double temperature = Double.parseDouble(parts[0]);
                String weatherDescription = parts[1];
                int weatherCode = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;

                String locationDisplay = getLocationDisplayText();
                String weatherText = String.format(Locale.getDefault(), "%s %.0f°C %s", locationDisplay, temperature, weatherDescription);

                if (weatherTextView != null) weatherTextView.setText(weatherText);
                setWeatherIcon(weatherCode);
                weatherContainer.setVisibility(View.VISIBLE);
                if (weatherTextView != null) {
                    weatherTextView.setTextColor(textColors[currentTextColorIndex]);
                }
            }
        } catch (Exception e) {
            if (weatherContainer != null) weatherContainer.setVisibility(View.GONE);
        }
    }

    private String getLocationDisplayText() {
        if (!currentDistrict.isEmpty()) return currentDistrict;
        else if (currentProvince.equals(currentCity)) return currentCity;
        else return currentCity;
    }

    private void setWeatherIcon(int weatherCode) {
        try {
            if (weatherIconImageView == null) return;
            int iconResource = R.drawable.ic_weather_clear;
            if (weatherCode == 0) iconResource = R.drawable.ic_weather_clear;
            else if (weatherCode > 0 && weatherCode < 4) iconResource = R.drawable.ic_weather_cloudy;
            else if (weatherCode >= 45 && weatherCode <= 48) iconResource = R.drawable.ic_weather_fog;
            else if (weatherCode >= 51 && weatherCode <= 67) iconResource = R.drawable.ic_weather_rain;
            else if (weatherCode >= 71 && weatherCode <= 77) iconResource = R.drawable.ic_weather_snow;
            else if (weatherCode >= 80 && weatherCode <= 82) iconResource = R.drawable.ic_weather_rain;
            else if (weatherCode >= 95 && weatherCode <= 99) iconResource = R.drawable.ic_weather_thunderstorm;
            
            Drawable weatherIcon = ContextCompat.getDrawable(this, iconResource);
            if (weatherIcon != null) {
                weatherIcon = DrawableCompat.wrap(weatherIcon);
                DrawableCompat.setTint(weatherIcon, textColors[currentTextColorIndex]);
                weatherIconImageView.setImageDrawable(weatherIcon);
                weatherIconImageView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            if (weatherIconImageView != null) weatherIconImageView.setVisibility(View.GONE);
        }
    }

    private void updateBatteryDisplay() {
        if (isTVMode) return;
        try {
            if (batteryLevelValue >= 0) {
                if (batteryPercentTextView != null) {
                    batteryPercentTextView.setText(String.format(Locale.getDefault(), "%d%%", batteryLevelValue));
                }
                if (chargingIndicatorImageView != null) {
                    if (isCharging) {
                        chargingIndicatorImageView.setVisibility(View.VISIBLE);
                        DrawableCompat.setTint(chargingIndicatorImageView.getDrawable(), textColors[currentTextColorIndex]);
                    } else {
                        chargingIndicatorImageView.setVisibility(View.GONE);
                    }
                }
                if (batteryBody != null && batteryLevel != null) {
                    int batteryBodyWidth = batteryBody.getWidth();
                    int batteryPadding = batteryBody.getPaddingLeft() + batteryBody.getPaddingRight();
                    int maxLevelWidth = batteryBodyWidth - batteryPadding;
                    int levelWidth = (int) (maxLevelWidth * batteryLevelValue / 100f);
                    android.view.ViewGroup.LayoutParams levelParams = batteryLevel.getLayoutParams();
                    levelParams.width = levelWidth;
                    batteryLevel.setLayoutParams(levelParams);

                    int textColor = textColors[currentTextColorIndex];
                    float[] hsv = new float[3];
                    Color.colorToHSV(textColor, hsv);
                    float brightnessFactor = 0.5f + (batteryLevelValue / 200f);
                    hsv[2] = Math.max(0.3f, Math.min(1.0f, hsv[2] * brightnessFactor));
                    int batteryColor = Color.HSVToColor(hsv);
                    int alphaBatteryColor = Color.argb(128, Color.red(batteryColor), Color.green(batteryColor), Color.blue(batteryColor));

                    GradientDrawable batteryLevelDrawable = (GradientDrawable) batteryLevel.getBackground();
                    if (batteryLevelDrawable != null) batteryLevelDrawable.setColor(alphaBatteryColor);
                    
                    GradientDrawable bodyDrawable = (GradientDrawable) batteryBody.getBackground();
                    if (bodyDrawable != null) bodyDrawable.setStroke(dpToPx(1), textColor);
                    
                    GradientDrawable tipDrawable = (GradientDrawable) batteryTip.getBackground();
                    if (tipDrawable != null) tipDrawable.setColor(textColor);
                    
                    if (batteryPercentTextView != null) {
                        batteryPercentTextView.setTextColor(textColor);
                        int bgColor = backgroundColors[currentBgColorIndex];
                        if (!isColorDark(bgColor)) {
                            batteryPercentTextView.setTextColor(darkenColor(textColor));
                        }
                    }
                }
            } else {
                if (batteryPercentTextView != null) batteryPercentTextView.setText("?");
                if (chargingIndicatorImageView != null) chargingIndicatorImageView.setVisibility(View.GONE);
                if (batteryPercentTextView != null) batteryPercentTextView.setTextColor(textColors[currentTextColorIndex]);
            }
        } catch (Exception e) {}
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    private void registerBatteryReceiver() {
        if (isTVMode || isBatteryReceiverRegistered) return;
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(batteryReceiver, filter);
            isBatteryReceiverRegistered = true;
        } catch (Exception e) {}
    }

    private void centerClock() {
        try {
            Resources resources = getResources();
            int screenWidth = resources.getDisplayMetrics().widthPixels;
            int screenHeight = resources.getDisplayMetrics().heightPixels;
            if (clockContainer.getWidth() == 0 || clockContainer.getHeight() == 0) {
                clockContainer.measure(
                        View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
                );
            }
            int clockWidth = clockContainer.getMeasuredWidth();
            int clockHeight = clockContainer.getMeasuredHeight();
            if (clockWidth == 0) clockWidth = 400;
            if (clockHeight == 0) clockHeight = 300;

            int centerX = (screenWidth - clockWidth) / 2;
            int centerY = (screenHeight - clockHeight) / 2;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) clockContainer.getLayoutParams();
            params.leftMargin = centerX;
            params.topMargin = centerY;
            clockContainer.setLayoutParams(params);
        } catch (Exception e) {}
    }

    private void moveClockPosition() {
        try {
            Resources resources = getResources();
            int screenWidth = resources.getDisplayMetrics().widthPixels;
            int screenHeight = resources.getDisplayMetrics().heightPixels;
            clockContainer.measure(
                    View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
            );
            int clockWidth = clockContainer.getMeasuredWidth();
            int clockHeight = clockContainer.getMeasuredHeight();
            if (clockWidth == 0 || clockHeight == 0) return;
            
            int minMargin = 80;
            int maxX = screenWidth - clockWidth - minMargin * 2;
            int maxY = screenHeight - clockHeight - minMargin * 2;
            if (maxX > 0 && maxY > 0) {
                int randomX = randomGenerator.nextInt(maxX) + minMargin;
                int randomY = randomGenerator.nextInt(maxY) + minMargin;
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) clockContainer.getLayoutParams();
                params.leftMargin = randomX;
                params.topMargin = randomY;
                clockContainer.setLayoutParams(params);
            } else {
                centerClock();
            }
        } catch (Exception e) {}
    }

    private void changeTextColors() {
        try {
            currentTextColorIndex = (currentTextColorIndex + 1) % textColors.length;
            if (currentTextColorIndex < 0 || currentTextColorIndex >= textColors.length) currentTextColorIndex = 0;
            int textColor = textColors[currentTextColorIndex];
            
            if (timeTextView != null) timeTextView.setTextColor(textColor);
            if (dateTextView != null) dateTextView.setTextColor(textColor);
            if (lunarTextView != null) lunarTextView.setTextColor(textColor);
            if (festivalTextView != null) festivalTextView.setTextColor(textColor);
            if (weatherTextView != null) weatherTextView.setTextColor(textColor);
            if (!isTVMode && batteryPercentTextView != null) batteryPercentTextView.setTextColor(textColor);
            
            updateBatteryDisplay();
            if (weatherIconImageView != null && weatherIconImageView.getVisibility() == View.VISIBLE) {
                Drawable weatherIcon = weatherIconImageView.getDrawable();
                if (weatherIcon != null) DrawableCompat.setTint(weatherIcon, textColor);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    String appLabel = getApplicationInfo().loadLabel(getPackageManager()).toString();
                    ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(
                            appLabel, null, textColor | 0xFF000000);
                    setTaskDescription(taskDescription);
                } catch (Exception e) {}
            }
            saveColorPreferences();
        } catch (Exception e) {}
    }

    private void changeStyle() {
        try {
            currentStyleIndex = (currentStyleIndex + 1) % 4;
            applyStyle();
            saveColorPreferences();
        } catch (Exception e) {}
    }

    private void applyStyle() {
        try {
            if (currentTextColorIndex < 0 || currentTextColorIndex >= textColors.length) currentTextColorIndex = 0;
            int textColor = textColors[currentTextColorIndex];

            FrameLayout.LayoutParams timeParams;
            FrameLayout.LayoutParams batteryParams;
            FrameLayout.LayoutParams tipParams;
            LinearLayout.LayoutParams lightningParams;
            LinearLayout.LayoutParams weatherIconParams;

            switch (currentStyleIndex) {
                case 0:
                    if (timeTextView != null) timeTextView.setTextSize(80);
                    if (dateTextView != null) dateTextView.setTextSize(30);
                    if (lunarTextView != null) lunarTextView.setTextSize(24);
                    if (festivalTextView != null) festivalTextView.setTextSize(24);
                    if (weatherTextView != null) weatherTextView.setTextSize(20);

                    if (timeTextView != null) timeTextView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                    if (dateTextView != null) dateTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                    if (lunarTextView != null) lunarTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                    if (festivalTextView != null) festivalTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                    if (weatherTextView != null) weatherTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

                    if (timeTextView != null) {
                        timeParams = (FrameLayout.LayoutParams) timeTextView.getLayoutParams();
                        timeParams.topMargin = dpToPx(0);
                        timeTextView.setLayoutParams(timeParams);
                    }

                    if (!isTVMode) {
                        if (batteryBody != null) {
                            batteryParams = (FrameLayout.LayoutParams) batteryBody.getLayoutParams();
                            batteryParams.width = dpToPx(40); batteryParams.height = dpToPx(20);
                            batteryBody.setLayoutParams(batteryParams);
                        }
                        if (batteryTip != null) {
                            tipParams = (FrameLayout.LayoutParams) batteryTip.getLayoutParams();
                            tipParams.width = dpToPx(3); tipParams.height = dpToPx(10);
                            batteryTip.setLayoutParams(tipParams);
                        }
                        if (chargingIndicatorImageView != null) {
                            lightningParams = (LinearLayout.LayoutParams) chargingIndicatorImageView.getLayoutParams();
                            lightningParams.width = dpToPx(16); lightningParams.height = dpToPx(16);
                            chargingIndicatorImageView.setLayoutParams(lightningParams);
                        }
                        if (batteryPercentTextView != null) batteryPercentTextView.setTextSize(10);
                        if (weatherIconImageView != null) {
                            weatherIconParams = (LinearLayout.LayoutParams) weatherIconImageView.getLayoutParams();
                            weatherIconParams.width = dpToPx(24); weatherIconParams.height = dpToPx(24);
                            weatherIconImageView.setLayoutParams(weatherIconParams);
                        }
                    }
                    break;
                case 1:
                    if (timeTextView != null) timeTextView.setTextSize(70);
                    if (dateTextView != null) dateTextView.setTextSize(28);
                    if (lunarTextView != null) lunarTextView.setTextSize(22);
                    if (festivalTextView != null) festivalTextView.setTextSize(22);
                    if (weatherTextView != null) weatherTextView.setTextSize(18);

                    if (timeTextView != null) timeTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                    if (dateTextView != null) dateTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
                    if (lunarTextView != null) lunarTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
                    if (festivalTextView != null) festivalTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    if (weatherTextView != null) weatherTextView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

                    if (timeTextView != null) {
                        timeParams = (FrameLayout.LayoutParams) timeTextView.getLayoutParams();
                        timeParams.topMargin = dpToPx(10);
                        timeTextView.setLayoutParams(timeParams);
                    }

                    if (!isTVMode) {
                        if (batteryBody != null) {
                            batteryParams = (FrameLayout.LayoutParams) batteryBody.getLayoutParams();
                            batteryParams.width = dpToPx(35); batteryParams.height = dpToPx(18);
                            batteryBody.setLayoutParams(batteryParams);
                        }
                        if (batteryTip != null) {
                            tipParams = (FrameLayout.LayoutParams) batteryTip.getLayoutParams();
                            tipParams.width = dpToPx(2); tipParams.height = dpToPx(8);
                            batteryTip.setLayoutParams(tipParams);
                        }
                        if (chargingIndicatorImageView != null) {
                            lightningParams = (LinearLayout.LayoutParams) chargingIndicatorImageView.getLayoutParams();
                            lightningParams.width = dpToPx(14); lightningParams.height = dpToPx(14);
                            chargingIndicatorImageView.setLayoutParams(lightningParams);
                        }
                        if (batteryPercentTextView != null) batteryPercentTextView.setTextSize(9);
                        if (weatherIconImageView != null) {
                            weatherIconParams = (LinearLayout.LayoutParams) weatherIconImageView.getLayoutParams();
                            weatherIconParams.width = dpToPx(22); weatherIconParams.height = dpToPx(22);
                            weatherIconImageView.setLayoutParams(weatherIconParams);
                        }
                    }
                    break;
                case 2:
                    if (timeTextView != null) timeTextView.setTextSize(90);
                    if (dateTextView != null) dateTextView.setTextSize(26);
                    if (lunarTextView != null) lunarTextView.setTextSize(20);
                    if (festivalTextView != null) festivalTextView.setTextSize(20);
                    if (weatherTextView != null) weatherTextView.setTextSize(16);

                    if (timeTextView != null) timeTextView.setTypeface(Typeface.create("serif", Typeface.BOLD));
                    if (dateTextView != null) dateTextView.setTypeface(Typeface.create("serif", Typeface.NORMAL));
                    if (lunarTextView != null) lunarTextView.setTypeface(Typeface.create("serif", Typeface.NORMAL));
                    if (festivalTextView != null) festivalTextView.setTypeface(Typeface.create("serif", Typeface.NORMAL));
                    if (weatherTextView != null) weatherTextView.setTypeface(Typeface.create("serif", Typeface.NORMAL));

                    if (timeTextView != null) {
                        timeParams = (FrameLayout.LayoutParams) timeTextView.getLayoutParams();
                        timeParams.topMargin = dpToPx(5);
                        timeTextView.setLayoutParams(timeParams);
                    }

                    if (!isTVMode) {
                        if (batteryBody != null) {
                            batteryParams = (FrameLayout.LayoutParams) batteryBody.getLayoutParams();
                            batteryParams.width = dpToPx(30); batteryParams.height = dpToPx(15);
                            batteryBody.setLayoutParams(batteryParams);
                        }
                        if (batteryTip != null) {
                            tipParams = (FrameLayout.LayoutParams) batteryTip.getLayoutParams();
                            tipParams.width = dpToPx(2); tipParams.height = dpToPx(6);
                            batteryTip.setLayoutParams(tipParams);
                        }
                        if (chargingIndicatorImageView != null) {
                            lightningParams = (LinearLayout.LayoutParams) chargingIndicatorImageView.getLayoutParams();
                            lightningParams.width = dpToPx(12); lightningParams.height = dpToPx(12);
                            chargingIndicatorImageView.setLayoutParams(lightningParams);
                        }
                        if (batteryPercentTextView != null) batteryPercentTextView.setTextSize(8);
                        if (weatherIconImageView != null) {
                            weatherIconParams = (LinearLayout.LayoutParams) weatherIconImageView.getLayoutParams();
                            weatherIconParams.width = dpToPx(20); weatherIconParams.height = dpToPx(20);
                            weatherIconImageView.setLayoutParams(weatherIconParams);
                        }
                    }
                    break;
                case 3:
                    if (timeTextView != null) timeTextView.setTextSize(75);
                    if (dateTextView != null) dateTextView.setTextSize(25);
                    if (lunarTextView != null) lunarTextView.setTextSize(20);
                    if (festivalTextView != null) festivalTextView.setTextSize(20);
                    if (weatherTextView != null) weatherTextView.setTextSize(16);

                    if (timeTextView != null) timeTextView.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                    if (dateTextView != null) dateTextView.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                    if (lunarTextView != null) lunarTextView.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
                    if (festivalTextView != null) festivalTextView.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                    if (weatherTextView != null) weatherTextView.setTypeface(Typeface.create("monospace", Typeface.BOLD));

                    if (timeTextView != null) {
                        timeParams = (FrameLayout.LayoutParams) timeTextView.getLayoutParams();
                        timeParams.topMargin = dpToPx(-5);
                        timeTextView.setLayoutParams(timeParams);
                    }

                    if (!isTVMode) {
                        if (batteryBody != null) {
                            batteryParams = (FrameLayout.LayoutParams) batteryBody.getLayoutParams();
                            batteryParams.width = dpToPx(38); batteryParams.height = dpToPx(16);
                            batteryBody.setLayoutParams(batteryParams);
                        }
                        if (batteryTip != null) {
                            tipParams = (FrameLayout.LayoutParams) batteryTip.getLayoutParams();
                            tipParams.width = dpToPx(2); tipParams.height = dpToPx(7);
                            batteryTip.setLayoutParams(tipParams);
                        }
                        if (chargingIndicatorImageView != null) {
                            lightningParams = (LinearLayout.LayoutParams) chargingIndicatorImageView.getLayoutParams();
                            lightningParams.width = dpToPx(14); lightningParams.height = dpToPx(14);
                            chargingIndicatorImageView.setLayoutParams(lightningParams);
                        }
                        if (batteryPercentTextView != null) batteryPercentTextView.setTextSize(9);
                        if (weatherIconImageView != null) {
                            weatherIconParams = (LinearLayout.LayoutParams) weatherIconImageView.getLayoutParams();
                            weatherIconParams.width = dpToPx(20); weatherIconParams.height = dpToPx(20);
                            weatherIconImageView.setLayoutParams(weatherIconParams);
                        }
                    }
                    break;
            }

            if (timeTextView != null) timeTextView.setTextColor(textColor);
            if (dateTextView != null) dateTextView.setTextColor(textColor);
            if (lunarTextView != null) lunarTextView.setTextColor(textColor);
            if (festivalTextView != null) festivalTextView.setTextColor(textColor);
            if (weatherTextView != null) weatherTextView.setTextColor(textColor);
            if (!isTVMode && batteryPercentTextView != null) batteryPercentTextView.setTextColor(textColor);
            
            if (weatherIconImageView != null && weatherIconImageView.getVisibility() == View.VISIBLE) {
                Drawable weatherIcon = weatherIconImageView.getDrawable();
                if (weatherIcon != null) DrawableCompat.setTint(weatherIcon, textColor);
            }
            updateBatteryDisplay();
            moveClockPosition();
        } catch (Exception e) {}
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void saveColorPreferences() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(PREF_TEXT_COLOR_INDEX, currentTextColorIndex);
            editor.putInt(PREF_BG_COLOR_INDEX, currentBgColorIndex);
            editor.putInt(PREF_STYLE_INDEX, currentStyleIndex);
            editor.putBoolean(PREF_SHOWED_MOBILE_WARNING, showedMobileWarning);
            editor.putInt(PREF_LAST_WALLPAPER_MINUTE, lastWallpaperMinute);
            editor.apply();
        } catch (Exception e) {}
    }

    private void loadColorPreferences() {
        try {
            currentTextColorIndex = preferences.getInt(PREF_TEXT_COLOR_INDEX, 0);
            currentBgColorIndex = preferences.getInt(PREF_BG_COLOR_INDEX, 0);
            currentStyleIndex = preferences.getInt(PREF_STYLE_INDEX, 0);
            showedMobileWarning = preferences.getBoolean(PREF_SHOWED_MOBILE_WARNING, false);
            lastWallpaperMinute = preferences.getInt(PREF_LAST_WALLPAPER_MINUTE, -1);

            if (currentTextColorIndex < 0 || currentTextColorIndex >= textColors.length) currentTextColorIndex = 0;
            if (currentBgColorIndex < 0 || currentBgColorIndex >= backgroundColors.length) currentBgColorIndex = 0;

            if (currentBgColorIndex < backgroundColors.length) {
                if (containerLayout != null) {
                    containerLayout.setBackgroundColor(backgroundColors[currentBgColorIndex]);
                }
            }
            if (currentTextColorIndex < textColors.length) {
                int textColor = textColors[currentTextColorIndex];
                if (timeTextView != null) timeTextView.setTextColor(textColor);
                if (dateTextView != null) dateTextView.setTextColor(textColor);
                if (lunarTextView != null) lunarTextView.setTextColor(textColor);
                if (festivalTextView != null) festivalTextView.setTextColor(textColor);
                if (weatherTextView != null) weatherTextView.setTextColor(textColor);
                if (!isTVMode && batteryPercentTextView != null) batteryPercentTextView.setTextColor(textColor);
            }
            updateBatteryDisplay();
        } catch (Exception e) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (needsFullRestart) return;
        try {
            hideNavigationBar();
            if (wakeLock != null && !wakeLock.isHeld()) wakeLock.acquire();
            if (handler == null) handler = new Handler();
            if (updateTimeRunnable == null) {
                updateTimeRunnable = new Runnable() {
                    @Override
                    public void run() {
                        updateTimeAndDate();
                        handler.postDelayed(this, 1000);
                    }
                };
            }
            if (!isTVMode) registerBatteryReceiver();
            checkNetworkType();
            forceUpdateWeather();
            if (handler != null) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!hasWallpaperDisplayed && !isAppInitializing) {
                            fetchAndDisplayWallpaper();
                        }
                    }
                }, 500);
                handler.removeCallbacks(updateTimeRunnable);
                handler.post(updateTimeRunnable);
            }
            if (clockContainer != null) {
                clockContainer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        centerClock();
                    }
                }, 100);
            }
        } catch (Exception e) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (needsFullRestart) return;
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            saveColorPreferences();
            if (handler != null) handler.removeCallbacks(updateTimeRunnable);
        } catch (Exception e) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            saveColorPreferences();
            if (wallpaperExecutor != null && !wallpaperExecutor.isShutdown()) {
                wallpaperExecutor.shutdownNow();
                isExecutorShutdown = true;
            }
            cleanupResources();
        } catch (Exception e) {}
    }
}
