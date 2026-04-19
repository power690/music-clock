package com.xiaowei.music;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

 
public class KeepAliveService extends Service {
    private static final String TAG = "KeepAliveService";
    private WindowManager windowManager;
    private View pixelView;
    private boolean isAdded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "保活服务启动");
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        createOnePixelWindow();
    }

    private void createOnePixelWindow() {
        if (isAdded) return; 

        pixelView = new View(this);
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        
        params.format = PixelFormat.TRANSLUCENT;
        params.width = 1;
        params.height = 1;
        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 0;
        params.y = 0;
        
        
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
                     | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL 
                     | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE 
                     | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN 
                     | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        try {
            windowManager.addView(pixelView, params);
            isAdded = true;
            Log.d(TAG, "1像素悬浮窗已添加");
        } catch (Exception e) {
            Log.e(TAG, "添加悬浮窗失败，可能未授权", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isAdded && windowManager != null && pixelView != null) {
            try {
                windowManager.removeView(pixelView);
                isAdded = false;
                Log.d(TAG, "1像素悬浮窗已移除");
            } catch (Exception e) {
                Log.e(TAG, "移除悬浮窗失败", e);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
