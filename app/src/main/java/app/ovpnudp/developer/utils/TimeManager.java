package app.ovpnudp.developer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import java.util.Locale;

/**
 * رصيد وقت داخلي: يُشحن بمشاهدة الإعلانات ويُستهلك فقط أثناء الاتصال.
 * يعتمد على SystemClock.elapsedRealtime() فلا يتأثر بتغيير ساعة الجهاز.
 */
public class TimeManager {
    private static final String PREF = "free_time_prefs";
    private static final String K_BALANCE = "balance_ms";
    private static final String K_SESSION = "session_start";
    private static final String K_WELCOME = "welcome_given";

    private final SharedPreferences sp;

    public TimeManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!sp.getBoolean(K_WELCOME, false)) {
            sp.edit()
              .putLong(K_BALANCE, FreeConfig.WELCOME_MINUTES * 60_000L)
              .putBoolean(K_WELCOME, true)
              .apply();
        }
        // بعد إعادة تشغيل الجهاز تبدأ elapsedRealtime من الصفر
        long start = sp.getLong(K_SESSION, 0);
        if (start > SystemClock.elapsedRealtime()) {
            sp.edit().putLong(K_SESSION, SystemClock.elapsedRealtime()).apply();
        }
    }

    public boolean isSessionActive() {
        return sp.getLong(K_SESSION, 0) > 0;
    }

    /** الوقت المتبقي بالميلي ثانية. */
    public long getRemainingMs() {
        long balance = sp.getLong(K_BALANCE, 0);
        long start = sp.getLong(K_SESSION, 0);
        if (start > 0) {
            balance -= Math.max(0, SystemClock.elapsedRealtime() - start);
        }
        return Math.max(0, balance);
    }

    public boolean hasTime() {
        return getRemainingMs() > 0;
    }

    /** يبدأ احتساب الاستهلاك (عند الاتصال). */
    public void startSession() {
        if (!isSessionActive()) {
            sp.edit().putLong(K_SESSION, Math.max(1, SystemClock.elapsedRealtime())).apply();
        }
    }

    /** يوقف الاحتساب ويثبّت الرصيد المتبقي (عند قطع الاتصال). */
    public void endSession() {
        long remaining = getRemainingMs();
        sp.edit().putLong(K_BALANCE, remaining).putLong(K_SESSION, 0).apply();
    }

    /** يضيف وقتاً (مكافأة إعلان) مع احترام الحد الأقصى. */
    public void addMinutes(int minutes) {
        boolean active = isSessionActive();
        long remaining = getRemainingMs();
        long max = FreeConfig.MAX_BALANCE_MINUTES * 60_000L;
        long updated = Math.min(max, remaining + minutes * 60_000L);
        SharedPreferences.Editor e = sp.edit().putLong(K_BALANCE, updated);
        if (active) {
            e.putLong(K_SESSION, Math.max(1, SystemClock.elapsedRealtime()));
        }
        e.apply();
    }

    public boolean isFull() {
        return getRemainingMs() >= FreeConfig.MAX_BALANCE_MINUTES * 60_000L;
    }

    public static String format(long ms) {
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
