package com.islamyolu.app.Service;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

public class LocationHelper {

    public interface CityCallback {
        void onCityFound(String cityName);
    }

    public static void getCurrentCityName(Activity activity, CityCallback callback) {
        // 1. Konum servisi açık mı? (GPS veya Ağ)
        if (!isLocationEnabled(activity)) {
            Toast.makeText(activity, "Lütfen konum servisini açın", Toast.LENGTH_LONG).show();
            // İsteğe bağlı: Kullanıcıyı ayarlara yönlendirme
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(intent);
            callback.onCityFound("Konum kapalı");
            return;
        }

        // 2. Konum izni var mı?
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            callback.onCityFound("İzin verilmedi");
            return;
        }

        // 3. Güncel konum isteği
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setWaitForAccurateLocation(true)
                .setMaxUpdates(1)
                .build();


        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                Location location = locationResult.getLastLocation();

                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(activity, new Locale("tr"));
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            String cityName = addresses.get(0).getAdminArea();
                            callback.onCityFound(cityName);
                        } else {
                            callback.onCityFound("Şehir bulunamadı");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onCityFound("Konum hatası");
                    }
                } else {
                    callback.onCityFound("Konum alınamadı");
                }
            }
        }, Looper.getMainLooper());
    }

    // Konum servisleri açık mı kontrol eder
    public static boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        );
    }
}
