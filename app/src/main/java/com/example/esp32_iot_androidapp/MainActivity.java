package com.example.esp32_iot_androidapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.esp32_iot_androidapp.databinding.ActivityMainBinding;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private static final int BLE_PERMISSIONS_REQUEST_CODE = 0x55; // Could be any other positive integer value
    private int permissionsCount;
    public static String BleDeviceAddress = "NA";
    public static String BleDeviceName = "NA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_measure, R.id.nav_scanner)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        checkBlePermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void checkBlePermissions() {
        String[] missingPermissions = getMissingBlePermissions();
        if(missingPermissions == null || missingPermissions.length == 0) {
            System.out.println("checkBlePermissions: Permissions is already granted");
            return;
        }

        for(String perm : missingPermissions) {
            System.out.println("checkBlePermissions: missing permissions " + perm);
            permissionsCount = missingPermissions.length;

            requestPermissions(missingPermissions, BLE_PERMISSIONS_REQUEST_CODE);
        }
    }
    private String[] getMissingBlePermissions() {
        String[] missingPermissions = null;

        String locationPermission = getMissingLocationPermission();
        // For Android 12 and above
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN)  != PackageManager.PERMISSION_GRANTED) {
                missingPermissions = new String[1];
                missingPermissions[0] = android.Manifest.permission.BLUETOOTH_SCAN;
            }

            if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (missingPermissions == null) {
                    missingPermissions = new String[1];
                    missingPermissions[0] = android.Manifest.permission.BLUETOOTH_CONNECT;
                } else {
                    missingPermissions = Arrays.copyOf(missingPermissions, missingPermissions.length + 1);
                    missingPermissions[missingPermissions.length-1] = android.Manifest.permission.BLUETOOTH_CONNECT;
                }
            }
            if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                if (missingPermissions == null) {
                    missingPermissions = new String[1];
                    missingPermissions[0] = android.Manifest.permission.BLUETOOTH_ADMIN;
                } else {
                    missingPermissions = Arrays.copyOf(missingPermissions, missingPermissions.length + 1);
                    missingPermissions[missingPermissions.length-1] = android.Manifest.permission.BLUETOOTH_ADMIN;
                }
            }

            if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (missingPermissions == null) {
                    missingPermissions = new String[1];
                    missingPermissions[0] = android.Manifest.permission.ACCESS_FINE_LOCATION;
                } else {
                    missingPermissions = Arrays.copyOf(missingPermissions, missingPermissions.length + 1);
                    missingPermissions[missingPermissions.length-1] = android.Manifest.permission.ACCESS_FINE_LOCATION;
                }
            }

            if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (missingPermissions == null) {
                    missingPermissions = new String[1];
                    missingPermissions[0] = android.Manifest.permission.ACCESS_COARSE_LOCATION;
                } else {
                    missingPermissions = Arrays.copyOf(missingPermissions, missingPermissions.length + 1);
                    missingPermissions[missingPermissions.length-1] = android.Manifest.permission.ACCESS_COARSE_LOCATION;
                }
            }

        }
        else if(!hasLocationPermission(locationPermission)) {
            missingPermissions = new String[1];
            missingPermissions[0] = getMissingLocationPermission();
        }
        return missingPermissions;
    }
    private String getMissingLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // COARSE is needed for Android 6 to Android 10
            return android.Manifest.permission.ACCESS_COARSE_LOCATION;
        } else {
            // FINE is needed for Android 10 and above
            return Manifest.permission.ACCESS_FINE_LOCATION;
        }
        // No location permission is needed for Android 6 and below
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == BLE_PERMISSIONS_REQUEST_CODE) {
            int index = 0;
            for(int result: grantResults) {
                if(result == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permission granted for "+permissions[index]);
                    if(permissionsCount > 0) permissionsCount--;
                    if(permissionsCount == 0) {
                        // All permissions have been granted from user.
                        // Here you can notify other parts of the app ie. using a custom callback or a viewmodel so on.
                    }
                } else {
                    System.out.println("Permission denied for "+permissions[index]);
                    // TODO handle user denial i.e. show an informing dialog
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private boolean hasLocationPermission() {
        String missingLocationPermission = getMissingLocationPermission();
        if(missingLocationPermission == null) return true; // No permissions needed
        return ContextCompat.checkSelfPermission(getApplicationContext(), missingLocationPermission) ==
                PackageManager.PERMISSION_GRANTED;
    }
    private boolean hasLocationPermission(String locPermission) {
        if(locPermission == null) return true; // An Android version that doesn't need a location permission
        return ContextCompat.checkSelfPermission(getApplicationContext(), locPermission) ==
                PackageManager.PERMISSION_GRANTED;
    }
}