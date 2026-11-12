package app.ovpnudp.developer.utils;
import android.content.*;
import android.preference.*;



public class ConfigUtil
{


    public static final int MODE_SSH_DIRECT = 0, MODE_SSH_DIRECT_WITH_PAYLOAD = 1,
    MODE_SSH_HTTP_PROXY = 2, MODE_SSL_DIRECT = 3, MODE_SSL_DIRECT_WITH_PAYLOAD = 4, 
    MODE_SSL_HTTP_PROXY = 5, MODE_UDP = 6;
    public static final int MODE_OVPN_DIRECT_UDP = 7;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private static ConfigUtil instance;

    public ConfigUtil(Context context)
    {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
    }
    public static ConfigUtil getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigUtil(context);
        }
        return instance;
    }
    public void setCustomSSLPortEnable(boolean enable)
    {
        editor.putBoolean("CustomSSLPortEnable", enable).apply();
        // TODO: Implement this method
    }
    public boolean isQueryMode()
    {
        return prefs.getBoolean("isQueryMode", false);
    }
    public void setIsQueryMode(boolean enable)
    {
        editor.putBoolean("isQueryMode", enable).apply();
    }
    public String getFrontQueryString()
    {
        return prefs.getString("FrontQuery", "");
    }
    public String getBackQueryString()
    {
        return  prefs.getString("BackQuery", "");
    }
    public void setFrontQuery(String query)
    {
        editor.putString("FrontQuery", query).apply();
    }
    public void setBackQuery(String query)
    {
        editor.putString("BackQuery", query).apply();
    }
    public void setNetworkSelectedName(String name)
    {
        editor.putString("NETWORK_SELECTED_NAME", name).apply();
    }
    public String getNetworkSelectedName()
    {
        // TODO: Implement this method
        return prefs.getString("NETWORK_SELECTED_NAME", "");
    }

    public void setServerSelectedName(String name)
    {
        editor.putString("SERVER_SELECTED_NAME", name).apply();
    }
    public String getServerSelectedName()
    {
        // TODO: Implement this method
        return prefs.getString("SERVER_SELECTED_NAME", "");
    }

    public boolean getCustomSSLPortEnabled()
    {
        return prefs.getBoolean("CustomSSLPortEnable", false);
    }
    public void setSSLPort(String string)
    {
        editor.putString("SSL_PORT", string).apply();
        // TODO: Implement this method
    }
    public void setServerSelectedPosition(int position)
    {
        editor.putInt("SERVER_SELECTED_POSITION", position).apply();
    }
    public int getServerSelectedPosition()
    {
        return prefs.getInt("SERVER_SELECTED_POSITION", 0);
    }
    public void setNetworkSelectedPosition(int position)
    {
        editor.putInt("NETWORK_SELECTED_POSITION", position).apply();
    }
    public int getNetworkSelectedPosition()
    {
        return prefs.getInt("NETWORK_SELECTED_POSITION", 0);
    }
    public void clear()
    {
        editor.clear().apply();
    }
    public String getSSHHost()
    {
        return prefs.getString("SSH_HOST","");
    }
    public int getSSHPort()
    {
        return Integer.parseInt(prefs.getString("SSH_PORT","443"));
    }
    public int getSSLPort()
    {
        return Integer.parseInt(prefs.getString("SSL_PORT","443"));
    }
    public String getSSHPortString()
    {
        return prefs.getString("SSH_PORT", "443");
    }
    public String getUsername()
    {
        return prefs.getString("USERNAME","");
    }
    public String getPassword()
    {
        return prefs.getString("PASSWORD","");
    }

    public String getPayload()
    {
        return prefs.getString("HTTP_PAYLOAD","");
    }
    public String getProxy()
    {
        return prefs.getString("PROXY_HOST", "");
    }
    public String getProxyPort()
    {
        return prefs.getString("PROXY_PORT", "");

    }
    public boolean getProxyAuthEnabled()
    {
        return prefs.getBoolean("ProxyAuth", false);
    }
    public String getProxyUsername()
    {
        return prefs.getString("ProxyUser", "");
    }
    public String getProxyPassword()
    {
        return prefs.getString("ProxyPass", "");
    }
    public String getBlockApps()
    {
        return prefs.getString("BlockApps", "");
    }
    public boolean getTorrentEnabled()
    {
        return prefs.getBoolean("AntiTorrent", false);
    }
    public void setAntiTorrentEnabled(boolean enabled)
    {
        editor.putBoolean("AntiTorrent", enabled).apply();
    }

    public String getInfo()
    {
        return prefs.getString("ConfigInfo","");
    }
    public String getSni()
    {
        return prefs.getString("SNI", "");
    }
    public void setSni(String sni)
    {
        editor.putString("SNI", sni).apply();
    }
    public int getTunnelType()
    {
        return prefs.getInt("TUNNEL_TYPE", 0);
    }
    public void setTunnelType(int type)
    {
        editor.putInt("TUNNEL_TYPE", type).apply();
    }
    public void setInfo(String info)
    {
        editor.putString("ConfigInfo", info).apply();
    }
    public void setProxyAuthEnabled(boolean enabled)
    {
        editor.putBoolean("ProxyAuth", enabled).apply();
    }
    public void setProxyPassword(String password)
    {
        editor.putString("ProxyPass", password).apply();
    }
    public void setProxyUsername(String username)
    {
        editor.putString("ProxyUser", username).apply();
    }
    public void setHTTPayload(String payload)
    {
        editor.putString("HTTP_PAYLOAD", payload).apply();
    }
    public void setProxy(String proxy)
    {
        editor.putString("PROXY_HOST", proxy).apply();
    }
    public void setProxyPort(String port)
    {
        editor.putString("PROXY_PORT", port).apply();
    }
    public void setLocalPort(String localport)
    {
        editor.putString("LOCAL_PORT", localport).apply();
    }
    public void setSSHHost(String host)
    {
        editor.putString("SSH_HOST", host).apply();
    }
    public void setSSHPort(String port)
    {
        editor.putString("SSH_PORT", port).apply();
    }
    public void setUsername(String username)
    {
        editor.putString("USERNAME", username).apply();
    }
    public void setPassword(String password)
    {
        editor.putString("PASSWORD", password).apply();
    }
    public static String hide(String str)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    @Override
    public String toString()
    {

        // TODO: Implement this method
        return super.toString();
    }
    public String getUDPConfig()
    {
        return prefs.getString("UDPConf", "");
    }


    public void setUDPConfig(String config)
    {
        //editor.putString("MyConfigVersion", version).apply();
        editor.putString("UDPConf", config).apply();
    }
    public void setUdp(boolean hmm)
    {
        editor.putBoolean("isUDPSet", hmm).apply();
    }
    public Boolean isUDP()
    {
        return prefs.getBoolean("isUDPSet", false);
    }
}
