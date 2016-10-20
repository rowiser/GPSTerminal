// IGpsTerminalFeature.aidl
package com.rowiser.android.gps.gpsterminal;

// Declare any non-default types here with import statements
import com.rowiser.android.gps.gpsterminal.bean.SettingInfo;
import com.rowiser.android.gps.gpsterminal.bean.LocationInfo;
import com.rowiser.android.gps.gpsterminal.IGPSTerminalCallback;
interface IGpsTerminalFeature {

    void registCallback(IGPSTerminalCallback aCallback);

    void unRegistCallback(IGPSTerminalCallback aCallback);

    void updateSettings(in SettingInfo aSettingInfo);

    SettingInfo getSettings();

    LocationInfo getLocationInfo();

    void stopUploadData();

    String getDeviceId();



}
