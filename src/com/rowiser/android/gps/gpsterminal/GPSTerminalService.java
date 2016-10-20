package com.rowiser.android.gps.gpsterminal;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.google.gson.Gson;
import com.rowiser.android.gps.gpsterminal.bean.LocationInfo;
import com.rowiser.android.gps.gpsterminal.bean.SettingInfo;
import com.rowiser.android.gps.gpsterminal.protocal.GPSTerminalProtocal;
import com.rowiser.android.gps.gpsterminal.protocal.ProtocalManager;
import com.rowiser.android.gps.gpsterminal.utils.JLog;
import com.rowiser.android.gps.gpsterminal.utils.JavaDecode;

/**
 * Created by likai on 7/4/16.
 */
public class GPSTerminalService extends Service {

    public final static boolean DEBUG = true;
    private final static String TAG = "GPSTerminalService";
    private final static JLog LOG = new JLog(TAG, DEBUG, JLog.TYPE_DEBUG);
    public final static String TERMINAL_SERVICE = "com.rowiser.android.gps.ACTION_GPS_TERMINAL_SERVICE";
    public final static String FILE_NAME = "gps_terminal_settings_info";
    public final static String KEY_SETTINGS_INFO = "key_settings_info";
    public final static String KEY_LOCATION_INFO = "key_location_info";
    private SharedPreferences mSharedPreferences;
    private ProtocalManager mProtocalManager;
    private GPSTerminalManager mGPSTerminalManager;
    private static LocationInfo mLocationInfo;
    private static int FAIL_LOCATION_COUNT = 0;
    private final static int MAX_FAIL_LOCATION_COUNT = 5;
    private static int FAIL_LOGIN_COUNT = 0;
    private final static int MAX_FAIL_LOGIN_COUNT = 10;

    private RemoteCallbackList<IGPSTerminalCallback> mCallbacks = new RemoteCallbackList<IGPSTerminalCallback>();

    @Override
    public void onCreate() {
        super.onCreate();
        LOG.print("onCreate...");
        registerScreenActionReceiver();
        mGPSTerminalManager = new GPSTerminalManager();
        mSharedPreferences = getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        updateUploadCycle();
        openGPSSettings();
        mProtocalManager = ProtocalManager.getInstance(getApplicationContext());
        getNormalLocation();
    }

    private void startUploadData() {
    	LOG.print("start...");
        SettingInfo settingInfo = getSettingInfo();
        if (settingInfo != null) {
            if (mProtocalManager != null) {
                mProtocalManager.setDataReceiveListener(mDataReceiveListener);
                LOG.print("is already stop ... " + mProtocalManager.isStop(), JLog.TYPE_ERROR);
                if (mProtocalManager.isStop()) {
                	mProtocalManager.onStop();
                	try {
						Thread.sleep(500);
					} catch (Exception e) {
						e.printStackTrace();
					}
                    mProtocalManager.onStart();
                    getNormalLocation();
                    startBaiDuLocation();
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        flags = START_STICKY;//停止后会自动启动
        startUploadData();
        mHandler.removeMessages(MSG_CHECK_DATA_IS_UPLOAD);
        mHandler.sendEmptyMessageDelayed(MSG_CHECK_DATA_IS_UPLOAD, 60 * 1000);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mGPSTerminalManager;
    }

    @Override
    public void onDestroy() {
        //停止上传数据的线程
        stop();
        if (mProtocalManager != null) {
            mProtocalManager.onDestroy();
        }
        if(receiver != null){
        	try{
        		unregisterReceiver(receiver);
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        }
        stopBaiDuLocation();
        mHandler.removeMessages(MSG_CHECK_DATA_IS_UPLOAD);
        FAIL_LOGIN_COUNT = 0;
        FAIL_LOCATION_COUNT = 0;
        super.onDestroy();
    }


    /**
     * 停止数据上传,并退出服务
     */
    private void stop() {
    	LOG.print("stop...");
    	finishActivity();
        if (mProtocalManager != null) {
            mProtocalManager.onStop();
        }
        stopBaiDuLocation();
        mHandler.removeMessages(MSG_UPDATE_LOCATION);
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                stopSelf();
//            }
//        }, 1000);
    }

    public boolean hasGPSDevice(Context context)
    {
        final LocationManager mgr = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        if ( mgr == null )
            return false;
        final List<String> providers = mgr.getAllProviders();
        if ( providers == null )
            return false;
        return providers.contains(LocationManager.GPS_PROVIDER);
    }
    
    private void openGPSSettings() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager == null){
        	return;
        }
        List<String> providers = locationManager.getAllProviders();
        if ( providers == null )
            return;
        if(!providers.contains(LocationManager.GPS_PROVIDER)){
        	return;
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent opentGpsSettingIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            opentGpsSettingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(opentGpsSettingIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public LocationClient mLocationClient = null;
    public LocationClientOption option;

    /**
     * 开启百度定位
     */
    private void startBaiDuLocation() {
        stopBaiDuLocation();
        mLocationClient = new LocationClient(this); // 声明LocationClient类
        mLocationClient.registerLocationListener(mLocationListener); // 注册监听函数
//        mLocationClient.registerNotify(mBDNotifyListener);
        option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置定位模式
        option.setCoorType("gcj02");// 返回的定位结果是国测局坐标系gcj02,默认值是百度经纬度bd09ll
        option.setScanSpan(tmpCycle * 1000);// 设置发起定位请求的间隔时间为:(cycle * 1000) ms
        option.setNeedDeviceDirect(true);//方向
        option.setIsNeedAddress(true);// 位置，一定要设置，否则后面得不到地址
        option.setOpenGps(true);// 打开GPS
        mLocationClient.setLocOption(option);
        mLocationClient.start();// 开启定位
        mLocationClient.requestLocation();
    }

    private void stopBaiDuLocation() {
        if (mLocationClient != null) {
            mLocationClient.unRegisterLocationListener(mLocationListener);
//                mLocationClient.removeNotifyEvent(mBDNotifyListener);
            mLocationClient.stop();
        }
    }

    double normalLongitude = 0;
    double normalLatitude = 0;
    private void getNormalLocation() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
                LocationManager locationManager = (LocationManager) GPSTerminalService.this.getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
                criteria.setAltitudeRequired(false);
                criteria.setBearingRequired(false);
                criteria.setCostAllowed(true);
                criteria.setPowerRequirement(Criteria.POWER_HIGH); //
                String provider = locationManager.getBestProvider(criteria, true); // 获取GPS信息
                if (ActivityCompat.checkSelfPermission(GPSTerminalService.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(GPSTerminalService.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    LOG.print("error , can not use gps....", JLog.TYPE_ERROR);
                    return;
                }
                Location location = locationManager.getLastKnownLocation(provider); // 通过GPS获取位置
                if(location != null) {
                    normalLongitude = location.getLongitude();
                    normalLatitude = location.getLatitude();
                }else{
                    LOG.print("error , location is null...", JLog.TYPE_ERROR);
                }
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
                        updateToNewLocation();
//                        
//                    }
//                });
                // 设置监听器，自动更新的最小时间为间隔N秒(1秒为1*1000，这样写主要为了方便)或最小位移变化超过N米
                locationManager.requestLocationUpdates(provider, 10 * 1000, 5, new LocationListener() {

                    @Override
                    public void onLocationChanged(Location location) {
                        if(location != null) {
                            normalLongitude = location.getLongitude();
                            normalLatitude = location.getLatitude();
                            if(tmpCycle > 60){
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateToNewLocation();
                                    }
                                });
                            }
                        }
                        LOG.print("------normalLatitude = " + normalLatitude + ", normalLongitude = " + normalLongitude);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                });
//            }
//        }).start();


    }

    private BDLocationListener mLocationListener = new BDLocationListener(){

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            location = bdLocation;
            isGspOk  = true;

            double latitude = bdLocation.getLatitude();
            double longitude = bdLocation.getLongitude();
            updateToNewLocation();
            int error = bdLocation.getLocType();
            LOG.print("------latitude = " + latitude + ", longitude = " + longitude + ", error = " + error);
        }
    };

//    private BDNotifyListener mBDNotifyListener = new BDNotifyListener() {
//        @Override
//        public void onNotify(BDLocation bdLocation, float v) {
//            double latitude = bdLocation.getLatitude();
//            double longitude = bdLocation.getLongitude();
//            int error = bdLocation.getLocType();
//            LOG.print("------latitude = " + latitude + ", longitude = " + longitude + ", v = " + v);
//        }
//    };

    public void getLBS(){
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = tm.getSubscriberId();
        if(imsi!=null){
            LOG.print("imsi = " + imsi);
            if(imsi.startsWith("46000") || imsi.startsWith("46002")){//因为移动网络编号46000下的IMSI已经用完，所以虚拟了一个46002编号，134/159号段使用了此编号
                //中国移动
                LOG.print("中国移动");
            }else if(imsi.startsWith("46001")){
                //中国联通
                LOG.print("中国联通");
            }else if(imsi.startsWith("46003")){
                //
                LOG.print("中国电信");
            }
        }
    }

    private boolean isGspOk = false;
//    private Location location;
    private BDLocation location;
    private int count = 0;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
    private void updateToNewLocation() {
//        TextView tv1 = (TextView) this.findViewById(R.id.tv);
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude= location.getLongitude();
            if(normalLatitude != 0 && normalLongitude != 0){
                longitude = normalLongitude;
                latitude = normalLatitude;
            }else{
            	LocationInfo locationInfo = getLocationInfo();
            	if(locationInfo != null){
            		latitude = locationInfo.latitude;
            		longitude = locationInfo.longitude;
            	}
            }
            float speed = location.getSpeed();
            if(cycle >= 10) {
                tmpCycle = cycle;
            }else{
                if (speed > 8) {
                    tmpCycle = 10;
                }else {
                    tmpCycle = 300;
                }
            }
            String time = location.getTime();
            // 精确度
//            float accuracy = location.getAccuracy();
            float accuracy = location.getRadius();
            //方位
//            float bearing = location.getBearing();
            float bearing = location.getDirection();
            //地址
            String address = location.getAddrStr();
            //收星个数
            int satelliteCount = location.getSatelliteNumber();
//            tv1.setText("纬度：" +  latitude+ "\n经度" + longitude + "\n速度:" + speed + "\n精度:" + accuracy + "\n方位:" + bearing + "\n收星:" + mSatelliteCount);
            LOG.print("纬度：" +  latitude+ "\n经度" + longitude + "\n速度:" + speed + "\n精度:" + accuracy + "\n方位:" + bearing + "\n收星:" + satelliteCount + "\n时间:" + time
                    + "\n地址:" + address, JLog.TYPE_INFO);

            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);
            if(isLogin) {
                byte[] gpsProtocal = GPSTerminalProtocal.generateNullProtocal(GPSTerminalProtocal.GPS_LBS_INFO, 26);
                int dataOffset = GPSTerminalProtocal.getDataOffset(gpsProtocal);
//                byte dataType = GPSTerminalProtocal.getDataType(gpsProtocal);
                //year
                gpsProtocal[dataOffset] = (byte)(year % 100);
                //month
                gpsProtocal[dataOffset + 1] = (byte)(month);
                //day
                gpsProtocal[dataOffset + 2] = (byte)(day);
                //hour
                gpsProtocal[dataOffset + 3] = (byte)(hour);
                //minute
                gpsProtocal[dataOffset + 4] = (byte)(minute);
                //second
                gpsProtocal[dataOffset + 5] = (byte)(second);
                //gps长度 + 卫星数
                gpsProtocal[dataOffset + 6] = (byte)(((byte)12 << 4) | (satelliteCount == -1 ? 0 : satelliteCount));

                boolean isSouth = false;//南纬??
                if(latitude <  0){//
                    isSouth = true;
                }
                boolean isEast = true;//东经??
                if(longitude < 0){
                    isEast = false;
                }

                int changeLat = change2protocalInt(latitude);// / 1 * 30000 * 60;
                JavaDecode.intToByteArr(gpsProtocal, dataOffset + 7, 4, changeLat, false);
                int changeLon = change2protocalInt(longitude);//(int)(longitude * 30000 * 60);
                LOG.print(changeLat + ", " + changeLon);
                JavaDecode.intToByteArr(gpsProtocal, dataOffset + 11, 4, changeLon, false);
                //速度
                gpsProtocal[dataOffset + 15] = (byte)speed;
                //状态/航向
                //先设置航向  占用两个字节中的,低10位,所以要先设置,然后状态再设置第16个数据字节就行了
                int heading = (int) (bearing < 0 ? 0 : bearing);//航向
                LOG.print(heading);
                JavaDecode.intToByteArr(gpsProtocal, dataOffset + 16, 2, heading, false);
                byte status = 0;
                if (!isSouth){// 北为1
                    status = (byte) (status | (1 << 2));
                }
                if (!isEast){//西为1
                    status = (byte) (status | (1 << 3));
                }
                if (isGspOk){
                    status = (byte) (status | (1 << 4));
                }
                gpsProtocal[dataOffset + 16] |= status;

                mProtocalManager.sendProtocal2Server(gpsProtocal);
                if(mLocationInfo == null){
                    mLocationInfo = new LocationInfo();
                }
                mLocationInfo.longitude = longitude;
                mLocationInfo.latitude = latitude;
                mLocationInfo.dateTime = format.format(calendar.getTime());
                mLocationInfo.module = String.valueOf(accuracy);
                mLocationInfo.address = address;
                mLocationInfo.satelliteCount = satelliteCount;
                saveLocationInfo(mLocationInfo);
                notifyLocationChange(mLocationInfo);
            }
            /*else{
            	FAIL_LOGIN_COUNT ++;
            	if(FAIL_LOGIN_COUNT == MAX_FAIL_LOGIN_COUNT){
            		stop();
	            	mHandler.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							startUploadData();
							LOG.print("login fail...");
						}
					}, 10 * 1000);
            	}else if(FAIL_LOGIN_COUNT > MAX_FAIL_LOGIN_COUNT + 5){//如果失败超过最大数的5次，把自己停掉
            		FAIL_LOGIN_COUNT = 1;
            		stopSelf();
            	}
            }*/
        } else {
//            tv1.setText("无法获取地理信息");
            LOG.print("无法获取地理信息", JLog.TYPE_INFO);
            mHandler.removeMessages(MSG_UPDATE_LOCATION);
            FAIL_LOCATION_COUNT ++;
            if(FAIL_LOCATION_COUNT == MAX_FAIL_LOCATION_COUNT){
            	getNormalLocation();
            	startBaiDuLocation();
            	LOG.print("5 min cann't get location...");
            }
            
        }
        if(count < 5){
            count ++;
            mHandler.removeMessages(MSG_UPDATE_LOCATION);
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_LOCATION, 1000);
        }else {
            mHandler.removeMessages(MSG_UPDATE_LOCATION);
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_LOCATION, tmpCycle * 1000);
        }
    }

    private int change2protocalInt(double data){
        int a = (int)(data * 30000 * 60);
        int hour = (int)data;
        double minute = (data - hour) * 60;
        int sum = (int)((hour * 60 + minute) * 30000);
        LOG.print("a = " + a + ", sum = " + sum);
        return sum;
    }

    //暂时认为只在有信息回来就是已经登陆成功
    private boolean isLogin = false;
    private ProtocalManager.DataReceiveListener mDataReceiveListener = new ProtocalManager.DataReceiveListener() {
        @Override
        public void onReceive(byte[] data) {
            if(!isLogin) {
                if (GPSTerminalProtocal.getProtocalSequence(data) == 1) {
                    isLogin = true;
                    LOG.print("登陆成功!!");
//                    getLocation();
                    getNormalLocation();
                    startBaiDuLocation();
                    getLBS();
                }else{
                    LOG.print("收到回复!!");
                    //没有登陆成功
                    //step 1: 先停止
                    stop();
                    //setp 2: 再重新连接 3秒后
                    mHandler.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							startUploadData();
						}
					}, 3 * 1000);
                }
            }
        }

        @Override
        public void exitByException(String msg) {
//            if(mProtocalManager != null){
//                mProtocalManager.onStop();
//            }
            stop();
            Toast.makeText(GPSTerminalService.this, msg, Toast.LENGTH_LONG).show();
            finishActivity();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                	startUploadData();
//                    if(mProtocalManager != null){
//                        mProtocalManager.onStart();
//                        try {
//                            Intent restart = new Intent(GPSTerminalService.this, MainActivity.class);
//                            restart.setPackage(GPSTerminalService.this.getPackageName());
//                            restart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            GPSTerminalService.this.startActivity(restart);
//                        }catch (Exception e){
//                            e.printStackTrace();
//                        }
//                    }
                }
            }, 3 * 1000);

        }

        @Override
        public void connect2serverError() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                	Toast.makeText(GPSTerminalService.this, "无法连接,请检查配置及网络状态!", Toast.LENGTH_LONG).show();
                    GPSTerminalService.this.stop();
                }
            });

        }
    };

    private final static int MSG_UPDATE_LOCATION = 0;
    private final static int MSG_CHECK_DATA_IS_UPLOAD = 1;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_UPDATE_LOCATION:
                    updateToNewLocation();
                    break;
                case MSG_CHECK_DATA_IS_UPLOAD:
                	startUploadData();
                	mHandler.removeMessages(MSG_CHECK_DATA_IS_UPLOAD);
                	mHandler.sendEmptyMessageDelayed(MSG_CHECK_DATA_IS_UPLOAD, 60 * 1000);
                	break;
            }
        }
    };

    private void notifyLocationChange(LocationInfo locationInfo){
        int n = mCallbacks.beginBroadcast();
        try {
            for (int i = 0; i < n; ++i) {
                if (mCallbacks.getBroadcastItem(i) != null) {
                    mCallbacks.getBroadcastItem(i).notifyLocationChange(locationInfo);
                }
            }
        } catch (RemoteException e) {
            LOG.print(e.getMessage());
        }
        mCallbacks.finishBroadcast();
    }

    private void finishActivity(){
        int n = mCallbacks.beginBroadcast();
        try {
            for (int i = 0; i < n; ++i) {
                if (mCallbacks.getBroadcastItem(i) != null) {
                    mCallbacks.getBroadcastItem(i).finishActivity();
                }
            }
        } catch (RemoteException e) {
            LOG.print(e.getMessage());
        }
        mCallbacks.finishBroadcast();
    }

    public String generateDeviceId(){
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        while (imei.length() < 15){
            imei = imei + "0";
        }
        return imei;
    }

    private SettingInfo getSettingInfo(){
        String settings = mSharedPreferences.getString(KEY_SETTINGS_INFO, null);
        if(settings != null){
            return new SettingInfo(settings);
        }
        return null;
    }
    
    private void saveLocationInfo(LocationInfo locationInfo){
    	SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putString(KEY_LOCATION_INFO, locationInfo.toString());
        edit.commit();
    }
    

    private LocationInfo getLocationInfo(){
        String locationStr = mSharedPreferences.getString(KEY_LOCATION_INFO, null);
        if(locationStr != null){
        	try {
				Gson gson = new Gson();
				return gson.fromJson(locationStr, LocationInfo.class);
			} catch (Exception e) {
				return null;
			}
        }
        return null;
    }

    private int cycle = 60, tmpCycle = 60;
    private void updateUploadCycle(){
        cycle = 60;
        SettingInfo settingInfo = getSettingInfo();
        if(settingInfo != null){
            cycle = settingInfo.uploadCycle;
        }
        mHandler.removeMessages(MSG_UPDATE_LOCATION);
        mHandler.sendEmptyMessage(MSG_UPDATE_LOCATION);
    }
    
    private void registerScreenActionReceiver(){  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(Intent.ACTION_SCREEN_OFF);  
        filter.addAction("android.mcu.device.action.ACC");  
//        filter.addAction(Intent.ACTION_SCREEN_ON);  
        registerReceiver(receiver, filter);  
    }
    
    private final BroadcastReceiver receiver = new BroadcastReceiver(){  
    	  
        @Override  
        public void onReceive(final Context context, final Intent intent) {  
            String action = intent.getAction();
            if(Intent.ACTION_SCREEN_OFF.equals(action)){
            	GPSTerminalService.this.stop();
            	GPSTerminalService.this.stopSelf();
            }/*else if(Intent.ACTION_SCREEN_ON.equals(action)){
            	
            }*/
            else if("android.mcu.device.action.ACC".equals(action)){
            	boolean status = intent.getBooleanExtra("AccStatus", false);
    			LOG.print("--acc status = "  + status);
    			if(!status){
    				GPSTerminalService.this.stop();
                	GPSTerminalService.this.stopSelf();
    			}
            }
        }  
      
    };

    class GPSTerminalManager extends IGpsTerminalFeature.Stub {

        private final JLog LOG = new JLog("GPSTerminalManager", GPSTerminalService.DEBUG, JLog.TYPE_DEBUG);


        @Override
        public void updateSettings(SettingInfo aSettingInfo) throws RemoteException {
            LOG.print("updateSettings...");
            if(aSettingInfo == null)return;
            SettingInfo old = getSettingInfo();
            SharedPreferences.Editor edit = mSharedPreferences.edit();
            edit.putString(KEY_SETTINGS_INFO, aSettingInfo.toString());
            edit.commit();
            final SettingInfo settingInfo = aSettingInfo;
            if(old != null && (!settingInfo.host.equals(old.host) || settingInfo.port != old.port)){
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mProtocalManager != null){
                            mProtocalManager.onStop();
                        }
                    }
                });
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mProtocalManager != null){
                            mProtocalManager.onStart();
                        }
                        updateUploadCycle();
                        getNormalLocation();
                        startBaiDuLocation();
                    }
                }, 2000);
            }else{
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateUploadCycle();
                        getNormalLocation();
                        startBaiDuLocation();
                    }
                });
            }

        }

        @Override
        public SettingInfo getSettings() throws RemoteException {
            return getSettingInfo();
        }

        //当主动取时,如果为空
        @Override
        public LocationInfo getLocationInfo() throws RemoteException {
            LOG.print("主动获取位置信息...");
            return mLocationInfo;
        }

        @Override
        public void stopUploadData() throws RemoteException {
            stop();
        }

        @Override
        public String getDeviceId() throws RemoteException {

            return generateDeviceId();
        }

        @Override
        public void registCallback(IGPSTerminalCallback aCallback) throws RemoteException {
//            registTerminalCallback(aCallback);
            if(aCallback != null){
                mCallbacks.register(aCallback);
            }
        }

        @Override
        public void unRegistCallback(IGPSTerminalCallback aCallback) throws RemoteException {
            if(aCallback != null){
                mCallbacks.unregister(aCallback);
            }
        }
    }
}
