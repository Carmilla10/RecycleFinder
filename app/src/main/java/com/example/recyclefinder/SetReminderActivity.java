/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SetReminderActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 102;

    private Button btnTakePhoto;
    private Button btnUploadPhoto;
    private Button btnSetReminder;
    private ImageView imageView;
    private EditText editTextItemName;

    private Bitmap selectedBitmap;
    private FirebaseFirestore firestore;
    private String itemName = "";
    private long reminderTime = 0;

    // Simple click prevention
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_reminder);

        firestore = FirebaseFirestore.getInstance();

        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnSetReminder = findViewById(R.id.btnSetReminder);
        imageView = findViewById(R.id.imageView);
        editTextItemName = findViewById(R.id.editTextItemName);

        // Take Photo button
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCameraPermission()) {
                    openCamera();
                } else {
                    requestCameraPermission();
                }
            }
        });

        // Upload Photo button
        btnUploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        // Set Reminder button with simple click prevention
        btnSetReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prevent rapid double-clicks (500ms threshold)
                if (System.currentTimeMillis() - lastClickTime < 500) {
                    return;
                }
                lastClickTime = System.currentTimeMillis();

                if (selectedBitmap == null) {
                    Toast.makeText(SetReminderActivity.this, "Please take or upload a photo first", Toast.LENGTH_SHORT).show();
                    return;
                }

                itemName = editTextItemName.getText().toString().trim();
                if (itemName.isEmpty()) {
                    Toast.makeText(SetReminderActivity.this, "Please enter a name/notes for this item", Toast.LENGTH_SHORT).show();
                    return;
                }

                checkAlarmPermissionsAndShowPicker();
            }
        });
    }

    // Check camera permission
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                // Camera photo
                if (data != null && data.getExtras() != null) {
                    selectedBitmap = (Bitmap) data.getExtras().get("data");
                    showImage();
                }
            }
            else if (requestCode == GALLERY_REQUEST_CODE) {
                // Gallery image
                if (data != null && data.getData() != null) {
                    Uri imageUri = data.getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        selectedBitmap = BitmapFactory.decodeStream(inputStream);
                        showImage();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    // Show selected image
    private void showImage() {
        if (selectedBitmap != null) {
            imageView.setImageBitmap(selectedBitmap);
            imageView.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Photo selected!", Toast.LENGTH_SHORT).show();
        }
    }

    // Open camera
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    // Open gallery
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    private void checkAlarmPermissionsAndShowPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                showExactAlarmPermissionDialog();
                return;
            }
        }
        showDateTimePicker();
    }

    private void showExactAlarmPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alarm Permission Required");
        builder.setMessage("This app needs permission to set exact alarms for accurate reminders. Please grant permission in the next screen.");
        builder.setPositiveButton("Grant Permission", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDateTimePicker() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (view1, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);

                                reminderTime = calendar.getTimeInMillis();

                                if (reminderTime < System.currentTimeMillis()) {
                                    Toast.makeText(this, "Please select a future date and time", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Show confirmation dialog before saving
                                showConfirmationDialog();
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showConfirmationDialog() {
        // Cleaner, more compact date format
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String formattedDateTime = sdf.format(reminderTime);

        String message = "📌 " + itemName + "\n" +
                "⏰ " + formattedDateTime + "\n\n" +
                "Save this reminder?";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Reminder");
        builder.setMessage(message);
        builder.setPositiveButton("Save", (dialog, which) -> {
            saveItemAndSetReminder();
        });
        builder.setNegativeButton("Cancel", null);
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveItemAndSetReminder() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "unknown";
        String itemId = firestore.collection("recycle_items").document().getId();

        try {
            // Compress and encode image to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // Create item data for Firestore
            java.util.Map<String, Object> itemData = new java.util.HashMap<>();
            itemData.put("name", itemName);
            itemData.put("imageUrl", imageBase64);
            itemData.put("timestamp", System.currentTimeMillis());
            itemData.put("userId", userId);

            // Save item to Firestore
            firestore.collection("recycle_items")
                    .document(itemId)
                    .set(itemData)
                    .addOnSuccessListener(aVoid -> {
                        // Set local alarm
                        int reminderId = RecycleReminderHelper.setRecycleReminder(
                                this,
                                reminderTime,
                                itemName,
                                itemId
                        );

                        if (reminderId > 0) {
                            // Save reminder to Firestore
                            String reminderDocId = firestore.collection("reminders").document().getId();
                            Reminder reminder = new Reminder(
                                    reminderDocId,
                                    itemId,
                                    itemName,
                                    userId,
                                    reminderTime,
                                    System.currentTimeMillis()
                            );

                            firestore.collection("reminders")
                                    .document(reminderDocId)
                                    .set(reminder)
                                    .addOnSuccessListener(aVoid2 -> {
                                        // Show simple success toast and close
                                        Toast.makeText(SetReminderActivity.this,
                                                "✓ Reminder saved successfully!",
                                                Toast.LENGTH_LONG).show();

                                        // Close activity after 1 second
                                        new Handler().postDelayed(() -> finish(), 1000);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("SetReminderActivity", "Failed to save reminder to Firestore: " + e.getMessage(), e);
                                        Toast.makeText(SetReminderActivity.this,
                                                "✓ Reminder saved! (sync may be delayed)",
                                                Toast.LENGTH_LONG).show();
                                        new Handler().postDelayed(() -> finish(), 1000);
                                    });
                        } else {
                            Toast.makeText(this, "Failed to set reminder", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("SetReminderActivity", "Failed to save to Firestore: " + e.getMessage(), e);
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("SetReminderActivity", "Error: " + e.getMessage(), e);
        }
    }
}