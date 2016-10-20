// IGPSTerminalCallback.aidl
package com.rowiser.android.gps.gpsterminal;

// Declare any non-default types here with import statements
import com.rowiser.android.gps.gpsterminal.bean.LocationInfo;
interface IGPSTerminalCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void finishActivity();

    void notifyLocationChange(in LocationInfo locationInfo);
}
