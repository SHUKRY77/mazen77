package com.slipkprojects.ultrasshservice.tunnel;

/*
 * Decompiled with JADX v0.9.0.
 */

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.slipkprojects.ultrasshservice.config.Settings;
import android.content.SharedPreferences;
import com.slipkprojects.ultrasshservice.logger.SkStatus;
import com.slipkprojects.sockshttp.MainActivity;
import io.michaelrocks.paranoid.Obfuscate;
import org.json.JSONObject;
import org.json.JSONException;
import android.os.Handler;
import java.io.InputStream;
import com.maxtunnel.vip.R;
import com.slipkprojects.sockshttp.util.ConfigUtil;
@Obfuscate
public class UDPNiNeth extends Thread {
    private String address;
    private String ca;
    Context context;
    private String dir;
    private UDPStreamGobbler err;
    private File fileConf;
    private File fileDns;
    private String listen;
    private final Settings mConfig;
    private UDPStreamGobbler out;
    private String serv;
    private Process udpProcess;
    private JSONObject JsonData;
    private String nethconfff;
    private File fileca;
    private String fckc = "-----BEGIN CERTIFICATE-----\nMIIDizCCAnOgAwIBAgIUGxLl5Ou4dR1h3c9lUcaM5bp4ZBswDQYJKoZIhvcNAQEL\nBQAwVTELMAkGA1UEBhMCQ04xCzAJBgNVBAgMAkdEMQswCQYDVQQHDAJTWjEUMBIG\nA1UECgwLWklWUE4sIEluYy4xFjAUBgNVBAMMDVpJVlBOIFJvb3QgQ0EwHhcNMjMw\nMjExMDkwMjM1WhcNMzMwMjA4MDkwMjM1WjBVMQswCQYDVQQGEwJDTjELMAkGA1UE\nCAwCR0QxCzAJBgNVBAcMAlNaMRQwEgYDVQQKDAtaSVZQTiwgSW5jLjEWMBQGA1UE\nAwwNWklWUE4gUm9vdCBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\nAMQsHTq2UD4WDOvNUFGQuKd0PEitgQzSh12qH9aJ5jnCtbWjqVNDRQSW0ietg4Po\nqOfKLOBvGOJcGkrYlAAynnwsufdkZd2Jj2+FAXloAbMBK5cjqRANfPJ7ns3S5zL2\nt2+Xv/O6H58NL5QksyIHb2Vcosfelwuvj5Lq+MvyqGZikce5IaykgjjV0OsrBnsC\neK4yAeoxsqVixGwmcJDLGOIJDGYcDdaElqJqFCyOjOhLLDymx9JbeOb3DpiRNFNN\nlwXi2rfvpnmpGNwNt9sclWAQTL3cfV4GsCovT02r1qxcAqqRE4U1nqMRqk0KfyQn\nUebOat/0jNJI9YxJByuVBK0CAwEAAaNTMFEwHQYDVR0OBBYEFGk91bjhFZfcKkpm\n5SxVkqnSGhXBMB8GA1UdIwQYMBaAFGk91bjhFZfcKkpm5SxVkqnSGhXBMA8GA1Ud\nEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAEr4aeE0ib5/7neEcRWCE1pg\nw0j/958bdaSdQJJvYEpc7brCHhp5lmNJA+MjVcCXCL4/8KfuEcyGNPPSPo7wbuYJ\nO9jsJmQOklfyvlKGJschvc8AZ0E0AGdrgGam1KApjrb6Xly5bqgV4KPBQ7KttBVw\nwFfTm0yjD3nAjaSXi3I/MG+gMGnUXoTMZa3iS2pomBMHLdTksiujbbH7RP9mzPT3\n7UvyVmtw7eQFEjEYceVWHlhXCjL9gpcJiX/wu9XzREDpNCqY2R3zb+ZGYuQD0L5h\nzv0u1CF+Cfkkg8luxol+aWc+1ac/8TGLV1WOGj4FuEMfxQPXWFqhc8VEyxZ/r/w=\n-----END CERTIFICATE-----";
    public UDPNiNeth(Context context) {
        this.context = context;
        this.dir = context.getFilesDir().getPath();
        this.mConfig = new Settings(context);
   }

    private boolean printToFile(File file, String str) {
        try {
            PrintWriter printWriter = new PrintWriter(file);
            printWriter.println(str);
            printWriter.flush();
            printWriter.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void run() {
        this.serv = mConfig.getPrivString(Settings.SERVIDOR_KEY);
        this.fileca = new File(this.dir, "by_neth");
        this.ca = printToFile(fileca, fckc) ? dir + "/by_neth" : dir + "/by_neth";
        try {
            JsonData = new JSONObject(mConfig.getPrivString("UDPData"));
            JsonData.put("server", serv + ":20000-50000");
            JsonData.put("obfs", mConfig.getPrivString(Settings.USUARIO_KEY));
            JsonData.put("auth_str", mConfig.getPrivString(Settings.SENHA_KEY));
            JsonData.put("ca", ca);
            JsonData.put("resolver", "udp://" + mConfig.getPrivString("UDPResolver"));
            nethconfff = JsonData.toString(1);
        } catch (JSONException e) {
            }
        InetAddress[] inetAddressArr = new InetAddress[0];
        try {
            inetAddressArr = InetAddress.getAllByName(this.serv);
        } catch (UnknownHostException e2) {
            e2.printStackTrace();
        }
        try {
            this.address = getIPv4Addresses(inetAddressArr).getHostAddress();
        } catch (Exception e3) {
            e3.printStackTrace();
            if (String.valueOf(e3).contains("on a null object reference")) {
                SkStatus.logInfo("<font color='red'><strong>Invalid UDP Server</strong></font>");
                stopVudp();
            }
        }
        
        
        this.listen = "127.0.0.1:1080";
        this.fileConf = new File(this.dir, "n.e.t.h");
        if (printToFile(this.fileConf, nethconfff)) {
            this.fileDns = new File(this.context.getApplicationInfo().nativeLibraryDir, "libuz.so");
            String[] strArr = new String[]{fileDns.getAbsolutePath(), "client", "--config", this.fileConf.getAbsolutePath()};
            UDPStreamGobbler.OnResultListener neth = new UDPStreamGobbler.OnResultListener(){

                @Override
                public void onResult(String struu) {
                    SharedPreferences prefsPrivate = mConfig.getPrefsPrivate();
                    //SkStatus.logInfo("<b>" + struu.replace(mConfig.getPrivString("UDPServer"), "********") + "</b>");
                    String str = struu.toLowerCase();
                    if (str.contains("zivpn udp running")) {
                        SkStatus.logInfo("<font color='green'><strong>UDP Connected Successfully</strong></font>");
                        SkStatus.logInfo("<font color='#FF7F27'><strong>You are ready to go</strong></font>");
                        SkStatus.updateStateString(SkStatus.SSH_CONECTADO, "SSH connection established");
                        
                    } else if (str.contains("no recent network activity")) {
                        if (prefsPrivate.getBoolean("reconnectup", false)) {
                            SkStatus.logInfo("<font color='red'><strong>Connection Lost retrying</strong></font>");
                            interrupt();
                            return;
                        }
                        SkStatus.logInfo("<font color='red'><strong>UDP connection Lost</strong></font>");
                        stopVudp();
                    } else if (str.contains("connection to server lost") || str.contains("handshake did not complete in time")) {
                        SkStatus.logInfo("<font color='red'><strong>Please wait reconnecting UDP</strong></font>");
                    } else if (str.contains("failed to parse client configuration")) {
                        SkStatus.logInfo("<font color='red'><strong>Invalid UDP setting</strong></font>");
                        stopVudp();
                    } else if (str.contains("auth error")) {
                        SkStatus.logInfo("<font color='red'><strong>Authentication failed, invalid password/expired/may logged-in on another device</strong></font>");
                        stopVudp();
                    } else if (str.contains("failed to set resolver")) {
                        SkStatus.logInfo("<font color='red'><strong>Failed to set resolver?</strong></font>");
                        stopVudp();
                    } else if (str.contains("too many connections")) {
                        SkStatus.logInfo("<font color='red'><strong>Same account on multi-device not support</strong></font>");
                        stopVudp();
                    } else if (str.contains("out of retries, exiting")){
                        SkStatus.logInfo("<font color='red'><strong>Retry limit reached</strong></font>");
                        stopVudp();
                    } else if (str.contains("failed to initialize client")){
                        SkStatus.logInfo("<font color='red'><strong>Failed to initialize client</strong></font>");
                        stopVudp();
                        }
                        }
            };
            try {
                this.udpProcess = new ProcessBuilder(new String[0]).command(strArr).redirectErrorStream(true).start();
                this.out = new UDPStreamGobbler(this.udpProcess.getInputStream(), neth);
                this.err = new UDPStreamGobbler(this.udpProcess.getErrorStream(), neth);
                this.out.setInterrupted(false);
                this.out.start();
                this.err.setInterrupted(false);
                this.err.start();
                SkStatus.updateStateString(SkStatus.SSH_CONECTANDO, context.getString(R.string.state_connecting));
                this.udpProcess.waitFor();
                return;
            } catch (IOException | InterruptedException e5) {
                e5.printStackTrace();
                return;
            }
        }
        interrupt();
    }

    public static Inet4Address getIPv4Addresses(InetAddress[] inetAddressArr) {
        for (InetAddress inetAddress : inetAddressArr) {
            if (inetAddress instanceof Inet4Address) {
                return (Inet4Address) inetAddress;
            }
        }
        return null;
    }

    public void stopVudp() {
        new Thread(new Runnable(){
                @Override
                public void run() {
                    SkStatus.logInfo("Stopping UDP....");
                    interrupt();
                    TunnelManagerHelper.stopSocksHttp(context);
                }
        }).start();
    }

    public void interrupt() {
        UDPStreamGobbler uDPStreamGobbler = this.out;
        if (uDPStreamGobbler != null) {
            uDPStreamGobbler.setInterrupted(true);
            this.out.interrupt();
            this.out = null;
        }
        uDPStreamGobbler = this.err;
        if (uDPStreamGobbler != null) {
            uDPStreamGobbler.setInterrupted(true);
            this.err.interrupt();
            this.err = null;
        }
        Process process = this.udpProcess;
        if (process != null) {
            process.destroy();
            this.udpProcess = null;
        }
        File file = this.fileConf;
        if (file != null && file.exists()) {
            this.fileConf.delete();
        }
        super.interrupt();
    }
}
