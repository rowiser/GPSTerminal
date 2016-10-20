package com.rowiser.android.gps.gpsterminal.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by likai on 7/5/16.
 */
public class SettingInfo implements Parcelable {

    /**
     * 远程主机名/ip
     */
    public String host = "";
    /**
     * 端口
     */
    public int port = 0;
    /**
     * 设备号
     */
    public String device = "";
    /**
     * 上传周期
     */
    public int uploadCycle = 0;

    public SettingInfo() {

    }

    public SettingInfo(String info){
        if(info != null){
            String[] infos = info.split(",");
            try {
                if(infos != null && infos.length >=4){
                    host = infos[0];
                    port = Integer.valueOf(infos[1]);
                    device = infos[2];
                    uploadCycle = Integer.valueOf(infos[3]);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    protected SettingInfo(Parcel in) {
        host = in.readString();
        port = in.readInt();
        device = in.readString();
        uploadCycle = in.readInt();
    }

    public static final Creator<SettingInfo> CREATOR = new Creator<SettingInfo>() {
        @Override
        public SettingInfo createFromParcel(Parcel in) {
            return new SettingInfo(in);
        }

        @Override
        public SettingInfo[] newArray(int size) {
            return new SettingInfo[size];
        }

    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(host);
        dest.writeInt(port);
        dest.writeString(device);
        dest.writeInt(uploadCycle);
    }

    public SettingInfo readFromParcel(Parcel in) {
        return new SettingInfo(in);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(host);
        stringBuilder.append(",");
        stringBuilder.append(port);
        stringBuilder.append(",");
        stringBuilder.append(device);
        stringBuilder.append(",");
        stringBuilder.append(uploadCycle);
        return stringBuilder.toString();
    }
}
