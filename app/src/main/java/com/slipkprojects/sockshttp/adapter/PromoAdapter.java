package com.slipkprojects.sockshttp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.maxtunnel.vip.R;
import java.util.ArrayList;
import org.json.JSONObject;
import android.widget.LinearLayout;
import android.view.animation.*;

public class PromoAdapter extends ArrayAdapter<JSONObject> {

    private int spinner_id;

	private ImageView im;

    public PromoAdapter(Context context, int spinner_id, ArrayList<JSONObject> list) {
        super(context, R.layout.promo_items, list);
        this.spinner_id = spinner_id;
    }

    @Override
    public JSONObject getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return view(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return view(position, convertView, parent);
    }

    private View view(int position, View convertView, ViewGroup parent) {

		
		View v = LayoutInflater.from(getContext()).inflate(R.layout.promo_items, parent, false);
		TextView tv = v.findViewById(R.id.itemName);
        TextView pInfos = v.findViewById(R.id.payload_info);
		ImageView im = v.findViewById(R.id.pImages);
		LinearLayout promo_item_layout = v.findViewById(R.id.promo_item_layout);

        try {
			String name = getItem(position).getString("Name");
            tv. setText(name);


			if (spinner_id == R.id.payloadSpinner) {
				getPayloadIcon(position, im, pInfos);
				pInfos.setText(getItem(position).getString("pInfo"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }


    private void getPayloadIcon(int position, ImageView im, TextView pInfos) throws Exception {
        String name = getItem(position).getString("Name").toLowerCase();


        if (name.contains("globe")) {
            im.setImageResource(R.drawable.ic_globe);
        } else if (name.contains("smart")) {
            im.setImageResource(R.drawable.ic_smart);
        } else if (name.contains("gtm")) {
            im.setImageResource(R.drawable.ic_gtm);
        } else if (name.contains("tm")) {
            im.setImageResource(R.drawable.ic_tm);
        } else if (name.contains("tnt")) {
            im.setImageResource(R.drawable.ic_tnt);
        }else if(name.contains("sun")) {
            im.setImageResource(R.drawable.ic_sun);
        }else if(name.contains("dito")) {
            im.setImageResource(R.drawable.ic_dito);
        }else if(name.contains("pisowifi")) {
            im.setImageResource(R.drawable.ic_pisowifi);
        }else if(name.contains("sts")) {
            im.setImageResource(R.drawable.ic_sts);       
		}else if(name.contains("gomo")) {
            im.setImageResource(R.drawable.ic_gomo);
        }else if(name.contains("")) {
            im.setImageResource(R.drawable.icon_speed);
        }

    }


}
