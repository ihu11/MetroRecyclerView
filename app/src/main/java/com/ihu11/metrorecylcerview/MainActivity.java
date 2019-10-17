package com.ihu11.metrorecylcerview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.ihu11.metro.flow.FlowNormalView;
import com.ihu11.metro.flow.FlowView;

public class MainActivity extends Activity implements View.OnFocusChangeListener, View.OnClickListener {
    private FlowView flowView;
    private TextView textView1;
    private TextView textView2;
    private TextView textView3;
    private TextView textView4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flowView = findViewById(R.id.flow_view);
        textView1 = findViewById(R.id.text1);
        textView2 = findViewById(R.id.text2);
        textView3 = findViewById(R.id.text3);
        textView4 = findViewById(R.id.text4);
        textView1.setOnFocusChangeListener(this);
        textView2.setOnFocusChangeListener(this);
        textView3.setOnFocusChangeListener(this);
        textView4.setOnFocusChangeListener(this);
        textView1.setOnClickListener(this);
        textView2.setOnClickListener(this);
        textView3.setOnClickListener(this);
        textView4.setOnClickListener(this);

        textView4.setNextFocusDownId(R.id.text1);
        textView1.setNextFocusUpId(R.id.text4);

        requestViewFocus(textView1);
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        if (b) {
            if (view == textView1) {
                //flowView.setSmooth(false);//直接到 不使用动画
            } else if (view == textView2) {
                flowView.setFlowPadding(20, 20, 20, 20);//增加边框距离
            } else if (view == textView3) {
                flowView.setNextShape(FlowNormalView.SHAPE_RECT);//变成直角方形
            } else if (view == textView4) {
                flowView.setNextShape(FlowNormalView.SHAPE_ROUND);//变成圆边方形
            }
            flowView.moveTo(view, 1.0f);
        }
    }

    public static void requestViewFocus(final View view) {
        if (view.getWidth() > 0 && view.getHeight() > 0) {
            view.requestFocus();
            return;
        }
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                int width = view.getWidth();
                int height = view.getHeight();
                if (width > 0 && height > 0) {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    view.setFocusable(true);
                    view.requestFocus();
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view == textView3) {
            startActivity(new Intent(this, GridActivity.class));
        } else if (view == textView2) {
            startActivity(new Intent(this, HorActivity.class));
        } else if (view == textView1) {
            startActivity(new Intent(this, VerActivity.class));
        } else if (view == textView4) {
            startActivity(new Intent(this, ModifyActivity.class));
        }
    }
}
