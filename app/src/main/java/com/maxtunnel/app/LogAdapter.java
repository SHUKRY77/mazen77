package com.maxtunnel.app;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private List<String> logList = new ArrayList<>();
    // تحديد شكل الوقت ليظهر هكذا: [14:30:05]
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    // دالة إضافة سطر جديد للسجلات
    public void addLog(String message) {
        String currentTime = timeFormat.format(new Date());
        logList.add("[" + currentTime + "] " + message);
        notifyItemInserted(logList.size() - 1);
    }

    // دالة لمسح السجلات (تعمل عند الضغط على Clear logs)
    public void clearLogs() {
        logList.clear();
        notifyDataSetChanged();
    }

    // 🌟 دالة تجميع كل السجلات في نص واحد (تعمل عند الضغط على Copy logs)
    public String getAllLogsAsString() {
        StringBuilder sb = new StringBuilder();
        for (String log : logList) {
            sb.append(log).append("\n");
        }
        return sb.toString();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // استدعاء تصميم السطر الواحد الذي أنشأناه سابقاً
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        String fullLog = logList.get(position);
        
        // البحث عن القوس المعكوس ] لتحديد نهاية الوقت
        int endBracketIndex = fullLog.indexOf("]");
        
        if (endBracketIndex != -1) {
            // استخدام SpannableString لتلوين جزء معين من النص
            SpannableString spannable = new SpannableString(fullLog);
            // تلوين الوقت باللون الأزرق الفاتح الجميل
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#2196F3")), 0, endBracketIndex + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.logTextView.setText(spannable);
        } else {
            // إذا لم يكن هناك قوس (نص عادي) يظهر باللون الرمادي الافتراضي
            holder.logTextView.setText(fullLog);
        }
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    // كلاس لربط عنصر النص داخل ملف item_log.xml
    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView logTextView;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            logTextView = itemView.findViewById(R.id.log_text_view);
        }
    }
}
