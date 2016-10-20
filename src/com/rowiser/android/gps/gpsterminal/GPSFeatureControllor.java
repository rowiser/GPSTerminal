package com.rowiser.android.gps.gpsterminal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.rowiser.android.gps.gpsterminal.bean.LocationInfo;
import com.rowiser.android.gps.gpsterminal.utils.AbsServiceControllerHelper;
import com.rowiser.android.gps.gpsterminal.utils.JLog;
import java.util.ArrayList;
import java.util.List;

public class GPSFeatureControllor extends AbsServiceControllerHelper<IGpsTerminalFeature> {

	private final static String TAG = "GPSFeatureControllor";
	private static final JLog LOG = new JLog(TAG, GPSTerminalService.DEBUG, JLog.TYPE_DEBUG);

	private static GPSFeatureControllor instance;

	private List<ServiceListener> mServiceListenerList;
	private List<MainFragmentInfoListener> mMainFragmentInfoListenerList;

	private GPSFeatureControllor(Context aContext) {
		super(aContext);
		mServiceListenerList = new ArrayList<ServiceListener>();
        mMainFragmentInfoListenerList = new ArrayList<MainFragmentInfoListener>();
	}

	public static final GPSFeatureControllor getInstance(Context aContext) {
		if (instance == null) {
			instance = new GPSFeatureControllor(aContext);
		}
		return instance;
	}
	
	public void addServerSettingsListener(ServiceListener listener) {
		if (!mServiceListenerList.contains(listener)) {
			mServiceListenerList.add(listener);
		}
	}
	
	public void removeServerSettingsListener(ServiceListener listener) {
		if (mServiceListenerList.contains(listener)) {
			mServiceListenerList.remove(listener);
		}
	}

	public void addMainFragmentInfoListener(MainFragmentInfoListener listener) {
		if (!mMainFragmentInfoListenerList.contains(listener)) {
			mMainFragmentInfoListenerList.add(listener);
		}
	}

	public void removeMainFragmentInfoListener(MainFragmentInfoListener listener) {
		if (mMainFragmentInfoListenerList.contains(listener)) {
			mMainFragmentInfoListenerList.remove(listener);
		}
	}

	
	public interface ServiceListener {

        /**
         * 服务通知结束界面
         */
        void finishActivity();

		
	}

    public interface MainFragmentInfoListener{

        void notifyLocationChanged(LocationInfo locationInfo);
    }


	private IGPSTerminalCallback.Stub mGPSTerminalRemoteCallback = new IGPSTerminalCallback.Stub() {

		@Override
		public void finishActivity() throws RemoteException {
			LOG.print("finishActivity");
			for (int i = 0; i < mServiceListenerList.size(); i++) {
				mServiceListenerList.get(i).finishActivity();
			}
		}

        @Override
        public void notifyLocationChange(LocationInfo locationInfo) throws RemoteException {
            for (int i = 0; i < mMainFragmentInfoListenerList.size(); i++) {
                mMainFragmentInfoListenerList.get(i).notifyLocationChanged(locationInfo);
            }
        }
    };

	@Override
	protected void doRelease() {
		mGPSTerminalRemoteCallback = null;
		instance = null;
	}

	@Override
	protected void doServiceDisconnected(ComponentName componentname) {
		try {
			if(mFeature != null)
			mFeature.unRegistCallback(mGPSTerminalRemoteCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doServiceConnected(ComponentName componentname,
			IBinder ibinder) {
		mFeature = IGpsTerminalFeature.Stub.asInterface(ibinder);
		try {
			mFeature.registCallback(mGPSTerminalRemoteCallback);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Intent getServiceIntent() {
		Intent startGpsTerminalService = new Intent(GPSTerminalService.TERMINAL_SERVICE);
		startGpsTerminalService.setPackage("com.rowiser.android.gps.gpsterminal");
		return startGpsTerminalService;
	}

}
