/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_SCREEN_DELAY = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Delay for 3 seconds then navigate
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if user is already logged in
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();

            Intent intent;
            if (user != null) {
                // User is logged in, go to MainActivity
                intent = new Intent(SplashScreenActivity.this, MainActivity.class);
            } else {
                // User not logged in, go to LoginActivity
                intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
            }
            
            startActivity(intent);
            finish(); // Close splash screen so it doesn't appear in back stack
        }, SPLASH_SCREEN_DELAY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Optional: Force clear cached user on each app start
        // Uncomment the line below if you want users to log in every time the app starts
        // FirebaseAuth.getInstance().signOut();
    }
}
