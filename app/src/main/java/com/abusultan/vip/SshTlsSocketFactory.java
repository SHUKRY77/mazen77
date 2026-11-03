package com.abusultan.vip;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * TLS transport for SSH-TLS servers. SSH-TLS commonly uses a self-signed or
 * tunnel-only certificate, so Android's public CA store cannot validate it.
 * The channel remains TLS encrypted; this exception is scoped to the explicit
 * SSH transport and is not used by V2Ray or ordinary HTTPS requests.
 */
public final class SshTlsSocketFactory {
    private SshTlsSocketFactory() {}

    public static SSLSocketFactory create() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }};
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAll, new SecureRandom());
        return context.getSocketFactory();
    }
}
