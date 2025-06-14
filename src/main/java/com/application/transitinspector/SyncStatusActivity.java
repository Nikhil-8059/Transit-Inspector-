package com.application.transitinspector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.transitinspector.ViolationAdapter;
import com.application.transitinspector.ViolationRecord;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class SyncStatusActivity extends AppCompatActivity {

    RecyclerView recyclerUnsynced;
    Button btnSyncNow;

    List<ViolationRecord> unsyncedList = new ArrayList<>();
    FirebaseDatabase db;
    File[] offlineFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_status);

        recyclerUnsynced = findViewById(R.id.recycler_unsynced);
        btnSyncNow = findViewById(R.id.btn_sync_now);
        db = FirebaseDatabase.getInstance();

        recyclerUnsynced.setLayoutManager(new LinearLayoutManager(this));
        loadOfflineData();

        btnSyncNow.setOnClickListener(v -> {
            if (isOnline()) {
                syncAllData();
            } else {
                Toast.makeText(this, "Not connected to internet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOfflineData() {
        unsyncedList.clear();

        File dir = new File(getFilesDir(), "offline_violations");
        if (!dir.exists()) {
            Toast.makeText(this, "No offline data found.", Toast.LENGTH_SHORT).show();
            return;
        }

        offlineFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (offlineFiles == null || offlineFiles.length == 0) {
            Toast.makeText(this, "All data synced!", Toast.LENGTH_SHORT).show();
            return;
        }

        for (File file : offlineFiles) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder jsonText = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) jsonText.append(line);
                reader.close();

                JSONObject obj = new JSONObject(jsonText.toString());

                unsyncedList.add(new ViolationRecord(
                        obj.getString("driverName"),
                        obj.getString("vehicleNumber"),
                        obj.getString("violationType"),
                        obj.getString("timestamp")
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ViolationAdapter adapter = new ViolationAdapter(unsyncedList);
        recyclerUnsynced.setAdapter(adapter);
    }

    private void syncAllData() {
        if (offlineFiles == null || offlineFiles.length == 0) return;

        for (File file : offlineFiles) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder jsonText = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) jsonText.append(line);
                reader.close();

                JSONObject obj = new JSONObject(jsonText.toString());
                String id = file.getName().replace(".json", "");

                // Convert JSONObject to Map<String, Object>
                Map<String, Object> violationMap = new HashMap<>();
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    violationMap.put(key, obj.get(key));
                }

                db.getReference("violations").child(id).setValue(violationMap);
                file.delete();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Toast.makeText(this, "All data synced to Firebase", Toast.LENGTH_SHORT).show();
        loadOfflineData(); // Refresh view
        loadOfflineData(); // Refresh view
    }


    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
