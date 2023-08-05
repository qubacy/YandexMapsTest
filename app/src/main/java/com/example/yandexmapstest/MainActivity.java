package com.example.yandexmapstest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;

public class MainActivity extends AppCompatActivity {
    private final int C_FINE_LOCATION_PERMISSION_REQUEST_CODE = 0;
    private final int C_COARSE_LOCATION_PERMISSION_REQUEST_CODE = 1;

    private final float C_LOCATION_CIRCLE_RADIUS = 400f;
    private final float C_LOCATION_CIRCLE_STROKE_WIDTH = 2f;

    private MapView m_mapView = null;

    private FusedLocationProviderClient m_fusedLocationProviderClient = null;
    private CancellationTokenSource m_cancellationTokenSource = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_cancellationTokenSource = new CancellationTokenSource();

        setContentView(R.layout.activity_main);

        m_mapView = findViewById(R.id.map);

        processAvailability();
    }

    private void processAvailability() {
        m_fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(this);

        if (!checkLocationPermissions()) {
            requestFineLocationPermission();

            return;
        }

        gripLocation();
    }

    private void requestFineLocationPermission() {
        requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                C_FINE_LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void requestCoarseLocationPermission() {
        requestPermissions(
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                C_COARSE_LOCATION_PERMISSION_REQUEST_CODE);
    }

    private boolean checkLocationPermissions() {
        return (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onStart() {
        super.onStart();

        MapKitFactory.getInstance().onStart();
        m_mapView.onStart();
    }

    @Override
    protected void onStop() {
        m_cancellationTokenSource.cancel();
        m_mapView.onStop();
        MapKitFactory.getInstance().onStop();

        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode,
            @NonNull final String[] permissions,
            @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissions.length <= 0 || grantResults.length <= 0) return;

        switch (requestCode) {
            case C_FINE_LOCATION_PERMISSION_REQUEST_CODE: {
                onFineLocationRequestPermissionsResult(permissions, grantResults);

                break;
            }
            case C_COARSE_LOCATION_PERMISSION_REQUEST_CODE: {
                onCoarseLocationRequestPermissionsResult(permissions, grantResults);

                break;
            }
        }
    }

    private void onFineLocationRequestPermissionsResult(
            final String[] permissions,
            final int[] grantResults)
    {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            gripLocation();

            return;
        }

        Log.d(getClass().getName(), "Fine geolocation permission hasn't been granted!");

        requestCoarseLocationPermission();
    }

    private void onCoarseLocationRequestPermissionsResult(
            final String[] permissions,
            final int[] grantResults)
    {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            gripLocation();

            return;
        }

        Log.d(getClass().getName(), "Coarse geolocation permission hasn't been granted!");

        // todo: doing something..
    }

    private void gripLocation() {
        @SuppressLint("MissingPermission") Task<Location> curLocationTask =
                m_fusedLocationProviderClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        m_cancellationTokenSource.getToken());

        curLocationTask.addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull final Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();

                    if (location == null) {
                        Log.d(getClass().getName(), "Location was null!");

                        // todo: doing something..

                        return;
                    }

                    StringBuilder locationStringBuilder =
                            new StringBuilder("Latitude: ");

                    locationStringBuilder.append(String.valueOf(location.getLatitude()));
                    locationStringBuilder.append("; Longitude: ");
                    locationStringBuilder.append(String.valueOf(location.getLongitude()));
                    locationStringBuilder.append(";");

                    Log.d("TEST", locationStringBuilder.toString());

                    designateMapLocation(location);

                } else {
                    Log.d(getClass().getName(), "Exception: " + task.getException().getMessage());

                    // todo: doing something..
                }
            }
        });
    }

    private void designateMapLocation(final Location location) {
        Point locationPoint = new Point(location.getLatitude(), location.getLongitude());
        Map map = m_mapView.getMap();

        map.
            move(new CameraPosition(
                        locationPoint,
                        14.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 5),
                null);

        Circle locationWithRadius = new Circle(locationPoint, C_LOCATION_CIRCLE_RADIUS);

        map.getMapObjects().addCircle(
                locationWithRadius,
                ContextCompat.getColor(this, R.color.red),
                C_LOCATION_CIRCLE_STROKE_WIDTH,
                ContextCompat.getColor(this, R.color.red_alpha));
    }
}