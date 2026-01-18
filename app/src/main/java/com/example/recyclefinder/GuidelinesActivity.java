/*
 * © 2026 RecycleFinder. All Rights Reserved.
 */

package com.example.recyclefinder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GuidelinesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GuidelinesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guidelines);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create guidelines data with images (using your JPG file names)
        List<GuidelineItem> guidelines = createGuidelines();
        adapter = new GuidelinesAdapter(guidelines);
        recyclerView.setAdapter(adapter);
    }

    private List<GuidelineItem> createGuidelines() {
        List<GuidelineItem> items = new ArrayList<>();

        // 1. PLASTIC
        items.add(new GuidelineItem(
                R.drawable.plastic,
                "♻️ Plastic",
                "What can be recycled:",
                "• Plastic bottles & containers\n• Plastic bags\n• Plastic food containers\n\nHow to prepare:\n• Rinse thoroughly\n• Remove caps and lids\n• Flatten to save space\n\nDon't recycle:\n✗ Plastic film/wrap\n✗ Plastic utensils\n✗ Styrofoam"
        ));

        // 2. PAPER
        items.add(new GuidelineItem(
                R.drawable.paper,
                "📄 Paper & Cardboard",
                "What can be recycled:",
                "• Newspapers & magazines\n• Cardboard boxes\n• Office paper\n• Paper bags\n\nHow to prepare:\n• Remove plastic windows\n• Flatten boxes\n• Keep dry\n\nDon't recycle:\n✗ Paper with food stains\n✗ Wax-coated paper\n✗ Paper towels"
        ));

        // 3. GLASS
        items.add(new GuidelineItem(
                R.drawable.glass,
                "🍾 Glass",
                "What can be recycled:",
                "• Glass bottles\n• Glass jars\n• Glass containers\n\nHow to prepare:\n• Rinse thoroughly\n• Remove caps/lids\n• Keep intact (no broken pieces)\n\nDon't recycle:\n✗ Broken glass\n✗ Light bulbs\n✗ Ceramics\n✗ Mirrors"
        ));

        // 4. METAL
        items.add(new GuidelineItem(
                R.drawable.metal,
                "🥫 Metal & Aluminum",
                "What can be recycled:",
                "• Aluminum cans\n• Metal cans\n• Aluminum foil\n• Metal jars\n\nHow to prepare:\n• Rinse thoroughly\n• Remove labels (optional)\n• Crush to save space\n\nDon't recycle:\n✗ Paint cans\n✗ Batteries\n✗ Hazardous containers"
        ));

        // 5. E-WASTE
        items.add(new GuidelineItem(
                R.drawable.ewaste,
                "📱 Electronics (e-Waste)",
                "What can be recycled:",
                "• Mobile phones\n• Computers & laptops\n• Tablets & monitors\n• TVs & cables\n\nHow to prepare:\n• Data wipe (important!)\n• Remove batteries\n• Keep cords together\n\nTip:\n→ Many retailers accept old electronics\n→ Contact local e-waste centers"
        ));

        return items;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // Model class for guidelines
    public static class GuidelineItem {
        public int iconResource;
        public String category;
        public String subtitle;
        public String content;

        public GuidelineItem(int iconResource, String category, String subtitle, String content) {
            this.iconResource = iconResource;
            this.category = category;
            this.subtitle = subtitle;
            this.content = content;
        }
    }

    // Adapter with ImageView
    public static class GuidelinesAdapter extends RecyclerView.Adapter<GuidelinesAdapter.GuidelineViewHolder> {
        private List<GuidelineItem> items;

        public GuidelinesAdapter(List<GuidelineItem> items) {
            this.items = items;
        }

        @Override
        public GuidelineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_guideline, parent, false);
            return new GuidelineViewHolder(view);
        }

        @Override
        public void onBindViewHolder(GuidelineViewHolder holder, int position) {
            GuidelineItem item = items.get(position);

            // Set image from your JPG file
            holder.categoryIcon.setImageResource(item.iconResource);

            // Set text
            holder.categoryTitle.setText(item.category);
            holder.categorySubtitle.setText(item.subtitle);
            holder.categoryContent.setText(item.content);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class GuidelineViewHolder extends RecyclerView.ViewHolder {
            ImageView categoryIcon;
            TextView categoryTitle;
            TextView categorySubtitle;
            TextView categoryContent;

            public GuidelineViewHolder(View itemView) {
                super(itemView);
                categoryIcon = itemView.findViewById(R.id.categoryIcon);
                categoryTitle = itemView.findViewById(R.id.categoryTitle);
                categorySubtitle = itemView.findViewById(R.id.categorySubtitle);
                categoryContent = itemView.findViewById(R.id.categoryContent);
            }
        }
    }
}