package com.global.globalapp.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.global.globalapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class ActiveFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private MapView mapView;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private static final int MULTIPLE_PERMISSION_REQUEST_CODE = 4;

    public ActiveFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_active, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkPermissionsState();
    }

    private void setupMap() {

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mapView = (MapView) getView().findViewById(R.id.mapview);
        mapView.setClickable(true);

        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getActivity().getApplication().getPackageName());

        mapView.getController().setZoom(16);
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);

        MyLocationNewOverlay oMapLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), mapView);

        Drawable currentDraw = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_location, null);
        Bitmap currentIcon = null;
        if (currentDraw != null) {
            currentIcon = ((BitmapDrawable) currentDraw).getBitmap();
        }

        oMapLocationOverlay.enableFollowLocation();
        oMapLocationOverlay.enableMyLocation();


        oMapLocationOverlay.setPersonIcon(currentIcon);
        mapView.getOverlays().add(oMapLocationOverlay);

        CompassOverlay compassOverlay = new CompassOverlay(getContext(), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        mapView.setMapListener(new DelayedMapListener(new MapListener() {
            public boolean onZoom(final ZoomEvent e) {
                return true;
            }

            public boolean onScroll(final ScrollEvent e) {
                return true;
            }
        }, 1000));
    }

    private void setCenterInMyCurrentLocation() {
        if (mLastLocation != null) {
            mapView.getController().setCenter(new GeoPoint(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

        } else {
            Toast.makeText(getContext(), getString(R.string.msg_location_tracking), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task locationTask = LocationServices.getFusedLocationProviderClient(getContext()).getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>()  {
            @Override
            public void onSuccess(Location location) {
                mLastLocation = location;
                setCenterInMyCurrentLocation();
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    private void checkPermissionsState() {

        int writeExternalStoragePermissionCheck = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        int coarseLocationPermissionCheck = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION);

        int fineLocationPermissionCheck = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);


        if (writeExternalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                fineLocationPermissionCheck == PackageManager.PERMISSION_GRANTED ) {

            setupMap();

        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MULTIPLE_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean somePermissionWasDenied = false;
                    for (int result : grantResults) {
                        if (result == PackageManager.PERMISSION_DENIED) {
                            somePermissionWasDenied = true;
                        }
                    }
                    if (somePermissionWasDenied) {
                        Toast.makeText(getContext(), getString(R.string.error_permisson_required), Toast.LENGTH_SHORT).show();
                    } else {
                        setupMap();
                    }
                } else {
                    Toast.makeText(getContext(), getString(R.string.error_permisson_required), Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    @Override
    public void onStart() {
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
            if (mGoogleApiClient != null)
                mGoogleApiClient.disconnect();
        super.onStop();
    }
}