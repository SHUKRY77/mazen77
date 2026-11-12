package app.ovpnudp.developer.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import app.sylnetdev.jhinuk.vpn.pro.R;

public class OpenVPNPrefs extends PreferenceActivity {
    public void onCreate(Bundle savedInstanceState) {
        if (OpenVPNClientBase.themeSet) {
            setTheme(OpenVPNClientBase.themeResId);
        }
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
