package com.example.safety_monitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPreferences;
    private TextView tvStatus, tvContactIndicator;
    private Button btnToggleProtection;
    private View btnInstantSOS;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("SafeGuardPrefs", Context.MODE_PRIVATE);

        // Priority 2 (Step 6): Enforce Google Sign-In and Setup Validation Sequence
//        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
//            return;
//        } else
       if (!sharedPreferences.getBoolean("is_setup_complete", false)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        // Initialize Views with Safety Verifications
        tvStatus = findViewById(R.id.tvStatus);
        tvContactIndicator = findViewById(R.id.tvContactIndicator);
        btnToggleProtection = findViewById(R.id.btnToggleProtection);
        btnInstantSOS = findViewById(R.id.btnInstantSOS);

        // Priority 4 (Step 17): Live confirmation tracking of configured emergency contact metadata
        String cName = sharedPreferences.getString("contact_name", "");
        String cPhone = sharedPreferences.getString("emergency_phone", "");

//        if (tvContactIndicator != null) {
//            if (!cName.isEmpty() && !cPhone.isEmpty()) {
//                tvContactIndicator.setText("Protected Contact: " + cName + " (" + cPhone + ")");
//            } else {
//                tvContactIndicator.setText("⚠️ WARNING: No Emergency Contact Set up!");
//            }
//        } else {
//            Log.e(TAG, "CRITICAL ERROR: tvContactIndicator is missing from activity_main.xml");
//        }

        // Priority 2 (Step 5): Mapping Bottom Navigation Actions Safely
        safelyBindNavigation(R.id.nav_alerts, AlertsActivity.class);
        safelyBindNavigation(R.id.nav_contacts, ContactsActivity.class);
        safelyBindNavigation(R.id.btn_profile, ProfileActivity.class);

        // Safe binding for protection toggle button
        if (btnToggleProtection != null) {
            btnToggleProtection.setOnClickListener(v -> {
                Intent serviceIntent = new Intent(MainActivity.this, SafetyService.class);
                if (!isServiceRunning) {
                    startService(serviceIntent);
                    if (tvStatus != null) tvStatus.setText("Status: Tracking Guard Active");
                    btnToggleProtection.setText("STOP PROTECTION");
                    isServiceRunning = true;
                } else {
                    stopService(serviceIntent);
                    if (tvStatus != null) tvStatus.setText("Status: Inactive");
                    btnToggleProtection.setText("START PROTECTION");
                    isServiceRunning = false;
                }
            });
        } else {
            Log.e(TAG, "CRITICAL ERROR: btnToggleProtection is missing from activity_main.xml");
        }

        // Safe binding for instant SOS button
        if (btnInstantSOS != null) {
            btnInstantSOS.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SafetyService.class);
                intent.setAction(SafetyService.ACTION_NOT_SAFE);
                startService(intent);
                Toast.makeText(MainActivity.this, "Emergency Alert Signal Deployed!", Toast.LENGTH_SHORT).show();
            });
        } else {
            Log.e(TAG, "CRITICAL ERROR: btnInstantSOS is missing from activity_main.xml");
        }
    }

    /**
     * Safely binds navigation buttons to prevent null pointer crashes if IDs are missing from the XML.
     */
    private void safelyBindNavigation(int viewId, Class<?> targetActivity) {
        View targetView = findViewById(viewId);
        if (targetView != null) {
            targetView.setOnClickListener(v -> startActivity(new Intent(this, targetActivity)));
        } else {
            Log.w(TAG, "Warning: View ID " + viewId + " not found in activity_main.xml layout template.");
        }
    }
}