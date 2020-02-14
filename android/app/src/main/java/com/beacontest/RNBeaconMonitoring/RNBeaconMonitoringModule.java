package com.beacontest.RNBeaconMonitoring;


import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import androidx.annotation.Nullable;

import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;

import java.util.HashMap;
import java.util.Map;

public class RNBeaconMonitoringModule extends ReactContextBaseJavaModule implements BeaconConsumer {
    private static final String LOG_TAG = "RNBeaconMonitoring";
    private ReactApplicationContext mReactContext;
    private Context mApplicationContext;
    private BeaconManager mBeaconManager;

    public RNBeaconMonitoringModule(ReactApplicationContext reactContext) {
        super(reactContext);
        Log.d(LOG_TAG, "RNBeaconMonitoringModule - started");
        this.mReactContext = reactContext;
        this.mApplicationContext = reactContext.getApplicationContext();
        this.mBeaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);
        // Detect iBeacons ( http://stackoverflow.com/questions/25027983/is-this-the-correct-layout-to-detect-ibeacons-with-altbeacons-android-beacon-li )
        addParser("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24");
        mBeaconManager.bind(this);
    }

    @Override
    public String getName() {
        return LOG_TAG;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("SUPPORTED", BeaconTransmitter.SUPPORTED);
        constants.put("NOT_SUPPORTED_MIN_SDK", BeaconTransmitter.NOT_SUPPORTED_MIN_SDK);
        constants.put("NOT_SUPPORTED_BLE", BeaconTransmitter.NOT_SUPPORTED_BLE);
        constants.put("NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS", BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS);
        constants.put("NOT_SUPPORTED_CANNOT_GET_ADVERTISER", BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER);
        return constants;
    }

    @ReactMethod
    public void addParser(String parser) {
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(parser));
    }

    @ReactMethod
    public void removeParser(String parser) {
        mBeaconManager.getBeaconParsers().remove(new BeaconParser().setBeaconLayout(parser));
    }

    @ReactMethod
    public void setBackgroundScanPeriod(int period) {
        mBeaconManager.setBackgroundScanPeriod((long) period);
    }

    @ReactMethod
    public void setBackgroundBetweenScanPeriod(int period) {
        mBeaconManager.setBackgroundBetweenScanPeriod((long) period);
    }

    @ReactMethod
    public void setForegroundScanPeriod(int period) {
        mBeaconManager.setForegroundScanPeriod((long) period);
    }

    @ReactMethod
    public void setForegroundBetweenScanPeriod(int period) {
        mBeaconManager.setForegroundBetweenScanPeriod((long) period);
    }

    @ReactMethod
    public void checkTransmissionSupported(Callback callback) {
        int result = BeaconTransmitter.checkTransmissionSupported(mReactContext);
        callback.invoke(result);
    }

    @ReactMethod
    public void getMonitoredRegions(Callback callback) {
        WritableArray array = new WritableNativeArray();
        for (Region region : mBeaconManager.getMonitoredRegions()) {
            WritableMap map = new WritableNativeMap();
            map.putString("identifier", region.getUniqueId());
            map.putString("uuid", region.getId1().toString());
            map.putInt("major", region.getId2() != null ? region.getId2().toInt() : 0);
            map.putInt("minor", region.getId3() != null ? region.getId3().toInt() : 0);
            array.pushMap(map);
        }
        callback.invoke(array);
    }


    @Override
    public void onBeaconServiceConnect() {
        Log.v(LOG_TAG, "onBeaconServiceConnect");
        mBeaconManager.setMonitorNotifier(mMonitorNotifier);
    }

    @Override
    public Context getApplicationContext() {
        return mApplicationContext;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        mApplicationContext.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return mApplicationContext.bindService(intent, serviceConnection, i);
    }

    @ReactMethod
    public void startMonitoring(ReadableMap params, Promise promise) {
        Log.d(LOG_TAG, "startMonitoring, monitoringRegionId: " + params.getString("uuid") + ", monitoringBeaconUuid: " + params.getString("uuid") + ", minor: " + params.getInt("minor") + ", major: " + params.getInt("major"));
        try {
            Region region = createRegion(params.getString("uuid"), params.getString("uuid"), params.getInt("minor"), params.getInt("major"));
            mBeaconManager.startMonitoringBeaconsInRegion(region);
            promise.resolve(null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "startMonitoring, error: ", e);
            promise.reject(e.getMessage());
        }
    }

    private MonitorNotifier mMonitorNotifier = new MonitorNotifier() {
        @Override
        public void didEnterRegion(Region region) {
            Log.d(LOG_TAG, "didEnter" + region);
            sendEvent(mReactContext, "didEnter", createMonitoringResponse(region));
        }

        @Override
        public void didExitRegion(Region region) {
            Log.d(LOG_TAG, "didExit" + region);
            sendEvent(mReactContext, "didExit", createMonitoringResponse(region));
        }

        @Override
        public void didDetermineStateForRegion(int i, Region region) {
            Log.d(LOG_TAG, "didDetermineState" + region);
            sendEvent(mReactContext, "didDetermineState", createMonitoringResponse(region));
        }
    };

    private WritableMap createMonitoringResponse(Region region) {
        WritableMap map = new WritableNativeMap();
        map.putString("identifier", region.getUniqueId());
        map.putString("uuid", region.getId1().toString());
        map.putInt("major", region.getId2() != null ? region.getId2().toInt() : 0);
        map.putInt("minor", region.getId3() != null ? region.getId3().toInt() : 0);
        return map;
    }

    @ReactMethod
    public void stopMonitoring(ReadableMap params, Promise promise) {
        Region region = createRegion(params.getString("uuid"), params.getString("uuid"), params.getInt("minor"), params.getInt("major"));
        try {
            mBeaconManager.stopMonitoringBeaconsInRegion(region);
            promise.resolve(null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "stopMonitoring, error: ", e);
            promise.reject(e.getMessage());
        }
    }

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private Region createRegion(String regionId, String beaconUuid, int minor, int major) {
        Identifier id1 = (beaconUuid == null) ? null : Identifier.parse(beaconUuid);
        return new Region(regionId, id1, Identifier.fromInt(major), Identifier.fromInt(minor));
    }
}
