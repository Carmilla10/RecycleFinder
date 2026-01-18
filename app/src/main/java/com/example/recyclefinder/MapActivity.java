/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient client;
    private LatLng currentLatLng;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 44;
    private static final String API_KEY = "AIzaSyDsdn8TUvxBUWCA3s0cDiut0DVcwk6_prE"; // Your key
    private java.util.List<LatLng> markerPositions = new java.util.ArrayList<>();
    private java.util.List<Marker> recyclingMarkers = new java.util.ArrayList<>(); // Store recycling center markers
    private EditText searchEditText;
    private ImageButton searchButton;
    private RecyclerView suggestionsRecyclerView;
    private ArrayList<PlaceSuggestion> suggestionsList;
    private PlaceSuggestionAdapter suggestionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map); // Make sure this is your map layout

        // Get map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize location client
        client = LocationServices.getFusedLocationProviderClient(this);

        // Initialize search views
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        suggestionsRecyclerView = findViewById(R.id.suggestionsRecyclerView);

        // Initialize suggestions list and adapter
        suggestionsList = new ArrayList<>();
        suggestionsAdapter = new PlaceSuggestionAdapter(suggestionsList, this::onPlaceSelected);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecyclerView.setAdapter(suggestionsAdapter);

        // Setup search button click listener
        searchButton.setOnClickListener(v -> performSearch());

        // Setup search on Enter/Go key press
        searchEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                performSearch();
                return true;
            }
            return false;
        });

        // Setup autocomplete as user types
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() > 2) {
                    fetchAutocompleteSuggestions(query);
                } else {
                    hideSuggestions();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Hide suggestions when search loses focus
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // Delay hiding to allow clicking on suggestions
                searchEditText.postDelayed(() -> hideSuggestions(), 200);
            }
        });

        // Check and request location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted
            getCurrentLocation();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable location on map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }

        // Set marker click listener to show info window (first tap shows name)
        mMap.setOnMarkerClickListener(marker -> {
            String placeId = (String) marker.getTag();
            Log.d("MapActivity", "Marker clicked - Tag: " + placeId);
            
            if (placeId != null && !placeId.equals("user_location")) {
                // Show info window with place name (first tap)
                marker.showInfoWindow();
                return true; // Consume the event
            }
            
            Log.d("MapActivity", "Marker click ignored - user location or null tag");
            return false;
        });
        
        // Set info window click listener to open place details (second tap on info window)
        mMap.setOnInfoWindowClickListener(marker -> {
            String placeId = (String) marker.getTag();
            if (placeId != null && !placeId.equals("user_location")) {
                Log.d("MapActivity", "Info window clicked - Opening PlaceDetailActivity with place_id: " + placeId);
                Intent intent = new Intent(MapActivity.this, PlaceDetailActivity.class);
                intent.putExtra("place_id", placeId);
                startActivity(intent);
            }
        });
        
        // Add zoom level listener to show/hide place names based on zoom
        mMap.setOnCameraMoveListener(() -> {
            float zoomLevel = mMap.getCameraPosition().zoom;
            // Show info windows when zoomed in (zoom level >= 14)
            if (zoomLevel >= 14) {
                for (Marker marker : recyclingMarkers) {
                    if (marker != null) {
                        marker.showInfoWindow();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                getCurrentLocation();
                // Auto-search will be triggered when location is obtained
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        client.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                // Use wider zoom level (11 instead of 14) to show more area
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 11));
                Marker userMarker = mMap.addMarker(new MarkerOptions()
                        .position(currentLatLng)
                        .title("📍 You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                if (userMarker != null) {
                    userMarker.setTag("user_location"); // Tag to identify user location marker
                }
                
                // Automatically search for recycling centers when location is ready
                searchRecyclingCenters();
            }
        });
    }

    private void searchRecyclingCenters() {
        if (currentLatLng == null) {
            Toast.makeText(this, "Getting location first...", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }

        Toast.makeText(this, "Searching recycling centers...", Toast.LENGTH_SHORT).show();

        // Clear existing markers and positions
        mMap.clear();
        markerPositions.clear();
        recyclingMarkers.clear(); // Clear stored markers
        
        // Re-add user location marker and add to positions list
        if (currentLatLng != null) {
            Marker userMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("📍 You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            if (userMarker != null) {
                userMarker.setTag("user_location"); // Tag to identify user location marker
            }
            markerPositions.add(currentLatLng); // Add user location to bounds
        }

        // Try multiple search strategies
        // Strategy 1: Use type parameter for recycling centers
        String url1 = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + currentLatLng.latitude + "," + currentLatLng.longitude +
                "&radius=10000" +
                "&type=establishment" +
                "&keyword=recycling" +
                "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);
        
        // First search attempt
        JsonObjectRequest request1 = new JsonObjectRequest(
                Request.Method.GET, url1, null,
                response -> {
                    try {
                        String status = response.getString("status");
                        Log.d("MapActivity", "API Response Status: " + status);
                        Log.d("MapActivity", "API Response: " + response.toString());

                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            String errorMsg = "API Error: " + status;
                            if (response.has("error_message")) {
                                errorMsg += " - " + response.getString("error_message");
                            }
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                            Log.e("MapActivity", errorMsg);
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        int foundCount = 0;

                        if (results.length() > 0) {
                            for (int i = 0; i < Math.min(results.length(), 20); i++) {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject location = place.getJSONObject("geometry")
                                        .getJSONObject("location");
                                String name = place.getString("name");
                                String placeId = place.getString("place_id");

                                LatLng latLng = new LatLng(
                                        location.getDouble("lat"),
                                        location.getDouble("lng")
                                );

                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(latLng)
                                        .title(name)
                                        .snippet("Recycling Center")
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED))); // Changed to RED
                                if (marker != null) {
                                    marker.setTag(placeId); // Store place_id for marker click
                                }
                                markerPositions.add(latLng); // Add to positions list for bounds
                                foundCount++;
                            }

                            // Fit camera to show all markers
                            fitMapToMarkers();

                            Toast.makeText(this,
                                    "Found " + foundCount + " recycling centers",
                                    Toast.LENGTH_SHORT).show();

                        } else {
                            // If first search found nothing, try second search
                            Log.d("MapActivity", "First search returned no results, trying broader search...");
                            performSecondSearch(queue);
                        }

                    } catch (JSONException e) {
                        Log.e("MapActivity", "Error parsing JSON: " + e.getMessage(), e);
                        Toast.makeText(this, "Error reading results: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e("MapActivity", "Volley error: " + error.getMessage(), error);
                    if (error.networkResponse != null) {
                        Log.e("MapActivity", "Network response code: " + error.networkResponse.statusCode);
                        Log.e("MapActivity", "Network response data: " + new String(error.networkResponse.data));
                    }
                    Toast.makeText(this, "Search failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        );

        queue.add(request1);
    }

    private void performSecondSearch(RequestQueue queue) {
        String url2 = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + currentLatLng.latitude + "," + currentLatLng.longitude +
                "&radius=10000" +
                "&keyword=recycling" +
                "&key=" + API_KEY;

        JsonObjectRequest request2 = new JsonObjectRequest(
                Request.Method.GET, url2, null,
                response -> {
                    try {
                        String status = response.getString("status");
                        Log.d("MapActivity", "Second search status: " + status);

                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            String errorMsg = "API Error: " + status;
                            if (response.has("error_message")) {
                                errorMsg += " - " + response.getString("error_message");
                            }
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        int foundCount = 0;

                        if (results.length() > 0) {
                            for (int i = 0; i < Math.min(results.length(), 20); i++) {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject location = place.getJSONObject("geometry")
                                        .getJSONObject("location");
                                String name = place.getString("name");
                                String placeId = place.getString("place_id");

                                LatLng latLng = new LatLng(
                                        location.getDouble("lat"),
                                        location.getDouble("lng")
                                );

                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(latLng)
                                        .title(name)
                                        .snippet("Recycling Center")
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED))); // Changed to RED
                                if (marker != null) {
                                    marker.setTag(placeId); // Store place_id for marker click
                                }
                                markerPositions.add(latLng); // Add to positions list for bounds
                                foundCount++;
                            }

                            // Fit camera to show all markers
                            fitMapToMarkers();

                            Toast.makeText(this,
                                    "Found " + foundCount + " recycling centers",
                                    Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(this,
                                    "No recycling centers found nearby. Try a different location or check your internet connection.",
                                    Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {
                        Log.e("MapActivity", "Error parsing second search JSON: " + e.getMessage(), e);
                        Toast.makeText(this, "Error reading results", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("MapActivity", "Second search error: " + error.getMessage(), error);
                    Toast.makeText(this, "No recycling centers found nearby", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request2);
    }

    private void fitMapToMarkers() {
        if (markerPositions.isEmpty()) {
            return;
        }

        // Build bounds to include all markers
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng position : markerPositions) {
            builder.include(position);
        }
        LatLngBounds bounds = builder.build();

        // Animate camera to fit all markers with padding (in pixels)
        // 100dp padding on all sides so markers aren't at the edge
        int padding = (int) (100 * getResources().getDisplayMetrics().density);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    private void performSearch() {
        String query = searchEditText.getText().toString().trim();
        
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide keyboard
        searchEditText.clearFocus();
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
            getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }

        Toast.makeText(this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
        searchPlaces(query);
    }

    private void searchPlaces(String query) {
        // Use current location if available, otherwise use a default location
        LatLng searchLocation = currentLatLng;
        if (searchLocation == null) {
            // Default to a central location (you can change this)
            searchLocation = new LatLng(3.1390, 101.6869); // Kuala Lumpur as default
            Toast.makeText(this, "Using default location for search", Toast.LENGTH_SHORT).show();
        }

        // Clear existing markers except user location
        mMap.clear();
        markerPositions.clear();
        
        // Re-add user location marker if available
        if (currentLatLng != null) {
            Marker userMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("📍 You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            if (userMarker != null) {
                userMarker.setTag("user_location");
            }
            markerPositions.add(currentLatLng);
        }

        // Use Google Places Text Search API
        RequestQueue queue = Volley.newRequestQueue(this);
        
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                    "?query=" + encodedQuery +
                    "&location=" + searchLocation.latitude + "," + searchLocation.longitude +
                    "&radius=50000" + // 50km radius
                    "&key=" + API_KEY;

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        String status = response.getString("status");
                        Log.d("MapActivity", "Search API Response Status: " + status);

                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            String errorMsg = "Search Error: " + status;
                            if (response.has("error_message")) {
                                errorMsg += " - " + response.getString("error_message");
                            }
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                            Log.e("MapActivity", errorMsg);
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        int foundCount = 0;

                        if (results.length() > 0) {
                            for (int i = 0; i < Math.min(results.length(), 20); i++) {
                                JSONObject place = results.getJSONObject(i);
                                JSONObject location = place.getJSONObject("geometry")
                                        .getJSONObject("location");
                                String name = place.getString("name");
                                String placeId = place.getString("place_id");

                                LatLng latLng = new LatLng(
                                        location.getDouble("lat"),
                                        location.getDouble("lng")
                                );

                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(latLng)
                                        .title(name)
                                        .snippet(place.optString("formatted_address", ""))
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED)));
                                if (marker != null) {
                                    marker.setTag(placeId);
                                }
                                markerPositions.add(latLng);
                                foundCount++;
                            }

                            // Fit camera to show all markers
                            fitMapToMarkers();

                            Toast.makeText(this,
                                    "Found " + foundCount + " places",
                                    Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(this,
                                    "No places found for: " + query,
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Log.e("MapActivity", "Error parsing search JSON: " + e.getMessage(), e);
                        Toast.makeText(this, "Error reading search results", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("MapActivity", "Search error: " + error.getMessage(), error);
                    Toast.makeText(this, "Search failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            );

            queue.add(request);
        } catch (java.io.UnsupportedEncodingException e) {
            Toast.makeText(this, "Error encoding search query", Toast.LENGTH_SHORT).show();
            Log.e("MapActivity", "Encoding error: " + e.getMessage());
        }
    }

    // Place Suggestion class
    private static class PlaceSuggestion {
        String placeId;
        String name;
        String address;

        PlaceSuggestion(String placeId, String name, String address) {
            this.placeId = placeId;
            this.name = name;
            this.address = address;
        }
    }

    // Adapter for suggestions RecyclerView
    private static class PlaceSuggestionAdapter extends RecyclerView.Adapter<PlaceSuggestionAdapter.ViewHolder> {
        private ArrayList<PlaceSuggestion> suggestions;
        private OnPlaceSelectedListener listener;

        interface OnPlaceSelectedListener {
            void onPlaceSelected(PlaceSuggestion suggestion);
        }

        PlaceSuggestionAdapter(ArrayList<PlaceSuggestion> suggestions, OnPlaceSelectedListener listener) {
            this.suggestions = suggestions;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_place_suggestion, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PlaceSuggestion suggestion = suggestions.get(position);
            holder.textViewName.setText(suggestion.name);
            holder.textViewAddress.setText(suggestion.address);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceSelected(suggestion);
                }
            });
        }

        @Override
        public int getItemCount() {
            return suggestions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewName;
            TextView textViewAddress;

            ViewHolder(View itemView) {
                super(itemView);
                textViewName = itemView.findViewById(R.id.textViewPlaceName);
                textViewAddress = itemView.findViewById(R.id.textViewPlaceAddress);
            }
        }
    }

    private void fetchAutocompleteSuggestions(String query) {
        LatLng searchLocation = currentLatLng;
        if (searchLocation == null) {
            searchLocation = new LatLng(3.1390, 101.6869); // Default location
        }

        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            String url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                    "?input=" + encodedQuery +
                    "&location=" + searchLocation.latitude + "," + searchLocation.longitude +
                    "&radius=50000" +
                    "&key=" + API_KEY;

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray predictions = response.getJSONArray("predictions");
                            suggestionsList.clear();

                            for (int i = 0; i < Math.min(predictions.length(), 5); i++) {
                                JSONObject prediction = predictions.getJSONObject(i);
                                String placeId = prediction.getString("place_id");
                                String description = prediction.getString("description");
                                
                                // Split description into name and address
                                String[] parts = description.split(",");
                                String name = parts[0].trim();
                                String address = description.substring(name.length()).trim();
                                if (address.startsWith(",")) {
                                    address = address.substring(1).trim();
                                }

                                suggestionsList.add(new PlaceSuggestion(placeId, name, address));
                            }

                            suggestionsAdapter.notifyDataSetChanged();
                            showSuggestions();

                        } catch (JSONException e) {
                            Log.e("MapActivity", "Error parsing autocomplete: " + e.getMessage());
                            hideSuggestions();
                        }
                    },
                    error -> {
                        Log.e("MapActivity", "Autocomplete error: " + error.getMessage());
                        hideSuggestions();
                    }
            );

            queue.add(request);
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e("MapActivity", "Encoding error: " + e.getMessage());
        }
    }

    private void onPlaceSelected(PlaceSuggestion suggestion) {
        // Hide suggestions
        hideSuggestions();
        
        // Update search text
        searchEditText.setText(suggestion.name);
        searchEditText.clearFocus();
        
        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
            getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }

        // Navigate to the selected place
        navigateToPlace(suggestion.placeId, suggestion.name);
    }

    private void navigateToPlace(String placeId, String placeName) {
        Toast.makeText(this, "Loading " + placeName + "...", Toast.LENGTH_SHORT).show();

        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + placeId +
                "&fields=geometry,name,formatted_address" +
                "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject result = response.getJSONObject("result");
                        JSONObject geometry = result.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");
                        
                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");
                        LatLng placeLocation = new LatLng(lat, lng);

                        // Clear existing markers
                        mMap.clear();
                        markerPositions.clear();

                        // Add user location if available
                        if (currentLatLng != null) {
                            Marker userMarker = mMap.addMarker(new MarkerOptions()
                                    .position(currentLatLng)
                                    .title("📍 You are here")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            if (userMarker != null) {
                                userMarker.setTag("user_location");
                            }
                        }

                        // Add marker for selected place
                        Marker placeMarker = mMap.addMarker(new MarkerOptions()
                                .position(placeLocation)
                                .title(placeName)
                                .snippet(result.optString("formatted_address", ""))
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                        if (placeMarker != null) {
                            placeMarker.setTag(placeId);
                        }

                        // Navigate camera to the place
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(placeLocation, 15));

                        Toast.makeText(this, "Showing " + placeName, Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        Log.e("MapActivity", "Error parsing place details: " + e.getMessage());
                        Toast.makeText(this, "Error loading place details", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("MapActivity", "Place details error: " + error.getMessage());
                    Toast.makeText(this, "Error loading place", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

    private void showSuggestions() {
        if (suggestionsList.size() > 0) {
            suggestionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void hideSuggestions() {
        suggestionsRecyclerView.setVisibility(View.GONE);
    }
}