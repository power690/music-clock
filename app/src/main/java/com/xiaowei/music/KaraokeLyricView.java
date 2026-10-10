package com.xiaowei.music;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class KaraokeLyricView extends View {

    private static final long LAST_WORD_SPAN_MS = 300L;
    private static final long MIN_WORD_SPAN_MS = 40L;

    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private int sungColor = 0xFFFFFFFF;
    private int dimColor = 0x99FFFFFF;

    private List<LyricWord> words = new ArrayList<>();
    private int[] wordStart = new int[0];
    private int[] wordEnd = new int[0];
    private float[] wordWidthPrefix = new float[0];
    private String displayText = "";
    private float textWidth;

    private long anchorPosMs;
    private long anchorWallMs;
    private boolean playing;
    private boolean frameRunning;

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            frameRunning = false;
            if (playing && !words.isEmpty() && isShown()) {
                invalidate();
                postFrameCallback();
            }
        }
    };

    public KaraokeLyricView(Context context) {
        super(context);
        init();
    }

    public KaraokeLyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KaraokeLyricView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setSubpixelText(true);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(34f);
    }

    public void setKaraokeColors(int sung, int dim) {
        sungColor = sung;
        dimColor = dim;
        invalidate();
    }

    public void setKaraokeTextSizePx(float sizePx) {
        paint.setTextSize(sizePx);
        rebuildMetrics();
        requestLayout();
        invalidate();
    }

    public void setLine(List<LyricWord> newWords) {
        if (newWords == null || newWords.isEmpty()) {
            clear();
            return;
        }
        if (newWords == words) return;
        this.words = new ArrayList<>(newWords);
        StringBuilder sb = new StringBuilder();
        wordStart = new int[words.size()];
        wordEnd = new int[words.size()];
        for (int i = 0; i < words.size(); i++) {
            String t = words.get(i).getText();
            if (t == null) t = "";
            wordStart[i] = sb.length();
            sb.append(t);
            wordEnd[i] = sb.length();
        }
        displayText = sb.toString();
        rebuildMetrics();
        requestLayout();
        invalidate();
        syncFrameState();
    }

    public void anchor(long posMs, boolean isPlaying) {
        anchorPosMs = posMs;
        anchorWallMs = SystemClock.elapsedRealtime();
        playing = isPlaying;
        invalidate();
        syncFrameState();
    }

    public void clear() {
        words = new ArrayList<>();
        wordStart = new int[0];
        wordEnd = new int[0];
        wordWidthPrefix = new float[0];
        displayText = "";
        textWidth = 0f;
        playing = false;
        anchorPosMs = 0L;
        requestLayout();
        invalidate();
    }

    private void rebuildMetrics() {
        wordWidthPrefix = new float[words.size() + 1];
        for (int i = 0; i < words.size(); i++) {
            int s = wordStart[i];
            int e = wordEnd[i];
            String seg;
            if (s < 0 || e > displayText.length() || s >= e) {
                seg = "";
            } else {
                seg = displayText.substring(s, e);
            }
            wordWidthPrefix[i + 1] = wordWidthPrefix[i] + paint.measureText(seg);
        }
        textWidth = paint.measureText(displayText);
    }

    private void syncFrameState() {
        if (playing && !words.isEmpty() && isShown() && !frameRunning) {
            frameRunning = true;
            postFrameCallback();
        }
    }

    private long effectivePosMs() {
        if (playing) {
            return anchorPosMs + (SystemClock.elapsedRealtime() - anchorWallMs);
        }
        return anchorPosMs;
    }

    private int findCurrentWord(long posMs) {
        int ans = -1;
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).getTimeMs() <= posMs) {
                ans = i;
            } else {
                break;
            }
        }
        return ans;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (int) Math.ceil(paint.descent() - paint.ascent()) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (displayText.isEmpty()) return;

        int viewW = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewH = getHeight() - getPaddingTop() - getPaddingBottom();
        if (viewW <= 0 || viewH <= 0) return;

        long posMs = effectivePosMs();
        float sungWidth;
        if (words.isEmpty()) {
            sungWidth = 0f;
        } else {
            int idx = findCurrentWord(posMs);
            if (idx < 0) {
                sungWidth = 0f;
            } else {
                float startX = wordWidthPrefix[idx];
                float endX;
                if (idx + 1 < words.size()) {
                    endX = wordWidthPrefix[idx + 1];
                } else {
                    endX = textWidth;
                }
                long startT = words.get(idx).getTimeMs();
                long endT;
                if (idx + 1 < words.size()) {
                    endT = words.get(idx + 1).getTimeMs();
                } else {
                    endT = startT + LAST_WORD_SPAN_MS;
                }
                long span = endT - startT;
                if (span < MIN_WORD_SPAN_MS) {
                    span = MIN_WORD_SPAN_MS;
                }
                float frac = (float) (posMs - startT) / (float) span;
                if (frac < 0f) frac = 0f;
                if (frac > 1f) frac = 1f;
                sungWidth = startX + (endX - startX) * frac;
            }
        }

        float drawX;
        if (textWidth <= viewW) {
            drawX = (viewW - textWidth) / 2f;
        } else {
            float focusX = sungWidth;
            float desired = focusX - viewW / 2f;
            if (desired < 0f) desired = 0f;
            float maxScroll = textWidth - viewW;
            if (desired > maxScroll) desired = maxScroll;
            drawX = -desired;
        }

        float baseline = getPaddingTop() + (viewH - (paint.descent() - paint.ascent())) / 2f - paint.ascent();

        paint.setColor(dimColor);
        canvas.drawText(displayText, drawX + getPaddingLeft(), baseline, paint);

        if (sungWidth > 0f) {
            float clipRight = drawX + getPaddingLeft() + sungWidth;
            if (clipRight > 0f) {
                int save = canvas.save();
                canvas.clipRect(0f, 0f, clipRight, (float) getHeight());
                paint.setColor(sungColor);
                canvas.drawText(displayText, drawX + getPaddingLeft(), baseline, paint);
                canvas.restoreToCount(save);
            }
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        syncFrameState();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        syncFrameState();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        frameRunning = false;
        Choreographer.getInstance().removeFrameCallback(frameCallback);
    }

    private void postFrameCallback() {
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }
}
