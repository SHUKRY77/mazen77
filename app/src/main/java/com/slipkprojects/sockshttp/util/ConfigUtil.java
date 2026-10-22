package com.slipkprojects.sockshttp.util;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import com.slipkprojects.ultrasshservice.util.securepreferences.model.SecurityConfig;

/**
 * Created by: KervzCodes
 * Date Crated: 08/10/2020
 * Project: SocksHttp-master (ENGLISH)
 **/
public class ConfigUtil {

    Context context;
    public static final String PASSWORD = new String
   (new Object() {
   int JMGCDev;
   public String toString() {
      byte[] buf = new byte[6];
      JMGCDev = 167957770;
      buf[0] = (byte) (JMGCDev >>> 9);
      JMGCDev = 1025559259;
      buf[1] = (byte) (JMGCDev >>> 1);
      JMGCDev = 1104109774;
      buf[2] = (byte) (JMGCDev >>> 1);
      JMGCDev = 1670769568;
      buf[3] = (byte) (JMGCDev >>> 24);
      JMGCDev = -1338328574;
      buf[4] = (byte) (JMGCDev >>> 7);
      JMGCDev = 438426358;
      buf[5] = (byte) (JMGCDev >>> 10);
      return new String(buf);
   }
}.toString());
          
    public ConfigUtil(Context context) {
        this.context = context;
    }

	public String geNote()
    {
        try {
            String releaseNote = getJSONConfig().getString("ReleaseNotes");
            return releaseNote;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
	
	public String geNote1()
    {
        try {
            String releaseNote1 = getJSONConfig().getString("ReleaseNotes1");
            return releaseNote1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
	

    public String getVersion() {
        try {
            String version = getJSONConfig().getString("Version");
            return version;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public JSONArray getServersArray() {
        try {
            if (getJSONConfig() != null) {
                JSONArray array = getJSONConfig().getJSONArray("Servers");
                return array;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

	public ArrayList getNetworkSSLArray(ArrayList arraylist) {
		try {
			if (getJSONConfig() != null) {
				JSONArray array = getJSONConfig().getJSONArray("Networks");
				JSONArray jarr2 = array.getJSONObject(0).getJSONArray("SSL");
				for (int i = 0; i < jarr2.length(); i++)
				{
					JSONObject obj = jarr2.getJSONObject(i);
					arraylist.add(obj.getString("Name"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public ArrayList getNetworkSSHArray(ArrayList arraylist) {
		try {
			if (getJSONConfig() != null) {
				JSONArray array = getJSONConfig().getJSONArray("Networks");
				JSONArray jarr2 = array.getJSONObject(0).getJSONArray("SSH");
				for (int i = 0; i < jarr2.length(); i++)
				{
					JSONObject obj = jarr2.getJSONObject(i);
					arraylist.add(obj.getString("Name"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


    public JSONArray getNetworksArray() {
        try {
            if (getJSONConfig() != null) {
                JSONArray array = getJSONConfig().getJSONArray("Networks");
                return array;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean versionCompare(String NewVersion, String OldVersion) {
        String[] vals1 = NewVersion.split("\\.");
        String[] vals2 = OldVersion.split("\\.");
        int i = 0;

        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff) > 0;
        }

        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        return Integer.signum(vals1.length - vals2.length) > 0;
    }

    private JSONObject getJSONConfig() {
        try {
            File file = new File(context.getFilesDir(), "Config.json");
            if (file.exists()) {
                String json_file = readStream(new FileInputStream(file));
                String json = AESCrypt.decrypt(PASSWORD, json_file);
                return new JSONObject(json);
            } else {
                InputStream inputStream = context.getAssets().open("config/config.json");
                String json = AESCrypt.decrypt(PASSWORD, readStream(inputStream));
                return new JSONObject(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readStream(InputStream in)
    {
        StringBuilder sb = new StringBuilder();
        try {
            Reader reader = new BufferedReader(new InputStreamReader(in));
            char[] buff = new char[1024];
            while (true) {
                int read = reader.read(buff, 0, buff.length);
                if (read <= 0) {
                    break;
                }
                sb.append(buff, 0, read);
            }
        } catch (Exception e) {

        }
        return sb.toString();
    }
}