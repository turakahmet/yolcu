package com.myapp.yolcu.ui.pusula;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Service.LocationService;
import com.myapp.yolcu.ui.Service.SoundService;

import java.util.Objects;

public class QiblaCompassFragment extends Fragment implements SensorEventListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private TextView degreeTextView, degreeAdressDetailTextView; // Dereceyi göstermek için TextView
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;


    private float currentAzimuth = 0f;
    private ImageView compassImageView;
    private float azimuth = 0f;

    // Kıble için koordinatlar (Mekke)
    private static final double MECCA_LAT = 21.4225;
    private static final double MECCA_LON = 39.8262;

    //    private FusedLocationProviderClient fusedLocationProviderClient;
    private double userLatitude = 0;
    private double userLongitude = 0;
    private Animation animation;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private LocationService locationService;
    private SoundService soundService;
    private Address addressDetail;
    private ProgressBar loadingLocationCompass;
    private QiblaDirectionView qiblaDirectionView;
    private Button compassInfoButton;
    private Dialog mDialog;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compass, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        initializeServices();
    }

    private void initializeServices() {
        soundService = new SoundService(requireContext());
        soundService.commonButtonEffect();
    }

    private void initializeLocationService() {
        locationService = LocationService.getInstance(requireContext());
        locationService.checkLocationPermission(requireActivity(), () -> {
            if (locationService.isLocationEnabled()) {
                locationService.getCurrentLocation(new LocationService.OnLocationRetrieved() {
                    @Override
                    public void onSuccess(double latitude, double longitude, Address address) {
                        userLatitude = latitude;
                        userLongitude = longitude;
                        addressDetail = address;
                        loadingLocationCompass.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(getContext(), getString(R.string.location_fail) + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Konum servisi kapalıysa, kullanıcıyı ayarlara yönlendirin.
                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.location_service_closed_header))
                        .setMessage(getString(R.string.location_service_closed_message))
                        .setPositiveButton(getString(R.string.evet), (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        })
                        .setNegativeButton(getString(R.string.hayir), (dialog, which) -> {
                            navigateToPreviousScreen();
                        })
                        .setCancelable(false)
                        .show();
            }
        }, () -> {
            // Konum izni verilmediyse, izin istemek için kullanıcıyı bilgilendirin.
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.location_permission_required_header))
                    .setMessage(getString(R.string.location_permission_required_message))
                    .setPositiveButton(getString(R.string.tamam), (dialog, which) -> {
                        ActivityCompat.requestPermissions(requireActivity(),
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_PERMISSION_REQUEST_CODE);
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    private void initializeViews(View view) {
        compassImageView = view.findViewById(R.id.compassImageView);
        degreeAdressDetailTextView = view.findViewById(R.id.degreeAdressDetailTextView);
        degreeTextView = view.findViewById(R.id.degreeTextView);
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        loadingLocationCompass = view.findViewById(R.id.loadingLocationCompass);
        qiblaDirectionView = view.findViewById(R.id.qiblaDirectionView);
        compassInfoButton = view.findViewById(R.id.compassInfoButton);
        compassInfoButton.setOnClickListener(view1 -> showInfoPopup());
    }

    private void showInfoPopup() {
        // Yeni bir Dialog oluştur
        mDialog = new Dialog(requireContext());
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // custom_popup.xml layoutunu dialog'a yükle
        mDialog.setContentView(R.layout.compass_info_popup);
        // Dialog dışına tıklanıldığında kapansın
        mDialog.setCancelable(true);
        Button closeButton = mDialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeLocationService();
        if (sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Sensör verilerini alıyoruz
        final float alpha = 0.97f;
        synchronized (this) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * sensorEvent.values[2];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * sensorEvent.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * sensorEvent.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * sensorEvent.values[2];
            }
            float[] RI = new float[9];
            float[] I = new float[9];
            boolean succcess = SensorManager.getRotationMatrix(RI, I, mGravity, mGeomagnetic);
            if (succcess) {
                float[] orientation = new float[3]; // Cihazın yönelim verilerini al
                SensorManager.getOrientation(RI, orientation);

                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360; // 0-360 derece arasında

                // Kıble yönünü hesaplıyoruz
                double qiblaDegree = calculateQiblaDirection(userLatitude, userLongitude);
                qiblaDirectionView.updateDegrees(azimuth, (float) qiblaDegree); //

                updateCompassSmoothly(); // Compass okunu güncelle
                degreeTextView.setText(String.format("%.0f°", azimuth));
                // Dereceyi TextView'de göster
                if (addressDetail != null) {
                    String addressInfo = "\n" + (addressDetail.getSubAdminArea() != null ? addressDetail.getSubAdminArea() + " " : "") +
                            (addressDetail.getSubLocality() != null ? addressDetail.getSubLocality() + "\n" : "") +
                            (addressDetail.getAdminArea() != null ? addressDetail.getAdminArea() + ", " : "") +
                            (addressDetail.getCountryName() != null ? addressDetail.getCountryName() : "");
                    degreeAdressDetailTextView.setText(String.format("Kıble Açısı: %.0f°", qiblaDegree) + addressInfo);
                    qiblaDirectionView.setVisibility(View.VISIBLE);

                }

                float threshold = 5f;

                // Azimut ve kıble açısını karşılaştır
                if (Math.abs(azimuth - qiblaDegree) < threshold) {
                    degreeTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                } else {
                    degreeTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.compassTextColor));
                }
            }
        }
    }


    private void updateCompassSmoothly() {
        // Ok animasyonunu uygula
        animation = new RotateAnimation(-currentAzimuth, -azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);


        // Yavaş dönüş sağlamak için animasyon süresini artır
        animation.setDuration(500); // Animasyon süresi 2 saniye
        animation.setFillAfter(true);
        animation.setRepeatCount(0);

        // Ok simgesini döndür
        currentAzimuth = azimuth;
        compassImageView.startAnimation(animation);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Gerekli değil
    }


    // Kıble yönünü hesaplamak için Haversine algoritmasını kullanıyoruz
    private double calculateQiblaDirection(double userLat, double userLon) {
        double deltaLon = Math.toRadians(MECCA_LON - userLon);
        double lat1 = Math.toRadians(userLat);
        double lat2 = Math.toRadians(MECCA_LAT);

        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);

        double qiblaDirection = Math.toDegrees(Math.atan2(y, x));
        qiblaDirection = (qiblaDirection + 360) % 360; // 0-360 derece arasında tutuyoruz
        return qiblaDirection;
    }

    private void navigateToPreviousScreen() {
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
    }
}
