/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

public class RecycleItem {
    public String name;
    public String imageUrl;
    public long timestamp;
    public String userId;
    public boolean isRecycled;
    public String itemId; // For deletion

    public RecycleItem() {
        // Required for Firebase
        this.isRecycled = false;
    }

    public RecycleItem(String name, String imageUrl, long timestamp, String userId) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.userId = userId;
        this.isRecycled = false;
    }
}

