package com.slipkprojects.sockshttp.custom;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import com.maxtunnel.vip.R;
import org.json.JSONException;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONObject;
import android.widget.LinearLayout;

public class ServerDialog
{
	public static class Server
	{
		private static EditText sName,sFlag, sHost,rHost, sPort, sslPort, rPort, user, pass, sfrep, sinfo,chavekey, snameserver, smess;
		
		private MaterialAlertDialogBuilder a;

		private Context c;

		private SharedPreferences sp;
		
		private String auto;
		
		public Server(Context c)
		{
			a=new MaterialAlertDialogBuilder(c);
           // MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(c);
			sp=PreferenceManager.getDefaultSharedPreferences(c);
			this.c=c;
		}
		public void add()
		{
			View v=LayoutInflater.from(c).inflate(R.layout.dialog_add_server, null);
			sName = v.findViewById(R.id.sName);
			sFlag = v.findViewById(R.id.sFlag);
			sHost = v.findViewById(R.id.sHost);
            rHost = v.findViewById(R.id.rHost);
			sPort = v.findViewById(R.id.sPort);
			sslPort = v.findViewById(R.id.sslPort);
	
			
			rPort = v.findViewById(R.id.rPort);
			
			user = v.findViewById(R.id.user);
			pass = v.findViewById(R.id.pass);
			sinfo = v.findViewById(R.id.sInfo);
			smess= v.findViewById(R.id.smess);
			sfrep = v.findViewById(R.id.Sfrep);
			chavekey = v.findViewById(R.id.chavekey);
			snameserver = v.findViewById(R.id.snameserver);
		
            
			a.setView(v);
		}
		public void edit(JSONObject json)
		{
			View v=LayoutInflater.from(c).inflate(R.layout.dialog_add_server, null);
			sName = v.findViewById(R.id.sName);
			sFlag = v.findViewById(R.id.sFlag);
			sHost = v.findViewById(R.id.sHost);
			sPort = v.findViewById(R.id.sPort);
            rHost = v.findViewById(R.id.rHost);
			sslPort = v.findViewById(R.id.sslPort);
			rPort = v.findViewById(R.id.rPort);
			user = v.findViewById(R.id.user);
			pass = v.findViewById(R.id.pass);
			sinfo = v.findViewById(R.id.sInfo);
			smess= v.findViewById(R.id.smess);
			sfrep = v.findViewById(R.id.Sfrep);
			chavekey = v.findViewById(R.id.chavekey);
			snameserver = v.findViewById(R.id.snameserver);
			
			try {
				sName.setText(json.getString("Name"));
				sFlag.setText(sp.getString("FLAG", ""));
				sHost.setText(json.getString("ServerIP"));
                rHost.setText(json.getString("ProxyIP"));
				sPort.setText(json.getString("ServerPort"));
				sslPort.setText(json.getString("SSLPort"));
				rPort.setText(json.getString("ProxyPort"));
				user.setText(json.getString("ServerUser"));
				pass.setText(json.getString("ServerPass"));
				sinfo.setText(json.getString("sInfo"));
				chavekey.setText(json.getString("Slowchave"));
				snameserver.setText(json.getString("Nameserver"));
				smess.setText(json.getString("servermessage"));
				sfrep.setText(json.getString("Sfrep"));
				

			} catch (JSONException e) {}
			a.setView(v);
		}
		public void onServerAdd(final SpinnerListener oca)
		{
			a.setNegativeButton("Close",null);
			a.setPositiveButton("Save",new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface p1, int p2)
					{
						JSONObject jo=new JSONObject();

						try
						{
							jo.put("Name", sName.getText().toString());
							jo.put("FLAG", sFlag.getText().toString() + ".png");
							
							jo.put("ServerIP", sHost.getText().toString());
                            jo.put("ProxyIP", rHost.getText().toString());
							jo.put("ServerPort", sPort.getText().toString());
							jo.put("SSLPort", sslPort.getText().toString());
							jo.put("ProxyPort", rPort.getText().toString());
							jo.put("ServerUser", user.getText().toString());
							jo.put("ServerPass", pass.getText().toString());
							jo.put("sInfo", "Custom Server");
							jo.put("Slowchave", chavekey.getText().toString());
							jo.put("Nameserver", snameserver.getText().toString());
							jo.put("servermessage", smess.getText().toString());
							jo.put("Sfrep", sfrep.getText().toString());
							
							sp.edit().putString("Name", sName.getText().toString()).apply();
							sp.edit().putString("FLAG", sFlag.getText().toString()).apply();
							sp.edit().putString("ServerIP", sHost.getText().toString()).apply();
                            sp.edit().putString("ProxyIP", rHost.getText().toString()).apply();
							sp.edit().putString("ServerPort", sPort.getText().toString()).apply();
							sp.edit().putString("SSLPort", sslPort.getText().toString()).apply();
							sp.edit().putString("ProxyPort", rPort.getText().toString()).apply();
							sp.edit().putString("ServerUser", user.getText().toString()).apply();
							sp.edit().putString("ServerPass", pass.getText().toString()).apply();
				    		sp.edit().putString("sInfo", "Custom Server").apply();
							sp.edit().putString("isAuto", auto).apply();
							sp.edit().putString("Slowchave", chavekey.getText().toString()).apply();
							sp.edit().putString("Nameserver", snameserver.getText().toString()).apply();
							sp.edit().putString("servermessage", smess.getText().toString()).apply();
							sp.edit().putString("Sfrep", sfrep.getText().toString()).apply();
							oca.onAdd(jo);
                            Toast.makeText(c, "Added "+sName.getText().toString(), Toast.LENGTH_LONG).show();

						}
						catch (JSONException e)
						{
							Toast.makeText(c,e.getMessage(),1).show();
						}
					}
				});
		}
		public void init()
		{
			a.create().show();
		}
	}
}