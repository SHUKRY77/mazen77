package com.maxtunnel.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class XrayVpnService extends VpnService {

    public static final String ACTION_START_VPN = "com.maxtunnel.app.START_VPN";
    public static final String ACTION_STOP_VPN = "com.maxtunnel.app.STOP_VPN";

    private ParcelFileDescriptor vpnInterface;
    private Process xrayProcess;
    private Process tun2socksProcess;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isProcessRunning = false;
    private int dynamicSocksPort = 10808;

    private static class Resolved {
        String link;
        List<String> ips = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        showNotification("جاري الاتصال بالسيرفر...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_START_VPN.equals(intent.getAction())) {
                startVpn(intent.getStringExtra("ServerLink"));
            } else if (ACTION_STOP_VPN.equals(intent.getAction())) {
                stopVpn();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopVpn();
    }

    private int getAvailablePort() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (Exception e) {
            return (int) (Math.random() * 10000) + 20000;
        }
    }

    private void showNotification(String text) {
        String CHANNEL_ID = "KING_VPN_CHANNEL";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "KING VPN", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        startForeground(1, builder.setContentTitle("KING VPN")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build());
    }

    private void sendStateToUI(String state, String message) {
        Intent intent = new Intent("KING_VPN_STATE");
        intent.putExtra("state", state);
        intent.putExtra("msg", message);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private boolean sendFdToTun2Socks(ParcelFileDescriptor pfd, File socketFile) {
        for (int i = 0; i < 25; i++) {
            try {
                LocalSocket socket = new LocalSocket();
                LocalSocketAddress address = new LocalSocketAddress(socketFile.getAbsolutePath(), LocalSocketAddress.Namespace.FILESYSTEM);
                socket.connect(address);
                socket.setFileDescriptorsForSend(new FileDescriptor[]{pfd.getFileDescriptor()});
                socket.getOutputStream().write(42);
                socket.shutdownOutput();
                socket.close();
                return true;
            } catch (Exception e) {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
        return false;
    }

    private boolean checkStrictRealInternet(int socksPort) {
        long startTime = System.currentTimeMillis();
        try { Thread.sleep(1200); } catch (Exception ignored) {}

        while (System.currentTimeMillis() - startTime < 9000) {
            try {
                java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", socksPort));
                URL url = new URL("http://connectivitycheck.gstatic.com/generate_204");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection(proxy);
                urlConnection.setConnectTimeout(2500);
                urlConnection.setReadTimeout(2500);
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                int responseCode = urlConnection.getResponseCode();
                urlConnection.disconnect();

                if (responseCode == 204 || responseCode == 200 || responseCode == 205) {
                    return true;
                }
                if (responseCode == 301 || responseCode == 302 || responseCode == 307) {
                    return false;
                }
            } catch (Exception e) {
                try { Thread.sleep(800); } catch (Exception ignored) {}
            }
        }
        return false;
    }

    // استخراج قيمة _sid من الرابط (أضافتها اللوحة لجلب السر)
    private static String extractSid(String link) {
        if (link == null) return null;
        int i = link.indexOf("_sid=");
        if (i < 0) return null;
        int s = i + 5;
        int e = s;
        while (e < link.length() && link.charAt(e) != '&' && link.charAt(e) != '#') e++;
        return link.substring(s, e);
    }

    // إزالة بارامتر _sid من الرابط قبل بناء كونفق Xray
    private static String stripSid(String link) {
        if (link == null) return null;
        return link.replaceAll("([?&])_sid=[^&#]*(&)?", "$1").replaceAll("[?&]$", "");
    }

    // ===== تجهيز الرابط: استخراج عنوان السيرفر وتحويله إلى IP لاستثنائه من النفق =====
    private Resolved prepareLink(String link) {
        Resolved r = new Resolved();
        r.link = link;
        try {
            if (link.startsWith("vmess://")) {
                String json = new String(Base64.decode(link.substring(8).trim(), Base64.DEFAULT), "UTF-8");
                JSONObject o = new JSONObject(json);
                String host = o.optString("add", "");
                if (host.isEmpty()) return r;

                List<String> ips = RouteHelper.resolveIpv4(host);
                r.ips.addAll(ips);

                if (!RouteHelper.isIpv4Literal(host) && !ips.isEmpty()) {
                    if (o.optString("sni", "").isEmpty()) o.put("sni", host);
                    if (o.optString("host", "").isEmpty()) o.put("host", host);
                    o.put("add", ips.get(0));
                    r.link = "vmess://" + Base64.encodeToString(o.toString().getBytes("UTF-8"), Base64.NO_WRAP);
                }
            } else if (link.startsWith("vless://") || link.startsWith("trojan://")) {
                int at = link.indexOf('@');
                if (at < 0) return r;
                int hostStart = at + 1;
                if (hostStart >= link.length() || link.charAt(hostStart) == '[') return r; // IPv6 غير مدعوم هنا

                int hostEnd = link.length();
                for (int i = hostStart; i < link.length(); i++) {
                    char c = link.charAt(i);
                    if (c == ':' || c == '?' || c == '/' || c == '#') { hostEnd = i; break; }
                }
                String host = link.substring(hostStart, hostEnd);
                if (host.isEmpty()) return r;

                List<String> ips = RouteHelper.resolveIpv4(host);
                r.ips.addAll(ips);

                if (!RouteHelper.isIpv4Literal(host) && !ips.isEmpty()) {
                    String newLink = link.substring(0, hostStart) + ips.get(0) + link.substring(hostEnd);

                    int hash = newLink.indexOf('#');
                    String frag = hash >= 0 ? newLink.substring(hash) : "";
                    String base = hash >= 0 ? newLink.substring(0, hash) : newLink;

                    if (!base.contains("?")) base += "?";
                    else if (!base.endsWith("?") && !base.endsWith("&")) base += "&";

                    if (!base.contains("?sni=") && !base.contains("&sni=")) base += "sni=" + host + "&";
                    if (!base.contains("?host=") && !base.contains("&host=")) base += "host=" + host + "&";

                    if (base.endsWith("&")) base = base.substring(0, base.length() - 1);
                    r.link = base + frag;
                }
            }
        } catch (Exception e) {
            Log.e("XrayVpnService", "prepareLink failed: " + e.getMessage());
        }
        return r;
    }

    private void startVpn(String serverLink) {
        isProcessRunning = true;

        new Thread(() -> {
            try {
                if (serverLink == null || serverLink.isEmpty()) {
                    sendStateToUI("ERROR", "رابط السيرفر فارغ!");
                    stopVpn(); return;
                }

                // ===== جلب الـ UUID/password الحقيقي لحظة الاتصال واستبدال AUTH =====
                // الرابط القادم من اللوحة لا يحوي السر (vless://AUTH@...)؛ نجلبه بمصافحة موقّعة
                String link = serverLink;
                String sid = extractSid(link);
                link = stripSid(link);
                if (link.contains("://AUTH@") || (sid != null && !sid.isEmpty())) {
                    org.json.JSONObject cr = SecureApi.fetchCreds(this, sid);
                    if (cr != null && cr.has("secret") && !cr.optString("secret").isEmpty()) {
                        String secret = cr.getString("secret");
                        link = link.replaceFirst("://AUTH@", "://" + java.util.regex.Matcher.quoteReplacement(secret) + "@");
                    } else {
                        sendStateToUI("ERROR", "فشل جلب مفتاح السيرفر المؤمّن (تحقّق من الاتصال)!");
                        stopVpn(); return;
                    }
                }

                dynamicSocksPort = getAvailablePort();

                SharedPreferences prefs = getSharedPreferences("vpn_settings_data", Context.MODE_PRIVATE);
                prefs.edit().putInt("current_socks_port", dynamicSocksPort).apply();

                String nativeDir = getApplicationInfo().nativeLibraryDir;
                File xrayFile = new File(nativeDir, "libxray.so");
                File tunFile = new File(nativeDir, "libtun2socks_xray.so");

                xrayFile.setExecutable(true);
                tunFile.setExecutable(true);

                if (!xrayFile.exists() || !tunFile.exists()) {
                    sendStateToUI("ERROR", "ملفات المحركات مفقودة داخل النظام (jniLibs)!");
                    stopVpn(); return;
                }

                // تحويل دومين السيرفر إلى IP (قبل إنشاء النفق) + تجهيز قائمة الاستثناء
                Resolved res = prepareLink(link);

                String configJson = VlessParser.generateXrayConfig(res.link);
                configJson = configJson.replace("10808", String.valueOf(dynamicSocksPort));

                File conf = new File(getFilesDir(), "config.json");
                if (conf.exists()) conf.delete();
                FileWriter fw = new FileWriter(conf);
                fw.write(configJson);
                fw.flush();
                fw.close();

                Builder builder = new Builder()
                        .setSession("KING-VPN")
                        .setMtu(1400)
                        .addAddress("10.0.8.2", 24)
                        .addDnsServer("1.1.1.1")
                        .addDnsServer("8.8.8.8");

                // كل الإنترنت داخل النفق (بما فيها إعلانات التطبيق) ما عدا IP السيرفر لمنع الحلقة
                RouteHelper.addRoutesExcluding(builder, res.ips);

                vpnInterface = builder.establish();
                if (vpnInterface == null) {
                    sendStateToUI("ERROR", "تم رفض إذن الـ VPN من النظام!");
                    stopVpn(); return;
                }

                xrayProcess = new ProcessBuilder(xrayFile.getAbsolutePath(), "run", "-c", conf.getAbsolutePath())
                        .directory(getFilesDir()).redirectErrorStream(true).start();
                startLogRadar(xrayProcess, "XRAY");

                Thread.sleep(400);
                if (!isProcessRunning || vpnInterface == null) return;

                File socketDir = new File(getApplicationInfo().dataDir, "tun_sockets");
                if (!socketDir.exists()) socketDir.mkdirs();
                File socketFile = new File(socketDir, "tun2socks.sock");
                if (socketFile.exists()) socketFile.delete();

                tun2socksProcess = new ProcessBuilder(
                        tunFile.getAbsolutePath(),
                        "--netif-ipaddr", "10.0.8.2",
                        "--netif-netmask", "255.255.255.0",
                        "--socks-server-addr", "127.0.0.1:" + dynamicSocksPort,
                        "--socks5-udp",
                        "--tunmtu", "1400",
                        "--sock-path", socketFile.getAbsolutePath(),
                        "--loglevel", "3"
                ).directory(socketDir).redirectErrorStream(true).start();

                startLogRadar(tun2socksProcess, "TUN2SOCKS");

                new Thread(() -> {
                    boolean success = sendFdToTun2Socks(vpnInterface, socketFile);
                    if (success) {
                        boolean hasRealInternet = checkStrictRealInternet(dynamicSocksPort);

                        handler.post(() -> {
                            if (isProcessRunning) {
                                if (hasRealInternet) {
                                    showNotification("متصل ومحمي بنجاح");
                                    sendStateToUI("CONNECTED", "تم الاتصال");
                                } else {
                                    sendStateToUI("ERROR", "السيرفر لا يمرر إنترنت فعلي لهذه الشبكة!");
                                    stopVpn();
                                }
                            }
                        });
                    } else {
                        sendStateToUI("ERROR", "فشل ربط منفذ النفق بالمحرك");
                        stopVpn();
                    }
                }).start();

            } catch (Exception e) {
                sendStateToUI("ERROR", "خطأ في تشغيل النفق: " + e.getMessage());
                stopVpn();
            }
        }).start();
    }

    private void startLogRadar(Process process, String name) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                StringBuilder fullLog = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    Log.e("VPN-RADAR-" + name, line);
                    fullLog.append(line).append("\n");
                }
                int exitCode = process.waitFor();
                if (exitCode != 0 && isProcessRunning) {
                    String errorMsg = fullLog.toString().trim();
                    if (errorMsg.isEmpty()) {
                        errorMsg = "المحرك توقف (Exit Code " + exitCode + ")";
                    } else if (errorMsg.length() > 120) {
                        errorMsg = errorMsg.substring(errorMsg.length() - 120);
                    }
                    sendStateToUI("ERROR", name + " حدث خطأ: " + errorMsg);
                }
            } catch (Exception e) {
                Log.e("VPN-RADAR-" + name, "سجل الأخطاء: " + e.getMessage());
            }
        }).start();
    }

    private void stopVpn() {
        isProcessRunning = false;

        try { if (xrayProcess != null) { xrayProcess.destroy(); xrayProcess = null; } } catch (Exception ignored) {}
        try { if (tun2socksProcess != null) { tun2socksProcess.destroy(); tun2socksProcess = null; } } catch (Exception ignored) {}
        try { if (vpnInterface != null) { vpnInterface.close(); vpnInterface = null; } } catch (Exception ignored) {}

        stopForeground(true);
        stopSelf();
    }
}