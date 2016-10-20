package com.rowiser.android.gps.gpsterminal;

import android.app.Application;

import com.rowiser.android.gps.gpsterminal.crash.CrashHandler;

public class CrashApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());
	}
}
