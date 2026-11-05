package com.abusultan.vip;

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
 * SSH transport with an optional HTTP payload. The payload is sent only after
 * the selected base transport is ready, then JSch continues over the same stream.
 */
public final class PayloadSocketProxy implements Proxy {
    public enum Mode { DIRECT, HTTP_CONNECT, TLS_DIRECT, TLS_PROXY }

    private final Mode mode;
    private final String proxyHost;
    private final int proxyPort;
    private final String sni;
    private final String payload;
    private final String targetHost;
    private final int targetPort;
    private InputStream in;
    private OutputStream out;
    private Socket socket;

    public PayloadSocketProxy(Mode mode, String proxyHost, int proxyPort, String sni,
                              String payload, String targetHost, int targetPort) {
        this.mode = mode;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.sni = sni;
        this.payload = payload == null ? "" : payload;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    @Override
    public void connect(SocketFactory ignored, String host, int port, int timeout) throws Exception {
        String destinationHost = targetHost == null || targetHost.isEmpty() ? host : targetHost;
        int destinationPort = targetPort > 0 ? targetPort : port;
        Socket raw = new Socket();
        if (mode == Mode.HTTP_CONNECT || mode == Mode.TLS_PROXY) {
            raw.connect(new InetSocketAddress(proxyHost, proxyPort), timeout);
        } else {
            raw.connect(new InetSocketAddress(destinationHost, destinationPort), timeout);
        }
        raw.setSoTimeout(timeout);
        socket = raw;

        if (mode == Mode.HTTP_CONNECT) {
            in = raw.getInputStream(); out = raw.getOutputStream();
            writeConnect(destinationHost, destinationPort);
        } else if (mode == Mode.TLS_PROXY) {
            SSLSocket ssl = tls(raw, proxyHost, proxyPort);
            in = ssl.getInputStream(); out = ssl.getOutputStream();
            writeConnect(destinationHost, destinationPort);
        } else if (mode == Mode.TLS_DIRECT) {
            SSLSocket ssl = tls(raw, sni == null || sni.isEmpty() ? destinationHost : sni, destinationPort);
            in = ssl.getInputStream(); out = ssl.getOutputStream(); socket = ssl;
        } else {
            in = raw.getInputStream(); out = raw.getOutputStream();
        }
        sendPayload(destinationHost);
    }

    private SSLSocket tls(Socket raw, String serverName, int port) throws Exception {
        SSLSocketFactory factory = SshTlsSocketFactory.create();
        SSLSocket ssl = (SSLSocket) factory.createSocket(raw, serverName, port, true);
        if (serverName != null && !serverName.isEmpty() && android.os.Build.VERSION.SDK_INT >= 24) {
            SSLParameters p = ssl.getSSLParameters();
            p.setServerNames(Collections.singletonList(new SNIHostName(serverName)));
            ssl.setSSLParameters(p);
        }
        ssl.startHandshake();
        return ssl;
    }

    private void writeConnect(String host, int port) throws Exception {
        String req = "CONNECT " + host + ":" + port + " HTTP/1.1\r\n"
                + "Host: " + host + ":" + port + "\r\n"
                + "Proxy-Connection: Keep-Alive\r\n\r\n";
        out.write(req.getBytes(StandardCharsets.UTF_8)); out.flush();
        String response = readHeaders();
        if (!response.matches("(?s)^HTTP/\\d(?:\\.\\d)?\\s+2\\d\\d.*"))
            throw new Exception("HTTP CONNECT failed: " + response.trim());
    }

    private void sendPayload(String host) throws Exception {
        if (payload.trim().isEmpty()) return;
        String deliveryHost = sni == null || sni.trim().isEmpty() ? host : sni.trim();
        String hostPort = host + ":" + targetPort;
        String random16 = randomToken(16);
        String text = payload.replace("[crlf]", "\r\n").replace("[cr]", "\r")
                .replace("[lf]", "\n").replace("[host]", host)
                .replace("[host_delivery]", deliveryHost)
                .replace("[host_port]", hostPort)
                .replace("[random=16]", random16)
                .replace("[sni]", sni == null ? "" : sni)
                .replace("[protocol]", "HTTP/1.1")
                .replace("[ua]", "Dalvik/2.1.0 (Linux; Android)");
        while (text.endsWith("\r\n")) text = text.substring(0, text.length() - 2);
        out.write((text + "\r\n\r\n").getBytes(StandardCharsets.UTF_8)); out.flush();
        if (text.contains("HTTP/")) {
            String response = readHeaders();
            if (!(response.contains(" 200 ") || response.contains(" 101 ") || response.contains(" 2")))
                throw new Exception("Payload rejected: " + response.trim());
        }
    }

    private static String randomToken(int length) {
        final char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        SecureRandom random = new SecureRandom();
        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) out.append(alphabet[random.nextInt(alphabet.length)]);
        return out.toString();
    }

    private String readHeaders() throws Exception {
        StringBuilder b = new StringBuilder(); int c;
        while ((c = in.read()) >= 0) {
            b.append((char)c);
            if (b.toString().endsWith("\r\n\r\n") || b.length() > 16384) break;
        }
        return b.toString();
    }

    @Override public InputStream getInputStream() { return in; }
    @Override public OutputStream getOutputStream() { return out; }
    @Override public Socket getSocket() { return socket; }
    @Override public void close() { try { if (socket != null) socket.close(); } catch (Exception ignored) {} }
}
