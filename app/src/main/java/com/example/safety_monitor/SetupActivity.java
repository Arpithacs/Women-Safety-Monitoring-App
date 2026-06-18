package com.example.safety_monitor;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SetupActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText etName, etBloodType, etContactName, etContactPhone;
    private TextView tvPermLocation, tvPermMic, tvPermSMS;
    private Button btnGrantPermissions, btnSave;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        etName           = findViewById(R.id.etSetupName);
        etBloodType      = findViewById(R.id.etSetupBloodType);
        etContactName    = findViewById(R.id.etContactName);
        etContactPhone   = findViewById(R.id.etSetupPhone);
        tvPermLocation   = findViewById(R.id.tvPermLocation);
        tvPermMic        = findViewById(R.id.tvPermMic);
        tvPermSMS        = findViewById(R.id.tvPermSMS);
        btnGrantPermissions = findViewById(R.id.btnGrantPermissions);
        btnSave          = findViewById(R.id.btnSaveSetup);

        // Pre-fill name from Google account if available
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            etName.setText(user.getDisplayName());
        }

        // Load existing prefs if editing
        SharedPreferences prefs = getSharedPreferences("SafetyPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("user_name", "");
        String savedBlood = prefs.getString("blood_type", "");
        String savedContactName = prefs.getString("contact_name", "");
        String savedPhone = prefs.getString("emergency_phone", "");
        if (!savedName.isEmpty()) etName.setText(savedName);
        if (!savedBlood.isEmpty()) etBloodType.setText(savedBlood);
        if (!savedContactName.isEmpty()) etContactName.setText(savedContactName);
        if (!savedPhone.isEmpty()) etContactPhone.setText(savedPhone);

        updatePermissionStatuses();

        btnGrantPermissions.setOnClickListener(v -> requestAllPermissions());

        btnSave.setOnClickListener(v -> saveAndContinue());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatuses();
    }

    private void updatePermissionStatuses() {
        tvPermLocation.setText(hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                ? "✓ Granted" : "Needed");
        tvPermLocation.setTextColor(hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                ? 0xFF16A34A : 0xFFEF4444);

        tvPermMic.setText(hasPermission(Manifest.permission.RECORD_AUDIO)
                ? "✓ Granted" : "Needed");
        tvPermMic.setTextColor(hasPermission(Manifest.permission.RECORD_AUDIO)
                ? 0xFF16A34A : 0xFFEF4444);

        tvPermSMS.setText(hasPermission(Manifest.permission.SEND_SMS)
                ? "✓ Granted" : "Needed");
        tvPermSMS.setTextColor(hasPermission(Manifest.permission.SEND_SMS)
                ? 0xFF16A34A : 0xFFEF4444);
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updatePermissionStatuses();
        }
    }

    private void saveAndContinue() {
        String name = etName.getText().toString().trim();
        String blood = etBloodType.getText().toString().trim();
        String contactName = etContactName.getText().toString().trim();
        String phone = etContactPhone.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Please enter your name");
            etName.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            etContactPhone.setError("Please enter an emergency contact number");
            etContactPhone.requestFocus();
            return;
        }
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                || !hasPermission(Manifest.permission.RECORD_AUDIO)
                || !hasPermission(Manifest.permission.SEND_SMS)) {
            Toast.makeText(this, "Please grant all permissions first", Toast.LENGTH_SHORT).show();
            requestAllPermissions();
            return;
        }

        // Save to SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences("SafetyPrefs", MODE_PRIVATE).edit();
        editor.putString("user_name", name);
        editor.putString("blood_type", blood);
        editor.putString("contact_name", contactName);
        editor.putString("emergency_phone", phone);
        editor.putBoolean("setup_complete", true);
        editor.apply();

        Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();

        // Go to home
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
