package com.maxtunnel.app;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecureApi {

    private static final String HANDSHAKE_KEY  = "Hx7K9pQ2mWvN4bRt6Ys1Zc8Df3Ge0La5Pj2Uq=";
    private static final String API_SECURE_KEY = "MazenDatabaseSecureKey2026@Vip!";
    private static final String BASE_URL       = "https://app.assag2026.com/core.php";

    public static class Session {
        public String token;
        public byte[] key;
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format("%02x", x));
        return s.toString();
    }

    private static String hmac(String msg) {
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(HANDSHAKE_KEY.getBytes("UTF-8"), "HmacSHA256"));
            return hex(m.doFinal(msg.getBytes("UTF-8")));
        } catch (Exception e) { return ""; }
    }

    private static byte[] sessionKey(String snonce) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            return d.digest((API_SECURE_KEY + "|" + snonce).getBytes("UTF-8"));
        } catch (Exception e) { return null; }
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    public static String deviceId(Context c) {
        try {
            String a = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
            return (a == null || a.isEmpty()) ? "unknown" : a;
        } catch (Exception e) { return "unknown"; }
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection(java.net.Proxy.NO_PROXY);
        con.setConnectTimeout(15000);
        con.setReadTimeout(15000);
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Cache-Control", "no-cache");
        int code = con.getResponseCode();
        InputStream is = (code == 200) ? con.getInputStream() : con.getErrorStream();
        if (is == null) return "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = is.read(buf)) != -1) bo.write(buf, 0, r);
        con.disconnect();
        return bo.toString("UTF-8");
    }

    private static String aesDecrypt(String b64, byte[] key) {
        try {
            byte[] all = Base64.decode(b64, Base64.DEFAULT);
            if (all.length < 16 || key == null) return null;
            byte[] iv = new byte[16];
            System.arraycopy(all, 0, iv, 0, 16);
            byte[] ct = new byte[all.length - 16];
            System.arraycopy(all, 16, ct, 0, ct.length);
            Cipher ci = Cipher.getInstance("AES/CBC/PKCS5Padding");
            ci.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return new String(ci.doFinal(ct), "UTF-8");
        } catch (Exception e) { return null; }
    }

    // ===== الخطوة 1: المصافحة =====
    public static Session handshake(Context c) {
        try {
            String did = deviceId(c);
            long ts = System.currentTimeMillis() / 1000L;
            byte[] nb = new byte[8];
            new SecureRandom().nextBytes(nb);
            String n = hex(nb);
            String sig = hmac(did + "|" + ts + "|" + n);
            String url = BASE_URL + "?action=hs&did=" + enc(did) + "&ts=" + ts + "&n=" + n + "&sig=" + sig;
            JSONObject o = new JSONObject(httpGet(url));
            if (!"ok".equals(o.optString("status"))) {
                Pq.p("🚨 [فشل المصافحة]: " + o.optString("message"));
                return null;
            }
            Session s = new Session();
            s.token = o.getString("token");
            s.key = sessionKey(o.getString("snonce"));
            return s;
        } catch (Exception e) { 
            Pq.p("🚨 [استثناء المصافحة]: " + e.getMessage());
            return null; 
        }
    }

    // ===== جلب قائمة السيرفرات =====
    public static String fetchServers(Context c) {
        Session s = handshake(c);
        if (s == null) return null;
        try {
            String did = deviceId(c);
            long ts = System.currentTimeMillis() / 1000L;
            String sig = hmac(did + "|" + s.token + "|" + ts);
            String url = BASE_URL + "?action=get_max_app_data&proto=3&did=" + enc(did)
                    + "&token=" + s.token + "&ts=" + ts + "&sig=" + sig;
            JSONObject o = new JSONObject(httpGet(url));
            if (!"success".equals(o.optString("status"))) {
                Pq.p("🚨 [فشل جلب السيرفرات]: " + o.optString("message"));
                return null;
            }
            return aesDecrypt(o.getString("data"), s.key);
        } catch (Exception e) { return null; }
    }

    // ===== جلب بيانات الدخول المؤمّنة للسيرفر =====
    public static JSONObject fetchCreds(Context c, String sid) {
        if (sid == null || sid.isEmpty()) return null;
        Session s = handshake(c);
        if (s == null) {
            Pq.p("🚨 [خطأ]: فشلت المصافحة في مرحلة طلب Creds");
            return null;
        }
        try {
            String did = deviceId(c);
            long ts = System.currentTimeMillis() / 1000L;
            String sig = hmac(did + "|" + s.token + "|" + ts + "|" + sid);
            String url = BASE_URL + "?action=creds&did=" + enc(did) + "&token=" + s.token
                    + "&ts=" + ts + "&sid=" + enc(sid) + "&sig=" + sig;
            
            JSONObject o = new JSONObject(httpGet(url));
            if (!"success".equals(o.optString("status"))) {
                Pq.p("🚨 [رفض اللوحة لتسليم المفتاح]: " + o.optString("message"));
                return null;
            }
            String dec = aesDecrypt(o.getString("data"), s.key);
            if (dec == null) {
                Pq.p("🚨 [خطأ]: فشل فك تشفير استجابة المفتاح من اللوحة");
                return null;
            }
            return new JSONObject(dec);
        } catch (Exception e) { 
            Pq.p("🚨 [استثناء Creds]: " + e.getMessage());
            return null; 
        }
    }
}
