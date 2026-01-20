/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

public class AboutDeveloperActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_developer);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("About Developer");
            }
        }

        // Initialize views
        ImageView imageViewDev1 = findViewById(R.id.imageViewDev1);
        ImageView imageViewDev2 = findViewById(R.id.imageViewDev2);
        ImageView imageViewDev3 = findViewById(R.id.imageViewDev3);
        ImageView imageViewDev4 = findViewById(R.id.imageViewDev4);

        TextView textViewName1 = findViewById(R.id.textViewName1);
        TextView textViewName2 = findViewById(R.id.textViewName2);
        TextView textViewName3 = findViewById(R.id.textViewName3);
        TextView textViewName4 = findViewById(R.id.textViewName4);

        TextView textViewStudentId1 = findViewById(R.id.textViewStudentId1);
        TextView textViewStudentId2 = findViewById(R.id.textViewStudentId2);
        TextView textViewStudentId3 = findViewById(R.id.textViewStudentId3);
        TextView textViewStudentId4 = findViewById(R.id.textViewStudentId4);

        // Developer 1
        textViewName1.setText("NUR CARMILLA BINTI ABDULLAH MUHAMMAD SHAFIQ");
        textViewStudentId1.setText("Student ID: 2023268272");

        // Developer 2
        textViewName2.setText("FATIN AQILAH BINTI MOHD ASRI ");
        textViewStudentId2.setText("Student ID: 2023674398");

        // Developer 3
        textViewName3.setText("WAN SYAHIRAH WAN KAMAL");
        textViewStudentId3.setText("Student ID: 2024530999");

        // Developer 4
        textViewName4.setText("FITRI JOHAN BIN SHAHRUL MAZLI");
        textViewStudentId4.setText("Student ID: 2023425852");

        imageViewDev1.setImageResource(R.drawable.carmilla);
        imageViewDev2.setImageResource(R.drawable.fatin);
        imageViewDev3.setImageResource(R.drawable.wansya);
        imageViewDev4.setImageResource(R.drawable.fitri);

        // Setup GitHub button
        Button btnGithub = findViewById(R.id.btnGithub);
        if (btnGithub != null) {
            btnGithub.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://github.com/Carmilla10/RecycleFinder"));
                startActivity(intent);
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
