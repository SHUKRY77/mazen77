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

/** TLS-wrapped direct SSH transport used by SSH-TLS. */
public final class TlsDirectProxy implements Proxy {
    private final String sni;
    private InputStream in;
    private OutputStream out;
    private Socket socket;
    public TlsDirectProxy(String sni) { this.sni = sni; }
    @Override public void connect(SocketFactory ignored, String host, int port, int timeout) throws Exception {
        Socket raw = new Socket();
        raw.connect(new InetSocketAddress(host, port), timeout);
        SSLSocket ssl = (SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(raw, host, port, true);
        if (sni != null && !sni.isEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            SSLParameters p = ssl.getSSLParameters();
            p.setServerNames(Collections.singletonList(new SNIHostName(sni)));
            ssl.setSSLParameters(p);
        }
        ssl.startHandshake();
        socket = ssl; in = ssl.getInputStream(); out = ssl.getOutputStream();
    }
    @Override public InputStream getInputStream() { return in; }
    @Override public OutputStream getOutputStream() { return out; }
    @Override public Socket getSocket() { return socket; }
    @Override public void close() { try { if (socket != null) socket.close(); } catch (Exception ignored) {} }
}
