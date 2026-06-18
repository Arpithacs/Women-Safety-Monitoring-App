package com.example.safety_monitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Auto-Login Check
        SharedPreferences prefs = getSharedPreferences("SafetyPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            goToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        // 2. UI Setup
        EditText etName = findViewById(R.id.etName);
        EditText etBlood = findViewById(R.id.etBloodType);
        EditText etPhone = findViewById(R.id.etEmergencyContact);
        Button btnSave = findViewById(R.id.btnSaveProfile);

        // 3. Save Logic
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String blood = etBlood.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Name and Phone are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("user_name", name);
            editor.putString("user_blood", blood);
            editor.putString("emergency_phone", phone);
            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            goToDashboard();
        });
    }

    private void goToDashboard() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // <--- THIS IS IMPORTANT: Removes Login from back stack
    }
}