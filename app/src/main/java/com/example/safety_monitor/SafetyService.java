package com.example.safety_monitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class SafetyService extends Service implements SensorEventListener {
    private static final String TAG = "SafetyService";
    private static final String CHANNEL_ID = "SafetyServiceChannel";

    // Action definitions sent from buttons/notifications
    public static final String ACTION_NOT_SAFE = "com.example.safety_monitor.ACTION_NOT_SAFE";
    public static final String ACTION_IAM_SAFE = "com.example.safety_monitor.ACTION_IAM_SAFE";

    private SensorManager sensorManager;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("SafeGuardPrefs", Context.MODE_PRIVATE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Priority 2 (Step 8): Read configuration toggles dynamically before starting tracking listeners
        boolean shakeEnabled = sharedPreferences.getBoolean("shake_enabled", true);
        if (shakeEnabled && sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        // (TFLite Audio initialization would go here if enabled via ai_enabled toggle)
    }

    // PRIORITY 1 (Step 4): Critical command interpreter implementation
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "Received background service action: " + action);

            if (ACTION_NOT_SAFE.equals(action)) {
                triggerEmergencyAlertPipeline();
            } else if (ACTION_IAM_SAFE.equals(action)) {
                stopSelf(); // Stand down active danger sequence safely
                return START_NOT_STICKY;
            }
        }

        // Keep service running in foreground persistently
        Notification notification = createPersistentNotification("SafeGuard is monitoring your surroundings.");
        startForeground(1, notification);

        return START_STICKY;
    }

    private void triggerEmergencyAlertPipeline() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                String mapsLink = "https://maps.google.com/?q=0,0 (Location Unavailable)";
                if (location != null) {
                    mapsLink = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                }
                sendEmergencySms(mapsLink);
            }).addOnFailureListener(e -> sendEmergencySms("https://maps.google.com/?q=0,0 (Location Timeout)"));
        } catch (SecurityException e) {
            Log.e(TAG, "Location permissions missing during active broadcast sequence: " + e.getMessage());
        }
    }

    private void sendEmergencySms(String locationUrl) {
        String phone = sharedPreferences.getString("emergency_phone", "");
        String contactName = sharedPreferences.getString("contact_name", "Emergency Contact");
        String userName = sharedPreferences.getString("user_name", "Someone");

        if (phone.isEmpty()) {
            Log.e(TAG, "Aborting broadcast: No emergency telephone specified.");
            return;
        }

        String message = "[SafeGuard Alert] " + userName + " might be in danger! Live Location link: " + locationUrl;

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Modern API context initialization wrapper approach
                smsManager = this.getSystemService(SmsManager.class);
            } else {
                // Classic legacy fallback compatibility handling
                @SuppressWarnings("deprecation")
                SmsManager legacySmsManager = SmsManager.getDefault();
                smsManager = legacySmsManager;
            }

            if (smsManager != null) {
                smsManager.sendTextMessage(phone, null, message, null, null);
                Log.d(TAG, "Emergency dispatch message deployed successfully to " + contactName);
                saveAlertToHistory(userName + " triggered SOS. Alert dispatched to " + contactName);
            }
        } catch (Exception e) {
            Log.e(TAG, "SmsManager delivery pipeline failed: " + e.getMessage());
        }
    }

    private void saveAlertToHistory(String logMessage) {
        String existingHistory = sharedPreferences.getString("alert_history_json", "");
        long timestamp = System.currentTimeMillis();
        String formattedLog = "[" + new java.util.Date(timestamp).toString() + "] " + logMessage + "\n" + existingHistory;
        sharedPreferences.edit().putString("alert_history_json", formattedLog).apply();
    }

    private Notification createPersistentNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Interactive Danger Resolution Actions
        Intent safeIntent = new Intent(this, SafetyService.class).setAction(ACTION_IAM_SAFE);
        PendingIntent safePending = PendingIntent.getService(this, 1, safeIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent dangerIntent = new Intent(this, SafetyService.class).setAction(ACTION_NOT_SAFE);
        PendingIntent dangerPending = PendingIntent.getService(this, 2, dangerIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafeGuard Active")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_secure)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.checkbox_on_background, "I AM SAFE", safePending)
                .addAction(android.R.drawable.ic_dialog_alert, "HELP ME", dangerPending)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "SafeGuard Background Engine Channel", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override public void onSensorChanged(SensorEvent event) { /* Accelerometer logic wrapper */ }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override public IBinder onBind(Intent intent) { return null; }
}