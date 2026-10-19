package com.maxtunnel.vip;

import android.util.Base64;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.SocketFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Random;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class CloudflareWsProxy implements Proxy {
    private String proxyIp;
    private int proxyPort;
    private String sni;
    private String rawPayload;
    private String sshHost;

    private InputStream in;
    private OutputStream out;
    private Socket socket;

    public CloudflareWsProxy(String proxyIp, int proxyPort, String sni, String payload, String sshHost) {
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.sni = sni;
        this.rawPayload = payload;
        this.sshHost = sshHost;
    }

    @Override
    public void connect(SocketFactory socketFactory, String host, int port, int timeout) throws Exception {
        Pq.p("Connecting to proxy server...");
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(proxyIp, proxyPort), timeout);
            socket.setSoTimeout(timeout);
            Pq.p("Proxy connected.");

            Pq.p("Establishing secure connection...");
            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(socket, proxyIp, proxyPort, true);
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                SSLParameters sslParameters = sslSocket.getSSLParameters();
                sslParameters.setServerNames(Collections.singletonList(new SNIHostName(sni)));
                sslSocket.setSSLParameters(sslParameters);
            }
            sslSocket.startHandshake();
            Pq.p("SSL handshake completed.");

            in = sslSocket.getInputStream();
            out = sslSocket.getOutputStream();

            String parsedPayload = rawPayload
                    .replace("[crlf]", "\r\n")
                    .replace("[cr]", "\r")
                    .replace("[lf]", "\n")
                    .replace("[host]", sshHost)
                    .replace("[sni]", sni)
                    .replace("[ua]", "Dalvik/2.1.0 (Linux; U; Android 11)");

            while (parsedPayload.endsWith("\r\n")) {
                parsedPayload = parsedPayload.substring(0, parsedPayload.length() - 2);
            }

            if (parsedPayload.toLowerCase().contains("websocket")) {
                if (!parsedPayload.toLowerCase().contains("sec-websocket-key")) {
                    byte[] nonce = new byte[16];
                    new Random().nextBytes(nonce);
                    String wsKey = Base64.encodeToString(nonce, Base64.NO_WRAP);
                    parsedPayload += "\r\nSec-WebSocket-Key: " + wsKey;
                }
                if (!parsedPayload.toLowerCase().contains("sec-websocket-version")) {
                    parsedPayload += "\r\nSec-WebSocket-Version: 13";
                }
            }

            parsedPayload += "\r\n\r\n";

            Pq.p("Injecting payload request...");
            out.write(parsedPayload.getBytes("UTF-8"));
            out.flush();

            int expectedResponses = parsedPayload.split("\r\n\r\n").length;

            StringBuilder response = new StringBuilder();
            int c;
            int responseCount = 0;
            boolean isTunnelOpen = false;

            while ((c = in.read()) >= 0) {
                response.append((char) c);
                if (response.toString().endsWith("\r\n\r\n")) {
                    responseCount++;
                    String currentRes = response.toString().trim();

                    if (currentRes.contains("HTTP/1.1 101") || currentRes.contains("HTTP/1.0 200 Connection established") || currentRes.contains("HTTP/1.1 200")) {
                        isTunnelOpen = true;
                        break; 
                    }

                    if (responseCount >= expectedResponses) {
                        break; 
                    }
                    response.setLength(0);
                }
            }

            if (!isTunnelOpen) {
                throw new Exception("Connection closed by peer.");
            }
            
            Pq.p("Tunnel successfully established.");
        } catch (Exception e) {
            Pq.p("Proxy Error: Connection failed.");
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
