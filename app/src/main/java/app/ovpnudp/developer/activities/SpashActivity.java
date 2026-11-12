package app.ovpnudp.developer.activities;
import androidx.appcompat.app.AppCompatActivity;


import android.os.*;
import app.sylnetdev.jhinuk.vpn.pro.R;
import android.content.*;
public class SpashActivity extends AppCompatActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		
		new Handler().postDelayed(new Runnable() {

				@Override
				public void run()
				{
					launchActivity();
					// TODO: Implement this method
				}
				
			
		}, 1000);
	}
	void launchActivity()
	{
		Intent intent = new Intent(this, OpenVPNClient.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		startActivity(intent);
		finish();
	}
}
