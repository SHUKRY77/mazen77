package com.maxtunnel.vip;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Debug;
import java.io.File;
import java.security.MessageDigest;

public class SecurityGuard {

    // ========================================================
    // حماية الإعلانات من التعديل: فحص توقيع التطبيق
    // أي شخص يعدّل الـ APK (مثلاً لإزالة الإعلانات) يجب أن يعيد توقيعه
    // بشهادة مختلفة، فتختلف بصمة SHA-256 ونكتشف التلاعب.
    //
    // ⚠️ ضع هنا بصمة شهادة الإصدار (release) الخاصة بك (SHA-256، أحرف صغيرة بلا نقطتين).
    // للحصول عليها:
    //   keytool -list -v -keystore your-release.keystore -alias your-alias
    // ثم انسخ قيمة SHA256 واحذف النقطتين (:) واجعلها أحرفاً صغيرة.
    // اتركها فارغة "" أثناء التطوير (يسمح بأي توقيع) ثم املأها قبل النشر.
    // ========================================================
    private static final String EXPECTED_SIG_SHA256 = "5d73faee0f4748b754f802b40146998db1f56d8d050f7268b80e86cbbd7d79b5"; // مثال: "a1b2c3...."

    // يرجّع بصمة SHA-256 لشهادة توقيع التطبيق الحالي (hex صغير)
    public static String getAppSignatureSha256(Context ctx) {
        try {
            Signature[] sigs;
            if (Build.VERSION.SDK_INT >= 28) {
                PackageInfo pi = ctx.getPackageManager().getPackageInfo(
                        ctx.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                sigs = pi.signingInfo.getApkContentsSigners();
            } else {
                PackageInfo pi = ctx.getPackageManager().getPackageInfo(
                        ctx.getPackageName(), PackageManager.GET_SIGNATURES);
                sigs = pi.signatures;
            }
            if (sigs == null || sigs.length == 0) return "";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sigs[0].toByteArray());
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // true إذا كان التوقيع صحيحاً (أو لم تُضبط البصمة بعد أثناء التطوير)
    public static boolean isSignatureValid(Context ctx) {
        // APKs التي تُبنى للاختبار عبر GitHub Actions تكون موقعة بمفتاح Debug.
        // لا نرفضها برسالة "نسخة غير رسمية"؛ فحص التوقيع يبقى مفعلًا في Release.
        if (BuildConfig.DEBUG) return true;
        if (EXPECTED_SIG_SHA256 == null || EXPECTED_SIG_SHA256.isEmpty()) return true; // وضع التطوير
        String actual = getAppSignatureSha256(ctx);
        return !actual.isEmpty() && actual.equalsIgnoreCase(EXPECTED_SIG_SHA256);
    }

    // فك تشفير رابط اللوحة المخفي بصيغة Hex
        // فك تشفير رابط اللوحة المحمي بتشفير البايتات المتقدم
    public String getHiddenApiUrl() {
        try {
            byte[] enc = new byte[] {
                0x2c, 0x0f, 0x1e, 0x69, 0x32, 0x10, 0x01, (byte)0xec,
                (byte)0xf8, (byte)0xd3, (byte)0xbc, (byte)0xb8, (byte)0x89, (byte)0x89, 0x64, 0x70,
                0x40, 0x45, 0x28, 0x3a, 0x17, 0x46, (byte)0xb0, (byte)0xa9,
                (byte)0x99, (byte)0xeb, (byte)0xac, (byte)0xc1, (byte)0xc8, 0x27, 0x79, 0x14,
                0x07, 0x6f, 0x63, 0x56, 0x46, (byte)0xe2, (byte)0xa9, (byte)0x98,
                (byte)0x98, (byte)0xdc, (byte)0xdf, (byte)0xc0, 0x28, 0x18, 0x17, 0x15,
                0x64, 0x54, 0x5e, 0x48, (byte)0xac, (byte)0xae
            };
            byte[] out = new byte[enc.length];
            for (int i = 0; i < enc.length; i++) {
                int k = (0x53 ^ ((i * 17) + 23)) & 0xff;
                out[i] = (byte) ((enc[i] & 0xff) ^ k);
            }
            return new String(out, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    // فك تشفير مفتاح التشفير المخفي بصيغة Hex
    public String getVaultKey() {
        String hexKey = "4d617a656e566f7274657850726f5365637572654b6579323032364056697021";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < hexKey.length() - 1; i += 2) {
            out.append((char) Integer.parseInt(hexKey.substring(i, (i + 2)), 16));
        }
        return out.toString();
    }

    // فحص شامل للروت 
    public static boolean isDeviceRooted() {
        String[] paths = {
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", 
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", 
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    // منع التنصت وتحليل الشبكة 
    public static boolean isDebuggerAttached() {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger();
    }

    // منع تشغيل التطبيق على محاكيات الكمبيوتر
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    // الدالة النهائية لفحص أمان الهاتف
    public static boolean isPhoneSafe() {
        return !isDeviceRooted() && !isDebuggerAttached() && !isEmulator();
    }
}
