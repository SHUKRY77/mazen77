package com.abusultan.vip;

import java.util.ArrayList;
import java.util.List;

public class Pq {
    // 🌟 هذه الواجهة (Interface) هي التي ستربط الخدمة بالشاشة
    public interface LogListener {
        void onNewLog(String message);
    }

    private static final List<LogListener> listeners = new ArrayList<>();

    // 🌟 تسجيل الشاشة لتستقبل السجلات
    public static void registerListener(LogListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // 🌟 إزالة الاستماع عند إغلاق التطبيق لتوفير البطارية
    public static void unregisterListener(LogListener listener) {
        listeners.remove(listener);
    }

    // 🌟 هذه هي الدالة السحرية التي يستخدمها التطبيق الأصلي لطباعة السجلات
    public static void p(String message) {
        message = redact(message);
        for (LogListener listener : listeners) {
            if (listener != null) {
                listener.onNewLog(message);
            }
        }
    }

    /** Keep server names and addresses out of the user-visible diagnostic console. */
    public static String redact(String message) {
        if (message == null) return "حدث خطأ غير معروف";
        String safe = message;
        safe = safe.replaceAll("(?i)(?<![A-Za-z0-9_-])(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}(?![A-Za-z0-9_-])", "[hidden-host]");
        safe = safe.replaceAll("(?<![0-9])(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?![0-9])", "[hidden-address]");
        return safe;
    }
}
