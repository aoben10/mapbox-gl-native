package com.mapbox.mapboxsdk.testapp.activity.offline;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.mapboxsdk.testapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by osanababayan on 11/17/17.
 */

public class OfflineRegionsListActivity extends AppCompatActivity {

  private static final String TAG = "TEST-OFFLINE";

  private static final String MAP_STYLE_URL = Style.MAPBOX_STREETS;
  private static final double MAP_MIN_ZOOM = 0;
  private static final double MAP_MAX_ZOOM = 15;
  private static final float MAP_PIXEL_DENSITY = 4;

  private List<Region> regions = new ArrayList<>();
  private ListView regionsList;
  private OfflineRegionListAdapter regionsAdapter;

  private OfflineManager offlineManager;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_offline_region_list);

    // Set up a list to hold available offline regions
    regionsAdapter = new OfflineRegionListAdapter(this, 0);
    regionsList = (ListView)findViewById(R.id.areas);
    regionsList.setAdapter(regionsAdapter);
    regionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        Region region = regionsAdapter.getItem(i);

        if (region != null) {
          OfflineRegionStatus status = region.getLastReportedStatus();
          if (status != null && !status.isComplete() && status.getDownloadState() != OfflineRegion.STATE_ACTIVE) {
            Log.d(TAG, "Region +" +region.getName()+ " clicked - start download");
            // Start measuring downlaod !
            region.startMeasureDownload();
            region.getOfflineRegion().setDownloadState(OfflineRegion.STATE_ACTIVE);

          } else if (status.isComplete()) {
            Log.d(TAG, "Region +" +region.getName()+ " clicked - download is complete -> show the region");

            Bundle bundle = new Bundle();
            OfflineRegion offlineRegion = region.getOfflineRegion();
            bundle.putParcelable(SimpleMapViewActivity.BOUNDS_ARG, offlineRegion.getDefinition().getBounds());
            bundle.putString(SimpleMapViewActivity.STYLE_ARG, MAP_STYLE_URL);

            Intent intent = new Intent(OfflineRegionsListActivity.this, SimpleMapViewActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);

          } else {
            Log.d(TAG, "Region +" +region.getName()+ " clicked - download is in progress");
          }
        }
      }
    });

    // Set up the offlineManager
    // This contains the MapView in XML and needs to be called after the access token is configured.
    // Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
    offlineManager = OfflineManager.getInstance(this);

    // Download available regions from the OfflineManager
    // If there are no regions available -> add default ones (Berlin and Hessen)
    offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
      @Override
      public void onList(OfflineRegion[] regions) {

        if (regions == null || regions.length == 0) {
          requestAddDefaultRegions();

        } else {
          for (final OfflineRegion region : regions) {
            addToList(new Region(region));
          }
        }
      }

      @Override
      public void onError(String error) {
        Log.d(TAG, " Error getting regions error=" +error);

      }
    });
  }

  @Override
  protected void onDestroy() {

    if (regions != null) {
      for (Region region : regions) {
        region.getOfflineRegion().setObserver(null);
      }
      regions.clear();
      regions = null;
    }
    if (regionsAdapter != null) {
      regionsAdapter.clear();
      regionsAdapter = null;
    }
    if (regionsList != null) {
      regionsList.setAdapter(null);
      regionsList = null;
    }
    super.onDestroy();
  }


  /**
   * Add a region to regions list and to the list adapter.
   * Start observing offline region's status.
   * When status changes list needs to be updated.
   * Given that getting status is an async operation, we will store last reported status in the Region instance
   */
  private void addToList(final Region newRegion) {

    regions.add(newRegion);
    regionsAdapter.add(newRegion);

    OfflineRegion offlineRegion = newRegion.getOfflineRegion();

    // Get observing offline region's status.
    offlineRegion.getStatus(new OfflineRegion.OfflineRegionStatusCallback() {
      @Override
      public void onStatus(OfflineRegionStatus status) {
        onOfflineRegionStatusChanged(newRegion, status);
      }

      @Override
      public void onError(String error) {

      }
    });

    //Start observing offline region's status
    newRegion.getOfflineRegion().setObserver(new OfflineRegion.OfflineRegionObserver() {
      @Override
      public void onStatusChanged(OfflineRegionStatus status) {
        onOfflineRegionStatusChanged(newRegion, status);

        // Stop measuring downlaod !
        if (status.isComplete()) {
          newRegion.stopMeasuringDownload();
        }
      }

      @Override
      public void onError(OfflineRegionError error) {
        Log.d(TAG, ">>>>> OfflineRegionError " + error);
      }

      @Override
      public void mapboxTileCountLimitExceeded(long limit) {
        Log.d(TAG, ">>>>> mapboxTileCountLimitExceeded " + limit);
      }
    });

  }

  /**
   * Request Region with given name and bounds to be added to Offline Map Regions
   * using default style, minZoom, maxZoom and pixelDensity.
   * @param regionName
   * @param bounds
   */
  private void requestOfflineRegionAdd(String regionName, LatLngBounds bounds) {
    requestOfflineRegionAdd(regionName, MAP_STYLE_URL, bounds, MAP_MIN_ZOOM, MAP_MAX_ZOOM, MAP_PIXEL_DENSITY);
  }


  /**
   * Request Region with given name, bounds, default style, minZoom, maxZoom and pixelDensity to be added
   * to Offline Map Regions.
   * @param regionName
   * @param styleUrl
   * @param bounds
   * @param minZoom
   * @param maxZoom
   * @param pixelDensity
   */
  private void requestOfflineRegionAdd(final String regionName, String styleUrl,
                                       LatLngBounds bounds, double minZoom , double maxZoom, float pixelDensity) {

    OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
      styleUrl, bounds, minZoom, maxZoom, pixelDensity);


    // Create the offline region and launch the download
    offlineManager.createOfflineRegion(definition, Region.createMetaData(regionName),
      new OfflineManager.CreateOfflineRegionCallback() {
        @Override
        public void onCreate(OfflineRegion offlineRegion) {
          Log.d(TAG, "Offline region created: " + regionName +
            " offlineRegion: id=" + offlineRegion.getID() + " defenition.bounds=" +offlineRegion.getDefinition().getBounds()+
            " nameFromMetadata=" +Region.getRegionNameFromMetaData(offlineRegion));
          addToList(new Region(regionName, offlineRegion));
        }

        @Override
        public void onError(String error) {
          Log.e(">>>>", "Error: " + error);
        }
      });
  }

  private void onOfflineRegionStatusChanged(Region region, OfflineRegionStatus status) {

    Log.d(TAG, String.format("STATUS CHANGED: Name=%s  %s - %s/%s resources; %s bytes downloaded.",
      region.getName(),
      (status.isComplete() ? " COMPLETE " :
        (status.getDownloadState() == OfflineRegion.STATE_ACTIVE) ? " ACTIVE " : " AVAILABLE"),
      String.valueOf(status.getCompletedResourceCount()),
      String.valueOf(status.getRequiredResourceCount()),
      String.valueOf(status.getCompletedResourceSize())));

    region.setLastReportedStatus(status);

    if (regionsAdapter != null) {
      regionsAdapter.notifyDataSetChanged();
    }
  }


  private void requestAddDefaultRegions() {

    LatLngBounds hessenBounds = new LatLngBounds.Builder()
      .include(new LatLng(51.3983488624, 9.0362548828))
      .include(new LatLng(51.0923105489, 8.5528564453))
      .build();
    //hessenBounds.union(LatLngBounds.from(51.3983488624, 9.0362548828, 51.0923105489, 8.5528564453));

    hessenBounds.union(LatLngBounds.from(51.52000000000001, 9.10499999999999, 51.399, 8.882000000000005));
    hessenBounds.union(LatLngBounds.from(51.399, 9.10499999999999, 51.16900000000001, 9.037000000000006));
    hessenBounds.union(LatLngBounds.from(51.16900000000001, 9.424000000000007, 51.119, 9.102000000000004));
    hessenBounds.union(LatLngBounds.from(51.52199999999999, 9.424000000000007, 51.52099999999999, 9.102000000000004));
    hessenBounds.union(LatLngBounds.from(51.52099999999999, 9.424000000000007, 51.16999999999999, 9.105999999999995));
    hessenBounds.union(LatLngBounds.from(51.65899999999999, 9.695999999999998, 51.522999999999996, 9.264999999999986));
    hessenBounds.union(LatLngBounds.from(51.522999999999996, 9.695999999999998, 51.22399999999999, 9.425000000000011));
    hessenBounds.union(LatLngBounds.from(51.22399999999999, 10.086000000000013, 51.17500000000001, 9.686000000000007));
    hessenBounds.union(LatLngBounds.from(51.42500000000001, 10.086000000000013, 51.224999999999994, 9.697000000000003));
    hessenBounds.union(LatLngBounds.from(51.22399999999999, 9.686000000000007, 50.95400000000001, 9.626000000000005));
    hessenBounds.union(LatLngBounds.from(51.17500000000001, 10.241000000000014, 50.95400000000001, 9.687000000000012));
    hessenBounds.union(LatLngBounds.from(51.260999999999996, 10.241000000000014, 51.17599999999999, 10.086999999999989));
    hessenBounds.union(LatLngBounds.from(51.010999999999996, 9.626000000000005, 50.67400000000001, 9.454000000000008));
    hessenBounds.union(LatLngBounds.from(50.95400000000001, 10.068999999999988, 50.67400000000001, 9.62700000000001));
    hessenBounds.union(LatLngBounds.from(50.67500000000001, 9.454000000000008, 50.39699999999999, 9.449999999999989));
    hessenBounds.union(LatLngBounds.from(50.67400000000001, 10.086000000000013, 50.39699999999999, 9.455000000000013));
    hessenBounds.union(LatLngBounds.from(51.09200000000001, 9.449999999999989, 50.21199999999999, 8.939999999999998));
    hessenBounds.union(LatLngBounds.from(51.16900000000001, 9.102000000000004, 51.09299999999999, 9.037000000000006));
    hessenBounds.union(LatLngBounds.from(51.119, 9.626000000000005, 51.09299999999999, 9.103000000000009));
    hessenBounds.union(LatLngBounds.from(51.22399999999999, 9.626000000000005, 51.120000000000005, 9.425000000000011));
    hessenBounds.union(LatLngBounds.from(50.39699999999999, 9.795999999999992, 50.21199999999999, 9.450999999999993));
    hessenBounds.union(LatLngBounds.from(51.09299999999999, 9.454000000000008, 50.67599999999999, 9.450999999999993));
    hessenBounds.union(LatLngBounds.from(51.09299999999999, 9.626000000000005, 51.012, 9.455000000000013));
    hessenBounds.union(LatLngBounds.from(50.345, 8.939999999999998, 50.078, 8.925999999999988));
    hessenBounds.union(LatLngBounds.from(50.21199999999999, 9.682999999999993, 50.078, 8.941000000000003));
    hessenBounds.union(LatLngBounds.from(51.20099999999999, 8.551999999999992, 50.837999999999994, 8.437999999999988));
    hessenBounds.union(LatLngBounds.from(51.09200000000001, 8.939999999999998, 50.837999999999994, 8.552999999999997));
    hessenBounds.union(LatLngBounds.from(50.88900000000001, 8.437999999999988, 50.531000000000006, 8.116000000000014));
    hessenBounds.union(LatLngBounds.from(50.837999999999994, 8.685000000000002, 50.531000000000006, 8.438999999999993));
    hessenBounds.union(LatLngBounds.from(50.56299999999999, 8.116000000000014, 50.202, 7.961000000000013));
    hessenBounds.union(LatLngBounds.from(50.531000000000006, 8.530000000000001, 50.202, 8.11699999999999));
    hessenBounds.union(LatLngBounds.from(50.27600000000001, 7.961000000000013, 49.91300000000001, 7.765999999999991));
    hessenBounds.union(LatLngBounds.from(50.202, 8.544999999999987, 49.91300000000001, 7.961999999999989));
    hessenBounds.union(LatLngBounds.from(50.27600000000001, 8.544999999999987, 50.203, 8.531000000000006));
    hessenBounds.union(LatLngBounds.from(49.91300000000001, 9.151999999999987, 49.52199999999999, 8.305000000000007));
    hessenBounds.union(LatLngBounds.from(50.94900000000001, 8.437999999999988, 50.889999999999986, 8.305000000000007));
    hessenBounds.union(LatLngBounds.from(50.531000000000006, 8.925999999999988, 50.27699999999999, 8.531000000000006));
    hessenBounds.union(LatLngBounds.from(50.27699999999999, 8.925999999999988, 49.91399999999999, 8.545999999999992));
    hessenBounds.union(LatLngBounds.from(50.837999999999994, 8.939999999999998, 50.53200000000001, 8.686000000000007));
    hessenBounds.union(LatLngBounds.from(50.078, 9.151999999999987, 49.91399999999999, 8.926999999999992));
    hessenBounds.union(LatLngBounds.from(50.53200000000001, 8.939999999999998, 50.346000000000004, 8.926999999999992));
    hessenBounds.union(LatLngBounds.from(49.52199999999999, 8.806000000000012, 49.497000000000014, 8.346000000000004));
    hessenBounds.union(LatLngBounds.from(49.497000000000014, 9.140999999999991, 49.387, 8.661000000000001));
    hessenBounds.union(LatLngBounds.from(49.52199999999999, 9.140999999999991, 49.49799999999999, 8.806999999999988));

    LatLngBounds berlinBounds = new LatLngBounds.Builder()
      .include(new LatLng(52.6780473464, 13.7603759766))
      .include(new LatLng(52.3305137868, 13.0627441406))
      .build();

    // float pixelRatio = this.getResources().getDisplayMetrics().density;
    requestOfflineRegionAdd("Hessen", hessenBounds);
    requestOfflineRegionAdd("Berlin", berlinBounds);

  };

}
