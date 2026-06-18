package com.example.safety_monitor;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AlertsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bottom navigation
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.navAlerts).setOnClickListener(v -> {
            // Already here
        });

        findViewById(R.id.navContacts).setOnClickListener(v -> {
            startActivity(new Intent(this, ContactsActivity.class));
            finish();
        });
    }
}
