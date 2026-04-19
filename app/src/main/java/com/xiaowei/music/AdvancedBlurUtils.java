package com.qian.lock;

import android.graphics.Bitmap;
import android.graphics.Color;

 
public class AdvancedBlurUtils {
    
    private static final int DEFAULT_BLUR_RADIUS = 25;
    private static final int DEFAULT_ITERATIONS = 3;
    
     
    public static Bitmap advancedBlur(Bitmap srcBitmap, int blurRadius) {
        return advancedBlur(srcBitmap, blurRadius, DEFAULT_ITERATIONS);
    }
    
     
    public static Bitmap advancedBlur(Bitmap srcBitmap, int blurRadius, int iterations) {
        if (srcBitmap == null) return null;

        try {
            
            int scaleRatio = calculateOptimalScaleRatio(srcBitmap.getWidth(), srcBitmap.getHeight());
            int width = Math.max(srcBitmap.getWidth() / scaleRatio, 1);
            int height = Math.max(srcBitmap.getHeight() / scaleRatio, 1);
            
            Bitmap inputBitmap = Bitmap.createScaledBitmap(srcBitmap, width, height, true);
            Bitmap outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            
            
            applyAdvancedGaussianBlur(inputBitmap, outputBitmap, blurRadius, iterations);
            
            
            Bitmap finalBitmap = Bitmap.createScaledBitmap(
                    outputBitmap, srcBitmap.getWidth(), srcBitmap.getHeight(), true);
            
            
            if (inputBitmap != srcBitmap) {
                inputBitmap.recycle();
            }
            outputBitmap.recycle();
            
            return finalBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return srcBitmap;
        }
    }
    
     
    private static int calculateOptimalScaleRatio(int width, int height) {
        int maxDimension = Math.max(width, height);
        if (maxDimension > 2000) return 8;
        if (maxDimension > 1000) return 4;
        return 2;
    }
    
     
    private static void applyAdvancedGaussianBlur(Bitmap input, Bitmap output, int radius, int iterations) {
        if (radius < 1) radius = 1;
        int w = input.getWidth();
        int h = input.getHeight();
        
        int[] pixels = new int[w * h];
        input.getPixels(pixels, 0, w, 0, 0, w, h);
        
        
        for (int i = 0; i < iterations; i++) {
            pixels = blurHorizontalAdvanced(pixels, w, h, radius);
            pixels = blurVerticalAdvanced(pixels, w, h, radius);
        }
        
        output.setPixels(pixels, 0, w, 0, 0, w, h);
    }
    
     
    private static int[] blurHorizontalAdvanced(int[] pixels, int w, int h, int radius) {
        int[] result = new int[w * h];
        float[] kernel = createGaussianKernel(radius);
        int kernelSize = kernel.length;
        int kernelRadius = kernelSize / 2;
        
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float a = 0, r = 0, g = 0, b = 0;
                float weightSum = 0;
                
                for (int i = -kernelRadius; i <= kernelRadius; i++) {
                    int pixelX = Math.max(0, Math.min(w - 1, x + i));
                    int pixel = pixels[y * w + pixelX];
                    float weight = kernel[i + kernelRadius];
                    
                    a += Color.alpha(pixel) * weight;
                    r += Color.red(pixel) * weight;
                    g += Color.green(pixel) * weight;
                    b += Color.blue(pixel) * weight;
                    weightSum += weight;
                }
                
                if (weightSum > 0) {
                    a = a / weightSum;
                    r = r / weightSum;
                    g = g / weightSum;
                    b = b / weightSum;
                    
                    
                    a = Math.max(0, Math.min(255, a));
                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));
                    
                    result[y * w + x] = Color.argb((int) a, (int) r, (int) g, (int) b);
                } else {
                    result[y * w + x] = pixels[y * w + x];
                }
            }
        }
        
        return result;
    }
    
     
    private static int[] blurVerticalAdvanced(int[] pixels, int w, int h, int radius) {
        int[] result = new int[w * h];
        float[] kernel = createGaussianKernel(radius);
        int kernelSize = kernel.length;
        int kernelRadius = kernelSize / 2;
        
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                float a = 0, r = 0, g = 0, b = 0;
                float weightSum = 0;
                
                for (int i = -kernelRadius; i <= kernelRadius; i++) {
                    int pixelY = Math.max(0, Math.min(h - 1, y + i));
                    int pixel = pixels[pixelY * w + x];
                    float weight = kernel[i + kernelRadius];
                    
                    a += Color.alpha(pixel) * weight;
                    r += Color.red(pixel) * weight;
                    g += Color.green(pixel) * weight;
                    b += Color.blue(pixel) * weight;
                    weightSum += weight;
                }
                
                if (weightSum > 0) {
                    a = a / weightSum;
                    r = r / weightSum;
                    g = g / weightSum;
                    b = b / weightSum;
                    
                    
                    a = Math.max(0, Math.min(255, a));
                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));
                    
                    result[y * w + x] = Color.argb((int) a, (int) r, (int) g, (int) b);
                } else {
                    result[y * w + x] = pixels[y * w + x];
                }
            }
        }
        
        return result;
    }
    
     
    private static float[] createGaussianKernel(int radius) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0;
        
        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            kernel[i + radius] = (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += kernel[i + radius];
        }
        
        
        for (int i = 0; i < size; i++) {
            kernel[i] /= total;
        }
        
        return kernel;
    }
    
     
    public static Bitmap createPerfectRoundedBitmap(Bitmap bitmap, int cornerRadius) {
        if (bitmap == null) return null;
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(output);
            final android.graphics.Paint paint = new android.graphics.Paint();
            final android.graphics.Rect rect = new android.graphics.Rect(0, 0, width, height);
            final android.graphics.RectF rectF = new android.graphics.RectF(rect);
            
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);
            
            
            canvas.drawARGB(0, 0, 0, 0);
            
            
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint);
            
            
            paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            
            return output;
            
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }
    
     
    public static Bitmap fastAdvancedBlur(Bitmap srcBitmap) {
        return advancedBlur(srcBitmap, 15, 2);
    }
    
     
    public static Bitmap ultraQualityBlur(Bitmap srcBitmap) {
        return advancedBlur(srcBitmap, 35, 4);
    }
    
     
    public static Bitmap createNotificationBlurBackground(Bitmap wallpaper, int screenWidth, int targetHeight, int cardIndex, int totalCards) {
        if (wallpaper == null) return null;
        try {
            
            int dwidth = wallpaper.getWidth();
            int dheight = wallpaper.getHeight();

            
            int screenHeight = (int) (dheight * (float) screenWidth / dwidth);
            
            
            int cardSpacing = 8; 
            int cardSpacingPx = (int) (cardSpacing * android.content.res.Resources.getSystem().getDisplayMetrics().density);
            
            
            int firstCardTop = (int) (screenHeight * 0.3f); 
            
            
            int cardTopPosition = firstCardTop + (cardIndex * (targetHeight + cardSpacingPx));
            
            
            float scale;
            float dx = 0, dy = 0;
            
            if (dwidth * screenHeight > screenWidth * dheight) {
                
                scale = (float) screenHeight / (float) dheight; 
                dx = (screenWidth - dwidth * scale) * 0.5f;
            } else {
                
                scale = (float) screenWidth / (float) dwidth;
                dy = (screenHeight - dheight * scale) * 0.5f;
            }

            
            int bitmapY = (int) ((cardTopPosition - dy) / scale);
            int bitmapHeight = (int) (targetHeight / scale);
            
            int bitmapX = (int) ((0 - dx) / scale);
            int bitmapWidth = (int) (screenWidth / scale);

            
            bitmapX = Math.max(0, bitmapX);
            bitmapY = Math.max(0, bitmapY);
            
            if (bitmapX + bitmapWidth > dwidth) {
                bitmapWidth = dwidth - bitmapX;
            }
            if (bitmapY + bitmapHeight > dheight) {
                bitmapHeight = dheight - bitmapY;
            }
            
            
            bitmapWidth = Math.max(bitmapWidth, dwidth / 4);
            bitmapHeight = Math.max(bitmapHeight, dheight / 4);
            
            if (bitmapWidth <= 0 || bitmapHeight <= 0) {
                
                int sectionHeight = dheight / Math.max(totalCards, 1);
                bitmapY = cardIndex * sectionHeight;
                bitmapHeight = Math.min(sectionHeight, dheight - bitmapY);
                bitmapX = dwidth / 4;
                bitmapWidth = dwidth / 2;
            }

            
            Bitmap croppedArea = Bitmap.createBitmap(wallpaper, bitmapX, bitmapY, bitmapWidth, bitmapHeight);
            
            
            Bitmap blurred = advancedBlur(croppedArea, 20, 2);
            
            
            Bitmap result = Bitmap.createScaledBitmap(blurred, screenWidth, targetHeight, true);
            
            
            if (croppedArea != wallpaper) {
                croppedArea.recycle();
            }
            if (blurred != result) {
                blurred.recycle();
            }
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
     
    public static Bitmap createPasswordBlurBackground(Bitmap wallpaper, int blurRadius, float progress) {
        if (wallpaper == null) return null;
        try {
            
            int adjustedBlurRadius = (int) (blurRadius * progress);
            if (adjustedBlurRadius < 1) adjustedBlurRadius = 1;
            
            
            Bitmap blurred = advancedBlur(wallpaper, adjustedBlurRadius, 2);
            
            
            if (progress < 1.0f) {
                int[] pixels = new int[blurred.getWidth() * blurred.getHeight()];
                blurred.getPixels(pixels, 0, blurred.getWidth(), 0, 0, blurred.getWidth(), blurred.getHeight());
                
                for (int i = 0; i < pixels.length; i++) {
                    int pixel = pixels[i];
                    int alpha = (int) (Color.alpha(pixel) * progress);
                    pixels[i] = Color.argb(alpha, Color.red(pixel), Color.green(pixel), Color.blue(pixel));
                }
                
                blurred.setPixels(pixels, 0, blurred.getWidth(), 0, 0, blurred.getWidth(), blurred.getHeight());
            }
            
            return blurred;
        } catch (Exception e) {
            e.printStackTrace();
            return wallpaper;
        }
    }
}
