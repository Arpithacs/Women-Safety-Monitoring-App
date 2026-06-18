package com.example.safety_monitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ContactsActivity extends AppCompatActivity {

    private EditText etContactName, etContactPhone;
    private TextView tvContactName, tvContactPhone, tvContactInitial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        etContactName   = findViewById(R.id.etContactName);
        etContactPhone  = findViewById(R.id.etContactPhone);
        tvContactName   = findViewById(R.id.tvContactName);
        tvContactPhone  = findViewById(R.id.tvContactPhone);
        tvContactInitial = findViewById(R.id.tvContactInitial);

        loadCurrentContact();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Save contact
        findViewById(R.id.btnSaveContact).setOnClickListener(v -> saveContact());

        // Bottom navigation
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navAlerts).setOnClickListener(v -> {
            startActivity(new Intent(this, AlertsActivity.class));
            finish();
        });
        findViewById(R.id.navContacts).setOnClickListener(v -> {
            // Already here
        });
    }

    private void loadCurrentContact() {
        SharedPreferences prefs = getSharedPreferences("SafetyPrefs", MODE_PRIVATE);
        String name = prefs.getString("contact_name", "");
        String phone = prefs.getString("emergency_phone", "");

        if (!name.isEmpty()) {
            tvContactName.setText(name);
            tvContactInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            etContactName.setText(name);
        }
        if (!phone.isEmpty()) {
            tvContactPhone.setText(phone);
            etContactPhone.setText(phone);
        }
    }

    private void saveContact() {
        String name = etContactName.getText().toString().trim();
        String phone = etContactPhone.getText().toString().trim();

        if (name.isEmpty()) {
            etContactName.setError("Please enter a name");
            return;
        }
        if (phone.isEmpty()) {
            etContactPhone.setError("Please enter a phone number");
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences("SafetyPrefs", MODE_PRIVATE).edit();
        editor.putString("contact_name", name);
        editor.putString("emergency_phone", phone);
        editor.apply();

        // Update display
        tvContactName.setText(name);
        tvContactPhone.setText(phone);
        tvContactInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());

        Toast.makeText(this, "Contact saved!", Toast.LENGTH_SHORT).show();
    }
}
