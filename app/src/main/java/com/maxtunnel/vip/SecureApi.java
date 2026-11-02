package com.maxtunnel.vip;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Secure API client with a non-exportable, per-device Android Keystore key. */
public class SecureApi {
    private static final String HANDSHAKE_KEY = "Hx7K9pQ2mWvN4bRt6Ys1Zc8Df3Ge0La5Pj2Uq=";
    private static final String API_SECURE_KEY = "MazenDatabaseSecureKey2026@Vip!";
    private static final String BASE_URL = "https://max2.shukry.online/core.php";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "max_tunnel_device_v1";

    public static class Session { public String token; public byte[] key; }
    private static String hex(byte[] bytes) { StringBuilder out=new StringBuilder(bytes.length*2); for(byte b:bytes)out.append(String.format("%02x",b&255)); return out.toString(); }
    private static String hmac(String message) throws Exception { Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(HANDSHAKE_KEY.getBytes(StandardCharsets.UTF_8),"HmacSHA256")); return hex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8))); }
    private static byte[] sessionKey(String nonce) throws Exception { MessageDigest d=MessageDigest.getInstance("SHA-256"); return d.digest((API_SECURE_KEY+"|"+nonce).getBytes(StandardCharsets.UTF_8)); }
    private static String enc(String v) throws Exception { return URLEncoder.encode(v,"UTF-8"); }
    public static String deviceId(Context c) { try { String id=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ANDROID_ID); return id==null||id.isEmpty()?"unknown":id; } catch(Exception e){return "unknown";} }

    private static KeyPair deviceKey() throws Exception {
        KeyStore ks=KeyStore.getInstance(KEYSTORE); ks.load(null);
        if(!ks.containsAlias(KEY_ALIAS)) {
            KeyPairGenerator g=KeyPairGenerator.getInstance("EC",KEYSTORE);
            g.initialize(new ECGenParameterSpec("secp256r1")); g.generateKeyPair();
        }
        return new KeyPair(ks.getCertificate(KEY_ALIAS).getPublicKey(), (PrivateKey)ks.getKey(KEY_ALIAS,null));
    }
    private static String publicKey() throws Exception { return Base64.encodeToString(deviceKey().getPublic().getEncoded(),Base64.NO_WRAP); }
    private static String sign(String value) throws Exception { Signature s=Signature.getInstance("SHA256withECDSA"); s.initSign(deviceKey().getPrivate()); s.update(value.getBytes(StandardCharsets.UTF_8)); return Base64.encodeToString(s.sign(),Base64.NO_WRAP); }
    private static String proof(String did,String token,String ts,String action,String sid,String nonce) throws Exception { return sign(did+"|"+token+"|"+ts+"|"+action+"|"+sid+"|"+nonce); }
    private static String httpGet(String url) throws Exception { HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(java.net.Proxy.NO_PROXY); c.setConnectTimeout(30000); c.setReadTimeout(30000); c.setRequestMethod("GET"); c.setRequestProperty("User-Agent","MaxTunnel/2"); c.setRequestProperty("Cache-Control","no-store"); int st=c.getResponseCode(); InputStream in=st>=200&&st<300?c.getInputStream():c.getErrorStream(); if(in==null)return ""; ByteArrayOutputStream o=new ByteArrayOutputStream(); byte[] b=new byte[4096]; int n; while((n=in.read(b))!=-1)o.write(b,0,n); in.close(); c.disconnect(); return o.toString("UTF-8"); }
    private static String aesGcmDecrypt(String encoded,byte[] key){try{byte[] e=Base64.decode(encoded,Base64.DEFAULT); if(key==null||e.length<29)return null; byte[] iv=new byte[12],ct=new byte[e.length-12]; System.arraycopy(e,0,iv,0,12);System.arraycopy(e,12,ct,0,ct.length);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));return new String(c.doFinal(ct),StandardCharsets.UTF_8);}catch(Exception e){return null;}}

    public static Session handshake(Context context) {
        try {
            String did=deviceId(context), ts=Long.toString(System.currentTimeMillis()/1000L), nonce=hex(java.security.SecureRandom.getSeed(16));
            String signature=hmac(did+"|"+ts+"|"+nonce), pk=publicKey(), dp=proof(did,"",ts,"hs","",nonce);
            String url=BASE_URL+"?action=hs&did="+enc(did)+"&ts="+ts+"&n="+nonce+"&sig="+signature+"&pk="+enc(pk)+"&dp="+enc(dp);
            JSONObject r=new JSONObject(httpGet(url)); if(!"ok".equals(r.optString("status"))){Pq.p("Handshake rejected");return null;}
            Session s=new Session();s.token=r.getString("token");s.key=sessionKey(r.getString("snonce"));return s;
        } catch(Exception e){Pq.p("Handshake failed");return null;}
    }
    public static String fetchServers(Context context){Session s=handshake(context);if(s==null)return null;try{String did=deviceId(context),ts=Long.toString(System.currentTimeMillis()/1000L),sig=hmac(did+"|"+s.token+"|"+ts),nonce=hex(java.security.SecureRandom.getSeed(16)),dp=proof(did,s.token,ts,"get_max_app_data","",nonce);String u=BASE_URL+"?action=get_max_app_data&proto=3&did="+enc(did)+"&token="+enc(s.token)+"&ts="+ts+"&sig="+sig+"&dp="+enc(dp)+"&dn="+enc(nonce);JSONObject r=new JSONObject(httpGet(u));if(!"success".equals(r.optString("status"))){Pq.p("Server list rejected");return null;}return aesGcmDecrypt(r.getString("data"),s.key);}catch(Exception e){Pq.p("Server list failed");return null;}}
    public static JSONObject fetchCreds(Context context,String sid){if(sid==null||sid.isEmpty())return null;Session s=handshake(context);if(s==null)return null;try{String did=deviceId(context),ts=Long.toString(System.currentTimeMillis()/1000L),sig=hmac(did+"|"+s.token+"|"+ts+"|"+sid),nonce=hex(java.security.SecureRandom.getSeed(16)),dp=proof(did,s.token,ts,"creds",sid,nonce);String u=BASE_URL+"?action=creds&did="+enc(did)+"&token="+enc(s.token)+"&ts="+ts+"&sid="+enc(sid)+"&sig="+sig+"&dp="+enc(dp)+"&dn="+enc(nonce);JSONObject r=new JSONObject(httpGet(u));if(!"success".equals(r.optString("status"))){Pq.p("Credential request rejected");return null;}String p=aesGcmDecrypt(r.getString("data"),s.key);return p==null?null:new JSONObject(p);}catch(Exception e){Pq.p("Credential request failed");return null;}}
}
