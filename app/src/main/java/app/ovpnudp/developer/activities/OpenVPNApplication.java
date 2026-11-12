package app.ovpnudp.developer.activities;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import app.ovpnudp.developer.interfaces.*;
import app.ovpnudp.developer.utils.AppOpenManager;

public class OpenVPNApplication extends Application {
    public static Context context;
	private static OnByteCountListener mOnByCountListener;
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
		MobileAds.initialize(this, new OnInitializationCompleteListener() {
			@Override
			public void onInitializationComplete(InitializationStatus initializationStatus) {
			}
		});
		new AppOpenManager(this);
    }
	public static void setByteCountListener(OnByteCountListener OnByCountListener)
	{
		mOnByCountListener = OnByCountListener;
	}
	public static void updateByteCount(long in, long out)
	{
		mOnByCountListener.onByteCount(in, out);
	}
    public static String resString(int res_id) {
        return context.getString(res_id);
    }
}
