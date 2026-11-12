package app.ovpnudp.developer.thread;


import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;


import app.ovpnudp.developer.service.OpenVPNService;
import app.ovpnudp.developer.service.SocksDNSService;
import app.ovpnudp.developer.service.vpn.TunnelManagerHelper;
import app.ovpnudp.developer.utils.ConfigUtil;
import app.ovpnudp.developer.utils.VPNUtil;


public class DNSTunnelThread extends Thread {

    private Context mContext;
    private static final String DNS_BIN = "liboglangyan";
    private Process dnsProcess;
    private File filedns;



    public DNSTunnelThread(Context context) {
        mContext = context;
    }
    public static SocksListener socksListener;

    public interface SocksListener
    {
        void startSocksOpenVPN();
        void addStatus(String log);
        void stopSocksOpenVPN();

    }
    public static void setSocksListener(SocksListener socksListener2)
    {
        socksListener = socksListener2;
    }

    @Override
    public void run(){
        try {

            //String mDns = mConfig.getPrivString(Settings.DNS_KEY);
            StringBuilder cmd1 = new StringBuilder();
            String a = mContext.getApplicationInfo().nativeLibraryDir;
           // filedns = CustomNativeLoader2.loadNativeBinary(mContext, DNS_BIN, new File(mContext.getFilesDir(),DNS_BIN));
            filedns =new File(a,DNS_BIN+".so"); //
           // CustomNativeLoader2.setExecutable(filedns);
            if (filedns == null){
                socksListener.addStatus("Bin Not found");
                throw new IOException("DNS bin not found");
            }

          //  socksListener.addStatus(filedns.getCanonicalPath());
         //   String contentcer = FileUtils.readFromRaw(mContext, R.raw.ca);
        //    File fcert = new File(mContext.getFilesDir(),"ca.crt");
         //   if (fcert.exists()) {
       //         fcert.delete();
       //     }
       //     FileUtils.saveTextFile(fcert, contentcer);

         /*   File cachecert = new File(mContext.getFilesDir(),"ca.crt");
            if (!cachecert.exists()) {
                try {
                    cachecert.createNewFile();
                } catch (Exception e) {}
            }

          */
            ConfigUtil config = new ConfigUtil(mContext);
            String content = config.getUDPConfig();//FileUtils.readFromRaw(mContext, R.raw.udp).replace("certcalink",fcert.getPath());
            if (content.contains(OpenVPNService.ar)) {
                String server = config.getSSHHost();
                InetAddress[] inetAddressArr = new InetAddress[0];
                try {
                    inetAddressArr = InetAddress.getAllByName(server);
                } catch (UnknownHostException e2) {
                    e2.printStackTrace();
                }
                try {
                    server = getIPv4Addresses(inetAddressArr).getHostAddress();
                } catch (Exception e) {}
                content = content.replace(OpenVPNService.ar, server);
                config.setSSHHost(server);
            }
            File fileConf = makeConfig(mContext.getFilesDir(),content);
            cmd1.append(filedns.getCanonicalPath());
            cmd1.append(" -c "+ fileConf.getPath() +" client");
            dnsProcess = Runtime.getRuntime().exec(cmd1.toString());
            socksListener.addStatus("Waiting for UDP response");
            StreamGobbler.OnLineListener onLineListener = new StreamGobbler.OnLineListener(){
                @Override
                public void onLine(String log){

                   // socksListener.addStatus(log);
                    if(log.contains("Connected")){

                      //  VpnStatus.logInfo("Connection Established");

                        socksListener.startSocksOpenVPN();
                        	SocksDNSService.startmanager();
                    }
                    if(log.contains("UDP running")){
                        socksListener.addStatus("UDP is running");
                     //   VpnStatus.logInfo("UDP running");
                    }
                    if(log.contains("auth error")){
                    //    VpnStatus.logInfo("Invalid authentication!");
                        socksListener.addStatus("Invalid Authentication!");
                        socksListener.stopSocksOpenVPN();
                        TunnelManagerHelper.stopSocksHttp(mContext);
                    }
                    if(log.contains("exiting")){
                        socksListener.stopSocksOpenVPN();
                        TunnelManagerHelper.stopSocksHttp(mContext);
                    }
                    if(log.contains("unknown command")){
                        socksListener.stopSocksOpenVPN();
                        TunnelManagerHelper.stopSocksHttp(mContext);
                    }
                    if(log.contains("reconnecting")){

                    }
                    if(log.contains("network is unreachable")){


                    }
                }
            };
            StreamGobbler stdoutGobbler = new StreamGobbler(dnsProcess.getInputStream(), onLineListener);
            StreamGobbler stderrGobbler = new StreamGobbler(dnsProcess.getErrorStream(), onLineListener);
            stdoutGobbler.start();
            stderrGobbler.start();

            dnsProcess.waitFor();
        } catch (IOException e) {
            //SkStatus.logInfo("SlowDNS: " + e);
        }catch (InterruptedException e){
            //SkStatus.logInfo("SlowDNS: " + e);
        }
    }
    @Override
    public void interrupt(){
        if (dnsProcess != null){
            socksListener.addStatus("Disconnected");
            dnsProcess.destroy();
        }


        try {
            if (filedns != null)
                VPNUtil.killProcess(filedns);
        } catch (Exception e) {}

        dnsProcess = null;
        filedns = null;
        super.interrupt();
    }
    private Inet4Address getIPv4Addresses(InetAddress[] inetAddressArr) {
        for (InetAddress inetAddress : inetAddressArr) {
            if (inetAddress instanceof Inet4Address) {
                return (Inet4Address) inetAddress;
            }
        }
        return null;
    }
    private File makeConfig(File fileDir,String content) throws FileNotFoundException, IOException {
        File f = new File(fileDir,"udpconfig.json");
        if (f.exists()) {
            f.delete();
        }
        VPNUtil.saveTextFile(f, content);
        File cache = new File(fileDir,"udpconfig.json");
        if (!cache.exists()) {
            try {
                cache.createNewFile();
            } catch (Exception e) {}
        }
        return f;
    }

}
