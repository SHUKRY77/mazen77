package com.maxtunnel.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class V2raySettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ربط الواجهة التي قمنا بإنشائها للتو
        setContentView(R.layout.activity_v2ray_settings); 

        // إعداد الشريط العلوي (Toolbar)
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        if (toolbar != null) {
            toolbar.setTitle("KING VPN"); 
            toolbar.setSubtitle("V2Ray Settings");
            
            // ضبط ألوان الشريط العلوي
            toolbar.setBackgroundColor(Color.parseColor("#00E676")); 
            toolbar.setTitleTextColor(Color.WHITE);
            toolbar.setSubtitleTextColor(Color.WHITE);
            
            setSupportActionBar(toolbar);
            
            // إظهار زر الرجوع
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            
            // برمجة زر الرجوع ليغلق الشاشة
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        
        // هنا سيتم استدعاء عناصر الإعدادات (المفاتيح والخيارات) لاحقاً
    }

    public void onModeHelpClicked(View view) {
        Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show();
    }
}
