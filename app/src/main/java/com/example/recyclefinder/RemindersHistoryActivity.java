/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemindersHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RemindersHistoryAdapter adapter;
    private List<Reminder> reminderList;
    private TextView emptyText;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private com.google.firebase.firestore.ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders_history);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        emptyText = findViewById(R.id.emptyText);

        reminderList = new ArrayList<>();
        adapter = new RemindersHistoryAdapter(reminderList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadReminders();
    }

    private void loadReminders() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view reminders", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        String userId = currentUser.getUid();

        // Remove previous listener if it exists
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        // Query reminders for current user (sorting will be done in code)
        listenerRegistration = firestore.collection("reminders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load reminders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmptyState();
                        return;
                    }

                    if (snapshot != null && !snapshot.isEmpty()) {
                        reminderList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            Reminder reminder = doc.toObject(Reminder.class);
                            if (reminder != null && !reminder.isDeleted) {
                                reminderList.add(reminder);
                            }
                        }

                        // Sort by reminderTime (newest first)
                        Collections.sort(reminderList, (o1, o2) -> Long.compare(o2.reminderTime, o1.reminderTime));

                        if (reminderList.isEmpty()) {
                            showEmptyState();
                        } else {
                            emptyText.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        showEmptyState();
                    }
                });
    }

    public void updateReminderStatus(String reminderId, boolean isClicked) {
        firestore.collection("reminders")
                .document(reminderId)
                .update("isClicked", isClicked)
                .addOnSuccessListener(aVoid -> {
                    String message = isClicked ? "Marked as done ✓" : "Unmarked";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void deleteReminder(String reminderId) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Reminder");
        builder.setMessage("Are you sure you want to delete this reminder?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            firestore.collection("reminders")
                    .document(reminderId)
                    .update("isDeleted", true)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEmptyState() {
        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText("No reminders yet.\nSet a reminder from the main screen!");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener to prevent errors after logout
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
