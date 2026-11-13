package app.ovpnudp.developer.utils;

import android.content.Context;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Encrypted, integrity-checked and atomic local cache for the server list. */
public final class ServerCache {
    private static final String FILE_NAME = "servers.cache.v2";
    private static final String TEMP_NAME = "servers.cache.v2.tmp";
    private static final byte[] MAGIC = new byte[]{'A', 'S', 'V', '2'};
    private static final int IV_LENGTH = 12;
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private ServerCache() { }

    private static SecretKeySpec key(Context context) throws Exception {
        // The key is app-specific and never stored beside the encrypted cache.
        String material = "abusultan-vip|server-cache|v2|" + context.getPackageName();
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(material.getBytes(UTF8));
        return new SecretKeySpec(digest, "AES");
    }

    public static void save(Context context, String json) throws Exception {
        if (json == null || json.trim().length() == 0 || json.length() > MAX_BYTES) {
            throw new IOException("Invalid server cache");
        }
        // Validate before replacing the last known-good cache.
        new org.json.JSONObject(json);

        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(context), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(json.getBytes(UTF8));

        File dir = context.getFilesDir();
        File temp = new File(dir, TEMP_NAME);
        File target = new File(dir, FILE_NAME);
        FileOutputStream out = new FileOutputStream(temp, false);
        try {
            out.write(MAGIC);
            out.write(iv);
            out.write(encrypted);
            out.flush();
            out.getFD().sync();
        } finally {
            out.close();
        }
        if (!temp.renameTo(target)) {
            // Some file systems cannot replace an existing file atomically.
            if (target.exists() && !target.delete()) {
                throw new IOException("Cannot replace server cache");
            }
            if (!temp.renameTo(target)) {
                throw new IOException("Cannot finalize server cache");
            }
        }
    }

    public static String load(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists() || file.length() < MAGIC.length + IV_LENGTH + 16
                || file.length() > MAX_BYTES) {
            return null;
        }
        try {
            byte[] all = readAll(file, MAX_BYTES);
            for (int i = 0; i < MAGIC.length; i++) {
                if (all[i] != MAGIC[i]) return null;
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, MAGIC.length, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[all.length - MAGIC.length - IV_LENGTH];
            System.arraycopy(all, MAGIC.length + IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(context), new GCMParameterSpec(128, iv));
            String json = new String(cipher.doFinal(encrypted), UTF8);
            new org.json.JSONObject(json); // authentication + JSON validation
            return json;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] readAll(File file, int max) throws Exception {
        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[8192];
            int count;
            int total = 0;
            while ((count = in.read(buffer)) != -1) {
                total += count;
                if (total > max) throw new IOException("Cache too large");
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
