package com.mapbox.mapboxsdk.testapp.activity.offline;

import android.util.Log;
import android.util.TimeUtils;

import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Created by osanababayan on 11/16/17.
 */

public class Region {


  // JSON encoding/decoding
  public static final String JSON_CHARSET = "UTF-8";
  public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

  private final String name;
  private final OfflineRegion offlineRegion;
  private OfflineRegionStatus lastReportedStatus = null;

  public Region(OfflineRegion offlineRegion) {
    this.name = getRegionNameFromMetaData(offlineRegion);
    this.offlineRegion = offlineRegion;
  }

  public Region(String name, OfflineRegion offlineRegion) {
    this.name = name;
    this.offlineRegion = offlineRegion;
  }

  public String getName() {
    return name;
  }

  public OfflineRegion getOfflineRegion() {
    return offlineRegion;
  }

  public OfflineRegionStatus getLastReportedStatus() {
    return lastReportedStatus;
  }

  public void setLastReportedStatus(OfflineRegionStatus lastReportedStatus) {
    this.lastReportedStatus = lastReportedStatus;
  }

  public static String  getRegionNameFromMetaData(OfflineRegion offlineRegion) {
    // Get the region name from the offline region metadata
    String regionName;

    try {
      byte[] metadata = offlineRegion.getMetadata();
      String json = new String(metadata, JSON_CHARSET);
      JSONObject jsonObject = new JSONObject(json);
      regionName = jsonObject.getString(JSON_FIELD_REGION_NAME);
    } catch (Exception exception) {
      Log.e(">>>", "Failed to decode metadata: " + exception.getMessage());
      regionName = "Region id:" +offlineRegion.getID();
    }
    return regionName;
  }

  public static byte[] createMetaData(String regionName) {
    // Build a JSONObject using the user-defined offline region title,
    // convert it into string, and use it to create a metadata variable.
    // The metadata variable will later be passed to createOfflineRegion()
    byte[] metadata;
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(JSON_FIELD_REGION_NAME, regionName);
      String json = jsonObject.toString();
      metadata = json.getBytes(JSON_CHARSET);
    } catch (Exception exception) {
      Log.e(">>>>>>", "Failed to encode metadata: " + exception.getMessage());
      metadata = null;

    }
    return metadata;
  }

  private long startTime = 0;
  void startMeasureDownload() {
    startTime = System.currentTimeMillis();
  }

  void stopMeasuringDownload() {
    Log.d("TEST-OFFLINE", " >>>>> It took " +
      TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime) + " minutes to load " + name + " map");
    startTime = 0;
  }
}
