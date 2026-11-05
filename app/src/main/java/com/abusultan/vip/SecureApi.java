package com.abusultan.vip;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.provider.Settings;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import javax.crypto.KeyAgreement;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Secure API client with a non-exportable, per-device Android Keystore key. */
public class SecureApi {
    private static final String BASE_URL = "https://abusultan.shukry.online/core.php";
    private static final String ORIGIN_URL = "https://193.106.249.79/core.php";
    private static final String API_HOST = "abusultan.shukry.online";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "max_tunnel_device_v2";
    private static final String API_PIN_SHA256 = "0d86ea57c54f2df4eb49de28d2c2461c9182d3058bf7811d6346cc129502e661";
    private static final String SERVER_SIGNING_PUBLIC_B64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKQYLSYD6g+Qs58e+FpLbx2IC3vC55O7CsWcVi5rEYCwJmJ2IHQHImWnORrOvC+7okTlmMYI8qkBWFUpUNp9ckg==";

    public static class Session { public String token; public byte[] key; }
    private static String hex(byte[] bytes) { StringBuilder out=new StringBuilder(bytes.length*2); for(byte b:bytes)out.append(String.format("%02x",b&255)); return out.toString(); }
    private static byte[] deriveSessionKey(String encodedServerPublic) throws Exception {
        byte[] der=Base64.decode(encodedServerPublic,Base64.DEFAULT);
        PublicKey server=KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
        KeyAgreement agreement=KeyAgreement.getInstance("ECDH"); agreement.init(deviceKey().getPrivate()); agreement.doPhase(server,true);
        return MessageDigest.getInstance("SHA-256").digest(agreement.generateSecret());
    }
    private static String enc(String v) throws Exception { return URLEncoder.encode(v,"UTF-8"); }
    private static boolean verifyServer(String message, String encoded) throws Exception {
        byte[] der=Base64.decode(SERVER_SIGNING_PUBLIC_B64,Base64.DEFAULT), sig=Base64.decode(encoded,Base64.DEFAULT);
        PublicKey key=KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
        Signature v=Signature.getInstance("SHA256withECDSA"); v.initVerify(key); v.update(message.getBytes(StandardCharsets.UTF_8)); return v.verify(sig);
    }
    private static String pinHex(X509Certificate cert) throws Exception { return hex(MessageDigest.getInstance("SHA-256").digest(cert.getPublicKey().getEncoded())); }
    private static javax.net.ssl.SSLSocketFactory apiSslFactory() throws Exception {
        TrustManagerFactory tmf=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); tmf.init((java.security.KeyStore)null);
        X509TrustManager base=null; for(TrustManager tm:tmf.getTrustManagers()) if(tm instanceof X509TrustManager){base=(X509TrustManager)tm;break;}
        final X509TrustManager system=base;
        TrustManager[] managers={new X509TrustManager(){ public void checkClientTrusted(X509Certificate[] c,String a){ } public void checkServerTrusted(X509Certificate[] c,String a) throws java.security.cert.CertificateException { system.checkServerTrusted(c,a); if(c==null||c.length==0)throw new java.security.cert.CertificateException("No certificate"); try{if(!API_PIN_SHA256.equalsIgnoreCase(pinHex(c[0])))throw new java.security.cert.CertificateException("API certificate pin mismatch");}catch(java.security.cert.CertificateException e){throw e;}catch(Exception e){throw new java.security.cert.CertificateException(e);} } public X509Certificate[] getAcceptedIssuers(){return system.getAcceptedIssuers();} }};
        SSLContext ctx=SSLContext.getInstance("TLS"); ctx.init(null,managers,new java.security.SecureRandom()); return ctx.getSocketFactory();
    }
    public static String deviceId(Context c) { try { String id=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ANDROID_ID); return id==null||id.isEmpty()?"unknown":id; } catch(Exception e){return "unknown";} }

    private static KeyPair deviceKey() throws Exception {
        KeyStore ks=KeyStore.getInstance(KEYSTORE); ks.load(null);
        if(!ks.containsAlias(KEY_ALIAS)) {
            KeyPairGenerator g=KeyPairGenerator.getInstance("EC",KEYSTORE);
            g.initialize(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY | KeyProperties.PURPOSE_AGREE_KEY)
                    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build());
            g.generateKeyPair();
        }
        return new KeyPair(ks.getCertificate(KEY_ALIAS).getPublicKey(), (PrivateKey)ks.getKey(KEY_ALIAS,null));
    }
    private static String publicKey() throws Exception { return Base64.encodeToString(deviceKey().getPublic().getEncoded(),Base64.NO_WRAP); }
    private static String sign(String value) throws Exception { Signature s=Signature.getInstance("SHA256withECDSA"); s.initSign(deviceKey().getPrivate()); s.update(value.getBytes(StandardCharsets.UTF_8)); return Base64.encodeToString(s.sign(),Base64.NO_WRAP); }
    private static String proof(String did,String token,String ts,String action,String sid,String nonce) throws Exception { return sign(did+"|"+token+"|"+ts+"|"+action+"|"+sid+"|"+nonce); }
    private static String httpGet(String url) throws Exception {
        Exception last = null;
        String[] targets = new String[]{url, url.startsWith(BASE_URL) ? ORIGIN_URL + url.substring(BASE_URL.length()) : url};
        for (int i = 0; i < targets.length; i++) {
            HttpURLConnection c = null;
            try {
                c=(HttpURLConnection)new URL(targets[i]).openConnection(java.net.Proxy.NO_PROXY);
                if(c instanceof HttpsURLConnection) {
                    HttpsURLConnection h=(HttpsURLConnection)c;
                    h.setSSLSocketFactory(i == 1 ? new SniSocketFactory(apiSslFactory(), API_HOST) : apiSslFactory());
                    if (i == 1) h.setHostnameVerifier((hostname, session) -> API_HOST.equalsIgnoreCase(session.getPeerHost()) || "193.106.249.79".equals(hostname));
                }
                c.setConnectTimeout(12000); c.setReadTimeout(15000); c.setRequestMethod("GET"); c.setRequestProperty("User-Agent","abusultan-vip/2"); c.setRequestProperty("Cache-Control","no-store");
                if (i == 1) c.setRequestProperty("Host", API_HOST);
                int st=c.getResponseCode(); InputStream in=st>=200&&st<300?c.getInputStream():c.getErrorStream(); if(in==null)return "";
                ByteArrayOutputStream o=new ByteArrayOutputStream(); byte[] b=new byte[4096]; int n; while((n=in.read(b))!=-1)o.write(b,0,n); in.close(); return o.toString("UTF-8");
            } catch (Exception e) { last=e; } finally { if(c!=null)c.disconnect(); }
        }
        throw last == null ? new IOException("API connection failed") : last;
    }

    private static final class SniSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate; private final String sniHost;
        SniSocketFactory(SSLSocketFactory delegate, String sniHost) { this.delegate=delegate; this.sniHost=sniHost; }
        private Socket configure(Socket socket) { if(socket instanceof SSLSocket){ SSLParameters p=((SSLSocket)socket).getSSLParameters(); p.setServerNames(java.util.Collections.singletonList(new javax.net.ssl.SNIHostName(sniHost))); ((SSLSocket)socket).setSSLParameters(p); } return socket; }
        @Override public String[] getDefaultCipherSuites(){return delegate.getDefaultCipherSuites();}
        @Override public String[] getSupportedCipherSuites(){return delegate.getSupportedCipherSuites();}
        @Override public Socket createSocket(Socket s,String h,int p,boolean a)throws IOException{return configure(delegate.createSocket(s,h,p,a));}
        @Override public Socket createSocket(String h,int p)throws IOException{return configure(delegate.createSocket(h,p));}
        @Override public Socket createSocket(String h,int p,InetAddress l,int lp)throws IOException{return configure(delegate.createSocket(h,p,l,lp));}
        @Override public Socket createSocket(InetAddress h,int p)throws IOException{return configure(delegate.createSocket(h,p));}
        @Override public Socket createSocket(InetAddress h,int p,InetAddress l,int lp)throws IOException{return configure(delegate.createSocket(h,p,l,lp));}
    }
    private static String aesGcmDecrypt(String encoded,byte[] key){try{byte[] e=Base64.decode(encoded,Base64.DEFAULT); if(key==null||e.length<29)return null; byte[] iv=new byte[12],ct=new byte[e.length-12]; System.arraycopy(e,0,iv,0,12);System.arraycopy(e,12,ct,0,ct.length);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));return new String(c.doFinal(ct),StandardCharsets.UTF_8);}catch(Exception e){return null;}}

    public static Session handshake(Context context) {
        try {
            String did=deviceId(context), ts=Long.toString(System.currentTimeMillis()/1000L), nonce=hex(java.security.SecureRandom.getSeed(16));
            String pk=publicKey(), dp=proof(did,"",ts,"hs","",nonce);
            String url=BASE_URL+"?action=hs&did="+enc(did)+"&ts="+ts+"&n="+nonce+"&pk="+enc(pk)+"&dp="+enc(dp);
            JSONObject r=new JSONObject(httpGet(url)); if(!"ok".equals(r.optString("status")) || !verifyServer(did+"|"+r.getString("token")+"|"+r.getString("snonce")+"|180|"+pk+"|"+r.getString("ephemeral_pub"),r.optString("server_sig"))){Pq.p("Handshake rejected: server response or signature invalid");return null;}
            Session s=new Session();s.token=r.getString("token");s.key=deriveSessionKey(r.getString("ephemeral_pub"));return s;
        } catch(Exception e){Pq.p("Handshake failed: "+safeError(e));return null;}
    }
    public static String fetchServers(Context context){Session s=handshake(context);if(s==null)return null;try{String did=deviceId(context),ts=Long.toString(System.currentTimeMillis()/1000L),nonce=hex(java.security.SecureRandom.getSeed(16)),dp=proof(did,s.token,ts,"get_max_app_data","",nonce);String u=BASE_URL+"?action=get_max_app_data&proto=3&did="+enc(did)+"&token="+enc(s.token)+"&ts="+ts+"&dp="+enc(dp)+"&dn="+enc(nonce);JSONObject r=new JSONObject(httpGet(u));if(!"success".equals(r.optString("status"))){Pq.p("Server list rejected: "+r.optString("message","unknown"));return null;}String data=aesGcmDecrypt(r.getString("data"),s.key);if(data==null)Pq.p("Server list decrypt failed");return data;}catch(Exception e){Pq.p("Server list failed: "+safeError(e));return null;}}
    public static JSONObject fetchCreds(Context context,String sid){if(sid==null||sid.isEmpty())return null;Session s=handshake(context);if(s==null)return null;try{String did=deviceId(context),ts=Long.toString(System.currentTimeMillis()/1000L),nonce=hex(java.security.SecureRandom.getSeed(16)),dp=proof(did,s.token,ts,"creds",sid,nonce);String u=BASE_URL+"?action=creds&did="+enc(did)+"&token="+enc(s.token)+"&ts="+ts+"&sid="+enc(sid)+"&dp="+enc(dp)+"&dn="+enc(nonce);JSONObject r=new JSONObject(httpGet(u));if(!"success".equals(r.optString("status"))){Pq.p("Credential request rejected: "+r.optString("message","unknown"));return null;}String p=aesGcmDecrypt(r.getString("data"),s.key);if(p==null)Pq.p("Credential decrypt failed");return p==null?null:new JSONObject(p);}catch(Exception e){Pq.p("Credential request failed: "+safeError(e));return null;}}

    private static String safeError(Exception e) { String m=e.getMessage(); if(m==null||m.isEmpty()) m=e.getClass().getSimpleName(); return m.length()>120?m.substring(0,120):m; }
}
