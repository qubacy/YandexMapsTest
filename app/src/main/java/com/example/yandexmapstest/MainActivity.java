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
import com.google.android.material.slider.Slider;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;

public class MainActivity extends AppCompatActivity {
    private final int C_FINE_LOCATION_PERMISSION_REQUEST_CODE = 0;
    private final int C_COARSE_LOCATION_PERMISSION_REQUEST_CODE = 1;

    private final float C_LOCATION_CIRCLE_STROKE_WIDTH = 2f;
    private final float C_VIEW_CIRCLE_MULTIPLIER = 1.2f;
    private final float C_CAMERA_MOVEMENT_ANIMATION_DURATION = 1;

    private MapView m_mapView = null;
    private Slider m_radiusSlider = null;

    private FusedLocationProviderClient m_fusedLocationProviderClient = null;
    private CancellationTokenSource m_cancellationTokenSource = null;

    private Point m_locationPoint = null;
    private CircleMapObject m_curLocationCircle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_cancellationTokenSource = new CancellationTokenSource();

        setContentView(R.layout.activity_main);

        m_mapView = findViewById(R.id.map);
        m_radiusSlider = findViewById(R.id.location_radius);

        m_radiusSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                onLocationRadiusChanged(value);
            }
        });

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

                    m_locationPoint = new Point(location.getLatitude(), location.getLongitude());

                    initMapLocation();

                } else {
                    Log.d(getClass().getName(), "Exception: " + task.getException().getMessage());

                    // todo: doing something..
                }
            }
        });
    }

    private void initMapLocation() {
        onLocationRadiusChanged(getResources().getInteger(R.integer.min_location_radius));
    }

    private void drawCurLocationCircle(
            final Map map,
            final float radius)
    {
        if (map == null || radius <= 0) return;

        Circle locationCircle = new Circle(m_locationPoint, radius);

        if (m_curLocationCircle != null)
            map.getMapObjects().remove(m_curLocationCircle);

        m_curLocationCircle =
                map.getMapObjects().addCircle(
                        locationCircle,
                        ContextCompat.getColor(this, R.color.red),
                        C_LOCATION_CIRCLE_STROKE_WIDTH,
                        ContextCompat.getColor(this, R.color.red_alpha));
    }

    private void changeCameraPosition(
            final Map map,
            final Point locationPoint,
            final float radius)
    {
        if (map == null || locationPoint == null || radius <= 0) return;

        Circle viewCircle =
                new Circle(
                        m_curLocationCircle.getGeometry().getCenter(),
                        m_curLocationCircle.getGeometry().getRadius() * C_VIEW_CIRCLE_MULTIPLIER);

        CameraPosition cameraPosition =
                map.cameraPosition(
                        Geometry.fromCircle(viewCircle), null, null, null);

        map.
                move(cameraPosition,
                        new Animation(
                                Animation.Type.SMOOTH, C_CAMERA_MOVEMENT_ANIMATION_DURATION),
                        null);
    }

    private void onLocationRadiusChanged(final float radius) {
        Map map = m_mapView.getMap();

        drawCurLocationCircle(map, radius);
        changeCameraPosition(map, m_locationPoint, radius);
    }
}