/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class RecycleReminderHelper {

    public static int setRecycleReminder(Context context, long reminderTimeMillis, String itemName, String itemId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        int requestCode = (int) ((reminderTimeMillis + (itemId != null ? itemId.hashCode() : 0)) % Integer.MAX_VALUE);
        
        Intent intent = new Intent(context, RecycleReminderReceiver.class);
        intent.putExtra("itemName", itemName);
        intent.putExtra("reminderTime", reminderTimeMillis);
        intent.putExtra("itemId", itemId);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTimeMillis,
                            pendingIntent
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                }
                return requestCode;
            } catch (SecurityException e) {
                // Permission not granted
                return -1;
            }
        }
        return -1;
    }

    public static void cancelReminder(Context context, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RecycleReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT
        );
        
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}

