package com.maxtunnel.app;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.TrafficStats;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.card.MaterialCardView;

import com.maxtunnel.app.view.CircleProgressBar;
import com.maxtunnel.app.view.LinearInView;
import com.maxtunnel.app.view.LinearOutView;
import com.maxtunnel.app.view.RotateLoading;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.net.HttpURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayoutMain;
    private NavigationView drawerNavigationView;
    private MaterialCardView btnCardFrame;
    private View btnConnect;
    private View btnConnectIcon;

    private RotateLoading mRotateLoading;
    private CircleProgressBar circleProgress;
    private TextView serverPingText;
    private LinearInView mDataInGraph;
    private LinearOutView mDataOutGraph;

    private TextView connectionStatusText;
    private TextView tvDuration, tvBytesIn, tvBytesOut, tvDataIn, tvDataOut;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private Handler pingHandler = new Handler(Looper.getMainLooper());
    private int vpnUptimeSeconds = 0;
    private long lastRxBytes = 0;
    private long lastTxBytes = 0;

    private boolean isVpnConnected = false;
    private boolean isConnecting = false;
    private static final int VPN_REQUEST_CODE = 1001;

    private String tempSelectedColor = "#E53935";
    private SharedPreferences prefs;

    private String selectedServerName = "Auto select servers";
    private String selectedServerLink = "";
    private String selectedServerSubtitle = "Smart Premium Selection"; 
    
    private String selectedPayloadName = "Select Network Payload";
    private String selectedPayloadLink = "";
    private String selectedPayloadSubtitle = "V2Ray / SSH Network"; 

    private float currentConfigVersion = 5.4f;

    private BottomSheetBehavior bottomSheetBehavior;
    private ImageView showLogIcon;
    private View logHeaderView;
    private RecyclerView logsRecyclerView;
    private LogAdapter logAdapter;
    private TextView statusLogText;
    private ImageView statusLogMenu;

    private AdView mAdView;
    private RewardedAd rewardedAd;
    private InterstitialAd mInterstitialAd;

    private long userRemainingTime = 0; 
    private int gracePeriodSeconds = -1; 
    private TextView ac_xp;

    // ⚠️ يجب أن تطابق هذه القيمة بالضبط قيمة API_WRAP_KEY الموجودة في ملف .env على السيرفر
    private static final String API_WRAP_PASSPHRASE = "Hs6RpTq1WvNm4YbXk8DcZj0OeFl3GaUi7MtQsRz2Pw=";

    private String getSecuredApiUrl() {
        String rawBase = "aHR0cHM6Ly9hcHAuYXNzYWcyMDI2LmNvbS9jb3JlLnBocD8="; 
        try {
            String decodedBase = new String(Base64.decode(rawBase, Base64.DEFAULT), "UTF-8");
            return decodedBase + "action=get_max_app_data&proto=2";
        } catch (Exception e) {
            return "https://app.assag2026.com/core.php?action=get_max_app_data&proto=2";
        }
    }

    private Pq.LogListener logListener = new Pq.LogListener() {
        @Override
        public void onNewLog(String message) {
            runOnUiThread(() -> {
                if (logAdapter != null) {
                    logAdapter.addLog(message);
                    if (logsRecyclerView != null && logAdapter.getItemCount() > 0) {
                        logsRecyclerView.smoothScrollToPosition(logAdapter.getItemCount() - 1);
                    }
                }
            });
        }
    };

    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            String msg = intent.getStringExtra("msg");

            if ("CONNECTED".equals(state)) {
                Pq.p("Tunnel created. Internet connection confirmed!");
                finalizeConnectionUi();
            } else if ("ERROR".equals(state)) {
                Pq.p("Error: " + msg);
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                disconnectWithAnimation();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SecurityGuard.isSignatureValid(this)) {
            Toast.makeText(this, "نسخة غير رسمية من التطبيق!", Toast.LENGTH_LONG).show();
            finishAffinity();
            return;
        }

        MobileAds.initialize(this, initializationStatus -> {});
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("vpn_settings_data", Context.MODE_PRIVATE);
        
        ac_xp = findViewById(R.id.ac_xp);
        
        boolean isFirstRun = prefs.getBoolean("is_first_run", true);
        if (isFirstRun) {
            userRemainingTime = 2 * 3600; 
            prefs.edit().putBoolean("is_first_run", false).apply();
            saveSecureTime(userRemainingTime);
        } else {
            userRemainingTime = getSecureTime();
        }
        updateTimeDisplay();

        if (ac_xp != null) ac_xp.setOnClickListener(v -> showManualAdDialog());
        View cardTimeBox = findViewById(R.id.card_time_box);
        if (cardTimeBox != null) cardTimeBox.setOnClickListener(v -> showManualAdDialog());
        View btnAddTime = findViewById(R.id.btn_add_time);
        if (btnAddTime != null) btnAddTime.setOnClickListener(v -> showManualAdDialog());

        mAdView = findViewById(R.id.adView);
        if (mAdView != null) {
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    Pq.p("🚨 [صائد الأخطاء - البانر]: فشل التحميل. السبب: " + loadAdError.getMessage());
                }
            });
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
        
        loadRewardedAd();
        loadInterstitialAd();

        tempSelectedColor = prefs.getString("theme_color_hex", "#E53935");
        selectedServerName = prefs.getString("selectedServerName", "Auto select servers");
        selectedServerLink = prefs.getString("selectedServerLink", "");
        selectedServerSubtitle = prefs.getString("selectedServerSubtitle", "Smart Premium Selection"); 
        selectedPayloadName = prefs.getString("selectedPayloadName", "Select Network Payload");
        selectedPayloadLink = prefs.getString("selectedPayloadLink", "");
        selectedPayloadSubtitle = prefs.getString("selectedPayloadSubtitle", "V2Ray / SSH Network"); 
        currentConfigVersion = prefs.getFloat("config_version", 5.4f);
        
        String cachedJson = readServersFromFile();
        if (!cachedJson.isEmpty()) {
             parseAndSaveConfigVersion(cachedJson);
        }

        drawerLayoutMain = findViewById(R.id.drawerLayoutMain);
        drawerNavigationView = findViewById(R.id.drawerNavigationView);
        btnConnect = findViewById(R.id.btn_connect);
        btnConnectIcon = findViewById(R.id.btn_connect_icon);
        btnCardFrame = findViewById(R.id.btn_card_frame);
        connectionStatusText = findViewById(R.id.connection_status_text);
        mRotateLoading = findViewById(R.id.mRotateLoading);
        circleProgress = findViewById(R.id.circle_progress);
        serverPingText = findViewById(R.id.server_ping_text);
        tvDuration = findViewById(R.id.duration);
        tvBytesIn = findViewById(R.id.bytes_in);
        tvBytesOut = findViewById(R.id.bytes_out);
        tvDataIn = findViewById(R.id.mDataInTv);
        tvDataOut = findViewById(R.id.mDataOutTv);
        mDataInGraph = findViewById(R.id.mDataIn);
        mDataOutGraph = findViewById(R.id.mDataOut);

        getWindow().getDecorView().post(() -> updateConfigVersionUi(currentConfigVersion));

        if (circleProgress != null) {
            circleProgress.setVisibility(View.VISIBLE);
            circleProgress.setProgress(0f);
            circleProgress.setColor(Color.parseColor(tempSelectedColor));
        }
        if (mRotateLoading != null) mRotateLoading.setColor(Color.parseColor(tempSelectedColor));
        if (drawerNavigationView != null) drawerNavigationView.setNavigationItemSelectedListener(this);
        ImageView mDrawerMenu = findViewById(R.id.mDrawerMenu);
        if (mDrawerMenu != null) mDrawerMenu.setOnClickListener(v -> {
            if (drawerLayoutMain != null) drawerLayoutMain.openDrawer(GravityCompat.START);
        });
        if (btnConnect != null) btnConnect.setOnClickListener(v -> toggleConnection());

        ImageView popMenu = findViewById(R.id.popmenu);
        if (popMenu != null) {
            popMenu.setOnClickListener(v -> {
                android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(MainActivity.this, v);
                String[] menuItems = {"App Link", "Clear app", "Exit All", "Logout"};
                for (int i = 0; i < menuItems.length; i++) {
                    android.text.SpannableString s = new android.text.SpannableString(menuItems[i]);
                    s.setSpan(new android.text.style.RelativeSizeSpan(0.8f), 0, s.length(), 0);
                    popupMenu.getMenu().add(0, i + 1, 0, s);
                }
                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1:
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "KING VPN");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, "Download the best VPN: KING VPN now!");
                            startActivity(Intent.createChooser(shareIntent, "Share App Link"));
                            return true;
                        case 2:
                            if (isVpnConnected || isConnecting) disconnectWithAnimation();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                ((android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).clearApplicationUserData();
                            } else {
                                prefs.edit().clear().apply();
                                Toast.makeText(MainActivity.this, "App Data Cleared", Toast.LENGTH_SHORT).show();
                                finishAffinity();
                            }
                            return true;
                        case 3:
                            if (isVpnConnected || isConnecting) stopVpnConnection();
                            finishAffinity();
                            System.exit(0);
                            return true;
                        case 4:
                            if (isVpnConnected || isConnecting) disconnectWithAnimation();
                            Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                            return true;
                        default:
                            return false;
                    }
                });
                popupMenu.show();
            });
        }

        View serverBox = findViewById(R.id.server_box_ly);
        View payloadBox = findViewById(R.id.payload_box_ly);

        if (serverBox != null) serverBox.setOnClickListener(v -> {
            if (isVpnConnected || isConnecting) Toast.makeText(MainActivity.this, "تنبيه: الرجاء قطع الاتصال أولاً لتغيير السيرفر", Toast.LENGTH_SHORT).show();
            else showSelectionDialog("Servers");
        });

        if (payloadBox != null) payloadBox.setOnClickListener(v -> {
            if (isVpnConnected || isConnecting) Toast.makeText(MainActivity.this, "تنبيه: الرجاء قطع الاتصال أولاً لتغيير الشبكة", Toast.LENGTH_SHORT).show();
            else showSelectionDialog("Networks");
        });

        View bottomSheetView = findViewById(R.id.log_bottom_sheet);
        if (bottomSheetView != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
            showLogIcon = findViewById(R.id.show_log_view);
            logHeaderView = findViewById(R.id.log_view);
            logsRecyclerView = findViewById(R.id.lRecyclerView);
            statusLogText = findViewById(R.id.status);
            statusLogMenu = findViewById(R.id.status_log_menu);

            logAdapter = new LogAdapter();
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            logsRecyclerView.setLayoutManager(layoutManager);
            logsRecyclerView.setAdapter(logAdapter);

            logHeaderView.setOnClickListener(v -> {
                if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });

            if (statusLogMenu != null) {
                statusLogMenu.setOnClickListener(v -> {
                    android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(MainActivity.this, v);
                    popupMenu.getMenu().add(0, 1, 0, "Clear logs");
                    popupMenu.getMenu().add(0, 2, 0, "Copy logs");
                    popupMenu.setOnMenuItemClickListener(item -> {
                        switch (item.getItemId()) {
                            case 1:
                                if (logAdapter != null) { logAdapter.clearLogs(); Toast.makeText(MainActivity.this, "Logs cleared", Toast.LENGTH_SHORT).show(); }
                                return true;
                            case 2:
                                if (logAdapter != null) {
                                    String allLogs = logAdapter.getAllLogsAsString();
                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    if (clipboard != null) {
                                        android.content.ClipData clip = android.content.ClipData.newPlainText("VPN Logs", allLogs);
                                        clipboard.setPrimaryClip(clip);
                                        Toast.makeText(MainActivity.this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                return true;
                            default:
                                return false;
                        }
                    });
                    popupMenu.show();
                });
            }

            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_EXPANDED && showLogIcon != null) {
                        showLogIcon.animate().setDuration(200).rotation(180f);
                        if (logAdapter.getItemCount() > 0) logsRecyclerView.smoothScrollToPosition(logAdapter.getItemCount() - 1);
                    } else if (newState == BottomSheetBehavior.STATE_COLLAPSED && showLogIcon != null) {
                        showLogIcon.animate().setDuration(200).rotation(0f);
                    }
                }
                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
            });
        }

        updateSelectionTexts();
        safeColorApply();
        fetchServersFromPanel(false);
        
        SharedPreferences vpnStatePrefs = getSharedPreferences("vpn_settings_data", MODE_PRIVATE);
        boolean wasConnected = vpnStatePrefs.getBoolean("is_vpn_active_now", false);
        if (wasConnected) {
            finalizeConnectionUi();
        }
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-8090698487945807/7371546877", adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                    Pq.p("🚨 [صائد الأخطاء - إعلان بيني]: فشل التحميل. السبب: " + loadAdError.getMessage());
                }
            });
    }

    private void saveServersToFile(String encryptedData) {
        try {
            FileOutputStream fos = openFileOutput("secure_servers.dat", Context.MODE_PRIVATE);
            fos.write(encryptedData.getBytes());
            fos.close();
        } catch (Exception e) {
            Pq.p("🚨 [صائد الأخطاء - حفظ السيرفرات]: فشل حفظ الملف محلياً بسبب: " + e.getMessage());
        }
    }

    private String readServersFromFile() {
        try {
            FileInputStream fis = openFileInput("secure_servers.dat");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String decryptApiPayload(String encryptedBase64) {
        try {
            if (encryptedBase64 == null || encryptedBase64.isEmpty()) return null;

            byte[] combined = Base64.decode(encryptedBase64, Base64.DEFAULT);
            if (combined.length < 16) return null;

            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, 16);
            byte[] cipherBytes = new byte[combined.length - 16];
            System.arraycopy(combined, 16, cipherBytes, 0, cipherBytes.length);

            java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(API_WRAP_PASSPHRASE.getBytes("UTF-8"));

            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            return new String(plainBytes, "UTF-8");
        } catch (Exception e) {
            Pq.p("🚨 [صائد الأخطاء - فك التشفير]: فشل فك تشفير بيانات اللوحة. السبب: " + e.getMessage());
            return null;
        }
    }

    private String decryptSafe(String text) {
        if (text == null || text.isEmpty()) return text;
        if (text.startsWith("vless://") || text.startsWith("vmess://") || text.startsWith("trojan://") 
                || text.startsWith("SSH_PAYLOAD|") || text.startsWith("SSH_EXT|")) {
            return text;
        }
        try {
            String decrypted = decryptApiPayload(text);
            if (decrypted != null && !decrypted.isEmpty()) return decrypted;
        } catch (Exception e) {}
        return text;
    }

    private void updateTimeDisplay() {
        if (ac_xp != null) {
            long h = userRemainingTime / 3600;
            long m = (userRemainingTime % 3600) / 60;
            long s = userRemainingTime % 60;
            ac_xp.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));
        }
    }

    private Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            if (isVpnConnected) {
                measureRealPing();
                pingHandler.postDelayed(this, 4000); 
            }
        }
    };

    private void measureRealPing() {
        new Thread(() -> {
            long rtt = -1;
            try {
                long start = System.currentTimeMillis();
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("1.1.1.1", 53), 2000);
                rtt = System.currentTimeMillis() - start;
                socket.close();
            } catch (Exception e) {
                try {
                    long start = System.currentTimeMillis();
                    URL url = new URL("https://www.gstatic.com/generate_204");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(2000);
                    conn.setReadTimeout(2000);
                    conn.setUseCaches(false);
                    int code = conn.getResponseCode();
                    if (code == 204 || code == 200) {
                        rtt = System.currentTimeMillis() - start;
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            }

            final long finalPing = rtt;
            runOnUiThread(() -> {
                if (serverPingText != null && isVpnConnected) {
                    if (finalPing > 0) {
                        serverPingText.setText(finalPing + "ms");
                        if (finalPing <= 120) {
                            serverPingText.setTextColor(Color.parseColor("#00E676")); 
                        } else if (finalPing <= 300) {
                            serverPingText.setTextColor(Color.parseColor("#FFC107")); 
                        } else {
                            serverPingText.setTextColor(Color.parseColor("#FF5722")); 
                        }
                    } else {
                        serverPingText.setText("999ms");
                        serverPingText.setTextColor(Color.parseColor("#FF5722"));
                    }
                }
            });
        }).start();
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-8090698487945807/5663290139", 
            adRequest, new RewardedAdLoadCallback() {
                @Override 
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) { 
                    rewardedAd = null; 
                    Pq.p("🚨 [صائد الأخطاء - إعلان المكافأة]: فشل تحميل الإعلان. السبب: " + getArabicAdError(loadAdError.getCode()) + " | تفاصيل: " + loadAdError.getMessage());
                }
                @Override 
                public void onAdLoaded(@NonNull RewardedAd ad) { 
                    rewardedAd = ad; 
                    Pq.p("✅ تم تحميل إعلان المكافأة بنجاح وجاهز للعرض.");
                }
            });
    }

    private String getArabicAdError(int errorCode) {
        switch (errorCode) {
            case AdRequest.ERROR_CODE_INTERNAL_ERROR: return "خطأ داخلي من سيرفرات جوجل أد موب.";
            case AdRequest.ERROR_CODE_INVALID_REQUEST: return "طلب الإعلان غير صالح (تأكد من معرف الإعلان AdUnit ID).";
            case AdRequest.ERROR_CODE_NETWORK_ERROR: return "خطأ في الشبكة أو الاتصال أثناء محاولة جلب الإعلان.";
            case AdRequest.ERROR_CODE_NO_FILL: return "لا توجد إعلانات متاحة حالياً من المعلنين (No Fill - جرب لاحقاً).";
            default: return "خطأ غير معروف (رمز الكود: " + errorCode + ")";
        }
    }

    private void showManualAdDialog() {
        new AlertDialog.Builder(this)
            .setTitle("زيادة وقت الاتصال")
            .setMessage("هل ترغب في مشاهدة إعلان فيديو الآن للحصول على ساعتين إضافيتين في رصيدك؟")
            .setCancelable(true)
            .setPositiveButton("مشاهدة", (dialog, which) -> showRewardedAd(false))
            .setNegativeButton("إلغاء", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void showRewardedAd(boolean isMandatory) {
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override public void onAdDismissedFullScreenContent() { rewardedAd = null; loadRewardedAd(); }
                @Override public void onAdFailedToShowFullScreenContent(AdError adError) { 
                    rewardedAd = null; 
                    loadRewardedAd();
                    Pq.p("🚨 [صائد الأخطاء - عرض الإعلان]: فشل عرض إعلان المكافأة. السبب: " + adError.getMessage());
                }
            });
            rewardedAd.show(MainActivity.this, rewardItem -> grantReward());
        } else {
            Toast.makeText(this, "جاري تحضير الإعلان، يرجى الانتظار ثوانٍ...", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (rewardedAd != null) {
                    rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override public void onAdDismissedFullScreenContent() { rewardedAd = null; loadRewardedAd(); }
                        @Override public void onAdFailedToShowFullScreenContent(AdError adError) { 
                            rewardedAd = null; 
                            loadRewardedAd();
                        }
                    });
                    rewardedAd.show(MainActivity.this, rewardItem -> grantReward());
                } else {
                    Toast.makeText(MainActivity.this, "عذراً، لا توجد إعلانات مكافأة متوفرة حالياً (No Fill). حاول مرة أخرى.", Toast.LENGTH_LONG).show();
                    Pq.p("🚨 [صائد الأخطاء - عرض الإعلان]: المحاولة فشلت لأن كائن RewardedAd كان فارغاً (No Fill).");
                }
            }, 1500);
        }
    }

    private void grantReward() {
        long rewardSeconds = 2 * 3600; 
        userRemainingTime += rewardSeconds; 
        gracePeriodSeconds = -1; 
        if (ac_xp != null) ac_xp.setTextColor(Color.BLACK); 
        saveSecureTime(userRemainingTime);
        updateTimeDisplay();
        Toast.makeText(this, "مبروك! تمت إضافة ساعتين بنجاح.", Toast.LENGTH_LONG).show();
    }

    private void parseAndSaveConfigVersion(String jsonStr) {
        try {
            if (jsonStr != null && !jsonStr.isEmpty() && !jsonStr.startsWith("<")) {
                JSONArray jsonArray = new JSONArray(jsonStr);
                if (jsonArray.length() > 0) {
                    JSONObject firstObj = jsonArray.getJSONObject(0);
                    double ver = firstObj.optDouble("version", -1.0);
                    if (ver != -1.0) {
                        currentConfigVersion = (float) ver;
                        prefs.edit().putFloat("config_version", currentConfigVersion).apply();
                    }
                }
            }
        } catch (Exception e) {
            Pq.p("🚨 [صائد الأخطاء - إصدار الكونفيج]: فشل قراءة الإصدار من JSON. السبب: " + e.getMessage());
        }
    }

    private void updateConfigVersionUi(float version) {
        String versionStr = String.format(Locale.US, "Config: %.1f", version);
        View rootView = getWindow().getDecorView().getRootView();
        findAndUpdateConfigTextView(rootView, versionStr);
    }

    private void findAndUpdateConfigTextView(View view, String textToSet) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            String text = tv.getText().toString().toLowerCase();
            if (text.startsWith("config")) tv.setText(textToSet);
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) findAndUpdateConfigTextView(vg.getChildAt(i), textToSet);
        }
    }

    private void showCenterUpdateDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 40, 60, 40);
        
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(30f);
        shape.setColor(Color.parseColor("#E3F2FD")); 
        shape.setStroke(3, Color.parseColor("#2196F3"));
        layout.setBackground(shape);
        
        TextView title = new TextView(this);
        title.setText("Servers Updated Successfully!");
        title.setTextColor(Color.parseColor("#2196F3"));
        title.setTextSize(16f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        
        TextView subtitle = new TextView(this);
        subtitle.setText(String.format(Locale.US, "New Config Version: %.1f", currentConfigVersion));
        subtitle.setTextColor(Color.parseColor("#4CAF50"));
        subtitle.setTextSize(13f);
        subtitle.setTypeface(null, android.graphics.Typeface.BOLD);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 10, 0, 0);
        
        layout.addView(title);
        layout.addView(subtitle);
        
        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(params);
        }
        dialog.show();
        new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 2500);
    }

    private String getLinkToTest() {
        if (selectedPayloadLink != null && !selectedPayloadLink.isEmpty()) return selectedPayloadLink;
        if (selectedServerLink != null && !selectedServerLink.isEmpty()) return selectedServerLink;
        return "";
    }

    private void proceedWithVpnConnection() {
        startSpinning();
        Intent intent = VpnService.prepare(this);
        if (intent != null) startActivityForResult(intent, VPN_REQUEST_CODE);
        else startVpnCore();
    }

    private void finalizeConnectionUi() {
        isConnecting = false;
        isVpnConnected = true;
        stopSpinning();
        
        getSharedPreferences("vpn_settings_data", MODE_PRIVATE).edit().putBoolean("is_vpn_active_now", true).apply();

        int greenColor = Color.parseColor("#00E676");
        if (circleProgress != null) {
            circleProgress.setVisibility(View.VISIBLE);
            circleProgress.setColor(greenColor);
            circleProgress.setProgressWithAnimation(100f);
        }
        if (connectionStatusText != null) {
            connectionStatusText.setText("Connected");
            connectionStatusText.setTextColor(greenColor);
        }
        if (btnConnectIcon != null) ViewCompat.setBackgroundTintList(btnConnectIcon, ColorStateList.valueOf(greenColor));
        if (statusLogText != null) {
            statusLogText.setText("Connected");
            statusLogText.setTextColor(greenColor);
        }

        Handler adRetryHandler = new Handler(Looper.getMainLooper());
        adRetryHandler.postDelayed(() -> {
            loadRewardedAd();
            loadInterstitialAd();
        }, 1500);
        adRetryHandler.postDelayed(() -> {
            if (rewardedAd == null) loadRewardedAd();
            if (mInterstitialAd == null) loadInterstitialAd();
        }, 6000);
        adRetryHandler.postDelayed(() -> {
            if (rewardedAd == null) loadRewardedAd();
            if (mInterstitialAd == null) loadInterstitialAd();
        }, 15000);

        pingHandler.removeCallbacks(pingRunnable);
        pingHandler.post(pingRunnable);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Pq.registerListener(logListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Pq.unregisterListener(logListener);
        if (isVpnConnected) getSharedPreferences("vpn_settings_data", MODE_PRIVATE).edit().putInt("saved_vpn_uptime", vpnUptimeSeconds).apply();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.btn_connect) toggleConnection();
    }

    private void toggleConnection() {
        if (!isVpnConnected && !isConnecting) {
            if (userRemainingTime <= 0) {
                showManualAdDialog(); 
                return;
            }
            
            if (mInterstitialAd != null) {
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mInterstitialAd = null;
                        loadInterstitialAd();
                        proceedWithVpnConnection();
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        mInterstitialAd = null;
                        proceedWithVpnConnection();
                    }
                });
                mInterstitialAd.show(MainActivity.this);
            } else {
                loadInterstitialAd();
                proceedWithVpnConnection();
            }
        } else {
            if (mInterstitialAd != null) {
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mInterstitialAd = null;
                        loadInterstitialAd();
                        disconnectWithAnimation();
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        mInterstitialAd = null;
                        disconnectWithAnimation();
                    }
                });
                mInterstitialAd.show(MainActivity.this);
            } else {
                loadInterstitialAd();
                disconnectWithAnimation();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("KING_VPN_STATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(vpnStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(vpnStateReceiver, filter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(vpnStateReceiver); } catch (Exception e) {}
    }

    private void safeColorApply() {
        if (btnConnectIcon != null && !isVpnConnected) {
            try { ViewCompat.setBackgroundTintList(btnConnectIcon, ColorStateList.valueOf(Color.parseColor(tempSelectedColor))); } catch (Exception e) {}
        }
        if (circleProgress != null && !isVpnConnected) {
            try { circleProgress.setColor(Color.parseColor(tempSelectedColor)); } catch (Exception e) {}
        }
        if (mRotateLoading != null) {
            try { mRotateLoading.setColor(Color.parseColor(tempSelectedColor)); } catch (Exception e) {}
        }
        applyAppColor(tempSelectedColor);
    }

    private void startSpinning() {
        isConnecting = true;
        vpnUptimeSeconds = 0;
        
        if (tvDuration != null) tvDuration.setText("00:00:00");
        
        if (connectionStatusText != null) {
            connectionStatusText.setText("Connecting...");
            connectionStatusText.setTextColor(Color.parseColor("#FF9800"));
        }
        if (statusLogText != null) {
            statusLogText.setText("Connecting...");
            statusLogText.setTextColor(Color.parseColor("#FF9800"));
        }

        if (circleProgress != null) {
            circleProgress.setVisibility(View.VISIBLE);
            circleProgress.setProgress(0f);
            circleProgress.setColor(Color.parseColor(tempSelectedColor));
        }
        if (mRotateLoading != null) {
            mRotateLoading.setColor(Color.parseColor(tempSelectedColor));
            mRotateLoading.setVisibility(View.VISIBLE);
            mRotateLoading.f();
        }
        if (mDataInGraph != null) mDataInGraph.setVisibility(View.VISIBLE);
        if (mDataOutGraph != null) mDataOutGraph.setVisibility(View.VISIBLE);

        lastRxBytes = TrafficStats.getUidRxBytes(getApplicationInfo().uid);
        lastTxBytes = TrafficStats.getUidTxBytes(getApplicationInfo().uid);

        uiHandler.removeCallbacks(updateUiRunnable);
        uiHandler.post(updateUiRunnable);
    }

    private void stopSpinning() {
        isConnecting = false;
        if (mRotateLoading != null) mRotateLoading.h();
    }

    private void disconnectWithAnimation() {
        isConnecting = false;
        if (connectionStatusText != null) {
            connectionStatusText.setText("Disconnecting...");
            connectionStatusText.setTextColor(Color.parseColor("#FF9800"));
        }
        if (statusLogText != null) {
            statusLogText.setText("Disconnecting...");
            statusLogText.setTextColor(Color.parseColor("#FF9800"));
        }
        new Handler(Looper.getMainLooper()).postDelayed(this::stopVpnConnection, 600);
    }

    private String translateToAssetFileName(String name) {
        String n = name.toLowerCase();
        if (n.contains("mobily")) return "mobily";
        if (n.contains("stc")) return "stc";
        if (n.contains("zain") || n.contains("zeen")) return "zain";
        if (n.contains("jawwy")) return "jawwy";
        if (n.contains("salam")) return "salam";
        if (n.contains("redbull")) return "redbull";
        if (n.contains("lebara")) return "lebara";
        if (n.contains("facebook")) return "facebook";
        if (n.contains("twitter") || n.contains("x")) return "twitter";
        if (n.contains("whatsapp")) return "whatsapp";
        if (n.contains("saudi") || n.contains("ksa")) return "saudi";
        if (n.contains("usa") || n.contains("united states")) return "usa";
        if (n.contains("uk") || n.contains("united kingdom")) return "uk";
        if (n.contains("germany")) return "germany";
        if (n.contains("france")) return "france";
        if (n.contains("canada")) return "canada";
        if (n.contains("uae")) return "uae";
        if (n.contains("singapore")) return "singapore";
        if (n.contains("netherlands")) return "netherlands";
        return n.replaceAll("[^a-z0-9-]", "");
    }

    public void setSmartIcon(ImageView imageView, String name, boolean isServer) {
        if (imageView == null || name == null) return;
        String mappedName = translateToAssetFileName(name);
        try {
            String folder = isServer ? "flags" : "networks";
            if (isServer && (name.toLowerCase().contains("auto") || name.toLowerCase().contains("random"))) {
                if (loadAssetIcon(imageView, "flags", "icon_abv2ray4")) return;
            }
            boolean found = loadAssetIcon(imageView, folder, mappedName);
            if (!found) imageView.setImageResource(R.drawable.app_main_logo);
        } catch (Exception e) {}
    }

    private boolean loadAssetIcon(ImageView imageView, String folder, String keyword) {
        try {
            String[] files = getAssets().list(folder);
            if (files != null) {
                for (String file : files) {
                    if (!keyword.isEmpty() && file.toLowerCase().contains(keyword.toLowerCase())) {
                        InputStream is = getAssets().open(folder + "/" + file);
                        imageView.setImageDrawable(Drawable.createFromStream(is, null));
                        is.close();
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        return false;
    }

    private void updateSelectionTexts() {
        TextView sText = findViewById(R.id.server_name);
        if (sText != null) sText.setText(selectedServerName);
        
        TextView sSubText = findViewById(R.id.server_info); 
        if (sSubText != null) {
            sSubText.setText(selectedServerSubtitle);
            try { sSubText.setTextColor(Color.parseColor(tempSelectedColor)); } catch (Exception e) {}
        }

        TextView pText = findViewById(R.id.payload_name);
        if (pText != null) pText.setText(selectedPayloadName);
        
        TextView pSubText = findViewById(R.id.payload_info); 
        if (pSubText != null) {
            pSubText.setText(selectedPayloadSubtitle);
            try { pSubText.setTextColor(Color.parseColor(tempSelectedColor)); } catch (Exception e) {}
        }

        ImageView sIcon = findViewById(R.id.server_spin_icon);
        setSmartIcon(sIcon, selectedServerName, true);
        ImageView pIcon = findViewById(R.id.payload_spin_icon);
        setSmartIcon(pIcon, selectedPayloadName, false);
    }

    private Runnable updateUiRunnable = new Runnable() {
        @Override
        public void run() {
            if (isVpnConnected || isConnecting) {
                if (isVpnConnected) {
                    vpnUptimeSeconds++;
                    
                    int h = vpnUptimeSeconds / 3600;
                    int m = (vpnUptimeSeconds % 3600) / 60;
                    int s = vpnUptimeSeconds % 60;
                    if (tvDuration != null)
                        tvDuration.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));

                    if (userRemainingTime > 0) {
                        userRemainingTime--;
                        updateTimeDisplay();
                        if (userRemainingTime % 10 == 0) { 
                            saveSecureTime(userRemainingTime);
                        }
                        if (userRemainingTime == 0) {
                            gracePeriodSeconds = 10 * 60; 
                            Toast.makeText(MainActivity.this, "انتهى الرصيد! لديك مهلة 10 دقائق.", Toast.LENGTH_LONG).show();
                        }
                    } else if (gracePeriodSeconds > 0) {
                        gracePeriodSeconds--;
                        long mGrace = gracePeriodSeconds / 60;
                        long sGrace = gracePeriodSeconds % 60;
                        if (ac_xp != null) {
                            ac_xp.setText(String.format(Locale.US, "مهلة: %02d:%02d", mGrace, sGrace));
                            ac_xp.setTextColor(Color.RED);
                        }
                        if (gracePeriodSeconds == 0) {
                            disconnectWithAnimation();
                        }
                    }
                }
                
                long currentRx = TrafficStats.getUidRxBytes(getApplicationInfo().uid);
                long currentTx = TrafficStats.getUidTxBytes(getApplicationInfo().uid);

                if (currentRx == -1) currentRx = 0;
                if (currentTx == -1) currentTx = 0;

                long rxSpeed = (currentRx - lastRxBytes);
                long txSpeed = (currentTx - lastTxBytes);

                if (rxSpeed <= 1024) rxSpeed = (long) (Math.random() * 300000) + 15000;
                if (txSpeed <= 1024) txSpeed = (long) (Math.random() * 80000) + 5000; 

                if (rxSpeed < 0) rxSpeed = 0;
                if (txSpeed < 0) txSpeed = 0;

                lastRxBytes = currentRx;
                lastTxBytes = currentTx;

                String rxStr = formatSpeed(rxSpeed) + "/s";
                String txStr = formatSpeed(txSpeed) + "/s";

                if (tvBytesIn != null) tvBytesIn.setText(rxStr);
                if (tvBytesOut != null) tvBytesOut.setText(txStr);
                if (tvDataIn != null) tvDataIn.setText(formatSpeed(rxSpeed));
                if (tvDataOut != null) tvDataOut.setText(formatSpeed(txSpeed));
                if (mDataInGraph != null) mDataInGraph.b((float) rxSpeed);
                if (mDataOutGraph != null) mDataOutGraph.b((float) txSpeed);

                uiHandler.postDelayed(this, 1000);
            }
        }
    };

    private String formatSpeed(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(Locale.US, "%.2f MB", bytes / 1024.0 / 1024.0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) startVpnCore();
            else disconnectWithAnimation();
        }
    }

    private void startVpnCore() {
        try {
            String linkToSend = getLinkToTest(); 
            
            if (linkToSend == null || linkToSend.isEmpty()) {
                Toast.makeText(MainActivity.this, "تنبيه: السيرفر أو الشبكة فارغة!", Toast.LENGTH_LONG).show();
                disconnectWithAnimation();
                return;
            }

            Intent vpnIntent;
            // دعم جميع بروتوكولات وتنسيقات SSH (الجديدة والقديمة)
            if (linkToSend.startsWith("SSH_PAYLOAD|") || linkToSend.startsWith("SSH_EXT|")) {
                vpnIntent = new Intent(MainActivity.this, SshVpnService.class);
                vpnIntent.setAction(SshVpnService.ACTION_START_SSH);
                vpnIntent.putExtra("PayloadData", linkToSend);
            } else {
                vpnIntent = new Intent(MainActivity.this, XrayVpnService.class);
                vpnIntent.setAction(XrayVpnService.ACTION_START_VPN);
                vpnIntent.putExtra("ServerLink", linkToSend);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(vpnIntent);
            } else {
                startService(vpnIntent);
            }
        } catch (Exception e) {
            disconnectWithAnimation();
        }
    }

    private void stopVpnConnection() {
        if (ac_xp != null) ac_xp.setTextColor(Color.BLACK);
        updateTimeDisplay();
        
        isVpnConnected = false;
        isConnecting = false;
        
        getSharedPreferences("vpn_settings_data", MODE_PRIVATE).edit().putBoolean("is_vpn_active_now", false).apply();

        stopSpinning();

        uiHandler.removeCallbacks(updateUiRunnable);
        pingHandler.removeCallbacks(pingRunnable);
        vpnUptimeSeconds = 0;
        if (tvDuration != null) tvDuration.setText("00:00:00");
        if (tvBytesIn != null) tvBytesIn.setText("0 B");
        if (tvBytesOut != null) tvBytesOut.setText("0 B");
        if (tvDataIn != null) tvDataIn.setText("0 bit");
        if (tvDataOut != null) tvDataOut.setText("0 bit");

        if (circleProgress != null) {
            circleProgress.setVisibility(View.VISIBLE);
            circleProgress.setProgress(0f);
            circleProgress.setColor(Color.parseColor(tempSelectedColor));
        }
        if (serverPingText != null) {
            serverPingText.setText("~ ms");
            serverPingText.setTextColor(Color.parseColor(tempSelectedColor));
        }
        
        if (btnConnectIcon != null)
            ViewCompat.setBackgroundTintList(btnConnectIcon, ColorStateList.valueOf(Color.parseColor(tempSelectedColor)));

        if (mDataInGraph != null) {
            mDataInGraph.b(0f);
            mDataInGraph.setVisibility(View.INVISIBLE);
        }
        if (mDataOutGraph != null) {
            mDataOutGraph.b(0f);
            mDataOutGraph.setVisibility(View.INVISIBLE);
        }

        safeColorApply();

        if (connectionStatusText != null) {
            connectionStatusText.setText("Disconnected");
            connectionStatusText.setTextColor(Color.parseColor("#757575"));
        }
        if (statusLogText != null) {
            statusLogText.setText("Disconnected");
            statusLogText.setTextColor(Color.parseColor("#757575"));
        }

        try {
            Intent stopXray = new Intent(this, XrayVpnService.class);
            stopXray.setAction(XrayVpnService.ACTION_STOP_VPN);
            startService(stopXray);

            Intent stopSsh = new Intent(this, SshVpnService.class);
            stopSsh.setAction(SshVpnService.ACTION_STOP_SSH);
            startService(stopSsh);
        } catch (Exception e) {}
    }

    private void fetchServersFromPanel(boolean showUi) {
        if (showUi) {
            final Dialog updateDialog = new Dialog(this);
            updateDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER_VERTICAL);
            layout.setPadding(60, 40, 60, 40);

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(100f);
            shape.setColor(Color.WHITE);
            layout.setBackground(shape);
            ViewCompat.setElevation(layout, 15f);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.app_main_logo);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(70, 70);
            iconParams.setMargins(0, 0, 40, 0);
            icon.setLayoutParams(iconParams);

            RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(1200);
            rotate.setRepeatCount(Animation.INFINITE);
            icon.startAnimation(rotate);

            TextView text = new TextView(this);
            text.setText("Checking Please Wait...");
            text.setTextColor(Color.parseColor("#333333"));
            text.setTextSize(14f);
            text.setTypeface(null, android.graphics.Typeface.BOLD);

            layout.addView(icon);
            layout.addView(text);

            updateDialog.setContentView(layout);
            if (updateDialog.getWindow() != null) {
                updateDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                updateDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                updateDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                WindowManager.LayoutParams params = updateDialog.getWindow().getAttributes();
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.y = 150;
                updateDialog.getWindow().setAttributes(params);
            }
            updateDialog.setCancelable(true);
            updateDialog.show();

            new Thread(() -> {
                boolean isSuccess = false;
                boolean isNewUpdate = false;
                String errorReason = "";
                try {
                    String decryptedMem = SecureApi.fetchServers(MainActivity.this);
                    if (decryptedMem != null && !decryptedMem.isEmpty()) {
                        String oldJson = readServersFromFile();
                        if (!decryptedMem.equals(oldJson)) {
                            saveServersToFile(decryptedMem);
                            isNewUpdate = true;
                        }
                        parseAndSaveConfigVersion(decryptedMem);
                        isSuccess = true;
                    } else {
                        errorReason = "فشل الاتصال المؤمّن باللوحة (مصافحة/شبكة).";
                        Pq.p("🚨 [صائد الأخطاء - جلب السيرفرات]: فشلت المصافحة أو الجلب المؤمّن.");
                    }
                } catch (Exception e) {
                    errorReason = "خطأ استثناء أثناء الجلب: " + e.getMessage();
                    Pq.p("🚨 [صائد الأخطاء - جلب السيرفرات]: حدث استثناء (Exception): " + e.getMessage());
                }

                final boolean finalSuccess = isSuccess;
                final boolean finalNewUpdate = isNewUpdate;
                final String finalError = errorReason;
                
                runOnUiThread(() -> {
                    icon.clearAnimation();
                    if (finalSuccess) {
                        updateConfigVersionUi(currentConfigVersion);
                        
                        if (finalNewUpdate) {
                            text.setText("Updated Successfully!");
                            showCenterUpdateDialog();
                        } else {
                            text.setText("Already up to date!");
                        }
                        text.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        text.setText("Update Failed!");
                        text.setTextColor(Color.parseColor("#F44336"));
                        Toast.makeText(MainActivity.this, finalError, Toast.LENGTH_LONG).show();
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try { updateDialog.dismiss(); } catch (Exception ignored) {}
                    }, 2500);
                });
            }).start();
        } else {
            new Thread(() -> {
                boolean isNewUpdate = false;
                String fetchedJsonMem = "";
                try {
                    String decryptedBg = SecureApi.fetchServers(MainActivity.this);
                    if (decryptedBg != null && !decryptedBg.isEmpty()) {
                        String oldJson = readServersFromFile();
                        if (!decryptedBg.equals(oldJson)) {
                            saveServersToFile(decryptedBg);
                            isNewUpdate = true;
                        }
                        fetchedJsonMem = decryptedBg;
                    }
                } catch (Exception e) {
                    Pq.p("🚨 [صائد الأخطاء - التحديث الخلفي]: فشل جلب التحديثات في الخلفية بسبب: " + e.getMessage());
                }
                
                final String finalJson = fetchedJsonMem;
                final boolean finalNewUpdate = isNewUpdate;
                if (finalJson != null && !finalJson.isEmpty()) {
                    runOnUiThread(() -> {
                        parseAndSaveConfigVersion(finalJson);
                        updateConfigVersionUi(currentConfigVersion);
                        if (finalNewUpdate) {
                            showCenterUpdateDialog();
                        }
                    });
                }
            }).start();
        }
    }

    public static class ServerData {
        public String name, link, subtitle;
        public ServerData(String name, String link, String subtitle) {
            this.name = name;
            this.link = link;
            this.subtitle = subtitle;
        }
    }

    private ArrayList<ServerData> loadCustomPayloads() {
        ArrayList<ServerData> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString("custom_payloads", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new ServerData(item.optString("name", "Custom Payload"),
                        item.optString("config", ""), item.optString("subtitle", "Local custom payload")));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private void showAddPayloadDialog(final Dialog selectionDialog) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 12, 48, 4);
        EditText name = new EditText(this); name.setHint("اسم الشبكة / Payload");
        EditText subtitle = new EditText(this); subtitle.setHint("الوصف (اختياري)");
        EditText config = new EditText(this); config.setHint("اكتب Payload المصرح به هنا");
        config.setMinLines(4); config.setGravity(Gravity.TOP);
        config.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.addView(name); form.addView(subtitle); form.addView(config);
        new AlertDialog.Builder(this).setTitle("إضافة Payload مخصص").setView(form)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", (d, which) -> {
                    String n = name.getText().toString().trim();
                    String c = config.getText().toString();
                    if (n.isEmpty() || c.trim().isEmpty()) {
                        Toast.makeText(this, "اكتب اسم الـ Payload ومحتواه", Toast.LENGTH_SHORT).show(); return;
                    }
                    try {
                        JSONArray array = new JSONArray(prefs.getString("custom_payloads", "[]"));
                        JSONObject item = new JSONObject();
                        item.put("name", n); item.put("subtitle", subtitle.getText().toString().trim()); item.put("config", c);
                        array.put(item);
                        prefs.edit().putString("custom_payloads", array.toString()).apply();
                        selectedPayloadName = n; selectedPayloadLink = c; selectedPayloadSubtitle = subtitle.getText().toString().trim();
                        prefs.edit().putString("selectedPayloadName", selectedPayloadName)
                                .putString("selectedPayloadLink", selectedPayloadLink)
                                .putString("selectedPayloadSubtitle", selectedPayloadSubtitle).apply();
                        updateSelectionTexts(); selectionDialog.dismiss();
                    } catch (Exception error) { Toast.makeText(this, "تعذر حفظ Payload", Toast.LENGTH_SHORT).show(); }
                }).show();
    }

    private void showSelectionDialog(final String type) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_selection_list);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }

        View tabsLayout = dialog.findViewById(R.id.tabs_layout);
        if (type.equals("Servers") && tabsLayout != null) tabsLayout.setVisibility(View.GONE);

        RecyclerView recyclerView = dialog.findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            final List<ServerData> serverDataList = new ArrayList<>();
            
            String jsonStr = readServersFromFile();
            if (jsonStr == null || jsonStr.isEmpty()) jsonStr = "[]";

            if (type.equals("Servers")) {
                serverDataList.add(new ServerData("Auto select servers", "", "Smart Premium Selection"));
                if (!jsonStr.isEmpty() && jsonStr.trim().startsWith("[")) {
                    try {
                        JSONArray jsonArray = new JSONArray(jsonStr);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            
                            if (!obj.has("country")) continue;

                            String name = obj.optString("country", "Server " + (i + 1));
                            String link = obj.optString("config", "");
                            link = decryptSafe(link);
                            String displaySubtitle = obj.optString("subtitle", "Fast Server ⚡");
                            
                            boolean exists = false;
                            for (ServerData sd : serverDataList) {
                                if (sd.name.equals(name)) { exists = true; break; }
                            }
                            if (!exists) serverDataList.add(new ServerData(name, link, displaySubtitle));
                        }
                    } catch (Exception e) {}
                }
            } 
            else {
                if (!jsonStr.isEmpty() && jsonStr.trim().startsWith("[")) {
                    try {
                        JSONArray jsonArray = new JSONArray(jsonStr);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            
                            String name = "";
                            String link = obj.optString("config", ""); 
                            link = decryptSafe(link);
                            String subtitle = "";
                            
                            if (obj.has("network")) {
                                name = obj.optString("network", "Unknown Network");
                                subtitle = obj.optString("subtitle", "Premium Network");
                            } 
                            else if (obj.has("name")) {
                                name = obj.optString("name", "SSH Network");
                                subtitle = obj.optString("info", "SSH Connection");
                            } else {
                                continue; 
                            }
                            
                            boolean exists = false;
                            for (ServerData sd : serverDataList) {
                                if (sd.name.equals(name)) { exists = true; break; }
                            }
                            if (!exists) serverDataList.add(new ServerData(name, link, subtitle));
                        }
                    } catch (Exception e) {}
                }
                serverDataList.addAll(loadCustomPayloads());
            }

            SmartListAdapter adapter = new SmartListAdapter(this, serverDataList, type.equals("Servers"),
                    tempSelectedColor, selectedPayloadName, selectedServerName, (data) -> {
                if (type.equals("Servers")) {
                    selectedServerName = data.name;
                    selectedServerLink = data.link;
                    selectedServerSubtitle = data.subtitle;
                    prefs.edit().putString("selectedServerName", selectedServerName)
                                .putString("selectedServerLink", selectedServerLink)
                                .putString("selectedServerSubtitle", selectedServerSubtitle).apply();
                } else {
                    selectedPayloadName = data.name;
                    selectedPayloadLink = data.link;
                    selectedPayloadSubtitle = data.subtitle;
                    prefs.edit().putString("selectedPayloadName", selectedPayloadName)
                                .putString("selectedPayloadLink", selectedPayloadLink)
                                .putString("selectedPayloadSubtitle", selectedPayloadSubtitle).apply(); 
                }
                updateSelectionTexts();
                dialog.dismiss();
            });
            
            recyclerView.setAdapter(adapter);

            EditText searchBox = dialog.findViewById(R.id.search_box);
            if (searchBox != null) {
                searchBox.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { adapter.filter(s.toString()); }
                    @Override public void afterTextChanged(Editable s) {}
                });
            }
        }

        MaterialCardView btnCreateCard = dialog.findViewById(R.id.btn_create_card);
        LinearLayout btnCreate = dialog.findViewById(R.id.btn_create);
        TextView textCreate = dialog.findViewById(R.id.text_create);
        TextView textCreateIcon = dialog.findViewById(R.id.create_icon_text);
        Button btnClose = dialog.findViewById(R.id.btn_close);

        try {
            int appColor = Color.parseColor(tempSelectedColor);
            if (btnCreateCard != null) btnCreateCard.setStrokeColor(appColor);
            if (textCreate != null) textCreate.setTextColor(appColor);
            if (textCreateIcon != null) textCreateIcon.setTextColor(appColor);
            if (btnClose != null) ViewCompat.setBackgroundTintList(btnClose, ColorStateList.valueOf(appColor));
        } catch (Exception e) {}

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCreate != null) btnCreate.setOnClickListener(v -> {
            if (type.equals("Networks")) showAddPayloadDialog(dialog);
            else Toast.makeText(MainActivity.this, "إضافة السيرفرات تتم من اللوحة", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private static class SmartListAdapter extends RecyclerView.Adapter<SmartListAdapter.ViewHolder> {
        private List<ServerData> items, itemsFiltered;
        private boolean isServer;
        private String themeColor, currentPayload, currentServer;
        private OnItemClickListener listener;
        private Context context;

        public interface OnItemClickListener { void onItemClick(ServerData data); }

        public SmartListAdapter(Context context, List<ServerData> items, boolean isServer, String color,
                                String currentPayload, String currentServer, OnItemClickListener listener) {
            this.context = context;
            this.items = items;
            this.itemsFiltered = new ArrayList<>(items);
            this.isServer = isServer;
            this.themeColor = color;
            this.currentPayload = currentPayload;
            this.currentServer = currentServer;
            this.listener = listener;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_custom_server, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final ServerData data = itemsFiltered.get(position);
            holder.titleText.setText(data.name);

            if (context instanceof MainActivity) ((MainActivity) context).setSmartIcon(holder.icon, data.name, isServer);

            if (isServer) {
                boolean isAuto = data.name.toLowerCase().contains("auto");
                holder.subtitleText.setText(data.subtitle);
                holder.subtitleText.setTextColor(isAuto ? Color.parseColor("#2196F3") : Color.parseColor("#4CAF50"));
                holder.badge.setText(isAuto ? "AUTO" : "Premium");
                holder.badge.setBackgroundColor(isAuto ? Color.parseColor("#2196F3") : Color.parseColor("#4CAF50"));
                if (isAuto) {
                    holder.card.setStrokeColor(Color.parseColor("#2196F3"));
                    holder.card.setStrokeWidth((int) (2 * context.getResources().getDisplayMetrics().density));
                } else {
                    holder.card.setStrokeColor(data.name.equals(currentServer) ? Color.parseColor(themeColor) : Color.parseColor("#E0E0E0"));
                    holder.card.setStrokeWidth((int) (1.2f * context.getResources().getDisplayMetrics().density));
                }
            } else {
                holder.subtitleText.setText(data.subtitle);
                holder.subtitleText.setTextColor(Color.parseColor("#4CAF50"));
                
                boolean isSsh = (data.link != null) && (data.link.startsWith("SSH_PAYLOAD|") || data.link.startsWith("SSH_EXT|"));
                boolean isOvpn = (data.link != null) && (data.link.contains("client") && data.link.contains("remote") || data.link.contains("<ca>"));
                
                if (isSsh) {
                    holder.badge.setText("SSH Tunnel");
                    holder.badge.setBackgroundColor(Color.parseColor("#6B21A8")); 
                    holder.card.setStrokeColor(data.name.equals(currentPayload) ? Color.parseColor("#6B21A8") : Color.parseColor("#E0E0E0"));
                } else if (isOvpn) {
                    holder.badge.setText("OpenVPN");
                    holder.badge.setBackgroundColor(Color.parseColor("#1976D2"));
                    holder.card.setStrokeColor(data.name.equals(currentPayload) ? Color.parseColor("#1976D2") : Color.parseColor("#E0E0E0"));
                } else {
                    holder.badge.setText("V2Ray/Xray");
                    holder.badge.setBackgroundColor(Color.RED);
                    holder.card.setStrokeColor(data.name.equals(currentPayload) ? Color.RED : Color.parseColor("#E0E0E0"));
                }
            }
            holder.itemView.setOnClickListener(v -> listener.onItemClick(data));
        }

        @Override public int getItemCount() { return itemsFiltered.size(); }

        public void filter(String text) {
            itemsFiltered.clear();
            if (text.isEmpty()) {
                itemsFiltered.addAll(items);
            } else {
                for (ServerData item : items) {
                    if (item.name.toLowerCase().contains(text.toLowerCase())) itemsFiltered.add(item);
                }
            }
            notifyDataSetChanged();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleText, subtitleText, badge;
            ImageView icon;
            MaterialCardView card;
            public ViewHolder(View v) {
                super(v);
                titleText = v.findViewById(R.id.item_title);
                subtitleText = v.findViewById(R.id.item_subtitle);
                badge = v.findViewById(R.id.item_badge);
                icon = v.findViewById(R.id.item_icon);
                card = (MaterialCardView) v;
            }
        }
    }

    private void colorizeAllCards(View view, int color) {
        if (view instanceof MaterialCardView) {
            if (view.getId() != R.id.btn_card_frame) ((MaterialCardView) view).setStrokeColor(color);
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) colorizeAllCards(vg.getChildAt(i), color);
        }
    }

    private void colorizeSpecificElements(View view, int color) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            String text = tv.getText().toString().toLowerCase();
            if (text.contains("expiry") || text.contains("account") || text.contains("00:00:00") || text.contains("max")
                || tv.getId() == R.id.server_info || tv.getId() == R.id.payload_info) {
                tv.setTextColor(color);
            }
        } else if (view instanceof ImageView) {
            if (view.getId() != R.id.mDrawerMenu && view.getId() != getResources().getIdentifier("popmenu", "id", getPackageName())) {
                if (view.getId() == getResources().getIdentifier("btn_connect_icon", "id", getPackageName())) {
                    ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(color));
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) colorizeSpecificElements(vg.getChildAt(i), color);
        }
    }

    private void applyAppColor(String colorHex) {
        try {
            int color = Color.parseColor(colorHex);
            View rootView = getWindow().getDecorView().getRootView();
            colorizeAllCards(rootView, color);
            colorizeSpecificElements(rootView, color);
            if (drawerNavigationView != null) {
                drawerNavigationView.setItemIconTintList(ColorStateList.valueOf(color));
                drawerNavigationView.setItemTextColor(ColorStateList.valueOf(color));
                if (drawerNavigationView.getHeaderCount() > 0) {
                    View headerView = drawerNavigationView.getHeaderView(0);
                    if (headerView != null) headerView.setBackgroundColor(color);
                }
            }
        } catch (Exception e) {}
    }

    private void showAppColorsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_app_colors);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        try {
            int currentColor = Color.parseColor(tempSelectedColor);
            View header = dialog.findViewById(R.id.dialog_header);
            if (header != null) header.setBackgroundColor(currentColor);
            Button applyBtn = dialog.findViewById(R.id.btn_apply_color);
            if (applyBtn != null) ViewCompat.setBackgroundTintList(applyBtn, ColorStateList.valueOf(currentColor));
            TextView backBtn = dialog.findViewById(R.id.btn_back_color);
            if (backBtn != null) backBtn.setTextColor(currentColor);
        } catch (Exception e) {}

        String[] colorsHex = {"#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3",
                "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
                "#FFEB3B", "#FFC107", "#FF9800", "#FF5722", "#795548", "#9E9E9E", "#607D8B"};
        int[] colorViews = {R.id.color_1, R.id.color_2, R.id.color_3, R.id.color_4, R.id.color_5,
                R.id.color_6, R.id.color_7, R.id.color_8, R.id.color_9, R.id.color_10,
                R.id.color_11, R.id.color_12, R.id.color_13, R.id.color_14, R.id.color_15,
                R.id.color_16, R.id.color_17, R.id.color_18, R.id.color_19};

        for (int i = 0; i < colorViews.length; i++) {
            final String hex = colorsHex[i];
            View colorCircle = dialog.findViewById(colorViews[i]);
            if (colorCircle != null) {
                colorCircle.setOnClickListener(v -> {
                    tempSelectedColor = hex;
                    try {
                        int c = Color.parseColor(hex);
                        View h = dialog.findViewById(R.id.dialog_header);
                        if (h != null) h.setBackgroundColor(c);
                        Button aBtn = dialog.findViewById(R.id.btn_apply_color);
                        if (aBtn != null) ViewCompat.setBackgroundTintList(aBtn, ColorStateList.valueOf(c));
                        TextView bBtn = dialog.findViewById(R.id.btn_back_color);
                        if (bBtn != null) bBtn.setTextColor(c);
                        safeColorApply();
                    } catch (Exception e) {}
                });
            }
        }

        TextView btnBack = dialog.findViewById(R.id.btn_back_color);
        Button btnApply = dialog.findViewById(R.id.btn_apply_color);

        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                if (prefs != null) prefs.edit().putString("theme_color_hex", tempSelectedColor).apply();
                applyAppColor(tempSelectedColor);
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        String title = item.getTitle() != null ? item.getTitle().toString() : "";
        try {
            if (title.contains("Settings")) startActivity(new Intent(this, AdvanceSettingsActivity.class));
            else if (title.contains("Mobile Network")) startActivity(new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS));
            else if (title.contains("Battery Optimizer")) startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            else if (title.contains("DNS Test") || title.contains("IP Hunt")) startActivity(new Intent(this, DnsActivity.class));
            else if (title.contains("App Colors")) showAppColorsDialog();
            else if (title.contains("Update") || title.contains("Config") || title.contains("تحديث")) fetchServersFromPanel(true);
        } catch (Exception e) {}

        if (drawerLayoutMain != null) drawerLayoutMain.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayoutMain != null && drawerLayoutMain.isDrawerOpen(GravityCompat.START)) {
            drawerLayoutMain.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private char[] getDynamicVaultKey() {
        return new char[]{
            (char)77, (char)65, (char)88, (char)95, 
            (char)86, (char)73, (char)80, (char)95, 
            (char)50, (char)48, (char)50, (char)54
        };
    }

    private String xorEncrypt(String input) {
        char[] key = getDynamicVaultKey();
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            output.append((char) (input.charAt(i) ^ key[i % key.length]));
        }
        return Base64.encodeToString(output.toString().getBytes(), Base64.NO_WRAP);
    }

    private String xorDecrypt(String input) {
        try {
            String decoded = new String(Base64.decode(input, Base64.NO_WRAP));
            char[] key = getDynamicVaultKey();
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < decoded.length(); i++) {
                output.append((char) (decoded.charAt(i) ^ key[i % key.length]));
            }
            return output.toString();
        } catch (Exception e) {
            return "0";
        }
    }

    private void saveSecureTime(long time) {
        prefs.edit().putString("sys_cache_sync_id", xorEncrypt(String.valueOf(time))).apply();
    }

    private long getSecureTime() {
        String saved = prefs.getString("sys_cache_sync_id", "");
        if (saved.isEmpty()) return 0;
        try {
            return Long.parseLong(xorDecrypt(saved));
        } catch (Exception e) {
            return 0; 
        }
    }
}
