package com.example.edubridge.parent;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentTrackingFragment extends Fragment {

    private static final String TAG = "StudentTracking";
    private AutoCompleteTextView studentSelector;
    private TextView tvStudentName, tvLocation, tvHeartbeatValue, tvHeartbeatStatus, tvLastUpdated;
    private MapView map;
    private Marker studentMarker;
    private FloatingActionButton fabCenterStudent;
    private GeoPoint currentStudentLocation;
    private boolean isFirstUpdate = true;
    
    private FirebaseFirestore db;
    private String parentId;
    private final List<StudentInfo> studentList = new ArrayList<>();
    private ListenerRegistration trackingListener;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Load OSM configuration
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        
        View v = inflater.inflate(R.layout.fragment_student_tracking, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        map = v.findViewById(R.id.map_view);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        
        IMapController mapController = map.getController();
        mapController.setZoom(17.0);
        
        initializeViews(v);
        loadStudents();

        return v;
    }

    private void initializeViews(View v) {
        studentSelector = v.findViewById(R.id.student_selector);
        tvStudentName = v.findViewById(R.id.tv_student_name);
        tvLocation = v.findViewById(R.id.tv_location);
        tvHeartbeatValue = v.findViewById(R.id.tv_heartbeat_value);
        tvHeartbeatStatus = v.findViewById(R.id.tv_heartbeat_status);
        tvLastUpdated = v.findViewById(R.id.tv_last_updated);
        fabCenterStudent = v.findViewById(R.id.fab_center_student);

        ImageView btnBack = v.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(view -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        studentSelector.setOnItemClickListener((parent, view, position, id) -> {
            StudentInfo selected = studentList.get(position);
            isFirstUpdate = true;
            updateTrackingListener(selected);
        });

        if (fabCenterStudent != null) {
            fabCenterStudent.setOnClickListener(view -> {
                if (currentStudentLocation != null && map != null) {
                    map.getController().animateTo(currentStudentLocation);
                } else {
                    Toast.makeText(getContext(), R.string.no_tracking_data, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadStudents() {
        if (parentId == null) return;

        db.collection("users")
                .document(parentId)
                .collection("students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    studentList.clear();
                    List<String> names = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        StudentInfo student = new StudentInfo();
                        student.id = doc.getString("studentId");
                        if (student.id == null || student.id.isEmpty()) {
                            student.id = doc.getId();
                        }
                        student.name = doc.getString("name");
                        if (student.name == null) student.name = getString(R.string.unknown);
                        
                        studentList.add(student);
                        names.add(student.name);
                    }

                    if (getContext() != null && !studentList.isEmpty()) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_dropdown_item_1line, names);
                        studentSelector.setAdapter(adapter);

                        studentSelector.setText(studentList.get(0).name, false);
                        updateTrackingListener(studentList.get(0));
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), getString(R.string.error_loading_data, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateTrackingListener(StudentInfo student) {
        tvStudentName.setText(student.name);
        
        if (trackingListener != null) {
            trackingListener.remove();
        }

        if (studentMarker != null && map != null) {
            map.getOverlays().remove(studentMarker);
            studentMarker = null;
        }

        trackingListener = db.collection("tracking").document(student.id)
                .addSnapshotListener((doc, e) -> {
                    if (!isAdded() || getContext() == null) return;
                    
                    if (e != null || doc == null || !doc.exists()) {
                        tvLocation.setText(R.string.no_tracking_data);
                        tvHeartbeatValue.setText(R.string.na);
                        tvHeartbeatStatus.setText(R.string.unknown);
                        if (tvLastUpdated != null) tvLastUpdated.setText("");
                        if (studentMarker != null && map != null) {
                            map.getOverlays().remove(studentMarker);
                            studentMarker = null;
                            map.invalidate();
                        }
                        currentStudentLocation = null;
                        return;
                    }

                    String locationStr = doc.getString("location");
                    Long heartbeat = doc.getLong("heartbeat");
                    String status = doc.getString("status");
                    Long timestamp = doc.getLong("lastUpdated");

                    if (heartbeat != null) {
                        tvHeartbeatValue.setText(String.format(Locale.getDefault(), getString(R.string.bpm_format), heartbeat));
                    } else {
                        tvHeartbeatValue.setText(R.string.na);
                    }
                    tvHeartbeatStatus.setText(status != null ? status : getString(R.string.unknown));

                    if (timestamp != null && tvLastUpdated != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                        tvLastUpdated.setText(getString(R.string.last_updated, sdf.format(new Date(timestamp))));
                    }

                    if (locationStr != null && map != null) {
                        try {
                            String[] parts = locationStr.split(",");
                            double lat = Double.parseDouble(parts[0].trim());
                            double lng = Double.parseDouble(parts[1].trim());
                            currentStudentLocation = new GeoPoint(lat, lng);

                            updateLocationText(lat, lng);

                            if (studentMarker == null) {
                                studentMarker = new Marker(map);
                                studentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                map.getOverlays().add(studentMarker);
                            }
                            
                            studentMarker.setPosition(currentStudentLocation);
                            studentMarker.setTitle(student.name);
                            
                            if (isFirstUpdate) {
                                map.getController().animateTo(currentStudentLocation);
                                isFirstUpdate = false;
                            }
                            map.invalidate();
                        } catch (Exception ex) {
                            Log.e(TAG, "Error parsing location: " + locationStr, ex);
                            tvLocation.setText(locationStr);
                        }
                    }
                });
    }

    private void updateLocationText(double lat, double lng) {
        executorService.execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            String addressText;
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    // Get a nice readable address summary
                    String feature = address.getFeatureName();
                    String thoroughfare = address.getThoroughfare();
                    String locality = address.getLocality();
                    String country = address.getCountryName();
                    
                    if (feature != null) sb.append(feature).append(", ");
                    if (thoroughfare != null && !thoroughfare.equals(feature)) sb.append(thoroughfare).append(", ");
                    if (locality != null) sb.append(locality).append(", ");
                    if (country != null) sb.append(country);
                    
                    addressText = sb.toString().trim();
                    if (addressText.endsWith(",")) addressText = addressText.substring(0, addressText.length() - 1);
                } else {
                    addressText = String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
                }
            } catch (Exception e) {
                addressText = String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
            }
            
            final String finalAddress = addressText;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) tvLocation.setText(finalAddress);
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (trackingListener != null) {
            trackingListener.remove();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private static class StudentInfo {
        String id;
        String name;
    }
}
