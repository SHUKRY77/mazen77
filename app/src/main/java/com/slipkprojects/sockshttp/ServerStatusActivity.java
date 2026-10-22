package com.slipkprojects.sockshttp;

import android.app.Activity;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import android.content.Context;
import com.maxtunnel.vip.R;
import java.io.IOException;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.slipkprojects.sockshttp.activities.BaseActivity;
import com.slipkprojects.sockshttp.util.ConfigUtil;
import androidx.appcompat.widget.Toolbar;
import com.slipkprojects.sockshttp.adapter.ServerStatus;                            
   
//    KAYO NA PO BAHALA DITO.  //
//    EXPLORE NYO NALANG PO.   //
 
public class ServerStatusActivity extends BaseActivity { 

    private ArrayList<HashMap<String, String>> mData = new ArrayList<HashMap<String, String>>();
	private ConfigUtil config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		
		this.config = new ConfigUtil(this);
            try {
				for (int i=0;i < config.getServersArray().length();i++) {         
			    JSONObject obj = config.getServersArray().getJSONObject(i);
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("NAME", obj.getString("Name"));
                map.put("FLAG", obj.getString("FLAG"));
				map.put("HOST", obj.getString("ServerIP"));
                map.put("PORT", obj.getString("ServerPort"));
			    mData.add(map); 
				}
            }catch (JSONException e) {
                e.printStackTrace();
            }
        
        LinearLayoutManager layoutManagerz = new LinearLayoutManager(this);
        ServerStatus adapter = new ServerStatus(this, mData); 
        RecyclerView recyclerView = findViewById(R.id.rv);        
        recyclerView.setAdapter(adapter);       
        recyclerView.setLayoutManager(layoutManagerz);    
    }
    @Override
	public boolean onSupportNavigateUp()
	{
		onBackPressed();
		return true;
	}
} 
