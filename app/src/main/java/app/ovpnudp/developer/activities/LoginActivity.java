package app.ovpnudp.developer.activities;

import android.view.View.*;
import app.ovpnudp.developer.view.*;
import android.widget.*;
import android.view.*;
import android.os.*;

import android.preference.*;
import android.content.*;
import android.net.*;

import java.util.*;

import app.sylnetdev.jhinuk.vpn.pro.R;
import androidx.appcompat.app.*;
import com.google.android.material.snackbar.*;
public class LoginActivity extends AppCompatActivity implements OnClickListener
{

	private EditText mUsername;

	private MaterialEditText mPassword;

	private Button loginBtn;

	

	

	private SharedPreferences prefs;

	private SharedPreferences.Editor editor;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_activity);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		editor = prefs.edit();

		mUsername = (EditText)findViewById(R.id.login_username);
		mPassword = (MaterialEditText)findViewById(R.id.login_password);
		loginBtn = (Button)findViewById(R.id.login_button);

		mUsername.setText(prefs.getString(OpenVPNClient.USERNAME, ""));
		mPassword.setText(prefs.getString(OpenVPNClient.PASSWORD, ""));
		loginBtn.setOnClickListener(this);

		if (prefs.getBoolean("isLogin", false)) {
			startActivity(new Intent(getApplicationContext(), OpenVPNClient.class));
			finish();
		}
	}

	@Override
	public void onClick(View p1)
	{
		doLogin();
		// TODO: Implement this method
	}

	private void doLogin()
	{
		final String username = mUsername.getText().toString();
		final String password = mPassword.getText().toString();

		if (username.isEmpty()) {
			mUsername.setError("Username is empty");
		} else if (password.isEmpty()) {
			mPassword.setError("Password is empty");
		} else {
			ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo info = cm.getActiveNetworkInfo();
			if (info == null || !info.isConnected()) {
				Snackbar.make(loginBtn, "No Internet Connection!", Snackbar.LENGTH_SHORT).show();
				return;
			}
			editor.putString(OpenVPNClient.USERNAME, username);
			editor.putString(OpenVPNClient.PASSWORD, password);
			editor.putBoolean("isLogin", true);
			editor.apply();

			startActivity(new Intent(getApplicationContext(), OpenVPNClient.class));
			finish();
			
			/*String format = "https://bulletvpn.us/api/auth.php?username=%s&password=%s&device_id=%s&device_model=%s";
			
			String model = Build.MODEL;
			String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
			
			String jsonUrl = String.format(format, username, password, id, model);

			StringRequest req = new StringRequest(jsonUrl,
				new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {

						try {
							//showToast(response);
							JSONObject js = new JSONObject(response);
							if (js.getString("device_match").equals("none")) {
								onAuthFailed("Authentication Failed");
								return;
							}
							if (js.getString("device_match").equals("false")) {
								onDeviceNotMatch("This pin is use in another device");
								return;
							}
							onExpireDate(js.getString("expiry"));
							
							editor.putString(OpenVPNClient.USERNAME, username);
							editor.putString(OpenVPNClient.PASSWORD, password);
							editor.putBoolean("isLogin", true);
							editor.apply();

							startActivity(new Intent(getApplicationContext(), OpenVPNClient.class));
							finish();
						} catch (Exception e) {
							//onError(e.getClass().getSimpleName() + ": " +e.getMessage());
						}
					}
				},   new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						if (error.getMessage() == null) {
							Snackbar.make(loginBtn, "Please Check Your Internet Connection!", Snackbar.LENGTH_SHORT).show();
							return;
						}
						onError(error.getClass().getName() + ": "+ error.getMessage());
					}

					private void onError(String message)
					{
						Snackbar.make(loginBtn, message, Snackbar.LENGTH_SHORT).show();
						// TODO: Implement this method
					}

				});
			RequestQueue requestQueue = Volley.newRequestQueue(this);
			requestQueue.add(req);*/
		}
		// TODO: Implement this method
	}
	
	public void onExpireDate(String expiry)
	{
		
		if (expiry.equals("none")) {
			editor.putString("ExpireDate", "none").apply();
		} else {
			editor.putString("ExpireDate", getDaysLeft(expiry)).apply();
		}
		// TODO: Implement this method
	}

	
	public void onDeviceNotMatch(String message)
	{
		Snackbar.make(loginBtn, message, Snackbar.LENGTH_SHORT).show();
		// TODO: Implement this method
	}
	private String getDaysLeft(String thatDate)
	{
		if (thatDate.contains(" ")) {
			thatDate = thatDate.split(" ")[0];
		}
		String[] split = thatDate.split("-");
		Calendar instance = Calendar.getInstance();
		instance.set(Integer.valueOf(split[0]).intValue(), Integer.valueOf(split[1]).intValue() - 1, Integer.valueOf(split[2]).intValue());
		return String.format("%s Days Left", new Object[]{(instance.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / ((long) 86400000)});
	}

	
	public void onAuthFailed(String message)
	{
		Snackbar.make(loginBtn, message, Snackbar.LENGTH_SHORT).show();
		// TODO: Implement this method
	}
	public void openWeb(View v)
	{
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://manfowa.xyz")));
	}
}
