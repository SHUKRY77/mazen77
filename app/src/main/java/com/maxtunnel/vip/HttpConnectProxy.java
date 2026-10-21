package com.maxtunnel.vip;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.SocketFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Plain HTTP CONNECT proxy used by SSH-Proxy. */
public final class HttpConnectProxy implements Proxy {
    private final String proxyHost;
    private final int proxyPort;
    private final String targetHost;
    private final int targetPort;
    private InputStream in;
    private OutputStream out;
    private Socket socket;

    public HttpConnectProxy(String proxyHost, int proxyPort, String target) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        String host = target;
        int port = 22;
        int colon = target == null ? -1 : target.lastIndexOf(':');
        if (colon > 0) {
            host = target.substring(0, colon);
            try { port = Integer.parseInt(target.substring(colon + 1)); } catch (Exception ignored) {}
        }
        this.targetHost = host;
        this.targetPort = port;
    }

    @Override public void connect(SocketFactory ignored, String host, int port, int timeout) throws Exception {
        socket = new Socket();
        socket.connect(new InetSocketAddress(proxyHost, proxyPort), timeout);
        socket.setSoTimeout(timeout);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        String request = "CONNECT " + targetHost + ":" + targetPort + " HTTP/1.1\r\n"
                + "Host: " + targetHost + ":" + targetPort + "\r\n"
                + "Proxy-Connection: Keep-Alive\r\n\r\n";
        out.write(request.getBytes("UTF-8")); out.flush();
        StringBuilder response = new StringBuilder(); int c;
        while ((c = in.read()) >= 0) {
            response.append((char)c);
            if (response.toString().endsWith("\r\n\r\n")) break;
        }
        if (!response.toString().matches("(?s)^HTTP/\\d(?:\\.\\d)?\\s+2\\d\\d.*"))
            throw new Exception("HTTP CONNECT failed: " + response.toString().trim());
    }
    @Override public InputStream getInputStream() { return in; }
    @Override public OutputStream getOutputStream() { return out; }
    @Override public Socket getSocket() { return socket; }
    @Override public void close() { try { if (socket != null) socket.close(); } catch (Exception ignored) {} }
}
