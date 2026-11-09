package com.abusultan.vip;

import android.util.Base64;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.SocketFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
            SSLSocketFactory sslFactory = SshTlsSocketFactory.create();
            SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(socket, sni, proxyPort, true);

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

            // SSH bytes must be transported as masked client WebSocket frames.
            InputStream tunnelIn = in;
            OutputStream tunnelOut = out;
            in = new WsInputStream(tunnelIn, tunnelOut);
            out = new WsOutputStream(tunnelOut);
            Pq.p("Tunnel successfully established with WebSocket framing.");
        } catch (Exception e) {
            Pq.p("Proxy Error: Connection failed.");
            throw e;
        }
    }

    private static final class WsOutputStream extends OutputStream {
        private final OutputStream raw;
        WsOutputStream(OutputStream raw) { this.raw = raw; }
        @Override public synchronized void write(int b) throws IOException { write(new byte[]{(byte)b},0,1); }
        @Override public synchronized void write(byte[] b, int off, int len) throws IOException {
            if (len == 0) return;
            byte[] mask = new byte[4]; new java.security.SecureRandom().nextBytes(mask);
            raw.write(0x82);
            if (len < 126) raw.write(0x80 | len);
            else if (len <= 65535) { raw.write(0x80 | 126); raw.write((len >>> 8) & 255); raw.write(len & 255); }
            else { raw.write(0x80 | 127); long n=len; for(int i=7;i>=0;i--) raw.write((int)(n >>> (i*8)) & 255); }
            raw.write(mask);
            for(int i=0;i<len;i++) raw.write(b[off+i] ^ mask[i&3]);
            raw.flush();
        }
    }

    private static final class WsInputStream extends InputStream {
        private final InputStream raw; private final OutputStream out; private byte[] frame = new byte[0]; private int pos;
        WsInputStream(InputStream raw, OutputStream out) { this.raw=raw; this.out=out; }
        private int one() throws IOException { int v=raw.read(); if(v<0)throw new IOException("WebSocket closed"); return v; }
        private void next() throws IOException {
            while(true) {
                int h=one(), b=one(), opcode=h&15; long n=b&127;
                if(n==126)n=((long)one()<<8)|one(); else if(n==127){n=0;for(int i=0;i<8;i++)n=(n<<8)|one();}
                if(n>16*1024*1024)throw new IOException("WebSocket frame too large");
                boolean masked=(b&128)!=0; byte[] mask=masked?new byte[4]:null; if(mask!=null)for(int i=0;i<4;i++)mask[i]=(byte)one();
                byte[] data=new byte[(int)n]; int got=0; while(got<data.length){int r=raw.read(data,got,data.length-got);if(r<0)throw new IOException("WebSocket closed");got+=r;}
                if(mask!=null)for(int i=0;i<data.length;i++)data[i]=(byte)(data[i]^mask[i&3]);
                if(opcode==8)throw new IOException("WebSocket closed by peer");
                if(opcode==9){out.write(0x8A);if(data.length<126)out.write(data.length);else{out.write(126);out.write(data.length>>>8);out.write(data.length);}out.write(data);out.flush();continue;}
                if(opcode==10)continue;
                if(opcode==0 || opcode==1 || opcode==2){frame=data;pos=0;return;}
            }
        }
        @Override public int read() throws IOException { if(pos>=frame.length)next(); return frame[pos++]&255; }
        @Override public int read(byte[] b,int off,int len)throws IOException { if(pos>=frame.length)next(); int n=Math.min(len,frame.length-pos);System.arraycopy(frame,pos,b,off,n);pos+=n;return n; }
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
