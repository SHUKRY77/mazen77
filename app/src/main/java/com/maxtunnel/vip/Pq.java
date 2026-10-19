package com.maxtunnel.vip;

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
        for (LogListener listener : listeners) {
            if (listener != null) {
                listener.onNewLog(message);
            }
        }
    }
}
