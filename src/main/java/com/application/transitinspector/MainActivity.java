package com.application.transitinspector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;


import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.*;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    EditText etVehicleNumber, etDriverName, etLicenseNumber, etDescription, etFineAmount;
    Spinner spinnerViolationType, spinnerVehicleStatus;
    TextView tvLocation;
    Button btnCapturePhoto, btnSubmit, btnSync;
    ImageView imagePreview;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap photoBitmap;
    private double latitude = 0.0, longitude = 0.0;

    FirebaseDatabase database;
    DatabaseReference violationsRef;
    FirebaseStorage storage;

    FusedLocationProviderClient locationClient;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        etVehicleNumber = findViewById(R.id.et_vehicle_number);
        etDriverName = findViewById(R.id.et_driver_name);
        etLicenseNumber = findViewById(R.id.et_license_number);
        etDescription = findViewById(R.id.et_violation_description);
        etFineAmount = findViewById(R.id.et_fine_amount);
        spinnerViolationType = findViewById(R.id.spinner_violation_type);
        spinnerVehicleStatus = findViewById(R.id.spinner_vehicle_status);
        tvLocation = findViewById(R.id.tv_location);
        btnCapturePhoto = findViewById(R.id.btn_capture_photo);
        btnSubmit = findViewById(R.id.btn_submit);
        btnSync = findViewById(R.id.btn_syncactivity);
        imagePreview = findViewById(R.id.image_preview);

        // Firebase
        database = FirebaseDatabase.getInstance();
        violationsRef = database.getReference("violations");
        storage = FirebaseStorage.getInstance();

        // Populate spinners
        ArrayAdapter<CharSequence> violationAdapter = ArrayAdapter.createFromResource(this,
                R.array.violation_types_array, android.R.layout.simple_spinner_item);
        violationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerViolationType.setAdapter(violationAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.vehicle_status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleStatus.setAdapter(statusAdapter);

        // Capture photo
        btnCapturePhoto.setOnClickListener(v -> {
            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePicture.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
            }
        });

        // Location
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        getCurrentLocation();

        // Submit form
        btnSubmit.setOnClickListener(v -> submitViolation());
        btnSync.setOnClickListener(v -> openSyncActivity());
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                tvLocation.setText("Location: " + latitude + ", " + longitude);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            photoBitmap = (Bitmap) data.getExtras().get("data");
            imagePreview.setImageBitmap(photoBitmap);
            imagePreview.setVisibility(ImageView.VISIBLE);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }


    private void submitViolation() {
        String vehicleNumber = etVehicleNumber.getText().toString().trim();
        String driverName = etDriverName.getText().toString().trim();
        String licenseNumber = etLicenseNumber.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String fineAmount = etFineAmount.getText().toString().trim();
        String violationType = spinnerViolationType.getSelectedItem().toString();
        String vehicleStatus = spinnerVehicleStatus.getSelectedItem().toString();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        String id = UUID.randomUUID().toString();

        try {
            JSONObject violationData = new JSONObject();
            violationData.put("vehicleNumber", vehicleNumber);
            violationData.put("driverName", driverName);
            violationData.put("licenseNumber", licenseNumber);
            violationData.put("description", description);
            violationData.put("fineAmount", fineAmount);
            violationData.put("violationType", violationType);
            violationData.put("vehicleStatus", vehicleStatus);
            violationData.put("latitude", latitude);
            violationData.put("longitude", longitude);
            violationData.put("timestamp", timestamp);
            violationData.put("officerId", "INSPECTOR_001");

            if (isOnline()) {
                // Directly upload to Firebase
                Map<String, Object> violationDataMap = new HashMap<>();
                violationDataMap.put("vehicleNumber", vehicleNumber);
                violationDataMap.put("driverName", driverName);
                violationDataMap.put("licenseNumber", licenseNumber);
                violationDataMap.put("description", description);
                violationDataMap.put("fineAmount", fineAmount);
                violationDataMap.put("violationType", violationType);
                violationDataMap.put("vehicleStatus", vehicleStatus);
                violationDataMap.put("latitude", latitude);
                violationDataMap.put("longitude", longitude);
                violationDataMap.put("timestamp", timestamp);
                violationDataMap.put("officerId", "INSPECTOR_001");

                violationsRef.child(id).setValue(violationDataMap);
                Toast.makeText(this, "Violation synced to Firebase", Toast.LENGTH_SHORT).show();
            } else {
                // Save locally
                File dir = new File(getFilesDir(), "offline_violations");
                if (!dir.exists()) dir.mkdir();
                File file = new File(dir, id + ".json");
                FileWriter writer = new FileWriter(file);
                writer.write(violationData.toString());
                writer.close();
                Toast.makeText(this, "Saved locally for sync", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openSyncActivity() {
        Intent intent = new Intent(MainActivity.this, SyncStatusActivity.class);
        startActivity(intent);
    }

}
