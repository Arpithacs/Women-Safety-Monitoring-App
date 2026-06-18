package com.example.safety_monitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private boolean isMonitoring = false;
    private TextView tvStatus;
    private View statusDot;
    private Button btnStartStop;
    private LinearLayout btnSOS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Check Permissions on Startup
        checkAndRequestPermissions();

        // 2. Connect UI Elements
        tvStatus = findViewById(R.id.tvStatus);
        statusDot = findViewById(R.id.statusDot);
        btnStartStop = findViewById(R.id.btnStartService);
        btnSOS = findViewById(R.id.btnSOS);

        // 3. SOS Button Logic (The Big Red Circle)
        btnSOS.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                Toast.makeText(MainActivity.this, "SENDING EMERGENCY ALERT...", Toast.LENGTH_LONG).show();
                // Trigger the service to send SMS immediately
                Intent serviceIntent = new Intent(MainActivity.this, SafetyService.class);
                serviceIntent.setAction("ACTION_NOT_SAFE"); // Custom action we defined earlier
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                Toast.makeText(MainActivity.this, "Permissions needed for SOS!", Toast.LENGTH_SHORT).show();
                checkAndRequestPermissions();
            }
        });

        // 4. Start/Stop Protection Button Logic
        btnStartStop.setOnClickListener(v -> {
            if (!isMonitoring) {
                // STARTING
                if (checkSetupAndPermissions()) {
                    startSafetyService();
                    updateUI(true);
                }
            } else {
                // STOPPING
                stopSafetyService();
                updateUI(false);
            }
        });
    }

    // --- UI HELPER: Update Colors & Text ---
    private void updateUI(boolean active) {
        isMonitoring = active;

        if (active) {
            tvStatus.setText("You're Protected");
            tvStatus.setTextColor(Color.parseColor("#166534")); // Dark Green
            btnStartStop.setText("Stop Protection");

            // Change Dot to Green
            Drawable background = statusDot.getBackground();
            DrawableCompat.setTint(background, Color.parseColor("#22C55E")); // Bright Green
        } else {
            tvStatus.setText("Not Protected");
            tvStatus.setTextColor(Color.parseColor("#4B5563")); // Gray
            btnStartStop.setText("Start Protection");

            // Change Dot to Gray
            Drawable background = statusDot.getBackground();
            DrawableCompat.setTint(background, Color.parseColor("#9CA3AF")); // Gray
        }
    }

    // --- SERVICE HELPERS ---
    private void startSafetyService() {
        Intent serviceIntent = new Intent(this, SafetyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "Protection Activated", Toast.LENGTH_SHORT).show();
    }

    private void stopSafetyService() {
        Intent serviceIntent = new Intent(this, SafetyService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }

    // --- PERMISSION & SETUP CHECKS ---
    private boolean checkSetupAndPermissions() {
        // 1. Check if Phone Number is set
        SharedPreferences prefs = getSharedPreferences("SafetyPrefs", MODE_PRIVATE);
        String phone = prefs.getString("emergency_phone", "");

        if (phone.isEmpty()) {
            Toast.makeText(this, "Please set an Emergency Contact first!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            return false;
        }

        // 2. Check Android Permissions
        if (!hasAllPermissions()) {
            Toast.makeText(this, "Permissions required!", Toast.LENGTH_SHORT).show();
            checkAndRequestPermissions();
            return false;
        }

        return true;
    }

    private boolean hasAllPermissions() {
        boolean location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean sms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        return location && audio && sms;
    }

    private void checkAndRequestPermissions() {
        if (!hasAllPermissions()) {
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.POST_NOTIFICATIONS
                };
            } else {
                permissions = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECORD_AUDIO
                };
            }
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }
}