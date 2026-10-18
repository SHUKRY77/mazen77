package com.maxtunnel.app;

import android.util.Base64;
import java.nio.charset.StandardCharsets;

public class Dl {

    // 1. دالة فك التشفير الأصلية (XOR + Base64) المطابقة للتطبيق المنافس
    public static String a(String str, String str2) {
        try {
            byte[] data = Base64.decode(str, 0); 
            byte[] key = Base64.decode(str2, 0);
            
            int length = data.length;
            int length2 = key.length;
            int i = 0;
            int i2 = 0;
            while (i < length) {
                if (i2 >= length2) {
                    i2 = 0;
                }
                data[i] = (byte) (data[i] ^ key[i2]);
                i++;
                i2++;
            }
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ""; // حماية ضد الانهيار
        }
    }

    // 2. دالة التشفير (استخدمها أنت برمجياً لتشفير روابطك ونصوصك المهمة)
    public static String encrypt(String plainText, String keyStringBase64) {
        try {
            byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] key = Base64.decode(keyStringBase64, 0);
            
            int length = data.length;
            int length2 = key.length;
            int i = 0;
            int i2 = 0;
            while (i < length) {
                if (i2 >= length2) {
                    i2 = 0;
                }
                data[i] = (byte) (data[i] ^ key[i2]);
                i++;
                i2++;
            }
            return Base64.encodeToString(data, 0).trim();
        } catch (Exception e) {
            return "";
        }
    }
}
