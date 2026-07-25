package com.xiaowei.music;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.app.UiModeManager;
import android.content.pm.PackageManager;

public class InstructionActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ClockPrefs";
    private static final String PREF_INSTRUCTION_SHOWN = "instructionShown";

    private boolean isTVMode = false;
    private long backPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isTVMode = (getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                UiModeManager.MODE_NIGHT_YES == getSystemService(UiModeManager.class).getCurrentModeType());

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        createInstructionLayout();
    }

    private void createInstructionLayout() {

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#FF333333"));
        mainLayout.setPadding(48, 48, 48, 48);

        TextView titleText = new TextView(this);
        titleText.setText(R.string.operation_guide_title);
        titleText.setTextSize(24);
        titleText.setTextColor(Color.WHITE);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setPadding(0, 0, 0, 32);

        TextView touchTitle = new TextView(this);
        touchTitle.setText(R.string.guide_section_touch);
        touchTitle.setTextSize(18);
        touchTitle.setTextColor(Color.parseColor("#FF80DEEA"));
        touchTitle.setTypeface(null, Typeface.BOLD);
        touchTitle.setPadding(0, 16, 0, 8);

        TextView touchContent = new TextView(this);
        touchContent.setText(R.string.touch_hint);
        touchContent.setTextSize(16);
        touchContent.setTextColor(Color.WHITE);
        touchContent.setPadding(24, 0, 0, 16);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(R.string.guide_section_tv_remote);
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.parseColor("#FF80DEEA"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 16, 0, 8);

        TextView tvContent = new TextView(this);
        tvContent.setText(R.string.tv_hint);
        tvContent.setTextSize(16);
        tvContent.setTextColor(Color.WHITE);
        tvContent.setPadding(24, 0, 0, 16);

        Button confirmButton = new Button(this);
        confirmButton.setText(R.string.i_know);
        confirmButton.setTextSize(18);
        confirmButton.setTextColor(Color.BLACK);
        confirmButton.setBackgroundColor(Color.parseColor("#FF80DEEA"));
        confirmButton.setPadding(32, 16, 32, 16);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(0, 32, 0, 0);
        confirmButton.setLayoutParams(buttonParams);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markInstructionShown();
                finish();
            }
        });

        mainLayout.addView(titleText);
        mainLayout.addView(touchTitle);
        mainLayout.addView(touchContent);
        mainLayout.addView(tvTitle);
        mainLayout.addView(tvContent);
        mainLayout.addView(confirmButton);

        setContentView(mainLayout);

        if (isTVMode) {
            confirmButton.setFocusable(true);
            confirmButton.setFocusableInTouchMode(true);
            confirmButton.requestFocus();
        }
    }

    private void markInstructionShown() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_INSTRUCTION_SHOWN, true);
        editor.apply();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isTVMode && keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish();
                    return true;
                } else {
                    backPressedTime = System.currentTimeMillis();
                    android.widget.Toast.makeText(this, R.string.toast_press_back_again_to_return, android.widget.Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
