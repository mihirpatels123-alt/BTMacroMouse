package com.btmacromouse;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditButtonActivity extends AppCompatActivity {

    private EditText nameEdit, xEdit, yEdit;
    private ImageView colorPreview;
    private int selectedColor;
    private int position;

    private final int[] COLOR_OPTIONS = {
            Color.parseColor("#E53935"), // Red
            Color.parseColor("#1E88E5"), // Blue
            Color.parseColor("#43A047"), // Green
            Color.parseColor("#FB8C00"), // Orange
            Color.parseColor("#8E24AA"), // Purple
            Color.parseColor("#00ACC1"), // Cyan
            Color.parseColor("#F4511E"), // Deep Orange
            Color.parseColor("#FFB300"), // Amber
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_button);

        nameEdit = findViewById(R.id.edit_name);
        xEdit = findViewById(R.id.edit_x);
        yEdit = findViewById(R.id.edit_y);
        colorPreview = findViewById(R.id.color_preview);
        Button saveBtn = findViewById(R.id.save_button);
        Button cancelBtn = findViewById(R.id.cancel_button);

        position = getIntent().getIntExtra("position", -1);
        String name = getIntent().getStringExtra("name");
        int x = getIntent().getIntExtra("x", 960);
        int y = getIntent().getIntExtra("y", 540);
        selectedColor = getIntent().getIntExtra("color", Color.parseColor("#1E88E5"));

        nameEdit.setText(name);
        xEdit.setText(String.valueOf(x));
        yEdit.setText(String.valueOf(y));
        updateColorPreview();

        // Color picker row
        int[] colorViews = {
                R.id.color_1, R.id.color_2, R.id.color_3, R.id.color_4,
                R.id.color_5, R.id.color_6, R.id.color_7, R.id.color_8
        };

        for (int i = 0; i < colorViews.length; i++) {
            ImageView cv = findViewById(colorViews[i]);
            final int color = COLOR_OPTIONS[i];
            cv.setBackgroundColor(color);
            cv.setOnClickListener(v -> {
                selectedColor = color;
                updateColorPreview();
            });
        }

        saveBtn.setOnClickListener(v -> {
            String btnName = nameEdit.getText().toString().trim();
            String xStr = xEdit.getText().toString().trim();
            String yStr = yEdit.getText().toString().trim();

            if (btnName.isEmpty() || xStr.isEmpty() || yStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int bx, by;
            try {
                bx = Integer.parseInt(xStr);
                by = Integer.parseInt(yStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "X and Y must be numbers", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent result = new Intent();
            result.putExtra("position", position);
            result.putExtra("name", btnName);
            result.putExtra("x", bx);
            result.putExtra("y", by);
            result.putExtra("color", selectedColor);
            setResult(RESULT_OK, result);
            finish();
        });

        cancelBtn.setOnClickListener(v -> finish());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(position == -1 ? "Add Button" : "Edit Button");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void updateColorPreview() {
        colorPreview.setBackgroundColor(selectedColor);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
