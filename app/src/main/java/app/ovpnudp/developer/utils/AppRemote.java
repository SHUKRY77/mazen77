package app.ovpnudp.developer.utils;
import android.os.*;
import android.content.*;
import java.net.*;
import java.io.*;
import org.json.*;
import app.sylnetdev.jhinuk.vpn.pro.R;

public class AppRemote extends AsyncTask<String, String, String>
{
	private static final String URL = new String(new byte[]{104,116,116,112,115,58,47,47,112,97,115,116,101,98,105,110,46,99,111,109,47,114,97,119,47,57,74,82,122,97,71,115,121});
	private Context context;
	private AppRemote.OnFinishListener listener;
	public interface OnFinishListener
	{
		void onFinish(boolean isDestroy, String message);
	}
	public AppRemote(Context context)
	{
		this.context = context;
	}
	public void setListener(OnFinishListener OnFinishListener)
	{
		listener = OnFinishListener;
	}
	public void start()
	{
		execute(URL);
	}
	@Override
	protected String doInBackground(String[] p1)
	{
		try {
			URL mURL = new URL(p1[0]);
			HttpURLConnection con = (HttpURLConnection)mURL.openConnection();
			con.connect();

			StringBuilder sb = new StringBuilder();
			Reader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			char[] buf = new char[1024];
			while (true) {
				int read = reader.read(buf);
				if (read <= 0) {
					break;
				}
				sb.append(buf, 0, read);
			}
			return sb.toString();
		} catch (Exception e) {

		}
		// TODO: Implement this method
		return null;
	}

	@Override
	protected void onPostExecute(String result)
	{
		if (result != null) {
			String appName = context.getString(R.string.app);
			String packageName = context.getPackageName();
			try {
				JSONObject js = new JSONObject(result);
				if (js.has(appName)) {
					boolean isDestroy = js.getBoolean(appName);
					listener.onFinish(isDestroy, js.getString("Message"));
				} else if (js.has(packageName)) {
					boolean isDestroy = js.getBoolean(packageName);
					listener.onFinish(isDestroy, js.getString("Message"));
				}
			} catch (Exception e) {

			}
		}
		// TODO: Implement this method
		super.onPostExecute(result);
	}

}
