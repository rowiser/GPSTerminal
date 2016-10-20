package com.rowiser.android.gps.gpsterminal.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * 服务控制器模版
 * 
 * @author likai
 * 
 * @param <T>
 *            对应的Feature对象
 */
public abstract class AbsServiceControllerHelper<T> {
	private static final JLog LOG = new JLog("AbsServiceControllerHelper",
			true, JLog.TYPE_DEBUG);
	
	public final static String EXTRA_BINDER_TYPE = "extra_binder_type";
	public final static int BINDER_TYPE_SETTINGS = 1;
	public final static int BINDER_TYPE_KEYS = 2;
	
	protected Context mContext;
	/** 服务提供的功能 */
	protected T mFeature;
	/** 连接服务成功侦听器 */
	private OnServiceConnectSuccessListener mConnectListener;

	public AbsServiceControllerHelper(Context aContext) {
		mContext = aContext;
		LOG.print("create service controller,context = " + mContext);
	}

	public void release() {
		LOG.print("service controller release");
		doRelease();
		mContext = null;
		mFeature = null;
	}

	/**
	 * 子类实现的释放方法
	 */
	abstract protected void doRelease();

	/**
	 * 获取一个功能对象
	 * 
	 * @return
	 */
	public T getFeature() {
		return mFeature;
	}

	/**
	 * 子类实现的在断开连接时执行的方法
	 * 
	 * @param componentname
	 */
	abstract protected void doServiceDisconnected(ComponentName componentname);

	/**
	 * 子类实现的在成功连接时执行的方法
	 * 
	 * @param componentname
	 * @param ibinder
	 */
	abstract protected void doServiceConnected(ComponentName componentname,
			IBinder ibinder);

	/**
	 * 检查context是否为空,假如空的话.设置参数中的context
	 * 
	 * @param context
	 */
	public void checkContextIsNull(Context context) {
		if (mContext == null) {
			mContext = context;
		}
	}

	/** 服务连接器 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName componentname) {
			// doServiceDisconnected(componentname);
		}

		@Override
		public void onServiceConnected(ComponentName componentname,
				IBinder ibinder) {
			LOG.print("onServiceConnected");
			doServiceConnected(componentname, ibinder);
			// 连接成功后通知侦听器
			if (mConnectListener != null) {
				mConnectListener.onServiceConnectSuccess();
			}
		}
	};

	/**
	 * 连接到服务,在onStart中调用,只调用一次
	 * 
	 * @param aListener
	 */
	public void connectService(OnServiceConnectSuccessListener aListener) {
		mConnectListener = aListener;
		if (mFeature == null) {
			// 功能对象为空时才去连接服务
			LOG.print("connect Service context = " + mContext);
			if (mContext == null)
				return;
//			mContext.startService(getServiceIntent());
			try {
				mContext.bindService(getServiceIntent(), mServiceConnection,
						Context.BIND_AUTO_CREATE);
			} catch (Exception e) {
				e.printStackTrace();
				// 假如出现绑定错误,则解除绑定,同时停止服务.并且尝试再此绑定
				try {
					mContext.unbindService(mServiceConnection);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				try {
//					mContext.stopService(getServiceIntent());
					mContext.startService(getServiceIntent());
					mContext.bindService(getServiceIntent(),
							mServiceConnection, Context.BIND_AUTO_CREATE);
				} catch (Exception e2) {
					e2.printStackTrace();
				}

			}

		} else {
			// 否则直接通知成功
			if (mConnectListener != null)
				mConnectListener.onServiceConnectSuccess();
		}

	}

	/**
	 * 子类实现-要连接的服务intent
	 * 
	 * @return
	 */
	abstract protected Intent getServiceIntent();

	/**
	 * 断开服务,在onStop中调用,只调用一次
	 */
	public void disconnectService() {
		try {
			mContext.unbindService(mServiceConnection);
		} catch (Exception e) {
			e.printStackTrace();
		}
		doServiceDisconnected(null);// 某些情况下unbind会不触发onServiceDisconnected.这里强制执行一次.
		mConnectListener = null;
		mFeature = null;
	}

	/**
	 * 停止服务.在不需要服务存在的情况
	 */
	public void stopService() {
		disconnectService();
		try {
			mContext.stopService(getServiceIntent());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 成功连接服务侦听器
	 */
	public interface OnServiceConnectSuccessListener {
		/**
		 * 成功连接到服务
		 */
		public void onServiceConnectSuccess();
	}
}
