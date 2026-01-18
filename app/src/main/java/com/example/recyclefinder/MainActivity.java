/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            // Initialize Firebase
            auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();

            // Check if user is logged in
            if (user == null) {
                Log.d(TAG, "User not logged in, redirecting to LoginActivity");
                // Clear any back stack so we can't go back to MainActivity from LoginActivity
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
            
            Log.d(TAG, "User logged in, initializing MainActivity");
            isInitialized = true; // Mark that we've successfully initialized

            // Initialize Firebase Database reference
            userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

            // Get views
            drawerLayout = findViewById(R.id.drawerLayout);
            NavigationView navigationView = findViewById(R.id.navigationView);
            Button menuButton = findViewById(R.id.menuButton);
            
            // Load user's full name into navigation header
            loadUserNameIntoHeader(navigationView, user);
            
            menuButton.setOnClickListener(v -> {
                drawerLayout.openDrawer(GravityCompat.START);
            });

            // Setup navigation drawer item clicks
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                // Handle item selection
                if (id == R.id.nav_home) {
                    // Already on home, just close drawer
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                else if (id == R.id.nav_profile) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                else if (id == R.id.nav_items) {
                    startActivity(new Intent(MainActivity.this, RemindersHistoryActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                else if (id == R.id.nav_how_to_use) {
                    startActivity(new Intent(MainActivity.this, HowToUseActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                else if (id == R.id.nav_guidelines) {
                    startActivity(new Intent(MainActivity.this, GuidelinesActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                else if (id == R.id.nav_about) {
                    startActivity(new Intent(MainActivity.this, AboutDeveloperActivity.class));
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                else if (id == R.id.nav_logout) {
                    // Logout user
                    auth.signOut();
                    Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                    Intent logoutIntent = new Intent(MainActivity.this, LoginActivity.class);
                    logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(logoutIntent);
                    finish();
                }

                return true;
            });

            // Main content button clicks
            findViewById(R.id.btnFind).setOnClickListener(v -> {
                startActivity(new Intent(this, MapActivity.class));
            });

            findViewById(R.id.btnSetReminder).setOnClickListener(v -> {
                startActivity(new Intent(this, SetReminderActivity.class));
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
            Toast.makeText(this, "Error initializing activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called, isInitialized: " + isInitialized);
        // Don't check auth in onStart to avoid redirect loop
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Don't check auth in onResume to avoid redirect loop
    }

    @Override
    public void onBackPressed() {
        // Close drawer if open, otherwise normal back
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Load user's full name from Firebase and display in navigation header
    private void loadUserNameIntoHeader(NavigationView navigationView, FirebaseUser firebaseUser) {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.name != null) {
                        // Update the welcome message with user's full name
                        android.view.View headerView = navigationView.getHeaderView(0);
                        android.widget.TextView navHeaderName = headerView.findViewById(R.id.nav_header_name);
                        
                        navHeaderName.setText(user.name);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading user data: " + error.getMessage());
            }
        });
    }
}