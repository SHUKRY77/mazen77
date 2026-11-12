package app.ovpnudp.developer.utils;

/**
 * إعدادات النظام المجاني (إعلانات مقابل وقت).
 * عدّل القيم هنا فقط.
 */
public final class FreeConfig {
    private FreeConfig() {}

    /** معرّف وحدة الإعلان المكافأ (Rewarded). القيمة الحالية هي معرّف اختبار رسمي من Google — استبدلها بمعرّفك قبل النشر. */
    public static final String REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917";

    /** عدد الدقائق التي يحصل عليها المستخدم عند مشاهدة إعلان كامل. */
    public static final int MINUTES_PER_AD = 120;

    /** الحد الأقصى للرصيد المتراكم بالدقائق (لمنع تخزين وقت غير محدود). */
    public static final int MAX_BALANCE_MINUTES = 1440;

    /** رصيد ترحيبي عند أول تشغيل للتطبيق (بالدقائق). 0 = لا يوجد. */
    public static final int WELCOME_MINUTES = 60;

    /**
     * بيانات الدخول للسيرفر (كانت تُدخل يدوياً كـ"كود").
     * الآن تُستخدم ثابتة داخلياً. ضع هنا حساب السيرفر المشترك الخاص بك.
     */
    public static final String VPN_USER = "freee";
    public static final String VPN_PASS = "freee";
}
