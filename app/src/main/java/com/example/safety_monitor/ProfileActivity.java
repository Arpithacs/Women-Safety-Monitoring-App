package com.example.safety_monitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileBlood, tvProfileInitial;
    private EditText etProfileName, etProfileBloodType;
    private SwitchCompat switchShake, switchAI;
    private Button btnSignOut, btnToggleEditMode, btnSaveProfile;
    private CardView cardEditProfile;
    private ImageView btnBack;

    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("SafeGuardPrefs", Context.MODE_PRIVATE);

        // Display components
        tvProfileInitial = findViewById(R.id.tvProfileInitial);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileBlood = findViewById(R.id.tvProfileBlood);

        // Editable hidden structural layers
        cardEditProfile = findViewById(R.id.cardEditProfile);
        etProfileName = findViewById(R.id.etProfileName);
        etProfileBloodType = findViewById(R.id.etProfileBloodType);

        // Action Controls
        btnToggleEditMode = findViewById(R.id.btnToggleEditMode);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnSignOut = findViewById(R.id.btnSignOut);
        btnBack = findViewById(R.id.btnBack);

        // Settings Toggles
        switchShake = findViewById(R.id.switchShake);
        switchAI = findViewById(R.id.switchAI);

        // Load initialization profile display values
        refreshProfileDisplay();

        // Load preference configuration toggles
        switchShake.setChecked(sharedPreferences.getBoolean("shake_enabled", true));
        switchAI.setChecked(sharedPreferences.getBoolean("ai_enabled", true));

        // Back button navigation anchor
        btnBack.setOnClickListener(v -> finish());

        // Toggle form visibility mode smoothly
        btnToggleEditMode.setOnClickListener(v -> {
            if (cardEditProfile.getVisibility() == View.GONE) {
                // Populate current records before unfolding layout fields
                etProfileName.setText(sharedPreferences.getString("user_name", ""));
                etProfileBloodType.setText(sharedPreferences.getString("blood_type", ""));
                cardEditProfile.setVisibility(View.VISIBLE);
                btnToggleEditMode.setText("Cancel Editing");
            } else {
                cardEditProfile.setVisibility(View.GONE);
                btnToggleEditMode.setText("Edit Profile Details");
            }
        });

        // Save Profile Changes Pipeline
        btnSaveProfile.setOnClickListener(v -> {
            String updatedName = etProfileName.getText().toString().trim();
            String updatedBlood = etProfileBloodType.getText().toString().trim();

            if (updatedName.isEmpty()) {
                Toast.makeText(this, "Name context cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Commit modifications directly into SharedPreferences storage maps
            sharedPreferences.edit()
                    .putString("user_name", updatedName)
                    .putString("blood_type", updatedBlood)
                    .apply();

            // Refresh UI values and hide the edit fields out of sight
            refreshProfileDisplay();
            cardEditProfile.setVisibility(View.GONE);
            btnToggleEditMode.setText("Edit Profile Details");
            Toast.makeText(this, "Profile adjustments applied successfully!", Toast.LENGTH_SHORT).show();
        });

        // Config Sensor Toggle Monitoring Actions
        switchShake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("shake_enabled", isChecked).apply();
            Toast.makeText(this, "Restart tracking service to update configurations", Toast.LENGTH_SHORT).show();
        });

        switchAI.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("ai_enabled", isChecked).apply();
            Toast.makeText(this, "Restart tracking service to update AI sound parameters", Toast.LENGTH_SHORT).show();
        });

        // Sign Out Pipeline
        btnSignOut.setOnClickListener(v -> handleSignOut());
    }

    private void refreshProfileDisplay() {
        String name = sharedPreferences.getString("user_name", "User Name");
        String blood = sharedPreferences.getString("blood_type", "Unknown");

        tvProfileName.setText(name);
        tvProfileBlood.setText("Blood Type: " + (blood.isEmpty() ? "Unknown" : blood));

        // Dynamically compute display asset text character marker badge
        if (!name.isEmpty()) {
            tvProfileInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            tvProfileInitial.setText("?");
        }
    }

    private void handleSignOut() {
        mAuth.signOut();
        sharedPreferences.edit().clear().apply();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}