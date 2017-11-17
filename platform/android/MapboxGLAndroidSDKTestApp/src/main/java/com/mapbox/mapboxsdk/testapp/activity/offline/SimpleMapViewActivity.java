package com.mapbox.mapboxsdk.testapp.activity.offline;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.testapp.R;

/**
 * The most basic example of adding a map to an activity.
 */
public class SimpleMapViewActivity extends AppCompatActivity {

  private static final String TAG = "TEST-OFFLINE";

  public static final String BOUNDS_ARG = "BOUNDS";
  public static final String STYLE_ARG = "STYLE";

  private MapView mapView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // This contains the MapView in XML and needs to be called after the access token is configured.
    // Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
    setContentView(R.layout.activity_offline_simple_map);

    mapView = (MapView) findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(MapboxMap mapboxMap) {

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
          String styleUrl = bundle.getString(STYLE_ARG);
          Log.d(TAG, " >>>> StyleURl: =" + styleUrl);
          if (styleUrl != null) {
            mapboxMap.setStyleUrl(styleUrl);
          }

          LatLngBounds latLngBounds = (LatLngBounds) bundle.getParcelable(BOUNDS_ARG);
          if (latLngBounds != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
              .target(latLngBounds.getCenter())
              .zoom(11)
              .build();
            mapboxMap.setCameraPosition(cameraPosition);
            // mapboxMap.setLatLngBoundsForCameraTarget(latLngBounds);

            Log.d(TAG, " >>>> BOUNDS: =" + latLngBounds);
            Log.d(TAG, " >>>> cameraPos: =" + cameraPosition);
          }
        }

      }
    });
  }

  // Add the mapView lifecycle to the activity's lifecycle methods
  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}