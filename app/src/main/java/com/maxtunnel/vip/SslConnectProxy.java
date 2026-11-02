package com.maxtunnel.vip;

import android.util.Base64;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.SocketFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * SSH TLS + Proxy transport. It supports a normal TLS-wrapped CONNECT proxy
 * and a Cloudflare HTTP upgrade endpoint, which rejects CONNECT with 400.
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
        this.sni = sni == null ? "" : sni;
        String host = sshHostField == null ? "" : sshHostField;
        int port = 22;
        int colon = host.lastIndexOf(':');
        if (colon > 0 && colon < host.length() - 1) {
            try { port = Integer.parseInt(host.substring(colon + 1)); host = host.substring(0, colon); }
            catch (Exception ignored) {}
        }
        this.targetHost = host;
        this.targetPort = port;
    }

    @Override public void connect(SocketFactory ignored, String host, int port, int timeout) throws Exception {
        Pq.p("[SSL Proxy] جاري الاتصال بالبروكسي...");
        try {
            openTls(timeout);
            Pq.p("[SSL Proxy] اكتملت مصافحة TLS بنجاح.");
            String response = sendConnectAndRead(timeout);
            if (isSuccess(response)) {
                Pq.p("[SSL Proxy] تم إنشاء النفق بنجاح عبر CONNECT.");
                return;
            }
            // Cloudflare's HTTP edge does not implement CONNECT. Reconnect and
            // use an HTTP upgrade request instead; the old TLS socket is closed.
            if (looksLikeCloudflare(response)) {
                Pq.p("[SSL Proxy] Cloudflare رفض CONNECT؛ تجربة HTTP Upgrade...");
                closeSocket();
                openTls(timeout);
                String upgrade = sendCloudflareUpgradeAndRead();
                if (upgrade.contains(" 101 ") || upgrade.contains(" 200 ")) {
                    Pq.p("[SSL Proxy] تم إنشاء النفق عبر HTTP Upgrade.");
                    return;
                }
                throw new Exception("فشل HTTP Upgrade عبر SSL Proxy. الاستجابة: " + upgrade.trim());
            }
            throw new Exception("فشل طلب CONNECT عبر SSL Proxy. الاستجابة: " + response.trim());
        } catch (Exception e) {
            Pq.p("[SSL Proxy] خطأ: " + e.getMessage());
            closeSocket();
            throw e;
        }
    }

    private void openTls(int timeout) throws Exception {
        Socket raw = new Socket();
        raw.connect(new InetSocketAddress(proxyIp, proxyPort), timeout);
        raw.setSoTimeout(timeout);
        Pq.p("[SSL Proxy] تم الاتصال بالبروكسي، جاري بدء مصافحة TLS...");
        SSLSocket ssl = (SSLSocket) SshTlsSocketFactory.create().createSocket(raw, proxyIp, proxyPort, true);
        if (!sni.isEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            SSLParameters p = ssl.getSSLParameters();
            p.setServerNames(Collections.singletonList(new SNIHostName(sni)));
            ssl.setSSLParameters(p);
        }
        ssl.startHandshake();
        socket = ssl;
        in = ssl.getInputStream();
        out = ssl.getOutputStream();
    }

    private String sendConnectAndRead(int timeout) throws Exception {
        String request = "CONNECT " + targetHost + ":" + targetPort + " HTTP/1.1\r\n"
                + "Host: " + targetHost + ":" + targetPort + "\r\n"
                + "Proxy-Connection: Keep-Alive\r\nConnection: Keep-Alive\r\n"
                + "User-Agent: Dalvik/2.1.0 (Linux; U; Android 11)\r\n\r\n";
        Pq.p("[SSL Proxy] إرسال طلب CONNECT إلى " + targetHost + ":" + targetPort);
        out.write(request.getBytes(StandardCharsets.UTF_8)); out.flush();
        return readHeaders();
    }

    private String sendCloudflareUpgradeAndRead() throws Exception {
        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        String key = Base64.encodeToString(nonce, Base64.NO_WRAP);
        String hostHeader = sni.isEmpty() ? targetHost : sni;
        String request = "GET / HTTP/1.1\r\nHost: " + hostHeader + "\r\n"
                + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                + "Sec-WebSocket-Key: " + key + "\r\nSec-WebSocket-Version: 13\r\n"
                + "User-Agent: Dalvik/2.1.0 (Linux; Android 11)\r\n\r\n";
        out.write(request.getBytes(StandardCharsets.UTF_8)); out.flush();
        return readHeaders();
    }

    private String readHeaders() throws Exception {
        StringBuilder response = new StringBuilder();
        int c;
        while ((c = in.read()) >= 0) {
            response.append((char) c);
            if (response.length() > 16384 || response.toString().endsWith("\r\n\r\n")) break;
        }
        return response.toString();
    }

    private boolean isSuccess(String response) {
        return response.contains(" 200 ") || response.contains("200 Connection established");
    }

    private boolean looksLikeCloudflare(String response) {
        String lower = response.toLowerCase();
        return lower.contains("server: cloudflare") || lower.contains(" 400 bad request")
                || lower.contains(" 405 method not allowed");
    }

    private void closeSocket() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        in = null; out = null; socket = null;
    }

    @Override public InputStream getInputStream() { return in; }
    @Override public OutputStream getOutputStream() { return out; }
    @Override public Socket getSocket() { return socket; }
    @Override public void close() { closeSocket(); }
}
