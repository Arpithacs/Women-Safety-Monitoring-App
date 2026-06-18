package com.example.safety_monitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private EditText etProfileName, etProfileBloodType;
    private TextView tvProfileName, tvProfileEmail, tvProfileInitial;
    private Switch switchShake, switchAI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etProfileName       = findViewById(R.id.etProfileName);
        etProfileBloodType  = findViewById(R.id.etProfileBloodType);
        tvProfileName       = findViewById(R.id.tvProfileName);
        tvProfileEmail      = findViewById(R.id.tvProfileEmail);
        tvProfileInitial    = findViewById(R.id.tvProfileInitial);
        switchShake         = findViewById(R.id.switchShake);
        switchAI            = findViewById(R.id.switchAI);

        loadProfile();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Save profile changes
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());

        // Sign out
        findViewById(R.id.btnSignOut).setOnClickListener(v -> confirmSignOut());
    }

    private void loadProfile() {
        SharedPreferences prefs = getSharedPreferences("SafetyPrefs", MODE_PRIVATE);
        String name = prefs.getString("user_name", "");
        String blood = prefs.getString("blood_type", "");
        boolean shakeEnabled = prefs.getBoolean("shake_enabled", true);
        boolean aiEnabled = prefs.getBoolean("ai_enabled", true);

        // Load from Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = name.isEmpty() ? user.getDisplayName() : name;
            String email = user.getEmail() != null ? user.getEmail() : "";

            tvProfileName.setText(displayName != null ? displayName : "User");
            tvProfileEmail.setText(email);

            if (displayName != null && !displayName.isEmpty()) {
                tvProfileInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
                etProfileName.setText(displayName);
            }
        }

        if (!blood.isEmpty()) etProfileBloodType.setText(blood);
        switchShake.setChecked(shakeEnabled);
        switchAI.setChecked(aiEnabled);
    }

    private void saveProfile() {
        String name = etProfileName.getText().toString().trim();
        String blood = etProfileBloodType.getText().toString().trim();

        if (name.isEmpty()) {
            etProfileName.setError("Name cannot be empty");
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences("SafetyPrefs", MODE_PRIVATE).edit();
        editor.putString("user_name", name);
        editor.putString("blood_type", blood);
        editor.putBoolean("shake_enabled", switchShake.isChecked());
        editor.putBoolean("ai_enabled", switchAI.isChecked());
        editor.apply();

        // Update displayed name
        tvProfileName.setText(name);
        tvProfileInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());

        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> signOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void signOut() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // Sign out from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            // Clear setup flag so setup runs again on next login
            getSharedPreferences("SafetyPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("setup_complete", false)
                    .apply();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
