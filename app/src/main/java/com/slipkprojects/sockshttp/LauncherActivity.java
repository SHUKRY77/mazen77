package com.slipkprojects.sockshttp;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.maxtunnel.vip.R;
import com.slipkprojects.sockshttp.activities.BaseActivity;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.os.CountDownTimer;
import android.view.View;
import android.app.Application;
import android.content.Intent;
import android.view.WindowManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.annotation.TargetApi;


/**
 * @author anuragdhunna
 */
public class LauncherActivity extends BaseActivity
{
	private static final long COUNTER_TIME = 5;
	private long secondsRemaining;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		createTimer(COUNTER_TIME);
		
        
    }
	
	
	private void createTimer(long seconds) {
	
		CountDownTimer countDownTimer =
			new CountDownTimer(seconds * 1000, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				secondsRemaining = ((millisUntilFinished / 1000) + 1);
			}

			@Override
			public void onFinish() {
				secondsRemaining = 0;
				//counterTextView.setText("Done.");

				Application application = getApplication();
				if (!(application instanceof MyApplication)) {
					new Handler().postDelayed(new Runnable(){
							@Override
							public void run() {
								Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
								startActivity(intent);
								finish();
							}
						}, 1000);
					return;
				}
				((MyApplication) application)
					.showAdIfAvailable(LauncherActivity.this,new MyApplication.OnShowAdCompleteListener() {
						@Override
						public void onShowAdComplete() {
							new Handler().postDelayed(new Runnable(){
									@Override
									public void run() {
										Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
										intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
										startActivity(intent);
										finish();
									}
								}, 1000);
						}
                    });
			}
        };
		countDownTimer.start();
	}
	
}