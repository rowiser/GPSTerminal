package com.rowiser.android.gps.gpsterminal.bean;

import com.google.gson.Gson;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by likai on 7/5/16.
 */
public class LocationInfo implements Parcelable {

    /**
     * 经度
     */
    public double longitude;
    /**
     * 纬度
     */
    public double latitude;
    /**
     * 时间
     */
    public String dateTime = "";
    /**
     * 模式
     */
    public String module = "";
    /**
     * 地址
     */
    public String address = "";
    /**
     * 收星数
     */
    public int satelliteCount = 0;

    public LocationInfo() {

    }

    protected LocationInfo(Parcel in) {
        longitude = in.readDouble();
        latitude = in.readDouble();
        dateTime = in.readString();
        module = in.readString();
        address = in.readString();
        satelliteCount = in.readInt();
    }

    public static final Creator<LocationInfo> CREATOR = new Creator<LocationInfo>() {
        @Override
        public LocationInfo createFromParcel(Parcel in) {
            return new LocationInfo(in);
        }

        @Override
        public LocationInfo[] newArray(int size) {
            return new LocationInfo[size];
        }

    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(longitude);
        dest.writeDouble(latitude);
        dest.writeString(dateTime);
        dest.writeString(module);
        dest.writeString(address);
        dest.writeInt(satelliteCount);
    }

    public LocationInfo readFromParcel(Parcel in) {
        return new LocationInfo(in);
    }

    @Override
    public String toString() {
    	return new Gson().toJson(this);
    }
    
}
