/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

public class Reminder {
    public String reminderId;
    public String itemId;
    public String itemName;
    public String userId;
    public long reminderTime;
    public long createdAt;
    public boolean isClicked;
    public boolean isDeleted;

    public Reminder() {
        // Required for Firestore
        this.isClicked = false;
        this.isDeleted = false;
    }

    public Reminder(String reminderId, String itemId, String itemName, String userId, long reminderTime, long createdAt) {
        this.reminderId = reminderId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.userId = userId;
        this.reminderTime = reminderTime;
        this.createdAt = createdAt;
        this.isClicked = false;
        this.isDeleted = false;
    }
}
