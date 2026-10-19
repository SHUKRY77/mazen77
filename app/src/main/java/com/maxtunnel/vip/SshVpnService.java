package com.maxtunnel.vip;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.util.Log;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SshVpnService extends VpnService {

    public static final String ACTION_START_SSH = "app.fixbuild.vpncore.assag.START_SSH";
    public static final String ACTION_STOP_SSH = "app.fixbuild.vpncore.assag.STOP_SSH";

    private static final String CONN_TYPE_SSL_PAYLOAD = "ssl_payload"; 
    private static final String CONN_TYPE_SSL_PROXY   = "ssl_proxy";   
    private static final String CONN_TYPE_DIRECT      = "direct";      
    private static final String CONN_TYPE_UDP         = "udp";         
    private static final String CONN_TYPE_SLOWDNS     = "slowdns";     

    private static final int REMOTE_UDPGW_PORT = 7300;
    private static final int LOCAL_UDPGW_FORWARD_PORT = 7300;

    private ParcelFileDescriptor vpnInterface;
    private Process tun2socksProcess;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isProcessRunning = false;

    private JSch jsch;
    private Session sshSession;
    private ServerSocket socksServerSocket;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        showNotification("Initializing VPN Core...");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KingVpn::SshWakeLock");
            wakeLock.acquire(10 * 60 * 1000L);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_START_SSH.equals(intent.getAction())) {
                startSshTunnel(intent.getStringExtra("PayloadData"));
            } else if (ACTION_STOP_SSH.equals(intent.getAction())) {
                stopVpn();
            }
        }
        return START_STICKY;
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

        startForeground(1, builder.setContentTitle("KING VPN Service")
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

    private void startSshTunnel(String payloadData) {
        isProcessRunning = true;
        new Thread(() -> {
            try {
                Pq.p("Starting SSH connection...");
                if (payloadData == null || payloadData.isEmpty()) {
                    throw new Exception("Payload data is empty!");
                }

                String[] parts = payloadData.split("\\|", -1);
                if (parts.length < 8) {
                    throw new Exception("تكوين السيرفر غير صالح!");
                }

                String connType = parts[1] == null ? "" : parts[1].trim().toLowerCase();
                String sni = parts[2].trim();
                String squidIp = parts[3].trim();
                int squidPort = 80;
                try {
                    squidPort = Integer.parseInt(parts[4].trim());
                } catch (Exception ignored) {}
                String customPayload = parts[5];
                String sshHostField = parts[6].trim();
                String sid = parts[7].trim();

                Pq.p("جلب بيانات الدخول بشكل مؤمّن...");
                org.json.JSONObject cr = SecureApi.fetchCreds(this, sid);
                if (cr == null || !cr.has("user") || !cr.has("pass")) {
                    throw new Exception("فشل جلب بيانات السيرفر المؤمّنة (تحقّق من الاتصال).");
                }
                String sshUser = cr.getString("user");
                String sshPass = cr.getString("pass");

                if (connType.isEmpty()) connType = CONN_TYPE_SSL_PAYLOAD;

                // SlowDNS هو الوحيد المؤجل لأنه يحتاج ملف dnstt-client
                if (connType.equals(CONN_TYPE_SLOWDNS)) {
                    throw new Exception("نوع الاتصال SlowDNS يحتاج ملف dnstt-client في jniLibs.");
                }

                String sshHostOnly = sshHostField.contains(":") ? sshHostField.split(":")[0] : sshHostField;
                int sshPort = 443;
                if (sshHostField.contains(":")) {
                    try {
                        sshPort = Integer.parseInt(sshHostField.split(":")[1]);
                    } catch (Exception ignored) {}
                }

                jsch = new JSch();
                sshSession = jsch.getSession(sshUser, sshHostOnly, sshPort);
                sshSession.setPassword(sshPass);
                sshSession.setConfig("StrictHostKeyChecking", "no");
                sshSession.setServerAliveInterval(15000);
                sshSession.setServerAliveCountMax(2);

                // تم تفعيل Direct SSH و SSH UDP للعمل مباشرة وبأقل زمن تأخير
                if (connType.equals(CONN_TYPE_DIRECT)) {
                    Pq.p("وضع الاتصال: Direct SSH (تم تفعيله بنجاح ✔️)");
                } else if (connType.equals(CONN_TYPE_UDP)) {
                    Pq.p("وضع الاتصال: SSH UDP / BadVPN (تم تفعيله بنجاح ✔️)");
                } else if (connType.equals(CONN_TYPE_SSL_PROXY)) {
                    Pq.p("وضع الاتصال: SSL Proxy (تم تفعيله بنجاح ✔️️)");
                    sshSession.setProxy(new SslConnectProxy(squidIp, squidPort, sni, sshHostField));
                } else {
                    Pq.p("وضع الاتصال: SSL + Payload (تم تفعيله بنجاح ✔️)");
                    sshSession.setProxy(new CloudflareWsProxy(squidIp, squidPort, sni, customPayload, sshHostOnly));
                }

                Pq.p("Authenticating...");
                sshSession.connect(20000);
                Pq.p("Authentication successful.");

                Pq.p("Starting local proxy routing...");
                startLocalSocksServer(sshSession, 10808);

                // فتح وإعادة توجيه منفذ UDP 7300 للألعاب والمكالمات
                boolean udpForwardOk = false;
                try {
                    sshSession.setPortForwardingL("127.0.0.1", LOCAL_UDPGW_FORWARD_PORT, "127.0.0.1", REMOTE_UDPGW_PORT);
                    udpForwardOk = true;
                    Pq.p("✅ تم فتح قناة UDP Gateway بنجاح (منفذ 7300)");
                } catch (Exception e) {
                    Pq.p("⚠️ [UDP Forwarding]: " + e.getMessage());
                }
                final boolean finalUdpForwardOk = udpForwardOk;

                Pq.p("Configuring network interface...");

                Builder builder = new Builder()
                        .setSession("KING-SSH")
                        .setMtu(1400)
                        .addAddress("10.0.8.2", 24)
                        .addRoute("0.0.0.0", 0)
                        .addDnsServer("1.1.1.1")
                        .addDnsServer("8.8.8.8");

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder.addDisallowedApplication(getPackageName());
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("SshVpnService", "Failed to exclude app: " + e.getMessage());
                }

                vpnInterface = builder.establish();
                if (vpnInterface == null) throw new Exception("VPN permission denied.");

                File socketDir = new File(getApplicationInfo().dataDir, "tun_sockets");
                if (!socketDir.exists()) socketDir.mkdirs();
                File socketFile = new File(socketDir, "ssh_tun2socks.sock");
                if (socketFile.exists()) socketFile.delete();

                String nativeDir = getApplicationInfo().nativeLibraryDir;
                File tunFile = new File(nativeDir, "libtun2socks.so");

                List<String> tun2socksArgs = new ArrayList<>();
                tun2socksArgs.add(tunFile.getAbsolutePath());
                tun2socksArgs.add("--netif-ipaddr"); tun2socksArgs.add("10.0.8.2");
                tun2socksArgs.add("--netif-netmask"); tun2socksArgs.add("255.255.255.0");
                tun2socksArgs.add("--socks-server-addr"); tun2socksArgs.add("127.0.0.1:10808");
                tun2socksArgs.add("--tunmtu"); tun2socksArgs.add("1400");
                tun2socksArgs.add("--sock-path"); tun2socksArgs.add(socketFile.getAbsolutePath());
                tun2socksArgs.add("--loglevel"); tun2socksArgs.add("3");

                // تمرير UDPGW لـ libtun2socks إذا تم اختيار UDP أو توفر التوجيه
                if (finalUdpForwardOk || connType.equals(CONN_TYPE_UDP)) {
                    tun2socksArgs.add("--udpgw-remote-server-addr");
                    tun2socksArgs.add("127.0.0.1:" + LOCAL_UDPGW_FORWARD_PORT);
                }

                tun2socksProcess = new ProcessBuilder(tun2socksArgs)
                        .directory(socketDir).redirectErrorStream(true).start();

                boolean fdSent = false;
                for (int i = 0; i < 20; i++) {
                    try {
                        LocalSocket socket = new LocalSocket();
                        LocalSocketAddress address = new LocalSocketAddress(socketFile.getAbsolutePath(), LocalSocketAddress.Namespace.FILESYSTEM);
                        socket.connect(address);
                        socket.setFileDescriptorsForSend(new FileDescriptor[]{vpnInterface.getFileDescriptor()});
                        socket.getOutputStream().write(42);
                        socket.shutdownOutput();
                        socket.close();
                        fdSent = true;
                        break;
                    } catch (Exception e) { Thread.sleep(200); }
                }

                if (fdSent) {
                    Pq.p("Connection established.");
                    handler.post(() -> {
                        if (isProcessRunning) {
                            showNotification("Connected");
                            sendStateToUI("CONNECTED", "VPN Connected!");
                        }
                    });
                    monitorConnection();
                } else {
                    throw new Exception("System binding failed.");
                }

            } catch (Exception e) {
                sendStateToUI("ERROR", e.getMessage());
                stopVpn();
            }
        }).start();
    }

    private void monitorConnection() {
        new Thread(() -> {
            int minutesPassed = 0;
            while (isProcessRunning) {
                try {
                    Thread.sleep(60000);
                    minutesPassed++;
                    if (sshSession != null && !sshSession.isConnected()) {
                        final int finalMinutes = minutesPassed;
                        handler.post(() -> {
                            sendStateToUI("ERROR", "Connection dropped at minute " + finalMinutes);
                            stopVpn();
                        });
                        break;
                    }
                } catch (Exception e) {}
            }
        }).start();
    }

    private void startLocalSocksServer(Session session, int port) {
        new Thread(() -> {
            try {
                socksServerSocket = new ServerSocket(port);
                while (isProcessRunning) {
                    Socket clientSocket = socksServerSocket.accept();
                    new Thread(() -> handleSocksClient(clientSocket, session)).start();
                }
            } catch (Exception e) {
                if (isProcessRunning) {
                    Pq.p("🚨 SOCKS5 Server Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void handleSocksClient(Socket clientSocket, Session session) {
        com.jcraft.jsch.Channel channel = null;
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            if (in.read() != 5) return;
            int nmethods = in.read();
            byte[] methods = new byte[nmethods];
            in.read(methods);
            out.write(new byte[]{5, 0});
            out.flush();

            if (in.read() != 5) return;
            int cmd = in.read();
            in.read();
            int atyp = in.read();

            String host = "";
            if (atyp == 1) {
                byte[] ip = new byte[4];
                in.read(ip);
                host = (ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "." + (ip[3] & 0xFF);
            } else if (atyp == 3) {
                int len = in.read();
                byte[] domain = new byte[len];
                in.read(domain);
                host = new String(domain);
            } else if (atyp == 4) {
                byte[] ip = new byte[16];
                in.read(ip);
            } else {
                return;
            }

            int destPort = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);

            if (cmd == 1 && host != null && !host.isEmpty()) {
                channel = session.openChannel("direct-tcpip");
                ((com.jcraft.jsch.ChannelDirectTCPIP) channel).setHost(host);
                ((com.jcraft.jsch.ChannelDirectTCPIP) channel).setPort(destPort);
                channel.connect(10000);

                out.write(new byte[]{5, 0, 0, 1, 0, 0, 0, 0, 0, 0});
                out.flush();

                InputStream channelIn = channel.getInputStream();
                OutputStream channelOut = channel.getOutputStream();

                final com.jcraft.jsch.Channel finalChannel = channel;
                new Thread(() -> forwardData(in, channelOut, clientSocket, finalChannel)).start();
                forwardData(channelIn, out, clientSocket, finalChannel);
            }
        } catch (Exception e) {
        } finally {
            try { clientSocket.close(); } catch (Exception ignored) {}
            try { if (channel != null) channel.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void forwardData(InputStream in, OutputStream out, Socket clientSocket, com.jcraft.jsch.Channel channel) {
        try {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
                out.flush();
            }
        } catch (Exception ignored) {
        } finally {
            try { clientSocket.close(); } catch (Exception ignored) {}
            try { channel.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void stopVpn() {
        isProcessRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) {}
        }
        try { if (socksServerSocket != null && !socksServerSocket.isClosed()) socksServerSocket.close(); } catch (Exception ignored) {}
        try { if (sshSession != null && sshSession.isConnected()) sshSession.disconnect(); } catch (Exception ignored) {}
        if (tun2socksProcess != null) { tun2socksProcess.destroy(); tun2socksProcess = null; }
        try { if (vpnInterface != null) { vpnInterface.close(); vpnInterface = null; } } catch (Exception ignored) {}
        stopForeground(true);
        stopSelf();
    }
}
