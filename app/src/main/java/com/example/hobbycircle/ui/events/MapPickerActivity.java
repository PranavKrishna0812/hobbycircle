package com.example.hobbycircle.ui.events;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ImageButton;
import android.view.inputmethod.EditorInfo;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hobbycircle.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvCurrentAddress;
    private Button btnConfirmLocation;
    private EditText etSearchLocation;
    private ImageButton btnSearchLocation;
    private FloatingActionButton fabMyLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private LatLng selectedLatLng = null;
    private String selectedAddress = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        etSearchLocation = findViewById(R.id.etSearchLocation);
        btnSearchLocation = findViewById(R.id.btnSearchLocation);
        fabMyLocation = findViewById(R.id.fabMyLocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnSearchLocation.setOnClickListener(v -> performSearch());
        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        fabMyLocation.setOnClickListener(v -> checkLocationPermissionAndFetch());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("lat", selectedLatLng.latitude);
                resultIntent.putExtra("lng", selectedLatLng.longitude);
                resultIntent.putExtra("address", selectedAddress);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Default to a generic location
        LatLng defaultLocation = new LatLng(39.8283, -98.5795); // USA Center
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 4));

        checkLocationPermissionAndFetch();

        mMap.setOnCameraIdleListener(() -> {
            selectedLatLng = mMap.getCameraPosition().target;
            updateAddressFromLatLng(selectedLatLng);
        });
    }

    private void updateAddressFromLatLng(LatLng latLng) {
        tvCurrentAddress.setText("Loading address...");
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(MapPickerActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    final String addressText = address.getAddressLine(0);
                    selectedAddress = addressText;
                    runOnUiThread(() -> tvCurrentAddress.setText(addressText));
                } else {
                    selectedAddress = latLng.latitude + "," + latLng.longitude;
                    runOnUiThread(() -> tvCurrentAddress.setText("Unknown Location"));
                }
            } catch (Exception e) {
                selectedAddress = latLng.latitude + "," + latLng.longitude;
                runOnUiThread(() -> tvCurrentAddress.setText("Error resolving location"));
            }
        }).start();
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {}
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                checkLocationPermissionAndFetch();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void performSearch() {
        String query = etSearchLocation.getText().toString().trim();
        if (query.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(MapPickerActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    LatLng searchLatLng = new LatLng(address.getLatitude(), address.getLongitude());
                    runOnUiThread(() -> {
                        if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchLatLng, 12));
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MapPickerActivity.this, "Location not found", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MapPickerActivity.this, "Error searching location", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
