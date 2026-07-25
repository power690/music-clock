package com.xiaowei.music;

import android.graphics.Bitmap;

public class NativeBlurUtils {
    static {
        System.loadLibrary("native-blur");
    }

    public static Bitmap blur(Bitmap bitmap, int radius) {
        if (bitmap == null || bitmap.isRecycled()) return null;
        Bitmap argb = bitmap;
        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            argb = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
        try {
            return nativeBlur(argb, radius);
        } finally {
            if (argb != bitmap && !argb.isRecycled()) {
                argb.recycle();
            }
        }
    }

    private static native Bitmap nativeBlur(Bitmap bitmap, int radius);
}
