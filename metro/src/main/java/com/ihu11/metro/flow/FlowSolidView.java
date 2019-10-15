package com.ihu11.metro.flow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

public class FlowSolidView extends FlowNormalView {

	private LinearGradient shader;

	private int color1;
	private int color2;

	public FlowSolidView(Context context) {
		super(context);
	}

	public FlowSolidView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public FlowSolidView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public FlowSolidView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	protected void init(Context context) {
		super.init(context);
		mPaint.setStyle(Paint.Style.FILL);
	}

	@Override
	public void setRectColor(int color1, int color2) {
		this.color1 = color1;
		this.color2 = color2;
	}

	@Override
	protected void drawRoundRectF(Canvas canvas, RoundRectF rectF) {
		if (color1 != color2) {
			shader = new LinearGradient(rectF.left,
					rectF.top,
					rectF.right,
					rectF.top,
					color1,
					color2,
					Shader.TileMode.CLAMP);
			mPaint.setShader(shader);
		} else {
			mPaint.setColor(color1);
		}

		canvas.drawRoundRect(rectF, rectF.radius, rectF.radius, mPaint);
	}
}
