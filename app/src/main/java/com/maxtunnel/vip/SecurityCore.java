package com.maxtunnel.vip;

import android.content.Context;

public class SecurityCore {
    // تحميل مكتبة C++
    static {
        System.loadLibrary("native-lib");
    }

    // دوال الجافا التي ترتبط مباشرة بكود C++
    public native String getSecureUrl();
    public native String getDecryptionKey(Context context);
}
