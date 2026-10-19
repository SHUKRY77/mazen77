package com.maxtunnel.vip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;

public class AdvanceSettingsActivity extends AppCompatActivity {

    private CheckBox[] checkBoxes;
    private View[] settingViews;
    private EditText[] editTexts;
    private TextInputLayout[] textInputLayouts;
    private SharedPreferences sharedPreferences;

    public AdvanceSettingsActivity() {
        this.checkBoxes = new CheckBox[10];
        this.settingViews = new View[13];
        this.editTexts = new EditText[9];
        this.textInputLayouts = new TextInputLayout[9];
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ربط الواجهة
        setContentView(R.layout.activity_advance_settings); // سننشئ هذا الملف لاحقاً
        
        sharedPreferences = getSharedPreferences("vpn_settings", Context.MODE_PRIVATE);

        // إعداد الشريط العلوي (Toolbar)
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        if (toolbar != null) {
            toolbar.setTitle("KING VPN"); 
            toolbar.setSubtitle("Advance Settings");
            toolbar.setBackgroundColor(Color.parseColor("#00E676")); 
            toolbar.setTitleTextColor(Color.WHITE);
            toolbar.setSubtitleTextColor(Color.WHITE);
            
            setSupportActionBar(toolbar);
            
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // قراءة وعرض الإعدادات
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    // الكلاس الداخلي المسؤول عن قراءة وعرض مفاتيح الإعدادات
    public static class SettingsFragment extends androidx.preference.PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // استدعاء ملف الـ XML الخاص بإعدادات OVPN
            setPreferencesFromResource(R.xml.ovpn_preferences, rootKey);
        }
    }
}
