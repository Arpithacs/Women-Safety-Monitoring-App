package com.example.safety_monitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private SharedPreferences sharedPreferences;

    // UI Elements
    private EditText etLoginEmail, etLoginPassword;
    private Button btnFormalLogin, btnGoogleSignIn;
    private TextView tvSwitchToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase and Session preferences
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        sharedPreferences = getSharedPreferences("SafeGuardPrefs", Context.MODE_PRIVATE);

        // Map Views directly from our soft-colored layout
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnFormalLogin = findViewById(R.id.btnFormalLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvSwitchToRegister = findViewById(R.id.tvSwitchToRegister);

        // Check if user session is already valid and setup parameters are complete
        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean isSetupComplete = sharedPreferences.getBoolean("is_setup_complete", false);

        if (currentUser != null && isSetupComplete) {
            navigateToMain();
            return;
        } else if (currentUser != null) {
            navigateToSetup();
            return;
        }

        // Formal Email / Password Auth Execution Loop
        btnFormalLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleFormalEmailLogin();
            }
        });

        // Google Authentication Credential Call
        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerGoogleSignIn();
            }
        });

        // Toggle redirection placeholder link
        tvSwitchToRegister.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Opening registration flow...", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            // startActivity(intent);
        });
    }

    private void handleFormalEmailLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email login authentication successful.");
                        checkRouteAndNavigate();
                    } else {
                        Log.e(TAG, "Email Sign-In failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void triggerGoogleSignIn() {
        String serverClientId = getString(R.string.default_web_client_id);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                credentialManager.getCredentialAsync(
                        LoginActivity.this,
                        request,
                        null,
                        Executors.newSingleThreadExecutor(),
                        new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                            @Override
                            public void onResult(GetCredentialResponse result) {
                                handleSignInResult(result);
                            }

                            @Override
                            public void onError(@NonNull GetCredentialException e) {
                                Log.e(TAG, "Credential Manager Error: " + e.getMessage());
                                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Sign-In canceled or failed.", Toast.LENGTH_SHORT).show());
                            }
                        }
                );
            } catch (Exception e) {
                Log.e(TAG, "Sign in execution failure: " + e.getMessage());
            }
        });
    }

    private void handleSignInResult(GetCredentialResponse result) {
        try {
            if (result.getCredential() instanceof androidx.credentials.CustomCredential) {
                androidx.credentials.CustomCredential customCredential = (androidx.credentials.CustomCredential) result.getCredential();
                if (customCredential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                    GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());
                    String idToken = googleIdTokenCredential.getIdToken();

                    // Cache profile details locally using standardized keys
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("user_name", googleIdTokenCredential.getDisplayName());
                    editor.apply();

                    firebaseAuthWithGoogle(idToken);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed parsing structural tokens: " + e.getMessage());
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth with Google successful.");
                        checkRouteAndNavigate();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkRouteAndNavigate() {
        boolean isSetupComplete = sharedPreferences.getBoolean("is_setup_complete", false);
        if (isSetupComplete) {
            navigateToMain();
        } else {
            navigateToSetup();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToSetup() {
        Intent intent = new Intent(LoginActivity.this, SetupActivity.class);
        startActivity(intent);
        finish();
    }
}