package com.example.safety_monitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import org.json.JSONArray;
import org.json.JSONObject;

public class ContactsActivity extends AppCompatActivity {

    private EditText etContactName, etContactPhone;
    private LinearLayout contactsContainer;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "SafetyPrefs";
    private static final String KEY_CONTACTS_JSON = "contacts_list_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        // Bind layout views
        etContactName = findViewById(R.id.etContactName);
        etContactPhone = findViewById(R.id.etContactPhone);
        contactsContainer = findViewById(R.id.contactsContainer);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Render existing saved entries immediately on screen startup
        loadAndDisplayContacts();

        // Handle Add Button (+) click action
        findViewById(R.id.btnSaveContact).setOnClickListener(v -> addContactItem());

        // Back button finish execution
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Navigation Bar listeners
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navAlerts).setOnClickListener(v -> {
            startActivity(new Intent(this, AlertsActivity.class));
            finish();
        });
    }

    /**
     * Reads contact collection string payload and inflates single unified CardViews programmatically.
     */
    private void loadAndDisplayContacts() {
        contactsContainer.removeAllViews(); // Wipe out any stale container view nodes

        try {
            String rawJson = sharedPreferences.getString(KEY_CONTACTS_JSON, "[]");
            JSONArray array = new JSONArray(rawJson);

            float scale = getResources().getDisplayMetrics().density;
            int paddingPx = (int) (14 * scale);
            int marginPx = (int) (10 * scale);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String contactName = obj.getString("name");
                String contactPhone = obj.getString("phone");
                final int positionIndex = i;

                // 1. The Main Outer CardView Component wrapper
                CardView cardView = new CardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, marginPx);
                cardView.setLayoutParams(cardParams);
                cardView.setRadius(8 * scale);
                cardView.setCardElevation(3 * scale);
                cardView.setUseCompatPadding(true);
                cardView.setCardBackgroundColor(Color.WHITE);

                // 2. Inner layout inside the CardView to arrange fields horizontally
                LinearLayout innerLayout = new LinearLayout(this);
                innerLayout.setOrientation(LinearLayout.HORIZONTAL);
                innerLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                innerLayout.setGravity(Gravity.CENTER_VERTICAL);

                // 3. Vertical layout text stack (Groups Name on top and Number below)
                LinearLayout labelStack = new LinearLayout(this);
                LinearLayout.LayoutParams stackParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                labelStack.setLayoutParams(stackParams);
                labelStack.setOrientation(LinearLayout.VERTICAL);

                // Name TextView (Top row inside the single card)
                TextView tvName = new TextView(this);
                tvName.setText(contactName);
                tvName.setTextSize(16);
                tvName.setTextColor(Color.parseColor("#1F2937"));
                tvName.setTypeface(null, Typeface.BOLD);
                labelStack.addView(tvName);

                // Phone TextView (Directly under name inside the exact same card)
                TextView tvPhone = new TextView(this);
                tvPhone.setText(contactPhone);
                tvPhone.setTextSize(14);
                tvPhone.setTextColor(Color.parseColor("#6B7280"));
                tvPhone.setPadding(0, 4, 0, 0);
                labelStack.addView(tvPhone);

                // 4. Highlighted Red "Remove" Option Button inside the same CardView
                Button btnRemove = new Button(this);
                LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        (int) (36 * scale));
                btnRemove.setLayoutParams(removeParams);
                btnRemove.setText("Remove");
                btnRemove.setTextColor(Color.WHITE);
                btnRemove.setTextSize(12);
                btnRemove.setAllCaps(false);
                btnRemove.setTypeface(null, Typeface.BOLD);
                btnRemove.setBackgroundColor(Color.parseColor("#EF4444")); // Clear warning red hex shade
                btnRemove.setPadding(24, 0, 24, 0);

                // Bind delete action to index position
                btnRemove.setOnClickListener(delView -> removeContactItem(positionIndex));

                // Nest and stack elements together natively inside the same card
                innerLayout.addView(labelStack);
                innerLayout.addView(btnRemove);
                cardView.addView(innerLayout);

                // Add the completed single card to the scrolling container
                contactsContainer.addView(cardView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Validates fields, saves the entry to storage, and refreshes the display instantly.
     */
    private void addContactItem() {
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

        try {
            String rawJson = sharedPreferences.getString(KEY_CONTACTS_JSON, "[]");
            JSONArray array = new JSONArray(rawJson);

            JSONObject newContact = new JSONObject();
            newContact.put("name", name);
            newContact.put("phone", phone);
            array.put(newContact);

            sharedPreferences.edit().putString(KEY_CONTACTS_JSON, array.toString()).apply();

            Toast.makeText(this, "Contact saved successfully", Toast.LENGTH_SHORT).show();
            etContactName.setText("");
            etContactPhone.setText("");

            // Re-render user screen list presentation layer instantly
            loadAndDisplayContacts();

        } catch (Exception e) {
            Toast.makeText(this, "Error processing entry context update", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes a contact from the array at a specific index and updates the UI layout feed.
     */
    private void removeContactItem(int index) {
        try {
            String rawJson = sharedPreferences.getString(KEY_CONTACTS_JSON, "[]");
            JSONArray array = new JSONArray(rawJson);

            JSONArray updatedArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                if (i != index) {
                    updatedArray.put(array.get(i));
                }
            }

            sharedPreferences.edit().putString(KEY_CONTACTS_JSON, updatedArray.toString()).apply();
            Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show();

            // Instantly re-render visual layout components tree
            loadAndDisplayContacts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}