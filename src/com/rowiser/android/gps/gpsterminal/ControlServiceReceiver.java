package com.rowiser.android.gps.gpsterminal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Handler;
import android.widget.Toast;

import com.rowiser.android.gps.gpsterminal.utils.JLog;

public class ControlServiceReceiver extends BroadcastReceiver {

	private final static String TAG = "ControlServiceReceiver";
	private final static JLog LOG = new JLog(TAG, GPSTerminalService.DEBUG,
			JLog.TYPE_DEBUG);
	private final static int MSG_CHECK_NETWORK_FOR_CONTROL_GPS_SERVICE = 1;
	private static boolean IS_NEED_TO_CHECK_START_GPS_SERVICE = false;
	private Context mContext;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		String action = intent.getAction();
		LOG.print("--action = " + action);
		if(ConnectivityManager.CONNECTIVITY_ACTION.equals(action)){
			netStateChange(context);
		}else if(Intent.ACTION_BOOT_COMPLETED.equals(action)){
			controlGpsTerminalService(context, true);
			LOG.print("--boot completed");
		}else if("android.intent.action.USER_PRESENT".equals(action)){//开屏
			IS_NEED_TO_CHECK_START_GPS_SERVICE = true;
			screenOn(context);
		}else if("com.android.action_acc_off".equals(action)){
			controlGpsTerminalService(context, false);
		}else if("com.android.action_acc_on".equals(action)){
			IS_NEED_TO_CHECK_START_GPS_SERVICE = true;
			screenOn(context);
		}else if("android.mcu.device.action.ACC".equals(action)){
			boolean status = intent.getBooleanExtra("AccStatus", false);
			LOG.print("--acc status = "  + status);
			controlGpsTerminalService(context, status);
		}
	}
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_CHECK_NETWORK_FOR_CONTROL_GPS_SERVICE:
				screenOn(mContext);
				break;

			default:
				break;
			}
		};
	};
	
	private void screenOn(Context context){
		if(IS_NEED_TO_CHECK_START_GPS_SERVICE){
			try {
				if(isNetworkWorking(context)){
					controlGpsTerminalService(context, true);
					IS_NEED_TO_CHECK_START_GPS_SERVICE = false;
					mHandler.removeMessages(MSG_CHECK_NETWORK_FOR_CONTROL_GPS_SERVICE);
					LOG.print("--screen on and start service..");
				}else{
					controlGpsTerminalService(context, false);
					IS_NEED_TO_CHECK_START_GPS_SERVICE = true;
					mHandler.removeMessages(MSG_CHECK_NETWORK_FOR_CONTROL_GPS_SERVICE);
					mHandler.sendEmptyMessageDelayed(MSG_CHECK_NETWORK_FOR_CONTROL_GPS_SERVICE, 3 * 1000);
					LOG.print("--screen on and can not start service, delay to retry...");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean isNetworkWorking(Context context){
		try {
			ConnectivityManager manager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeInfo = manager.getActiveNetworkInfo();
			if(activeInfo != null){
				LOG.print("--network is connective");
				return true;
			}else{
				LOG.print("--network is disconnective");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private void netStateChange(Context context) {
		try {
			if(isNetworkWorking(context)){
				controlGpsTerminalService(context, true);
			}else{
				controlGpsTerminalService(context, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void controlGpsTerminalService(final Context context, boolean start){
		final Intent gpsTerminalIntent = new Intent(GPSTerminalService.TERMINAL_SERVICE);
		gpsTerminalIntent.setPackage("com.rowiser.android.gps.gpsterminal");
		try {
			context.stopService(gpsTerminalIntent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if(start){
				mHandler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						context.startService(gpsTerminalIntent);
					}
				}, 1000);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
