/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecycleItemsAdapter extends RecyclerView.Adapter<RecycleItemsAdapter.ItemViewHolder> {

    private List<RecycleItem> itemList;
    private RecycleItemsActivity activity;

    public RecycleItemsAdapter(List<RecycleItem> itemList, RecycleItemsActivity activity) {
        this.itemList = itemList;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recycle_detail, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        RecycleItem item = itemList.get(position);

        holder.itemName.setText(item.name != null ? item.name : "Recycle Item");

        if (item.timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault());
            holder.itemDate.setText("Added: " + sdf.format(new Date(item.timestamp)));
        } else {
            holder.itemDate.setText("");
        }

        // Load Base64 image from Firestore
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            try {
                // Check if it's a URL (starts with http) - for backward compatibility
                if (item.imageUrl.startsWith("http://") || item.imageUrl.startsWith("https://")) {
                    // Load from URL using Glide (for old Storage URLs)
                    Glide.with(holder.itemView.getContext())
                            .load(item.imageUrl)
                            .placeholder(android.R.drawable.ic_menu_camera)
                            .error(android.R.drawable.ic_menu_camera)
                            .into(holder.itemImage);
                } else {
                    // Decode Base64 image from Firestore
                    byte[] decodedBytes = Base64.decode(item.imageUrl, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    holder.itemImage.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                holder.itemImage.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            holder.itemImage.setImageResource(android.R.drawable.ic_menu_camera);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName;
        TextView itemDate;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemDate = itemView.findViewById(R.id.itemDate);
        }
    }
}