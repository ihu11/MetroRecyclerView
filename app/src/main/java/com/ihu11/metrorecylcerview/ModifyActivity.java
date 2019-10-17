package com.ihu11.metrorecylcerview;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.ihu11.metro.flow.FlowView;
import com.ihu11.metro.recycler.MetroItemClickListener;
import com.ihu11.metro.recycler.MetroItemFocusListener;
import com.ihu11.metro.recycler.MetroRecyclerView;
import com.ihu11.metro.recycler.OnMoveToListener;

import java.util.ArrayList;
import java.util.List;

public class ModifyActivity extends Activity {

    private MetroRecyclerView recyclerView;
    private FlowView flowView;
    private MyAdapter adapter;
    private List<Integer> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify);

        flowView = findViewById(R.id.flow_view);
        recyclerView = findViewById(R.id.recycler_view);

        recyclerView.setScrollType(MetroRecyclerView.SCROLL_TYPE_ALWAYS_CENTER);
        MetroRecyclerView.MetroGridLayoutManager layoutManager = new MetroRecyclerView.MetroGridLayoutManager(
                this, 6, MetroRecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setOnMoveToListener(new OnMoveToListener() {
            @Override
            public void onMoveTo(View view, float scale, int offsetX, int offsetY, boolean isSmooth) {
                flowView.moveTo(view, scale, offsetX, offsetY, isSmooth);
            }
        });
        recyclerView.setOnItemClickListener(new MetroItemClickListener() {
            @Override
            public void onItemClick(View parentView, View itemView, int position) {
                recyclerView.deleteItem(position, dataList);
            }
        });
        recyclerView.setOnItemFocusListener(new MetroItemFocusListener() {
            @Override
            public void onItemFocus(View parentView, View itemView, int position, int total) {
                //TODO
            }
        });

        dataList = genData();
        adapter = new MyAdapter(dataList);
        recyclerView.setAdapter(adapter);
        recyclerView.requestFocus();
    }

    private List<Integer> genData() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            if (i % 5 == 0) {
                list.add(R.drawable.a1);
            } else if (i % 5 == 1) {
                list.add(R.drawable.a2);
            } else if (i % 5 == 2) {
                list.add(R.drawable.a3);
            } else if (i % 5 == 3) {
                list.add(R.drawable.a4);
            } else if (i % 5 == 4) {
                list.add(R.drawable.a5);
            }
        }
        return list;
    }
}
