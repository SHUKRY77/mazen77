package com.slipkprojects.sockshttp.ui;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Html;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import com.maxtunnel.vip.R;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.slipkprojects.ultrasshservice.tunnel.TunnelUtils;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
//import android.os.AsyncTask;
import android.widget.Button;
import android.view.View;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.appcompat.widget.Toolbar;
import com.slipkprojects.ultrasshservice.logger.SkStatus;

public class ProxySettings extends Activity implements ServiceConnection,
		OnCheckedChangeListener {
	public static final String TAG = "ProxySettings";

	protected static final String KEY_PREFS = "proxy_pref";
	protected static final String KEY_ENABALE = "proxy_enable";

	private static int NOTIFICATION_ID = 20140701;

	private IProxyControl proxyControl = null;

	private TextView tvInfo;
	private Switch cbEnable; 
    private Button wifiTetherButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.wifi_share);
        
		tvInfo = (TextView) findViewById(R.id.tv_info);
		cbEnable = (Switch) findViewById(R.id.cb_enable);
		cbEnable.setOnCheckedChangeListener(this);

		Intent intent = new Intent(this, ProxyService.class);
		bindService(intent, this, Context.BIND_AUTO_CREATE);
        
        

	}

	@Override
	public void onServiceConnected(ComponentName cn, IBinder binder) {
		proxyControl = (IProxyControl) binder;
		if (proxyControl != null) {
			updateProxy();
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName cn) {
		proxyControl = null;
	}

	@Override
	protected void onDestroy() {
		unbindService(this);
		super.onDestroy();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		SharedPreferences sp = getSharedPreferences(KEY_PREFS, MODE_PRIVATE);
		sp.edit().putBoolean(KEY_ENABALE, isChecked).commit();
		updateProxy();
	}

	private void updateProxy() {
		if (proxyControl == null) {
			return;
		}

		boolean isRunning = false;
		try {
			isRunning = proxyControl.isRunning();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		boolean shouldRun = getSharedPreferences(KEY_PREFS, MODE_PRIVATE)
				.getBoolean(KEY_ENABALE, false);
		if (shouldRun && !isRunning) {
			startProxy();
		} else if (!shouldRun && isRunning) {
			stopProxy();
		}

		try {
			isRunning = proxyControl.isRunning();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		if (isRunning) {
			tvInfo.setText(R.string.proxy_on);
			cbEnable.setChecked(true);
		} else {
			tvInfo.setText(R.string.proxy_off);
			cbEnable.setChecked(false);
		}
	}

	private void startProxy() {
		boolean started = false;
		try {
			started = proxyControl.start();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		if (!started) {
			return;
		}
    	Context context = getApplicationContext();
            Toast.makeText(ProxySettings.this, "Started", 0).show();
            SkStatus.logInfo(Html.fromHtml("<b><font color='red'>Proxy Hostname:</font></b>   ") +TunnelUtils.getLocalIpAddress() + "\n\n" + Html.fromHtml("<b><font color='red'>Proxy Port:    </font></b>") + "8080");
	}

	private void stopProxy() {
		boolean stopped = false;

		try {
			stopped = proxyControl.stop();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		if (!stopped) {
			return;
		}

		tvInfo.setText(R.string.proxy_off);
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(NOTIFICATION_ID);
		/*Toast.makeText(this, getResources().getString(R.string.proxy_stopped),
				Toast.LENGTH_SHORT).show();*/ 
        Toast.makeText(ProxySettings.this, "Stopped ", 0).show();
		//toastutil.showConfusingToast("Usted Desactivo El WiFi Share");
	}
    public void btnHardwareID(View view){
		launchHotspotSettings();
	}
    private void launchHotspotSettings() {
		Intent tetherSettings = new Intent();
		tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
		startActivity(tetherSettings);
	}
    public void btnHardwareID2(View view){
		errorUpdateDialog();
	}
    private void errorUpdateDialog() {
             MaterialAlertDialogBuilder dialogoi = new MaterialAlertDialogBuilder(this);
             dialogoi.setMessage(Html.fromHtml("<b><font color='red'>Proxy Hostname:</font></b>   ") +TunnelUtils.getLocalIpAddress() + "\n\n" + Html.fromHtml("<b><font color='red'>Proxy Port:    </font></b>") + "8080")
            .setPositiveButton("Ok", null)
            .create().show();
    }
    public void btnHardwareIDi(View view){
		onBackPressed();
	}
    
}
