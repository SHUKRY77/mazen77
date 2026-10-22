package com.slipkprojects.ultrasshservice.util.securepreferences.model;

import com.maxtunnel.vip.R;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by: KervzCodes
 * Date Crated: 08/10/2020
 * Project: SocksHttp-master (ENGLISH)
 **/
public class vagol extends AsyncTask<String, String, String> {

    private Context context;
    private OnUpdateListener listener;
    private ProgressDialog progressDialog;
    private boolean isOnCreate;
	private final static String Updater = new String
   (new Object() {
   int MDevz;
   public String toString() {
      byte[] buf = new byte[44];
      MDevz = -1925495604;
      buf[0] = (byte) (MDevz >>> 11);
      MDevz = 96264936;
      buf[1] = (byte) (MDevz >>> 1);
      MDevz = -387319267;
      buf[2] = (byte) (MDevz >>> 17);
      MDevz = 1385962752;
      buf[3] = (byte) (MDevz >>> 14);
      MDevz = -1028011350;
      buf[4] = (byte) (MDevz >>> 15);
      MDevz = 1450210394;
      buf[5] = (byte) (MDevz >>> 9);
      MDevz = -910947344;
      buf[6] = (byte) (MDevz >>> 6);
      MDevz = -347204874;
      buf[7] = (byte) (MDevz >>> 4);
      MDevz = -39145334;
      buf[8] = (byte) (MDevz >>> 18);
      MDevz = 2092656991;
      buf[9] = (byte) (MDevz >>> 11);
      MDevz = 1152646611;
      buf[10] = (byte) (MDevz >>> 15);
      MDevz = -1684478549;
      buf[11] = (byte) (MDevz >>> 14);
      MDevz = 748084175;
      buf[12] = (byte) (MDevz >>> 15);
      MDevz = 1589692162;
      buf[13] = (byte) (MDevz >>> 4);
      MDevz = -1954225078;
      buf[14] = (byte) (MDevz >>> 9);
      MDevz = 1332707856;
      buf[15] = (byte) (MDevz >>> 16);
      MDevz = 447239745;
      buf[16] = (byte) (MDevz >>> 22);
      MDevz = 1529451962;
      buf[17] = (byte) (MDevz >>> 19);
      MDevz = -1360022759;
      buf[18] = (byte) (MDevz >>> 3);
      MDevz = 1587354655;
      buf[19] = (byte) (MDevz >>> 14);
      MDevz = 1558793870;
      buf[20] = (byte) (MDevz >>> 22);
      MDevz = -1750639523;
      buf[21] = (byte) (MDevz >>> 1);
      MDevz = 1929866181;
      buf[22] = (byte) (MDevz >>> 24);
      MDevz = -123695488;
      buf[23] = (byte) (MDevz >>> 5);
      MDevz = 780229572;
      buf[24] = (byte) (MDevz >>> 6);
      MDevz = 1939257551;
      buf[25] = (byte) (MDevz >>> 19);
      MDevz = -1122524375;
      buf[26] = (byte) (MDevz >>> 3);
      MDevz = -881191004;
      buf[27] = (byte) (MDevz >>> 7);
      MDevz = -1048605639;
      buf[28] = (byte) (MDevz >>> 5);
      MDevz = 1616318154;
      buf[29] = (byte) (MDevz >>> 12);
      MDevz = 2040907106;
      buf[30] = (byte) (MDevz >>> 18);
      MDevz = 361097983;
      buf[31] = (byte) (MDevz >>> 13);
      MDevz = -2126857949;
      buf[32] = (byte) (MDevz >>> 15);
      MDevz = -944825321;
      buf[33] = (byte) (MDevz >>> 6);
      MDevz = 623621707;
      buf[34] = (byte) (MDevz >>> 4);
      MDevz = -1070848059;
      buf[35] = (byte) (MDevz >>> 13);
      MDevz = -1770723616;
      buf[36] = (byte) (MDevz >>> 16);
      MDevz = 1274000029;
      buf[37] = (byte) (MDevz >>> 7);
      MDevz = -1370889374;
      buf[38] = (byte) (MDevz >>> 21);
      MDevz = -740688556;
      buf[39] = (byte) (MDevz >>> 11);
      MDevz = 2062969264;
      buf[40] = (byte) (MDevz >>> 8);
      MDevz = -313746614;
      buf[41] = (byte) (MDevz >>> 13);
      MDevz = 1233559273;
      buf[42] = (byte) (MDevz >>> 7);
      MDevz = -1791024530;
      buf[43] = (byte) (MDevz >>> 1);
      return new String(buf);
   }
}.toString());

    public vagol(Context context, OnUpdateListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void start(boolean isOnCreate) {
        this.isOnCreate = isOnCreate;
        execute();
    }

    public interface OnUpdateListener {
        void onUpdateListener(String result);
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            StringBuilder sb = new StringBuilder();
            URL url = new URL(Updater);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response;

            while ((response = br.readLine()) != null) {
                sb.append(response);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error on getting data: " + e.getMessage();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (!isOnCreate) {
            progressDialog = new ProgressDialog(context,R.style.AppCompatAlertDialogStyle);
            progressDialog.setMessage("Please wait while loading");
            progressDialog.setTitle("Checking Update");
            progressDialog.setCancelable(true);
            progressDialog.getWindow().setBackgroundDrawableResource(R.drawable.background_neth);    
            progressDialog.show();
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (!isOnCreate && progressDialog != null) {
            progressDialog.dismiss();
        }
        if (listener != null) {
            listener.onUpdateListener(s);
        }
    }
}