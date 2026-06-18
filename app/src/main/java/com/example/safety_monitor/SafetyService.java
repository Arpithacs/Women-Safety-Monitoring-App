package com.example.safety_monitor;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

import org.tensorflow.lite.Interpreter;
import com.jlibrosa.audio.JLibrosa;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class SafetyService extends Service implements SensorEventListener, LocationListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private LocationManager locationManager;

    private static final float SHAKE_THRESHOLD = 12.0f;
    private long lastShakeTime;

    private Location lastKnownLocation;

    // --- SCALER VALUES ---
    private static final double[] SCALER_MEANS = { 0.125622, 0.107803, 2150.699072, 21.466955, -221.317602, 104.873841, -43.222217, 21.391006, -16.348396, 9.038348, -10.447314, 6.844460, -10.258889, 7.250434, -8.718747, 5.533568, -7.273030, 4.728472, -6.715767, 3.868351, -5.967559, 3.127981, -4.679508, 2.274707, -4.163557, 1.991229, -3.717191, 1.583045, -3.136680, 1.235084, -2.516389, 0.686088, -1.905880, 0.455119, -1.680706, 0.192363, -1.239201, 0.023662, -1.026694, -0.124254, -0.906956, -0.401787, -0.860243, -0.171172, 46.914225, 19.695994, 17.254539, 11.846053, 10.673124, 9.357741, 8.910028, 8.093704, 8.070385, 7.647888, 7.560765, 7.225028, 7.168838, 6.905834, 6.656903, 6.338155, 6.260738, 6.301353, 6.488584, 6.535792, 6.548924, 6.244244, 5.935193, 5.590678, 5.435876, 5.302776, 5.228050, 5.191423, 5.321695, 5.523637, 5.707929, 5.564182, 5.214549, 4.805683, 4.544072, 4.435030, 4.522338, 4.752750, 4.936233, 4.903231 };
    private static final double[] SCALER_STD = { 0.074992, 0.113679, 840.426511, 2.472180, 146.179209, 50.519466, 40.583740, 23.868191, 19.721875, 18.768383, 15.980342, 14.076903, 13.137158, 11.058125, 10.587044, 9.870925, 9.892640, 8.835300, 8.366088, 7.707416, 7.588401, 7.345473, 7.298164, 7.208292, 7.202168, 6.390562, 6.107892, 5.497941, 5.250933, 5.033716, 4.797614, 4.750077, 4.973354, 4.978919, 5.335876, 4.934218, 4.589208, 4.092514, 3.879730, 3.635907, 3.715948, 4.007643, 4.239545, 4.203537, 41.814333, 12.996475, 11.826150, 6.881510, 6.157128, 5.183515, 4.687556, 4.178862, 4.127136, 3.935744, 3.858708, 3.759320, 3.666217, 3.550563, 3.244135, 3.108221, 2.989797, 3.102342, 3.350780, 3.517324, 3.521308, 3.249244, 2.906625, 2.651914, 2.461635, 2.423353, 2.312471, 2.342087, 2.488267, 2.791274, 3.028492, 2.886524, 2.471292, 2.091165, 1.857395, 1.792179, 1.883848, 2.174266, 2.458021, 2.441354 };

    private static final int USER_RESPONSE_DELAY_MS = 15000;
    private static final int RETRY_DELAY_MS = 5000;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isAlertActive = false;
    private int checkAttempt = 0;

    public static final String CHANNEL_ID = "SafetyChannel_AI_v16";

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        createNotificationChannel();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(1, getMonitoringNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, getMonitoringNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(1, getMonitoringNotification());
            }
        } catch (Exception e) {
            Log.e("SafetyApp", "STARTUP ERROR: " + e.getMessage());
            try { startForeground(1, getMonitoringNotification()); } catch (Exception ex) {}
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, this);

            Location lastGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (lastGPS != null) lastKnownLocation = lastGPS;
            else if (lastNet != null) lastKnownLocation = lastNet;

        } catch (Exception e) { Log.e("SafetyApp", "Location Setup Failed: " + e.getMessage()); }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isAlertActive) return;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0]; float y = event.values[1]; float z = event.values[2];
            double gForce = Math.sqrt(x*x + y*y + z*z);
            if (Math.abs(gForce - 9.8) > SHAKE_THRESHOLD) {
                long now = System.currentTimeMillis();
                if ((now - lastShakeTime) > 1000) {
                    lastShakeTime = now;
                    triggerSafetyCheck();
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.lastKnownLocation = location;
        if (isAlertActive) return;
        if (location.hasSpeed()) {
            float speed = location.getSpeed();
            if (speed >= 8.0f && speed <= 10.0f) triggerSafetyCheck();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onProviderEnabled(String p) {}
    @Override public void onProviderDisabled(String p) {}
    @Override public void onStatusChanged(String p, int s, android.os.Bundle e) {}

    private void triggerSafetyCheck() {
        isAlertActive = true;
        checkAttempt = 0;
        showInteractiveNotification("Abnormal movement! Are you safe?", true);
        handler.postDelayed(runnableActivateMic, USER_RESPONSE_DELAY_MS);
    }
    // Add this anywhere inside the class, outside of other methods
    private Runnable runnableSendAlert = new Runnable() {
        @Override
        public void run() {
            sendLocationAndSMS();
        }
    };
    private Runnable runnableActivateMic = new Runnable() {
        @Override
        public void run() {
            checkAttempt++;
            showInteractiveNotification("Listening (Attempt " + checkAttempt + ")...", false);

            new Thread(() -> {
                try {


                    float dangerResult = getDangerProbability();

                    handler.post(() -> {
                        // 2. CHECK THE RESULT

                        if (dangerResult == -1.0f) {
                            // --- CASE: SILENCE DETECTED ---
                            Log.d("SafetyApp", "It is silent. Asking user again.");

                            // "Ask the user again whether they are safe"
                            showInteractiveNotification("No sound detected. Are you safe?", true);

                            // Reset the timer to wait for user input again (Loop back to Step 1)
                            if (checkAttempt < 3) {
                                handler.postDelayed(runnableActivateMic, USER_RESPONSE_DELAY_MS);
                            } else {
                                // If silent 3 times in a row, assume safe or just stop
                                cancelAlerts();
                            }

                        } else if (dangerResult > 0.85f) {
                            // --- CASE: DANGER DETECTED ---
                            Log.d("SafetyApp", "DANGER DETECTED BY AI!");
                            sendLocationAndSMS();

                        } else {
                            // --- CASE: NOISY, BUT SAFE (e.g., Traffic, Talking) ---
                            Log.d("SafetyApp", "Noise detected, but AI says safe.");
                            cancelAlerts();
                        }
                    });
                } catch (Exception e) { Log.e("SafetyApp", "Error: " + e.getMessage()); }
            }).start();
        }
    };
    private void sendLocationAndSMS() {
        android.content.SharedPreferences prefs = getSharedPreferences("SafetyPrefs", MODE_PRIVATE);
        String emergencyNumber = prefs.getString("emergency_phone", "");
        String userName = prefs.getString("user_name", "User");

        if (emergencyNumber.isEmpty()) {
            Toast.makeText(this, "No Contact Set!", Toast.LENGTH_LONG).show();
            cancelAlerts();
            return;
        }

        String messageBody;
        if (lastKnownLocation != null) {
            String mapsLink = "http://maps.google.com/?q=" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude();
            messageBody = "HELP! " + userName + " is in danger! Location: " + mapsLink;
        } else {
            messageBody = "HELP! " + userName + " is in danger! (GPS searching...)";
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emergencyNumber, null, messageBody, null, null);
            Toast.makeText(this, "SMS SENT to " + emergencyNumber, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("SafetyApp", "SMS Failed: " + e.getMessage());
            e.printStackTrace();
        }
        cancelAlerts();
    }

    @SuppressLint("MissingPermission")
    private float getDangerProbability() {
        String MODEL_FILENAME = "distress_model.tflite";
        int SAMPLE_RATE = 22050;
        int RECORDING_LENGTH = 22050*10;
        int NUM_FEATURES = 84;

        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minBufferSize, RECORDING_LENGTH * 2));
        short[] audioBuffer = new short[RECORDING_LENGTH];

        try {
            audioRecord.startRecording();
            int read = audioRecord.read(audioBuffer, 0, RECORDING_LENGTH);
            audioRecord.stop();
            audioRecord.release();
            if (read < RECORDING_LENGTH) return 0.0f;
        } catch (Exception e) { return 0.0f; }

        // --- 1. SMART GAIN (AGC) ---
        // Calculate the raw peak volume first
        short maxRaw = 0;
        for (short s : audioBuffer) {
            if (Math.abs(s) > maxRaw) maxRaw = (short) Math.abs(s);
        }

        // Decide how much to boost based on raw volume
        float gainFactor = 1.0f;
        if (maxRaw < 2000) {
            gainFactor = 10.0f; // Very quiet -> Huge Boost
        } else if (maxRaw < 5000) {
            gainFactor = 5.0f;  // Quiet -> Medium Boost
        } else if (maxRaw < 15000) {
            gainFactor = 2.0f;  // Normal -> Slight Boost
        } else {
            gainFactor = 1.0f;  // Loud -> NO BOOST (Prevent Clipping)
        }

        Log.d("SafetyApp", "Raw Max: " + maxRaw + " | Gain Applied: " + gainFactor + "x");

        float[] audioFloat = new float[RECORDING_LENGTH];
        for (int i = 0; i < RECORDING_LENGTH; i++) {
            float val = (audioBuffer[i] / 32767.0f) * gainFactor;
            // Prevent distortion limits
            if (val > 1.0f) val = 1.0f;
            if (val < -1.0f) val = -1.0f;
            audioFloat[i] = val;
        }

        // --- 2. CALCULATE METRICS ---
        float currentVolume = calculateRMS(audioFloat);
        float currentZCR = calculateZCR(audioFloat);
        Log.d("SafetyApp", "Vol: " + currentVolume + " | Pitch: " + currentZCR);

        // --- 3. SMART GATES ---

        // A. Silence Gate (Always active)
        if (currentVolume < 0.03f) {
            Log.d("SafetyApp", "Result: Silence.");
            return -1.0f; // <--- SPECIAL CODE FOR SILENCE
        }

        // B. Pitch Gate (WITH OVERRIDE)
        // If volume is super loud (> 0.4), assume it's a scream and SKIP the pitch check.
        // This fixes the "10cm away" issue where distortion ruins the pitch calculation.
        if (currentVolume < 0.4f) {
            if (currentZCR < 0.08f) { // Very permissive pitch threshold
                Log.d("SafetyApp", "Ignored: Normal Sound (Low Pitch).");
                return 0.0f;
            }
        } else {
            Log.d("SafetyApp", "Loud Audio Detected! Bypassing Pitch Filter.");
        }

        // --- 4. AI INFERENCE ---
        JLibrosa jLibrosa = new JLibrosa();
        float[] features = new float[NUM_FEATURES];
        try {
            features[0] = currentZCR;
            features[1] = currentVolume;
            features[2] = (float) SCALER_MEANS[2];
            features[3] = (float) SCALER_MEANS[3];
            float[][] mfccValues = jLibrosa.generateMFCCFeatures(audioFloat, SAMPLE_RATE, 40);
            for (int i = 0; i < 40; i++) {
                if (i < mfccValues.length) {
                    features[4 + i] = calculateMean(mfccValues[i]);
                    features[44 + i] = calculateStd(mfccValues[i]);
                }
            }
        } catch (Exception e) { return 0.0f; }

        float[][] input = new float[1][NUM_FEATURES];
        for (int i = 0; i < NUM_FEATURES; i++) {
            input[0][i] = (float) ((features[i] - SCALER_MEANS[i]) / SCALER_STD[i]);
        }

        try (AssetFileDescriptor fileDescriptor = getAssets().openFd(MODEL_FILENAME);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {

            FileChannel fileChannel = inputStream.getChannel();
            MappedByteBuffer tfliteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());

            try (Interpreter interpreter = new Interpreter(tfliteModel)) {
                float[][] output = new float[1][1];
                interpreter.run(input, output);
                return output[0][0];
            }
        } catch (Exception e) { return 0.0f; }
    }
    private float calculateMean(float[] data) { float sum = 0; for (float f : data) sum += f; return sum / data.length; }
    private float calculateZCR(float[] audio) { float count = 0; for (int i = 1; i < audio.length; i++) { if ((audio[i] >= 0 && audio[i-1] < 0) || (audio[i] < 0 && audio[i-1] >= 0)) count++; } return count / (float)(audio.length - 1); }
    private float calculateStd(float[] data) { float mean = calculateMean(data); float sum = 0; for (float f : data) sum += Math.pow(f - mean, 2); return (float) Math.sqrt(sum / data.length); }
    private float calculateRMS(float[] audio) { float sum = 0; for (float f : audio) sum += f * f; return (float) Math.sqrt(sum / audio.length); }

    private void cancelAlerts() {
        isAlertActive = false;
        checkAttempt = 0;
        handler.removeCallbacks(runnableActivateMic);
        handler.removeCallbacks(runnableSendAlert);
        NotificationManager m = getSystemService(NotificationManager.class);
        if(m!=null) m.notify(1, getMonitoringNotification());
    }

    @SuppressLint("MissingPermission")
    private void showInteractiveNotification(String text, boolean sound) {
        Intent s = new Intent(this, SafetyService.class); s.setAction("ACTION_IAM_SAFE");
        PendingIntent pS = PendingIntent.getService(this, 0, s, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent d = new Intent(this, SafetyService.class); d.setAction("ACTION_NOT_SAFE");
        PendingIntent pD = PendingIntent.getService(this, 1, d, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safety Alert").setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_MAX).setOngoing(true)
                .addAction(android.R.drawable.ic_input_add, "I AM SAFE", pS)
                .addAction(android.R.drawable.ic_delete, "HELP ME", pD);

        if(sound) {
            b.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            b.setVibrate(new long[]{0,500,200,500});
        }
        NotificationManager m = getSystemService(NotificationManager.class); if(m!=null) m.notify(1, b.build());
    }
    private Notification getMonitoringNotification() { return new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Safety Monitor").setContentText("Active").setSmallIcon(android.R.drawable.ic_menu_mylocation).build(); }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(CHANNEL_ID, "Safety Channel", NotificationManager.IMPORTANCE_HIGH);
            c.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}