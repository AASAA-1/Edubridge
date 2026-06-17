package com.example.edubridge.shared;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.edubridge.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackingService";
    public static final String ACTION_START_TRACKING = "ACTION_START_TRACKING";
    public static final String ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING";
    public static final String EXTRA_STUDENT_ID = "EXTRA_STUDENT_ID";
    
    private static final String CHANNEL_ID = "LocationTrackingChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String studentId;
    private FirebaseFirestore db;
    private boolean isTracking = false;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                    updateFirestore(location);
                }
            }
        };
        
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_TRACKING.equals(action)) {
                String newStudentId = intent.getStringExtra(EXTRA_STUDENT_ID);
                if (newStudentId != null && !newStudentId.isEmpty()) {
                    studentId = newStudentId;
                }
                
                Log.d(TAG, "Starting tracking for student: " + studentId);
                startForegroundService();
                
                if (!isTracking) {
                    requestLocationUpdates();
                    isTracking = true;
                }
            } else if (ACTION_STOP_TRACKING.equals(action)) {
                Log.d(TAG, "Stopping tracking service");
                stopTracking();
                stopForeground(true);
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Student Safety Tracking")
                .setContentText("Automatic location tracking is active")
                .setSmallIcon(R.drawable.ic_mylocation)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .setMinUpdateDistanceMeters(5)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing location permission");
            return;
        }
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopTracking() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        isTracking = false;
    }

    private void updateFirestore(Location location) {
        if (studentId == null || studentId.isEmpty()) return;

        // Simulate heartbeat for demonstration
        long heartbeat = (long)(70 + Math.random() * 15);
        String status = "Normal";

        String locStr = String.format(Locale.US, "%.6f, %.6f", 
                location.getLatitude(), location.getLongitude());

        Map<String, Object> data = new HashMap<>();
        data.put("location", locStr);
        data.put("lastUpdated", System.currentTimeMillis());
        data.put("status", status);
        data.put("heartbeat", heartbeat);

        db.collection("tracking").document(studentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore update success for " + studentId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update Firestore", e));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Location Tracking", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopTracking();
        super.onDestroy();
    }
}
