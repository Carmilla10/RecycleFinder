/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

public class PlaceDetailActivity extends AppCompatActivity {

    private static final String API_KEY = "AIzaSyDsdn8TUvxBUWCA3s0cDiut0DVcwk6_prE";

    private TextView textViewName, textViewAddress, textViewPhone, textViewRating, textViewStatus;
    private ImageView imageView;
    private Button btnCall, btnDirections;

    private String placeId;
    private String phoneNumber;
    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        // Initialize views
        textViewName = findViewById(R.id.textViewName);
        textViewAddress = findViewById(R.id.textViewAddress);
        textViewPhone = findViewById(R.id.textViewPhone);
        textViewRating = findViewById(R.id.textViewRating);
        textViewStatus = findViewById(R.id.textViewStatus);
        imageView = findViewById(R.id.imageView);
        btnCall = findViewById(R.id.btnCall);
        btnDirections = findViewById(R.id.btnDirections);

        // Get place ID from intent
        Intent intent = getIntent();
        placeId = intent.getStringExtra("place_id");

        if (placeId != null) {
            fetchPlaceDetails(placeId);
        }

        // Set up button click listeners
        btnCall.setOnClickListener(v -> callPlace());
        btnDirections.setOnClickListener(v -> navigateToPlace());

        // Make phone number clickable
        textViewPhone.setOnClickListener(v -> callPlace());

        // Make address clickable for navigation
        textViewAddress.setOnClickListener(v -> navigateToPlace());
    }

    private void fetchPlaceDetails(String placeId) {
        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + placeId +
                "&fields=name,formatted_address,formatted_phone_number,rating,opening_hours,photo,geometry" +
                "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject result = response.getJSONObject("result");

                        // Extract and display data
                        textViewName.setText(result.getString("name"));
                        
                        // Set address and make it clickable
                        String address = result.optString("formatted_address", "N/A");
                        if (!address.equals("N/A")) {
                            SpannableString addressSpan = new SpannableString("📍 " + address);
                            addressSpan.setSpan(new ForegroundColorSpan(0xFF2196F3), 0, addressSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            addressSpan.setSpan(new UnderlineSpan(), 0, addressSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            textViewAddress.setText(addressSpan);
                            textViewAddress.setClickable(true);
                        } else {
                            textViewAddress.setText("📍 Address: N/A");
                            textViewAddress.setClickable(false);
                        }
                        
                        // Set phone and make it clickable
                        phoneNumber = result.optString("formatted_phone_number", "N/A");
                        if (!phoneNumber.equals("N/A")) {
                            SpannableString phoneSpan = new SpannableString("📞 " + phoneNumber);
                            phoneSpan.setSpan(new ForegroundColorSpan(0xFF4CAF50), 0, phoneSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            phoneSpan.setSpan(new UnderlineSpan(), 0, phoneSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            textViewPhone.setText(phoneSpan);
                            textViewPhone.setClickable(true);
                        } else {
                            textViewPhone.setText("📞 Phone: N/A");
                            textViewPhone.setClickable(false);
                            phoneNumber = null;
                        }

                        if (result.has("rating")) {
                            textViewRating.setText("Rating: " + result.getDouble("rating") + "/5");
                        } else {
                            textViewRating.setText("Rating: N/A");
                        }

                        if (result.has("opening_hours")) {
                            JSONObject openingHours = result.getJSONObject("opening_hours");
                            boolean isOpen = openingHours.getBoolean("open_now");
                            textViewStatus.setText(isOpen ? "Open Now" : "Closed");
                            textViewStatus.setTextColor(isOpen ?
                                    0xFF2E7D32 : // Green color
                                    0xFFD32F2F); // Red color
                        }

                        // Extract latitude and longitude for directions
                        if (result.has("geometry")) {
                            JSONObject geometry = result.getJSONObject("geometry");
                            if (geometry.has("location")) {
                                JSONObject location = geometry.getJSONObject("location");
                                latitude = location.getDouble("lat");
                                longitude = location.getDouble("lng");
                            }
                        }

                        // Fetch photo if available
                        if (result.has("photos")) {
                            String photoReference = result.getJSONArray("photos")
                                    .getJSONObject(0)
                                    .getString("photo_reference");
                            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                                    "?maxwidth=400" +
                                    "&photo_reference=" + photoReference +
                                    "&key=" + API_KEY;

                            Glide.with(this)
                                    .load(photoUrl)
                                    .into(imageView);
                        }

                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing place details", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Failed to load place details", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }

    private void callPlace() {
        if (phoneNumber != null && !phoneNumber.equals("N/A") && !phoneNumber.isEmpty()) {
            // Remove any non-digit characters except + for international numbers
            String cleanPhone = phoneNumber.replaceAll("[^+\\d]", "");
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + cleanPhone));
            startActivity(callIntent);
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToPlace() {
        if (latitude != 0.0 && longitude != 0.0) {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            
            // If Google Maps is not available, try with geo URI
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback to geo URI
                Uri geoUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude);
                Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
                startActivity(geoIntent);
            }
        } else {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
        }
    }
}