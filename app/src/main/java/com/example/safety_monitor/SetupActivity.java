package com.example.safety_monitor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SetupActivity extends AppCompatActivity {

    private EditText etName, etBloodType, etContactName, etSetupPhone;
    private TextView tvPermLocation, tvPermMic, tvPermSMS;
    private Button btnGrantPermissions, btnSave;
    private SharedPreferences sharedPreferences;

    private static final int PERMISSION_REQUEST_CODE = 101;
    private final String[] requiredPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        sharedPreferences = getSharedPreferences("SafeGuardPrefs", Context.MODE_PRIVATE);

        // Initialize Form Inputs
        etName = findViewById(R.id.etName);
        etBloodType = findViewById(R.id.etBloodType);
        etContactName = findViewById(R.id.etContactName);
        etSetupPhone = findViewById(R.id.etSetupPhone);

        // Initialize Permission Status Text Modifiers
        tvPermLocation = findViewById(R.id.tvPermLocation);
        tvPermMic = findViewById(R.id.tvPermMic);
        tvPermSMS = findViewById(R.id.tvPermSMS);

        // Initialize Action Triggers
        btnGrantPermissions = findViewById(R.id.btnGrantPermissions);
        btnSave = findViewById(R.id.btnSave);

        // MISTAKE 1 FIX: Clean, accurate state evaluations on start
        updatePermissionStatusLabels();

        // MISTAKE 2 FIX: Trigger the official OS prompt window sequence
        btnGrantPermissions.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(SetupActivity.this, requiredPermissions, PERMISSION_REQUEST_CODE);
        });

        // 2. Crash-proof Data Persistence Pipeline
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String blood = etBloodType.getText().toString().trim();
            String contactName = etContactName.getText().toString().trim();
            String phone = etSetupPhone.getText().toString().trim();

            // Validate mandatory fields to avoid empty states
            if (name.isEmpty() || contactName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(SetupActivity.this, "Please fill in all mandatory fields!", Toast.LENGTH_LONG).show();
                return;
            }

            // Optional Security: Don't let them advance unless permissions are verified
            if (!areAllPermissionsGranted()) {
                Toast.makeText(this, "Please grant all required permissions before saving!", Toast.LENGTH_LONG).show();
                return;
            }

            // Save variables cleanly using uniform data mapping keys
            sharedPreferences.edit()
                    .putString("user_name", name)
                    .putString("blood_type", blood)
                    .putString("contact_name", contactName)
                    .putString("emergency_phone", phone)
                    .putBoolean("is_setup_complete", true) // Crucial verification flag
                    .apply();

            Toast.makeText(SetupActivity.this, "Setup finalized successfully!", Toast.LENGTH_SHORT).show();

            // MISTAKE 3 FIX: Clear the activity backstack completely so it doesn't bounce back or loop
            Intent intent = new Intent(SetupActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // MISTAKE 1 & 2 FIX: Added explicit else conditions so text toggles back to red "Needed" dynamically
    private void updatePermissionStatusLabels() {
        // Location Status Check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            tvPermLocation.setText("Granted");
            tvPermLocation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvPermLocation.setText("Needed");
            tvPermLocation.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }

        // Microphone Status Check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            tvPermMic.setText("Granted");
            tvPermMic.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvPermMic.setText("Needed");
            tvPermMic.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }

        // SMS Status Check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            tvPermSMS.setText("Granted");
            tvPermSMS.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvPermSMS.setText("Needed");
            tvPermSMS.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }

    private boolean areAllPermissionsGranted() {
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Capture the runtime user interaction feedback from the OS Dialog popups
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Update labels in real-time based on user selections
            updatePermissionStatusLabels();
            Toast.makeText(this, "Permissions updated successfully", Toast.LENGTH_SHORT).show();
        }
    }
}