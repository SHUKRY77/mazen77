package com.slipkprojects.ultrasshservice.tunnel.vpn;

import android.os.AsyncTask;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.net.UnknownHostException;

public class Pinger2 extends AsyncTask<Void,Void,Boolean> {

    private OnPingListener mListener;
    private String mHost;
    private int mTimeOut;
    private String mPort;
    Integer latency = 999;

    public interface OnPingListener {
        void accept(Boolean isOnline);
        void ms(int isOnline);
    }

    public Pinger2(OnPingListener Listener, String host, String port ,int timeout) {
        mListener = Listener; 
        mHost = host;
        mPort = port;
        mTimeOut = timeout;
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            long date = new Date().getTime();
            Socket sock = new Socket();
            sock.connect(new InetSocketAddress(mHost, Integer.parseInt(mPort)), mTimeOut);
            latency = (int) (new Date().getTime() - date);//this code is base from github 
            sock.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean internet) { 
        mListener.accept(internet);
        mListener.ms(latency);
    }
}