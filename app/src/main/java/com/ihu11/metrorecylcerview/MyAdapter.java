package com.ihu11.metrorecylcerview;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ihu11.metro.recycler.MetroRecyclerView;

import java.util.List;

public class MyAdapter extends MetroRecyclerView.MetroAdapter<MyAdapter.ItemViewHolder> {

    private List<Integer> list;

    public MyAdapter(List<Integer> list) {
        this.list = list;
    }

    @Override
    public void onPrepareBindViewHolder(ItemViewHolder holder, int position) {
        holder.dataTxt.setText("p-" + position);
    }

    @Override
    public void onDelayBindViewHolder(ItemViewHolder holder, int position) {
        holder.icon.setBackgroundResource(list.get(position));
        Log.i("Catch", "onDelayBindViewHolder:" + position);
    }

    @Override
    public void onUnBindDelayViewHolder(ItemViewHolder holder) {
        holder.icon.setBackgroundResource(R.drawable.translate);
        Log.i("Catch", "onUnBindDelayViewHolder:" + holder.getAdapterPosition());
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View convertView = LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.item, viewGroup, false);
        return new ItemViewHolder(convertView);
    }

    @Override
    public int getItemCount() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public final static class ItemViewHolder extends MetroRecyclerView.MetroViewHolder {
        TextView dataTxt;
        ImageView icon;

        public ItemViewHolder(View itemView) {
            super(itemView);
            dataTxt = itemView
                    .findViewById(R.id.text);
            icon = itemView.findViewById(R.id.img);
        }
    }
}
