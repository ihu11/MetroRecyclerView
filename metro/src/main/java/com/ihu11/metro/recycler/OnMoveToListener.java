package com.ihu11.metro.recycler;

import android.view.View;

public interface OnMoveToListener {
	public void onMoveTo(View view, float scale, int offsetX, int offsetY, boolean isSmooth);
}
