package com.maxtunnel.vip;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.SocketFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * نوع اتصال "SSL Proxy": يفتح نفق TLS مع بروكسي وسيط (Squid / 3proxy / Nginx stream...)
 * ثم يرسل طلب HTTP CONNECT القياسي للوصول لسيرفر SSH الحقيقي خلف البروكسي.
 *
 * يختلف عن CloudflareWsProxy (SSL + Payload) في أن هذا النوع لا يحتاج تمويه WebSocket
 * أو حقن رؤوس مخصصة — فقط طلب CONNECT عادي كما تفعله المتصفحات مع أي بروكسي HTTPS قياسي.
 *
 * تنسيق القراءة من الحقل ssh_host: يدعم "host" فقط (ويُفترض المنفذ 22 افتراضياً)
 * أو "host:port" صراحة لو منفذ SSH الحقيقي خلف البروكسي مختلف عن 22.
 */
public class SslConnectProxy implements Proxy {

    private final String proxyIp;
    private final int proxyPort;
    private final String sni;
    private final String targetHost;
    private final int targetPort;

    private InputStream in;
    private OutputStream out;
    private Socket socket;

    public SslConnectProxy(String proxyIp, int proxyPort, String sni, String sshHostField) {
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.sni = sni;

        String host = sshHostField;
        int port = 22;
        if (sshHostField != null && sshHostField.contains(":")) {
            String[] hp = sshHostField.split(":");
            host = hp[0];
            try {
                port = Integer.parseInt(hp[1]);
            } catch (Exception ignored) {}
        }
        this.targetHost = host;
        this.targetPort = port;
    }

    @Override
    public void connect(SocketFactory socketFactory, String host, int port, int timeout) throws Exception {
        Pq.p("[SSL Proxy] جاري الاتصال بالبروكسي...");
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(proxyIp, proxyPort), timeout);
            socket.setSoTimeout(timeout);
            Pq.p("[SSL Proxy] تم الاتصال بالبروكسي، جاري بدء مصافحة TLS...");

            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(socket, proxyIp, proxyPort, true);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                SSLParameters sslParameters = sslSocket.getSSLParameters();
                sslParameters.setServerNames(Collections.singletonList(new SNIHostName(sni)));
                sslSocket.setSSLParameters(sslParameters);
            }
            sslSocket.startHandshake();
            Pq.p("[SSL Proxy] اكتملت مصافحة TLS بنجاح.");

            in = sslSocket.getInputStream();
            out = sslSocket.getOutputStream();

            String connectRequest = "CONNECT " + targetHost + ":" + targetPort + " HTTP/1.1\r\n"
                    + "Host: " + targetHost + ":" + targetPort + "\r\n"
                    + "Proxy-Connection: Keep-Alive\r\n"
                    + "Connection: Keep-Alive\r\n"
                    + "User-Agent: Dalvik/2.1.0 (Linux; U; Android 11)\r\n"
                    + "\r\n";

            Pq.p("[SSL Proxy] إرسال طلب CONNECT إلى " + targetHost + ":" + targetPort);
            out.write(connectRequest.getBytes("UTF-8"));
            out.flush();

            // قراءة رؤوس الاستجابة حرفاً بحرف (بدون BufferedReader) حتى لا نسحب
            // بالخطأ أي بايتات تخص بروتوكول SSH القادمة بعد نهاية الرؤوس
            StringBuilder response = new StringBuilder();
            int c;
            while ((c = in.read()) >= 0) {
                response.append((char) c);
                if (response.toString().endsWith("\r\n\r\n")) {
                    break;
                }
            }

            String responseStr = response.toString();
            boolean isTunnelOpen = responseStr.contains(" 200 ") || responseStr.contains("200 Connection established");

            if (!isTunnelOpen) {
                throw new Exception("فشل طلب CONNECT عبر SSL Proxy. الاستجابة: " + responseStr.trim());
            }

            Pq.p("[SSL Proxy] تم إنشاء النفق بنجاح عبر البروكسي.");
        } catch (Exception e) {
            Pq.p("[SSL Proxy] خطأ: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public InputStream getInputStream() { return in; }

    @Override
    public OutputStream getOutputStream() { return out; }

    @Override
    public Socket getSocket() { return socket; }

    @Override
    public void close() {
        try { if (in != null) in.close(); } catch (Exception e) {}
        try { if (out != null) out.close(); } catch (Exception e) {}
        try { if (socket != null) socket.close(); } catch (Exception e) {}
    }
}
