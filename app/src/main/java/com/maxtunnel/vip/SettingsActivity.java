package com.maxtunnel.vip;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {
    
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ربط الكلاس بملف التصميم الأسطوري
        setContentView(R.layout.settings_preferences);

        // تجهيز الذاكرة لحفظ إعدادات المستخدم
        prefs = getSharedPreferences("MAX_TUNNEL_SETTINGS", MODE_PRIVATE);
        editor = prefs.edit();

        // 1. إعداد شريط العنوان (Toolbar)
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // إظهار زر الرجوع
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // تفعيل زر الرجوع
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 2. تعريف كل أزرار الـ CheckBox بناءً على الـ IDs في التصميم
        CheckBox chkCompression = findViewById(R.id.check2);
        CheckBox chkTethering = findViewById(R.id.check3);
        CheckBox chkPauseVpn = findViewById(R.id.check4);
        CheckBox chkAutoClear = findViewById(R.id.check5);
        CheckBox chkDisableDelay = findViewById(R.id.check6);
        CheckBox chkUdpForward = findViewById(R.id.check7);
        CheckBox chkDnsForward = findViewById(R.id.check8);

        // 3. استرجاع الحالة السابقة للمستخدم عند فتح الواجهة
        if(chkCompression != null) chkCompression.setChecked(prefs.getBoolean("data_compression", false));
        if(chkTethering != null) chkTethering.setChecked(prefs.getBoolean("tethering", false));
        if(chkPauseVpn != null) chkPauseVpn.setChecked(prefs.getBoolean("pause_vpn", false));
        if(chkAutoClear != null) chkAutoClear.setChecked(prefs.getBoolean("auto_clear", false));
        if(chkDisableDelay != null) chkDisableDelay.setChecked(prefs.getBoolean("disable_delay", false));
        if(chkUdpForward != null) chkUdpForward.setChecked(prefs.getBoolean("udp_forward", false));
        if(chkDnsForward != null) chkDnsForward.setChecked(prefs.getBoolean("dns_forward", false));

        // 4. حفظ الحالة فوراً عند قيام المستخدم بتغيير أي خيار
        if(chkCompression != null) chkCompression.setOnCheckedChangeListener((btn, isChecked) -> editor.putBoolean("data_compression", isChecked).apply());
        if(chkTethering != null) chkTethering.setOnCheckedChangeListener((btn, isChecked) -> editor.putBoolean("tethering", isChecked).apply());
        if(chkPauseVpn != null) chkPauseVpn.setOnCheckedChangeListener((btn, isChecked) -> editor.putBoolean("pause_vpn", isChecked).apply());
        if(chkAutoClear != null) chkAutoClear.setOnCheckedChangeListener((btn, isChecked) -> editor.putBoolean("auto_clear", isChecked).apply());
        if(chkDisableDelay != null) chkDisableDelay.setOnCheckedChangeListener((btn, isChecked) -> editor.putBoolean("disable_delay", isChecked).apply());
        if(chkUdpForward != null) chkUdpForward.setOnCheckedChangeListener((btn, isChecked) -> editor.putBoolean("udp_forward", isChecked).apply());
        if(chkDnsForward != null) chkDnsForward.setOnCheckedChangeListener((btn, isChecked) -> editor.putBoolean("dns_forward", isChecked).apply());

        // 5. تعريف مربعات النصوص (EditText)
        EditText edUdpResolver = findViewById(R.id.set_ed1);
        EditText edPrimaryDns = findViewById(R.id.set_ed2);
        EditText edSecondaryDns = findViewById(R.id.set_ed3);
        EditText edPingServer = findViewById(R.id.set_ed5);
        EditText edProxyAddress = findViewById(R.id.set_ed7);

        // 6. استرجاع النصوص المحفوظة سابقاً
        if(edUdpResolver != null) edUdpResolver.setText(prefs.getString("udp_resolver", ""));
        if(edPrimaryDns != null) edPrimaryDns.setText(prefs.getString("primary_dns", ""));
        if(edSecondaryDns != null) edSecondaryDns.setText(prefs.getString("secondary_dns", ""));
        if(edPingServer != null) edPingServer.setText(prefs.getString("ping_server", ""));
        if(edProxyAddress != null) edProxyAddress.setText(prefs.getString("proxy_address", ""));

        // 7. حفظ النصوص فوراً عند كتابة أي حرف جديد
        setupTextWatcher(edUdpResolver, "udp_resolver");
        setupTextWatcher(edPrimaryDns, "primary_dns");
        setupTextWatcher(edSecondaryDns, "secondary_dns");
        setupTextWatcher(edPingServer, "ping_server");
        setupTextWatcher(edProxyAddress, "proxy_address");
    }

    // دالة مساعدة لتسجيل النصوص أثناء الكتابة
    private void setupTextWatcher(EditText editText, String prefKey) {
        if(editText == null) return;
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editor.putString(prefKey, s.toString().trim()).apply();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
