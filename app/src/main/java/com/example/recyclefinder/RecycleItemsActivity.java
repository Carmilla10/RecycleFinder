/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import java.util.List;

public class RecycleItemsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecycleItemsAdapter adapter;
    private List<RecycleItem> itemList;
    private TextView emptyText;
    private Button btnFindCenters;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle_items);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        emptyText = findViewById(R.id.emptyText);
        btnFindCenters = findViewById(R.id.btnFindCenters);

        itemList = new ArrayList<>();
        adapter = new RecycleItemsAdapter(itemList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnFindCenters.setOnClickListener(v -> {
            startActivity(new Intent(this, MapActivity.class));
        });

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Check if opened from notification with specific item
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("fromNotification", false)) {
            String itemId = intent.getStringExtra("itemId");
            String itemName = intent.getStringExtra("itemName");
            
            if (itemId != null) {
                // Show specific item details
                loadSpecificItem(itemId, itemName);
            } else {
                // This shouldn't happen when called from notification, but just in case
                finish();
            }
        } else {
            // Not called from notification - show empty
            showEmptyState();
        }
    }
    
    private void loadSpecificItem(String itemId, String itemName) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view items", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }
        
        firestore.collection("recycle_items")
                .document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        RecycleItem item = documentSnapshot.toObject(RecycleItem.class);
                        if (item != null) {
                            // Only show item if it belongs to current user
                            if (currentUser.getUid().equals(item.userId)) {
                                item.itemId = itemId;
                                itemList.clear();
                                itemList.add(item);
                                adapter.notifyDataSetChanged();
                                emptyText.setVisibility(View.GONE);
                                
                                // Scroll to top
                                recyclerView.scrollToPosition(0);
                            } else {
                                Toast.makeText(RecycleItemsActivity.this,
                                        "You don't have permission to view this item",
                                        Toast.LENGTH_SHORT).show();
                                showEmptyState();
                            }
                        } else {
                            showEmptyState();
                        }
                    } else {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RecycleItemsActivity.this,
                            "Failed to load item: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void showEmptyState() {
        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText("No item details available.");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

