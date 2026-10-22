package com.slipkprojects.ultrasshservice.tunnel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class UDPStreamGobbler extends Thread {
    private boolean isInterrupted;
    private final OnResultListener listener;
    private final BufferedReader reader;

    public void setInterrupted(boolean z) {
        this.isInterrupted = z;
    }

    public UDPStreamGobbler(InputStream inputStream, OnResultListener onResultListener) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.listener = onResultListener;
    }

    public interface OnResultListener {
        void onResult(String result);
    }
    /* JADX WARNING: Removed duplicated region for block: B:0:0x0000 A:{LOOP_START, SYNTHETIC, Splitter:B:0:0x0000, LOOP:0: B:0:0x0000->B:14:0x0000} */
    /* JADX WARNING: Missing exception handler attribute for start block: B:0:0x0000 */
    /* JADX WARNING: Can't wrap try/catch for region: R(5:4|(4:9|10|(3:12|13|17)(1:16)|14)(0)|6|0|1) */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        while (!this.isInterrupted) {
            try {
                String readLine = this.reader.readLine();
                if (readLine != null) {
                    OnResultListener onResultListener = this.listener;
                    if (onResultListener != null) {
                        onResultListener.onResult(readLine);
                    }
                }
            } catch (IOException e) {}
        }
        try {
            this.reader.close();
        } catch (IOException unused) {
        }
    }
}
