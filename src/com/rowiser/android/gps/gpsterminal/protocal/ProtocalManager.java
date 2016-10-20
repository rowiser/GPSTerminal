package com.rowiser.android.gps.gpsterminal.protocal;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.telephony.TelephonyManager;

import com.rowiser.android.gps.gpsterminal.GPSTerminalService;
import com.rowiser.android.gps.gpsterminal.bean.SettingInfo;
import com.rowiser.android.gps.gpsterminal.utils.JavaDecode;
import com.rowiser.android.gps.gpsterminal.utils.JLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by likai on 7/1/16.
 */
public class ProtocalManager {

    private final static String TAG = "ProtocalManager";
    private final static JLog LOG = new JLog(TAG, true, JLog.TYPE_DEBUG);
    private Context mContext;
    public boolean mIsStop = true;
    private OutputStream mOutputStream = null;
    private InputStream mInputStream = null;
    private ReceiveThread mReceiveThread;

    private static ProtocalManager mProtocalManager;
    private Socket mClient;

    private DataReceiveListener mDataReceiveListener;


    public ProtocalManager(Context context) {
        mContext = context;
    }

    public synchronized final static ProtocalManager getInstance(Context context) {
        if (mProtocalManager == null && context != null) {
            mProtocalManager = new ProtocalManager(context);
        }
        return mProtocalManager;
    }

    public void onStart() {
        String settings = mContext.getSharedPreferences(GPSTerminalService.FILE_NAME, Context.MODE_PRIVATE).getString(GPSTerminalService.KEY_SETTINGS_INFO, null);
        SettingInfo settingInfo = null;
        if(settings != null){
            settingInfo = new SettingInfo(settings);
        }
        if(settingInfo == null) {
            LOG.print("config is null...");
            return;
        }
        final String host = settingInfo.host;
        final int port = settingInfo.port;
        if(isStop()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
//                    mClient = new Socket("222.216.28.83", 8841);
//                        mClient = new Socket("116.252.185.93", 6969);
//                    mClient = new Socket("116.252.185.93", 6767);
                    	if(mClient != null){
                    		onStop();
                    		try {
                    			Thread.sleep(1000);
							} catch (Exception e) {
								e.printStackTrace();
							}
                    	}
                        mClient = new Socket(host, port);
                        mInputStream = mClient.getInputStream();
                        mOutputStream = mClient.getOutputStream();
                        mReceiveThread = new ReceiveThread(mInputStream, mHandler);
                        mIsStop = false;
                        mReceiveThread.start();
                    } catch (Exception e) {
                        mIsStop = true;
                        e.printStackTrace();
                        LOG.print("无法连接到服务器!");
                        if(mDataReceiveListener != null){
                            mDataReceiveListener.connect2serverError();
                        }
                    } finally {

                    }
                }
            }).start();
        }
        mHandler.removeMessages(MSG_SEND_LOGIN);
        mHandler.sendEmptyMessageDelayed(MSG_SEND_LOGIN, 2000);
    }

    public void onStop() {
        mIsStop = true;
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
                mOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
                mInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mReceiveThread != null) {
            mReceiveThread.onStop();
            try {
                mReceiveThread.interrupt();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if (mClient != null) {
            try {
                mClient.close();
                mClient = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mHandler.removeMessages(MSG_SEND_LOGIN);
        mHandler.removeMessages(MSG_SEND_GPS_INFO);
        mHandler.removeMessages(MSG_RECEIVE_MESSAGE);
    }

    public void onDestroy() {

    }

    public boolean isStop(){
        boolean threadStop = true;
        if(mReceiveThread != null) {
            threadStop = mReceiveThread.alreadyStop();
        }
        return  threadStop || mIsStop;
    }

    private final static int MSG_SEND_LOGIN = 1;
    private final static int MSG_SEND_GPS_INFO = 3;
    public final static int MSG_RECEIVE_MESSAGE = 2;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SEND_LOGIN:
                    sendLoginInfo();
                    break;
                case MSG_SEND_GPS_INFO:

                    break;
                case MSG_RECEIVE_MESSAGE:
                    byte[] realData = (byte[]) msg.obj;
                    String str = JavaDecode.byteArray2String(realData, 0, realData.length);
                    LOG.print("receive = " + str);
//                    Toast.makeText(MainActivity.this, "receive = " + msg.obj.toString(), Toast.LENGTH_LONG).show();
                    if(mDataReceiveListener != null){
                        mDataReceiveListener.onReceive(realData);
                    }
                    break;

            }
        }
    };

    public void sendProtocal2Server(byte[] protocal) {
        if (mOutputStream != null) {
            GPSTerminalProtocal.countCRC(protocal);
            LOG.print("protocal = ", protocal);
            try {
                mOutputStream.write(protocal);
            } catch (IOException e) {
            	mIsStop = true;
                e.printStackTrace();
                if(mDataReceiveListener != null){
                    mDataReceiveListener.exitByException("主机或端口更改,正在重启!");
                }
            }
        }else{
        	mIsStop = true;
        	if(mDataReceiveListener != null){
                mDataReceiveListener.exitByException("上传通道为空!");
            }
        }
    }

    private void sendLoginInfo() {
//        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
//        String imei = tm.getDeviceId();
        String imei = generateDeviceId();
        if (imei == null) {
            LOG.print("Can't get the imei number...", JLog.TYPE_ERROR);
            mIsStop = true;
        } else {
            while (imei.length() < 15){
                imei = imei + "0";
            }
            LOG.print("imei number = " + imei, JLog.TYPE_INFO);
            byte[] loginProtocal = GPSTerminalProtocal.generateNullProtocal(GPSTerminalProtocal.LOGIN_INFO, 8);
            int len = imei.length();
            if (len >= 15) {
                int start = 0, end = 0;
                for (int i = 0; i < 8; i++) {
                    start = len - i * 2 - 2;
                    end = len - i * 2;
                    if (start <= -2) {
                        break;
                    }else if(start == -1){
                        start = 0;
                    }
                    String subStr = imei.substring(start, end);
//                    LOG.print("subStr = " + subStr);
                    loginProtocal[loginProtocal.length - i - 7] = JavaDecode.int2bcd(Integer.valueOf(subStr));
                }
            }
            sendProtocal2Server(loginProtocal);
        }
    }

    public String generateDeviceId(){
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        String settings = mContext.getSharedPreferences(GPSTerminalService.FILE_NAME, Context.MODE_PRIVATE).getString(GPSTerminalService.KEY_SETTINGS_INFO, null);
        SettingInfo settingInfo = null;
        if(settings != null){
            settingInfo = new SettingInfo(settings);
        }
        if(settingInfo == null) {
            LOG.print("config is null...");
            while (imei.length() < 15){
                imei = imei + "0";
            }
            char chars[] = imei.toCharArray();
            for(int i = 0; i < chars.length; i ++){
            	char c = chars[i];
            	if(c < '0' || c > '9'){
            		int number = c % 10;
            		chars[i] = String.valueOf(number).charAt(0);
            	}
            }
            imei = new String(chars);
        }else{
        	imei = settingInfo.device;//如果
        }
        return imei;
    }

    public void setDataReceiveListener(DataReceiveListener listener){
        mDataReceiveListener = listener;
    }

    public interface DataReceiveListener{
        void onReceive(byte[] data);

        void exitByException(String msg);

        void connect2serverError();
    }

}



class ReceiveThread extends Thread {

    private byte[] dataBuffer = new byte[128];
    private InputStream inputStream = null;
    private boolean isStop = false;
    private Handler managerHandler;

    public ReceiveThread(InputStream is, Handler handler) {
        this.inputStream = is;
        managerHandler = handler;
    }

    public boolean alreadyStop(){
        return isStop;
    }

    public void onStop() {
        isStop = true;
        GPSTerminalProtocal.sequence = 0;
    }

    @Override
    public void run() {

        int len = 0;
        try {
            while (!isStop) {
                if ((len = inputStream.read(dataBuffer)) != -1) {
                    byte[] realData = new byte[len];
                    System.arraycopy(dataBuffer, 0, realData, 0, len);
                    managerHandler.obtainMessage(ProtocalManager.MSG_RECEIVE_MESSAGE, realData).sendToTarget();
                }
            }
            isStop = true;
        } catch (Exception e) {
            e.printStackTrace();
            isStop = true;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                    inputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}

