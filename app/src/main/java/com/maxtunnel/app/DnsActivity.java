package com.maxtunnel.app;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.net.InetAddress;

public class DnsActivity extends AppCompatActivity {

    private EditText etDomain;
    private Spinner spinnerDnsType;
    private Button btnTest;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dns);

        etDomain = findViewById(R.id.et_domain);
        spinnerDnsType = findViewById(R.id.spinner_dns_type);
        btnTest = findViewById(R.id.btn_test_dns);
        tvResult = findViewById(R.id.tv_result);

        setupToolbar();
        setupSpinner();

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String domain = etDomain.getText().toString().trim();
                if (TextUtils.isEmpty(domain)) {
                    Toast.makeText(DnsActivity.this, "Please enter a valid domain or IP", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // بدء فحص الشبكة
                performNetworkTest(domain);
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_dns);
        toolbar.setTitle("DNS Test / IP Hunt");
        toolbar.setSubtitle("Network Diagnostics Tool");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupSpinner() {
        // الخيارات التي ستظهر في القائمة
        String[] types = {"A Record (IPv4 Resolution)", "Ping Test", "IP Hunt"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerDnsType.setAdapter(adapter);
    }

    // الدالة التي تقوم بعمل المهمة في الخلفية (نفس وظيفة الكلاس r2 المشفر)
    private void performNetworkTest(final String domain) {
        tvResult.setText("Resolving host: " + domain + "...\n");
        btnTest.setEnabled(false);
        btnTest.setText("TESTING...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder resultBuilder = new StringBuilder();
                try {
                    long startTime = System.currentTimeMillis();
                    
                    // محاولة الاتصال وجلب الآيبي
                    InetAddress[] addresses = InetAddress.getAllByName(domain);
                    
                    long timeTaken = System.currentTimeMillis() - startTime;

                    resultBuilder.append("✓ SUCCESS!\n");
                    resultBuilder.append("Response Time: ").append(timeTaken).append(" ms\n\n");
                    resultBuilder.append("Resolved IP Addresses:\n");
                    
                    for (InetAddress address : addresses) {
                        resultBuilder.append("➔ ").append(address.getHostAddress()).append("\n");
                    }
                    
                } catch (Exception e) {
                    resultBuilder.append("✗ FAILED to resolve.\n");
                    resultBuilder.append("Check your internet connection or domain name.\n");
                    resultBuilder.append("Error: ").append(e.getMessage());
                }

                // عرض النتيجة على الشاشة بعد انتهاء الفحص
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvResult.append("\n" + resultBuilder.toString());
                        btnTest.setEnabled(true);
                        btnTest.setText("START TEST");
                    }
                });
            }
        }).start();
    }
}
