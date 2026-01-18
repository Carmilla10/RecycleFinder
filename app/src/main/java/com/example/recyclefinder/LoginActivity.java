/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    EditText email, password;
    Button loginBtn, registerBtn;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);

        loginBtn.setOnClickListener(v -> {
            String userEmail = email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();

            if (userEmail.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(this, task -> {
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Login");

                        if (task.isSuccessful()) {
                            // Login success
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(LoginActivity.this, "Login successful!",
                                    Toast.LENGTH_SHORT).show();

                            // Go to MainActivity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();

                        } else {
                            // Login failed
                            Log.w(TAG, "signInWithEmail:failure", task.getException());

                            String errorMessage = "Login failed. ";

                            if (task.getException() != null) {
                                Exception exception = task.getException();

                                if (exception instanceof FirebaseAuthInvalidUserException) {
                                    errorMessage += "No account found with this email.";
                                } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                    errorMessage += "Invalid password.";
                                } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                                    errorMessage += "Password is too weak.";
                                } else if (exception instanceof FirebaseAuthUserCollisionException) {
                                    errorMessage += "Email already in use.";
                                } else {
                                    errorMessage += exception.getMessage();
                                }

                                // Show LONG toast with full error
                                Toast.makeText(LoginActivity.this,
                                        "Error: " + exception.getMessage(),
                                        Toast.LENGTH_LONG).show();

                                // Also log to console
                                Log.e(TAG, "Login error: ", exception);
                            }

                            // Show error message
                            Toast.makeText(LoginActivity.this, errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        registerBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}