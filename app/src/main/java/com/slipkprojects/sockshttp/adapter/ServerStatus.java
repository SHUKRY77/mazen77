package com.slipkprojects.sockshttp.adapter;

import android.content.Context;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.maxtunnel.vip.R;
import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import com.slipkprojects.ultrasshservice.tunnel.vpn.Pinger2;
import java.io.InputStream;
import android.graphics.drawable.Drawable;
import java.io.IOException;

public class ServerStatus extends RecyclerView.Adapter<ServerStatus.MyViewHolder> {

    private ArrayList<HashMap<String, String>> data;
    private HashMap<String, String> resultp = new HashMap<String, String>();
    private Activity ctx;
    Boolean pingTestStarted = false;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView name,pingTextView;
        public TextView ss;

        public MyViewHolder(View view) {
            super(view);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            
            pingTextView = itemView.findViewById(R.id.ping);
            ss = itemView.findViewById(R.id.isol);
        }
    }


    public ServerStatus(Activity c, ArrayList<HashMap<String, String>> arraylist) {
        data = arraylist;
        ctx = c;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.layout_spinner_row1, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        resultp = data.get(position);      
        String str = "FLAG";
        
        try {
            InputStream inputStream = this.ctx.getAssets().open("flags/" + resultp.get("FLAG").concat(".png"));
            holder.icon.setImageDrawable(Drawable.createFromStream(inputStream, resultp.get("FLAG").concat(".png")));      
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
        }
        holder.name.setText(resultp.get("NAME"));
        holder.ss.setText("loading...");
        String nimal = resultp.get("HOST");
        String yawa = resultp.get("PORT");
        initS(holder, nimal.equals("") ? "127.0.0.1" : nimal, yawa.equals("") ? "2222" :yawa);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void initS(final MyViewHolder holder, String host, String port){

        new Pinger2(new Pinger2.OnPingListener(){
            
            @Override
				public void ms(final int i) {
					ctx.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
								if(i<100){
									holder.pingTextView.setTextColor(Color.parseColor("#04D000"));
								} else {
									holder.pingTextView.setTextColor(Color.parseColor("#FD1C0D"));
								}
								holder.pingTextView.setText(i+"ms");
                            }
                        });
					// TODO: Implement this method
				}

                @Override
                public void accept(final Boolean isOnline) {
                    ctx.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                if(isOnline){
                                    holder.ss.setText("online");
                                    holder.ss.setTextColor(Color.parseColor("#04D000"));
                                } else {
                                    holder.ss.setText("offline");
                                    holder.ss.setTextColor(Color.parseColor("#FD1C0D"));
                                }
                            }
                        });

                }
            }, host, port , 3000);
    }
}




