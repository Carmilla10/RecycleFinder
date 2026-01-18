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
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersHistoryAdapter extends RecyclerView.Adapter<RemindersHistoryAdapter.ReminderViewHolder> {

    private List<Reminder> reminderList;
    private RemindersHistoryActivity activity;
    private DatabaseReference databaseRef;

    public RemindersHistoryAdapter(List<Reminder> reminderList, RemindersHistoryActivity activity) {
        this.reminderList = reminderList;
        this.activity = activity;
        this.databaseRef = FirebaseDatabase.getInstance().getReference("recycle_items");
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder_history, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);

        holder.reminderName.setText(reminder.itemName != null ? reminder.itemName : "Recycled Item");

        if (reminder.reminderTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault());
            holder.reminderDate.setText("Reminder: " + sdf.format(new Date(reminder.reminderTime)));
        } else {
            holder.reminderDate.setText("");
        }

        // Load image from database
        databaseRef.child(reminder.itemId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                RecycleItem item = task.getResult().getValue(RecycleItem.class);
                if (item != null && item.imageUrl != null && !item.imageUrl.isEmpty()) {
                    try {
                        if (item.imageUrl.startsWith("http://") || item.imageUrl.startsWith("https://")) {
                            // URL-based image
                        } else {
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
            } else {
                holder.itemImage.setImageResource(android.R.drawable.ic_menu_camera);
            }
        });

        // Set checkbox state with label
        holder.checkboxDone.setOnCheckedChangeListener(null);
        holder.checkboxDone.setChecked(reminder.isClicked);
        holder.checkboxDone.setText(reminder.isClicked ? "✓ Recycled" : "Mark Recycled");
        
        holder.checkboxDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activity.updateReminderStatus(reminder.reminderId, isChecked);
            reminder.isClicked = isChecked;
            holder.checkboxDone.setText(isChecked ? "✓ Recycled" : "Mark Recycled");
        });

        // Set delete button listener
        holder.btnDelete.setOnClickListener(v -> {
            activity.deleteReminder(reminder.reminderId);
        });

        // Update visual state if reminder is clicked
        if (reminder.isClicked) {
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    public static class ReminderViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView reminderName;
        TextView reminderDate;
        CheckBox checkboxDone;
        Button btnDelete;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            reminderName = itemView.findViewById(R.id.reminderName);
            reminderDate = itemView.findViewById(R.id.reminderDate);
            checkboxDone = itemView.findViewById(R.id.checkboxDone);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
