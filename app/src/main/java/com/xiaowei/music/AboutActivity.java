package com.xiaowei.music;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.KeyEvent;
import android.content.pm.PackageManager;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.Toast;

public class AboutActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ClockPrefs";
    private static final String PREF_ABOUT_SHOWN = "aboutShown";

    private Button nextButton;
    private TextView titleText;
    private TextView nameTextView;
    private TextView versionTextView;
    private LinearLayout githubContainer;
    private LinearLayout qqContainer;
    private LinearLayout developerContainer;

    private GestureDetector gestureDetector;
    private boolean isTVMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemUI();

        isTVMode = (getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                (getSystemService(Context.UI_MODE_SERVICE) != null &&
                 ((UiModeManager) getSystemService(Context.UI_MODE_SERVICE)).getCurrentModeType() == UiModeManager.MODE_NIGHT_YES));

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_about);

        initViews();
        setupTVFocusNavigation();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

                if (e2.getX() - e1.getX() > 100 && Math.abs(velocityX) > 100) {
                    goToMainActivity();
                    return true;
                }

                if (e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 100) {
                    onBackPressed();
                    return true;
                }
                return false;
            }
        });
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void initViews() {
        titleText = findViewById(R.id.titleText);
        nextButton = findViewById(R.id.confirmButton);
        nameTextView = findViewById(R.id.nameTextView);
        versionTextView = findViewById(R.id.versionTextView);
        githubContainer = findViewById(R.id.githubContainer);
        qqContainer = findViewById(R.id.qqContainer);
        developerContainer = findViewById(R.id.developerContainer);

        nameTextView.setText(R.string.developer_name);
        versionTextView.setText(R.string.version_1_1_0);

        boolean isFirstLaunch = getIntent().getBooleanExtra(getString(R.string.extra_first_launch), false);
        if (isFirstLaunch) {
            nextButton.setText(R.string.next_step);
        } else {
            nextButton.setText(R.string.i_know);
        }

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToMainActivity();
            }
        });

        githubContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/power690/music-clock"));
                try {
                    startActivity(browserIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        qqContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("QQ群号", "1009750444");
                clipboard.setPrimaryClip(clip);
                Toast.makeText(AboutActivity.this, R.string.toast_copy_success, Toast.LENGTH_SHORT).show();
            }
        });

        developerContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/power690"));
                try {
                    startActivity(browserIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupTVFocusNavigation() {
        if (!isTVMode) return;

        nextButton.setFocusable(true);
        nextButton.setFocusableInTouchMode(true);

        githubContainer.setFocusable(true);
        githubContainer.setFocusableInTouchMode(true);

        qqContainer.setFocusable(true);
        qqContainer.setFocusableInTouchMode(true);

        developerContainer.setFocusable(true);
        developerContainer.setFocusableInTouchMode(true);

        nextButton.requestFocus();

        nextButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {

                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setColor(Color.WHITE);
                    drawable.setCornerRadius(8);
                    nextButton.setBackground(drawable);
                    nextButton.setTextColor(Color.BLACK);
                } else {

                    nextButton.setBackground(getResources().getDrawable(R.drawable.spinner_background_white_border));
                    nextButton.setTextColor(Color.WHITE);
                }
            }
        });

        githubContainer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    githubContainer.setBackgroundColor(Color.parseColor("#80FFFFFF"));
                } else {
                    githubContainer.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        qqContainer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    qqContainer.setBackgroundColor(Color.parseColor("#80FFFFFF"));
                } else {
                    qqContainer.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        developerContainer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    developerContainer.setBackgroundColor(Color.parseColor("#80FFFFFF"));
                } else {
                    developerContainer.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });
    }

    private void goToMainActivity() {

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_ABOUT_SHOWN, true);
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (isTVMode) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (githubContainer.isFocused()) {
                        githubContainer.performClick();
                    } else if (qqContainer.isFocused()) {
                        qqContainer.performClick();
                    } else if (developerContainer.isFocused()) {
                        developerContainer.performClick();
                    } else {
                        goToMainActivity();
                    }
                    return true;

                case KeyEvent.KEYCODE_BACK:

                    onBackPressed();
                    return true;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (nextButton.isFocused()) {
                        qqContainer.requestFocus();
                    } else if (qqContainer.isFocused()) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            nextButton.requestFocus();
                        } else {
                            developerContainer.requestFocus();
                        }
                    } else if (developerContainer.isFocused()) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            qqContainer.requestFocus();
                        } else {
                            githubContainer.requestFocus();
                        }
                    } else if (githubContainer.isFocused()) {
                        developerContainer.requestFocus();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }
}