/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText email, password, confirmPassword, name, phone;
    private Button registerBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ImageView togglePassword, toggleConfirmPassword;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    // Track validation states
    private boolean isNameValid = false;
    private boolean isEmailValid = false;
    private boolean isEmailAvailable = true;
    private boolean isPasswordValid = false;
    private boolean isConfirmPasswordValid = false;
    private boolean isPhoneValid = true;

    // For debouncing email checks
    private Handler emailCheckHandler = new Handler();
    private Runnable emailCheckRunnable;
    private static final long EMAIL_CHECK_DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize all views
        initializeViews();

        // Set up password visibility toggles
        setupPasswordToggles();

        // Set up real-time validation
        setupRealTimeValidation();

        // Set register button click listener
        registerBtn.setOnClickListener(v -> registerUser());

        // Initial button state
        updateRegisterButtonState();
    }

    private void initializeViews() {
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        name = findViewById(R.id.name);
        phone = findViewById(R.id.phone);
        registerBtn = findViewById(R.id.registerBtn);
        togglePassword = findViewById(R.id.togglePassword);
        toggleConfirmPassword = findViewById(R.id.toggleConfirmPassword);

        name.setHint("Enter your full name");
        email.setHint("Enter your email");
        password.setHint("Create a password");
        confirmPassword.setHint("Confirm your password");
        phone.setHint("Optional phone number");
    }

    private void setupPasswordToggles() {
        // Password toggle
        togglePassword.setOnClickListener(v -> {
            if (passwordVisible) {
                // Hide password
                password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                // Show password
                password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_visibility);
            }
            passwordVisible = !passwordVisible;
            password.setSelection(password.getText().length());
        });

        // Confirm password toggle
        toggleConfirmPassword.setOnClickListener(v -> {
            if (confirmPasswordVisible) {
                // Hide confirm password
                confirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                // Show confirm password
                confirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                toggleConfirmPassword.setImageResource(R.drawable.ic_visibility);
            }
            confirmPasswordVisible = !confirmPasswordVisible;
            confirmPassword.setSelection(confirmPassword.getText().length());
        });
    }

    private void setupRealTimeValidation() {
        // Name validation
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Email validation with debouncing
        email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous check
                if (emailCheckRunnable != null) {
                    emailCheckHandler.removeCallbacks(emailCheckRunnable);
                }

                emailCheckRunnable = new Runnable() {
                    @Override
                    public void run() {
                        validateEmail(s.toString());
                        if (isEmailValid) {
                            checkEmailAvailability(s.toString());
                        }
                    }
                };
                emailCheckHandler.postDelayed(emailCheckRunnable, EMAIL_CHECK_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Password validation
        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(s.toString());
                validateConfirmPassword();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Confirm password validation
        confirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateConfirmPassword();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Phone validation (optional)
        phone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePhone(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validateName(String nameStr) {
        if (nameStr.isEmpty()) {
            name.setError("Full name is required");
            isNameValid = false;
        } else if (nameStr.length() < 2) {
            name.setError("Name must be at least 2 characters");
            isNameValid = false;
        } else {
            name.setError(null);
            isNameValid = true;
        }
        updateRegisterButtonState();
    }

    private void validateEmail(String emailStr) {
        if (emailStr.isEmpty()) {
            email.setError("Email is required");
            isEmailValid = false;
            isEmailAvailable = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            email.setError("Enter a valid email address");
            isEmailValid = false;
            isEmailAvailable = true;
        } else {
            email.setError(null);
            isEmailValid = true;
        }
        updateRegisterButtonState();
    }

    private void checkEmailAvailability(String emailStr) {
        if (!isEmailValid) return;

        // Show checking indicator
        email.setError("Checking availability...");

        auth.fetchSignInMethodsForEmail(emailStr)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isRegistered = !task.getResult().getSignInMethods().isEmpty();

                        if (isRegistered) {
                            email.setError("Email already registered");
                            isEmailAvailable = false;
                        } else {
                            email.setError(null);
                            isEmailAvailable = true;
                        }
                    } else {
                        email.setError(null);
                        isEmailAvailable = true;
                    }
                    updateRegisterButtonState();
                })
                .addOnFailureListener(e -> {
                    if (email.getError() != null && email.getError().toString().equals("Checking availability...")) {
                        email.setError(null);
                    }
                    isEmailAvailable = true;
                    updateRegisterButtonState();
                });
    }

    private void validatePassword(String passwordStr) {
        if (passwordStr.isEmpty()) {
            password.setError("Password is required");
            isPasswordValid = false;
        } else if (passwordStr.length() < 6) {
            password.setError("Password must be at least 6 characters");
            isPasswordValid = false;
        } else {
            password.setError(null);
            isPasswordValid = true;
        }
        updateRegisterButtonState();
    }

    private void validateConfirmPassword() {
        String passwordStr = password.getText().toString().trim();
        String confirmPasswordStr = confirmPassword.getText().toString().trim();

        if (confirmPasswordStr.isEmpty()) {
            confirmPassword.setError("Please confirm your password");
            isConfirmPasswordValid = false;
        } else if (!passwordStr.equals(confirmPasswordStr)) {
            confirmPassword.setError("Passwords do not match");
            isConfirmPasswordValid = false;
        } else {
            confirmPassword.setError(null);
            isConfirmPasswordValid = true;
        }
        updateRegisterButtonState();
    }

    private void validatePhone(String phoneStr) {
        if (!phoneStr.isEmpty() && !android.util.Patterns.PHONE.matcher(phoneStr).matches()) {
            phone.setError("Enter a valid phone number");
            isPhoneValid = false;
        } else {
            phone.setError(null);
            isPhoneValid = true;
        }
        updateRegisterButtonState();
    }

    private void updateRegisterButtonState() {
        // Enable button only if all required fields are valid AND email is available
        boolean isFormValid = isNameValid && isEmailValid && isEmailAvailable &&
                isPasswordValid && isConfirmPasswordValid && isPhoneValid;
        registerBtn.setEnabled(isFormValid);
        registerBtn.setAlpha(isFormValid ? 1f : 0.6f);
    }

    private void registerUser() {
        // Final validation
        if (!isEmailAvailable) {
            email.setError("Email already registered");
            email.requestFocus();
            return;
        }

        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();
        String userConfirmPassword = confirmPassword.getText().toString().trim();
        String userName = name.getText().toString().trim();
        String userPhone = phone.getText().toString().trim();

        // One more check for passwords match
        if (!userPassword.equals(userConfirmPassword)) {
            confirmPassword.setError("Passwords do not match");
            confirmPassword.requestFocus();
            return;
        }

        setLoadingState(true);

        // Create user with Firebase Auth
        auth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnSuccessListener(result -> {
                    String userId = result.getUser().getUid();
                    User user = new User(userName, userEmail, userPhone);

                    // Save additional user data to Firestore
                    firestore.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener(aVoid -> {
                                showSuccessAndNavigate();
                            })
                            .addOnFailureListener(e -> {
                                setLoadingState(false);
                                Toast.makeText(this, "Account created! Please log in.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    // Registration failed
                    setLoadingState(false);

                    // Update email availability based on error
                    if (e.getMessage().contains("email address is already in use")) {
                        email.setError("Email already registered");
                        isEmailAvailable = false;
                        email.requestFocus();
                        email.selectAll();
                    }
                    handleRegistrationError(e);
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            registerBtn.setEnabled(false);
            registerBtn.setAlpha(0.6f);

            disableFormInputs();
        } else {

            enableFormInputs();
            updateRegisterButtonState();
        }
    }

    private void disableFormInputs() {
        email.setEnabled(false);
        password.setEnabled(false);
        confirmPassword.setEnabled(false);
        name.setEnabled(false);
        phone.setEnabled(false);
        togglePassword.setEnabled(false);
        toggleConfirmPassword.setEnabled(false);

        email.setAlpha(0.7f);
        password.setAlpha(0.7f);
        confirmPassword.setAlpha(0.7f);
        name.setAlpha(0.7f);
        phone.setAlpha(0.7f);
    }

    private void enableFormInputs() {
        email.setEnabled(true);
        password.setEnabled(true);
        confirmPassword.setEnabled(true);
        name.setEnabled(true);
        phone.setEnabled(true);
        togglePassword.setEnabled(true);
        toggleConfirmPassword.setEnabled(true);

        // Restore full opacity
        email.setAlpha(1f);
        password.setAlpha(1f);
        confirmPassword.setAlpha(1f);
        name.setAlpha(1f);
        phone.setAlpha(1f);
    }

    private void showSuccessAndNavigate() {
        // Optional: Quick visual success feedback
        registerBtn.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.success_green)));

        // Navigate after brief delay for visual feedback
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }, 300);
    }

    private void handleRegistrationError(Exception e) {
        String errorMessage;

        if (e.getMessage().contains("email address is already in use")) {
            // Already handled in registerUser()
            return;
        } else if (e.getMessage().contains("invalid email") ||
                e.getMessage().contains("malformed")) {
            errorMessage = "Invalid email format";
        } else if (e.getMessage().contains("password is invalid")) {
            errorMessage = "Password must be at least 6 characters";
        } else if (e.getMessage().contains("network error") ||
                e.getMessage().contains("timeout") ||
                e.getMessage().contains("unreachable")) {
            errorMessage = "No internet connection. Please check your network.";
        } else if (e.getMessage().contains("too many requests")) {
            errorMessage = "Too many attempts. Please try again later.";
        } else {
            errorMessage = "Registration failed. Please try again.";
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler to prevent memory leaks
        if (emailCheckHandler != null) {
            emailCheckHandler.removeCallbacksAndMessages(null);
        }
    }
}