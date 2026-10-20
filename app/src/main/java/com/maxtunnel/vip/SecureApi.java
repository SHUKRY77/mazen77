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
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Secure API client. The panel must be deployed over HTTPS. */
public class SecureApi {
    private static final String HANDSHAKE_KEY = "Hx7K9pQ2mWvN4bRt6Ys1Zc8Df3Ge0La5Pj2Uq=";
    private static final String API_SECURE_KEY = "MazenDatabaseSecureKey2026@Vip!";
    private static final String BASE_URL = "https://max2.shukry.online/core.php";
    private static final int CLOCK_SKEW_SECONDS = 90;

    public static class Session {
        public String token;
        public byte[] key;
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format("%02x", b & 0xff));
        return out.toString();
    }

    private static String hmac(String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(HANDSHAKE_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return hex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] sessionKey(String serverNonce) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest((API_SECURE_KEY + "|" + serverNonce).getBytes(StandardCharsets.UTF_8));
    }

    private static String enc(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    public static String deviceId(Context context) {
        try {
            String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return (id == null || id.isEmpty()) ? "unknown" : id;
        } catch (Exception ignored) { return "unknown"; }
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(java.net.Proxy.NO_PROXY);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "MaxTunnel/1.0");
        connection.setRequestProperty("Cache-Control", "no-store");
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        if (stream == null) return "";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) != -1) output.write(buffer, 0, read);
        stream.close();
        connection.disconnect();
        return output.toString("UTF-8");
    }

    /** Envelope: base64(12-byte IV || ciphertext || 16-byte GCM tag). */
    private static String aesGcmDecrypt(String encoded, byte[] key) {
        try {
            byte[] envelope = Base64.decode(encoded, Base64.DEFAULT);
            if (key == null || envelope.length < 12 + 16 + 1) return null;
            byte[] iv = new byte[12];
            byte[] ciphertextAndTag = new byte[envelope.length - 12];
            System.arraycopy(envelope, 0, iv, 0, iv.length);
            System.arraycopy(envelope, iv.length, ciphertextAndTag, 0, ciphertextAndTag.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertextAndTag), StandardCharsets.UTF_8);
        } catch (Exception ignored) { return null; }
    }

    public static Session handshake(Context context) {
        try {
            String did = deviceId(context);
            long timestamp = System.currentTimeMillis() / 1000L;
            byte[] nonceBytes = new byte[16];
            new SecureRandom().nextBytes(nonceBytes);
            String nonce = hex(nonceBytes);
            String signature = hmac(did + "|" + timestamp + "|" + nonce);
            String url = BASE_URL + "?action=hs&did=" + enc(did) + "&ts=" + timestamp
                    + "&n=" + nonce + "&sig=" + signature;
            JSONObject response = new JSONObject(httpGet(url));
            if (!"ok".equals(response.optString("status"))) {
                Pq.p("🚨 [فشل المصافحة]: " + response.optString("message"));
                return null;
            }
            Session session = new Session();
            session.token = response.getString("token");
            session.key = sessionKey(response.getString("snonce"));
            return session;
        } catch (Exception error) {
            Pq.p("🚨 [استثناء المصافحة]: " + error.getMessage());
            return null;
        }
    }

    public static String fetchServers(Context context) {
        Session session = handshake(context);
        if (session == null) return null;
        try {
            String did = deviceId(context);
            long timestamp = System.currentTimeMillis() / 1000L;
            String signature = hmac(did + "|" + session.token + "|" + timestamp);
            String url = BASE_URL + "?action=get_max_app_data&proto=3&did=" + enc(did)
                    + "&token=" + enc(session.token) + "&ts=" + timestamp + "&sig=" + signature;
            JSONObject response = new JSONObject(httpGet(url));
            if (!"success".equals(response.optString("status"))) {
                Pq.p("🚨 [فشل جلب السيرفرات]: " + response.optString("message"));
                return null;
            }
            return aesGcmDecrypt(response.getString("data"), session.key);
        } catch (Exception error) {
            Pq.p("🚨 [استثناء جلب السيرفرات]: " + error.getMessage());
            return null;
        }
    }

    public static JSONObject fetchCreds(Context context, String sid) {
        if (sid == null || sid.isEmpty()) return null;
        Session session = handshake(context);
        if (session == null) return null;
        try {
            String did = deviceId(context);
            long timestamp = System.currentTimeMillis() / 1000L;
            String signature = hmac(did + "|" + session.token + "|" + timestamp + "|" + sid);
            String url = BASE_URL + "?action=creds&did=" + enc(did) + "&token=" + enc(session.token)
                    + "&ts=" + timestamp + "&sid=" + enc(sid) + "&sig=" + signature;
            JSONObject response = new JSONObject(httpGet(url));
            if (!"success".equals(response.optString("status"))) {
                Pq.p("🚨 [رفض اللوحة لتسليم مفتاح السيرفر]");
                return null;
            }
            String plaintext = aesGcmDecrypt(response.getString("data"), session.key);
            return plaintext == null ? null : new JSONObject(plaintext);
        } catch (Exception error) {
            Pq.p("🚨 [استثناء بيانات السيرفر]: " + error.getMessage());
            return null;
        }
    }
}
