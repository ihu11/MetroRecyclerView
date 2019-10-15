package com.ihu11.metro.flow;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.ihu11.metro.R;

public class FlowView extends ViewGroup {

    public final static int VIEW_TYPE_NORMAL = 0;
    public final static int VIEW_TYPE_SOLID = 1;
    public final static int VIEW_TYPE_NO_SHADOW = 2;

    protected FlowNormalView iFlowView;
    private int viewType;
    private float roundRadius;

    public FlowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public FlowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowView(Context context) {
        this(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        int color1 = -1;
        int color2 = -1;
        int strokeWidth = -1;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowView);
            viewType = a.getInt(R.styleable.FlowView_viewType, VIEW_TYPE_NORMAL);
            roundRadius = a.getDimensionPixelSize(R.styleable.FlowView_round_radius, 0);
            color1 = a.getResourceId(R.styleable.FlowView_flow_color1, -1);
            color2 = a.getResourceId(R.styleable.FlowView_flow_color2, -1);
            strokeWidth = a.getResourceId(R.styleable.FlowView_flow_stroke_width, -1);
            a.recycle();
        } else {
            viewType = VIEW_TYPE_NORMAL;
        }

        if (viewType == VIEW_TYPE_NORMAL) {
            iFlowView = new FlowNormalView(context);
            addView(iFlowView);
        } else if (viewType == VIEW_TYPE_SOLID) {
            iFlowView = new FlowSolidView(context);
            addView(iFlowView);
        } else if (viewType == VIEW_TYPE_NO_SHADOW) {
            iFlowView = new FlowNormalView(context);
            iFlowView.setNoShawdow();
            addView(iFlowView);
        }

        iFlowView.setDefaultRadius(roundRadius);
        iFlowView.setStrokeWidth(strokeWidth);
        int c1 = -1;
        if (color1 != -1) {
            c1 = getResources().getColor(color1);
        }
        int c2 = -1;
        if (color2 != -1) {
            c2 = getResources().getColor(color2);
        }
        iFlowView.setRectColor(c1, c2);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int cCount = getChildCount();
        for (int i = 0; i < cCount; i++) {
            View childView = getChildAt(i);
            childView.layout(l, t, r, b);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        int cCount = getChildCount();
        for (int i = 0; i < cCount; i++) {
            View childView = getChildAt(i);
            childView.setVisibility(visibility);
        }
    }

    public void moveTo(View view, float scale) {
        if (iFlowView != null) {
            iFlowView.moveTo(view, scale);
        }
    }

    public void setFlowPadding(int left, int top, int right, int bottom) {
        if (iFlowView != null) {
            iFlowView.setFlowPadding(left, top, right, bottom);
        }
    }

    public void moveTo(float x, float y, float width, float height, float scale) {
        if (iFlowView != null) {
            iFlowView.moveTo(x, y, width, height, scale);
        }
    }

    public void moveTo(View view, float scale, int offsetX, int offsetY, boolean isSmooth) {
        if (iFlowView != null) {
            iFlowView.moveTo(view, scale, offsetX, offsetY, isSmooth);
        }
    }

    public void setOffset(int x, int y) {
        if (iFlowView != null) {
            iFlowView.setOffset(x, y);
        }
    }

    public void setSmooth(boolean isSmooth) {
        if (iFlowView != null) {
            iFlowView.setSmooth(isSmooth);
        }
    }

    public void setNextShape(int shape) {
        if (iFlowView != null) {
            iFlowView.setNextShape(shape);
        }
    }

    public void setRectColor(int rectColor, int shadowColor) {
        if (iFlowView != null) {
            iFlowView.setRectColor(rectColor, shadowColor);
        }
    }

    public void setStrokeWidth(float strokeWidth) {
        iFlowView.setStrokeWidth(strokeWidth);
    }

    public void setDefaultRadius(float roundRadius) {
        iFlowView.setDefaultRadius(roundRadius);
    }
}