package com.ihu11.metro.flow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public class FlowNormalView extends View {

    public final static int SHAPE_ROUND_RECT = 0;
    public final static int SHAPE_RECT = 1;
    public final static int SHAPE_ROUND = 2;

    private final static String TAG = "FlowHand";
    private final static float SPEED = 2f;// 默认速度
    private final static long MIN_FLOW_TIME = 200;// 最小时间
    private final static long MAX_FLOW_TIME = 300;// 最大时间
    private final static float SHADOW_TOTAL_WIDTH = 40f;
    private final static float SHADOW_STROKE_WIDTH = 1f;
    private final static int SHADOW_ALPHA_START = 150;
    private static final Interpolator mInterpolator1 = new AccelerateDecelerateInterpolator();
    private static final Interpolator mInterpolator2 = new DecelerateInterpolator();
    private static final Interpolator mInterpolatorShadow = new AccelerateInterpolator();
    private static final int INTERVAL_TIME_NORMAL = 20;

    private boolean isRun = false;
    private int offsetX = 0;
    private int offsetY = 0;
    private int paddingLeft = 0;
    private int paddingRight = 0;
    private int paddingTop = 0;
    private int paddingBottom = 0;
    private int shape = SHAPE_ROUND_RECT;
    private float strokeWidth;
    protected float screenScale;
    private boolean isSmooth = true;
    private boolean isContinuityMove = false;
    private boolean noShawdow = false;

    private int rectColor = 0xFFFFFFFF;
    private int shadowColor = 0xFF000000;

    private RoundRectF lastRectF;
    private RoundRectF currRectF;
    private RoundRectF destRectF;
    private RoundRectF shadowRectF;

    protected Paint mPaint;
    private Paint mPaintShadow;
    private Paint paintEraser;
    private float defaultRadius;

    private long startTime = 0L;
    private long totalTime = 0L;
    private float distance = 0f;

    class RoundRectF extends RectF {
        float radius;

        public RoundRectF() {
        }

        public void set(RoundRectF src) {
            this.left = src.left;
            this.right = src.right;
            this.top = src.top;
            this.bottom = src.bottom;
            this.radius = src.radius;
        }

    }

    public FlowNormalView(Context context) {
        super(context);
        init(context);
    }

    public FlowNormalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FlowNormalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public FlowNormalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    protected void init(Context context) {

        lastRectF = new RoundRectF();
        currRectF = new RoundRectF();
        destRectF = new RoundRectF();
        shadowRectF = new RoundRectF();

        screenScale = context.getResources().getDisplayMetrics().widthPixels / 1920f;
        strokeWidth = 2f * screenScale;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(rectColor);
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);

        mPaintShadow = new Paint();
        mPaintShadow.setAntiAlias(true);
        mPaintShadow.setColor(shadowColor);
        mPaintShadow.setStyle(Paint.Style.STROKE);

        paintEraser = new Paint();
        paintEraser.setAntiAlias(true);
        paintEraser.setColor(Color.BLACK);
        paintEraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }

    private void start() {
        if (isRun) {
            return;
        }
        isRun = true;
        new Thread() {
            public void run() {
                while (isRun) {
                    refreshView();
                }
                isRun = false;
            }
        }.start();
    }

    protected void refreshView() {
        try {
            Thread.sleep(INTERVAL_TIME_NORMAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        postInvalidate();
    }

    protected void moveToLocation(float l, float t, float width, float height, float roundScale) {
        resetLast();

        // 偏移量
        l -= offsetX;
        t -= offsetY;

        destRectF.left = l - paddingLeft;
        destRectF.right = l + width + paddingRight;
        destRectF.top = t - paddingTop;
        destRectF.bottom = t + height + paddingBottom;
        if (shape == SHAPE_ROUND_RECT) {
            destRectF.radius = defaultRadius * roundScale;
        } else if (shape == SHAPE_RECT) {
            destRectF.radius = 0f;
        } else if (shape == SHAPE_ROUND) {
            destRectF.radius = Math.min(destRectF.bottom - destRectF.top, destRectF.right - destRectF.left) / 2;
        }

        startTime = System.currentTimeMillis();
        distance = getDistance(lastRectF.centerX(), lastRectF.centerY(), destRectF.centerX(), destRectF.centerY());

        if (isSmooth) {
            if (lastRectF.width() < 0.1f || lastRectF.height() < 0.1f) {
                totalTime = 0;
            } else {
                totalTime = (long) (distance / SPEED);
                if (totalTime > MAX_FLOW_TIME) {
                    totalTime = MAX_FLOW_TIME;
                } else if (totalTime < MIN_FLOW_TIME) {
                    totalTime = MIN_FLOW_TIME;
                }
            }
        } else {
            totalTime = 0;
        }
        reset();
        start();
    }

    private float getDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    private float getCurrDistance() {
        long time = System.currentTimeMillis();
        if (time >= startTime + totalTime) {
            return distance;
        }
        float p = (time - startTime) / (float) totalTime;
        if (isContinuityMove) {
            return mInterpolator2.getInterpolation(p) * distance;
        } else {
            return mInterpolator1.getInterpolation(p) * distance;
        }
    }

    private void resetLast() {
        if (Math.abs(getCurrDistance() - distance) < 0.1f) {
            isContinuityMove = false;
        } else {
            isContinuityMove = true;
        }

        lastRectF.set(currRectF);
    }

    @Override
    public void onDraw(Canvas canvas) {
        drawView(canvas);
    }

    private void drawView(Canvas canvas) {
        float currDistance = getCurrDistance();
        if (distance != 0) {
            currRectF.left = lastRectF.left + (destRectF.left - lastRectF.left) * (currDistance / distance);
            currRectF.right = lastRectF.right + (destRectF.right - lastRectF.right) * (currDistance / distance);
            currRectF.top = lastRectF.top + (destRectF.top - lastRectF.top) * (currDistance / distance);
            currRectF.bottom = lastRectF.bottom + (destRectF.bottom - lastRectF.bottom) * (currDistance / distance);
            currRectF.radius = lastRectF.radius + (destRectF.radius - lastRectF.radius) * (currDistance / distance);
        }

        if (currRectF.width() > 0.1f && currRectF.height() > 0.1f) {
            drawRoundRectF(canvas, currRectF);
            drawExtra(canvas, currRectF);
        }

        if (Math.abs(currDistance - distance) < 0.1f) {
            isContinuityMove = false;
            isRun = false;
        }
    }

    protected void drawExtra(Canvas canvas, RectF currRectF) {

    }

    protected void drawRoundRectF(Canvas canvas, RoundRectF currRectF) {
        shadowRectF.set(currRectF);
        if (!noShawdow) {
            mPaintShadow.setStrokeWidth(SHADOW_STROKE_WIDTH);
            for (float i = 0f; i < SHADOW_TOTAL_WIDTH; i += SHADOW_STROKE_WIDTH) {
                shadowRectF.left = currRectF.left - i;
                shadowRectF.top = currRectF.top - i;
                shadowRectF.right = currRectF.right + i;
                shadowRectF.bottom = currRectF.bottom + i;
                shadowRectF.radius = currRectF.radius + i;
                float p = mInterpolatorShadow.getInterpolation(((SHADOW_TOTAL_WIDTH - i) / SHADOW_TOTAL_WIDTH));
                mPaintShadow.setAlpha((int) (p * SHADOW_ALPHA_START));
                canvas.drawRoundRect(shadowRectF, shadowRectF.radius, shadowRectF.radius, mPaintShadow);
            }
        }
        canvas.drawRoundRect(currRectF, currRectF.radius, currRectF.radius, mPaint);
    }

    public void setNoShawdow() {
        noShawdow = true;
    }

    public void setDefaultRadius(float defaultRadius) {
        this.defaultRadius = defaultRadius;
    }

    public void setNextShape(int shape) {
        this.shape = shape;
    }

    /**
     * 设置偏移
     *
     * @param x X轴的偏移量
     * @param y Y轴的偏移量
     */
    public void setOffset(int x, int y) {
        offsetX = x;
        offsetY = y;
    }

    public void setSmooth(boolean isSmooth) {
        this.isSmooth &= isSmooth;
    }

    private void reset() {
        offsetX = 0;
        offsetY = 0;
        isSmooth = true;
        paddingLeft = 0;
        paddingRight = 0;
        paddingTop = 0;
        paddingBottom = 0;
        shape = SHAPE_ROUND_RECT;
    }

    /**
     * 移动到view的位置
     *
     * @param view
     * @param scale    是否缩放
     * @param offsetX  X轴的偏移量
     * @param offsetY  Y轴的偏移量
     * @param isSmooth 是否平滑滚动
     */
    public void moveTo(View view, float scale, int offsetX, int offsetY, boolean isSmooth) {
        if (view == null) {
            Log.e(TAG, "view is null");
            return;
        }
        setOffset(offsetX, offsetY);
        setSmooth(isSmooth);
        moveTo(view, scale);
    }

    public void moveTo(View view, float scale) {
        int h = view.getHeight();
        int w = view.getWidth();
        // Log.d(TAG, "width:" + w);
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int x = location[0];
        int y = location[1];

        x += ((view.getScaleX() - 1) / 2) * w;
        y += ((view.getScaleY() - 1) / 2) * h;

        moveTo(x, y, w, h, scale);
    }

    public void moveTo(float x, float y, float width, float height, float scale) {
        if (width > 0 && height > 0) {
            int[] location = new int[2];
            ((ViewGroup) getParent().getParent()).getLocationInWindow(location);
            int pX = location[0];
            int pY = location[1];
            float l = x - pX;
            float t = y - pY;
            moveToLocation(l - ((scale - 1) / 2) * width, t - ((scale - 1) / 2) * height, width * scale, height * scale,
                    scale);
        } else {
            reset();
            Log.e(TAG, "width or height is 0");
        }
    }

    public void setFlowPadding(int left, int top, int right, int bottom) {
        this.paddingLeft = left;
        this.paddingRight = right;
        this.paddingTop = top;
        this.paddingBottom = bottom;
    }

    public void setRectColor(int rectColor, int shadowColor) {
        if (rectColor != -1) {
            this.rectColor = rectColor;
            mPaint.setColor(rectColor);
        }
        if (shadowColor != -1) {
            this.shadowColor = shadowColor;
            mPaintShadow.setColor(shadowColor);
        }
    }

    public void setStrokeWidth(float strokeWidth) {
        if (strokeWidth > 0) {
            this.strokeWidth = strokeWidth;
            mPaint.setStrokeWidth(strokeWidth);
        }
    }
}
