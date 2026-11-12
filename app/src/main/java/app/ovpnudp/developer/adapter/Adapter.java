package app.ovpnudp.developer.adapter;
import android.content.*;
import android.widget.*;

import java.io.InputStream;
import java.util.*;
import android.view.*;
import app.ovpnudp.developer.service.OpenVPNService.*;

import android.text.*;
import org.json.*;
import app.sylnetdev.jhinuk.vpn.pro.R;

import android.view.animation.*;
import android.preference.*;
import android.graphics.*;
import android.graphics.drawable.*;

public class Adapter
{
	public static class DrawerListAdapter extends ArrayAdapter<String>
	{
		public DrawerListAdapter(Context context, ArrayList<String> names)
		{
			super (context, R.layout.drawer_list_item, names);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View v = LayoutInflater.from(getContext()).inflate(R.layout.drawer_list_item, parent, false);
			TextView tv = (TextView)v.findViewById(R.id.drawer_list_item_name);
			tv.setText(getItem(position));
			// TODO: Implement this method
			return v;
		}
	
	}
    public static class LogAdapter extends ArrayAdapter<LogMsg>
    {
        public LogAdapter(Context context, ArrayList<LogMsg> listLog)
        {
            super(context, R.layout.log_item, listLog);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.log_item, parent, false);
            TextView tv = (TextView)v.findViewById(R.id.log_item);
            LogMsg lm = getItem(position);
            tv.setText(Html.fromHtml(lm.line));
            // TODO: Implement this method
            return v;
        }
    }
    public static class ServerAdapter extends ArrayAdapter<JSONObject>
    {

        private ArrayList<JSONObject> listServer;

        private Context context;

        private SharedPreferences.Editor editor;

        private SharedPreferences pref;

        private Adapter.ServerAdapter.OnServerSelectedListener OnServerSelectedListener;

        private int last_checked = 0;
        //private TextView category;

        public interface OnServerSelectedListener
        {
            void onServerSelected(int i);
        }
        public void setServerSelectedListener(OnServerSelectedListener OnServerSelectedListener)
        {
            this.OnServerSelectedListener = OnServerSelectedListener;
        }
        public ServerAdapter(Context context, ArrayList<JSONObject> listServer)
        {
            super(context, R.layout.server_item, listServer);
            this.listServer = listServer;
            this.context = context;
            pref = PreferenceManager.getDefaultSharedPreferences(context);
            editor = pref.edit();
        }
        public int getServerPosition()
        {

            // TODO: Implement this method
            return pref.getInt("ServerChecked", 0);
        }
        public JSONObject getItem(int position)
        {
            // TODO: Implement this method
            return listServer.get(position);
        }


        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.animator.jump_d);

            View v = MyView(position, convertView, parent);
          //  v.startAnimation(anim);
            return v;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            // TODO: Implement this method
            return MyView(position, convertView, parent);
        }
        public View MyView(final int position, View convertView, ViewGroup parent)
        {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.server_item, parent, false);
            TextView tv = (TextView)v.findViewById(R.id.server_item_title);
            ImageView iv = (ImageView)v.findViewById(R.id.server_item_icon);
            TextView tv2 = (TextView)v.findViewById(R.id.server_description);

            last_checked = pref.getInt("ServerChecked", 0);





            try {
                JSONObject js = getItem(position);

                tv.setText(js.getString("Name"));
                setFlag(iv,js.getString("Flag"));

                if (position == 0) {
                    tv2.setText("Random");
                    tv2.setTextColor(Color.BLUE);
                } else {
                    tv2.setText("Ultimate");
                    tv2.setTextColor(Color.RED);
                }


            } catch (Exception e) {

            }
            // TODO: Implement this method
            return v;
        }
        public void setFlag(ImageView f, String ff){
            try {
                InputStream open = context.getAssets().open(new StringBuffer().append("flag/").append(ff).toString());
                f.setImageDrawable(Drawable.createFromStream(open, (String) null));
                if (open != null) {
                    open.close();
                }
            } catch(Exception e) {
                f.setImageResource(R.drawable.ic_app_icon);
            }
        }

        private void setIcon(ImageView iv, int icon)
        {
            iv.setImageResource(icon);
        }
    }

    public static class NetworkAdapter extends ArrayAdapter<JSONObject>
    {

        private List<JSONObject> listNetwork;
        private String last_checked_network = "";
        private Adapter.NetworkAdapter.OnNetworkSelectedListener OnNetworkSelectedListener;

        private SharedPreferences.Editor editor;

        private SharedPreferences pref;

        
        public interface OnNetworkSelectedListener
        {
            void onNetworkSelected(JSONObject network);
        }
        public void setNetworkSelectedListener(OnNetworkSelectedListener OnNetworkSelectedListener)
        {
            this.OnNetworkSelectedListener = OnNetworkSelectedListener;
        }
        public NetworkAdapter(Context context, List<JSONObject> listNetwork)
        {
            super(context, R.layout.network_item, listNetwork);
            this.listNetwork = listNetwork;
            pref = PreferenceManager.getDefaultSharedPreferences(context);
            editor = pref.edit();
        }
        public int getNetworkPosition()
        {
            try {
                for (int i = 0; i < listNetwork.size(); i++) {
                    JSONObject json = listNetwork.get(i);
                    if (json.getString("Name").equals(pref.getString("LastCheckedNetwork", ""))) {
                        return i;
                    }
                }
            } catch (Exception e) {
                
            }
            // TODO: Implement this method
            return 0;
        }
        @Override
        public JSONObject getItem(int position)
        {
            // TODO: Implement this method
            return listNetwork.get(position);
        }

        @Override
        public int getCount()
        {
            // TODO: Implement this method
            return listNetwork.size();
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {
            // TODO: Implement this method
            return MyView(position, convertView, parent);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            // TODO: Implement this method
            return MyView(position, convertView, parent);
        }
        public View MyView(final int position, View convertView, final ViewGroup parent)
        {
            
            View v = LayoutInflater.from(getContext()).inflate(R.layout.network_item, parent, false);
            TextView tv = (TextView)v.findViewById(R.id.network_item_title);
            TextView mInfo = (TextView)v.findViewById(R.id.network_item_info);
            ImageView iv = (ImageView)v.findViewById(R.id.network_item_icon);
            

            last_checked_network = pref.getString("LastCheckedNetwork", "");

            JSONObject js = getItem(position);
            
            
            try
            {
                String info = js.getString("Info");
                if (info.isEmpty()) {
                    mInfo.setVisibility(View.GONE);
                }
                mInfo.setText(info);
                String name = js.getString("Name");
                tv.setText(Html.fromHtml(name));
                name = name.toLowerCase();
                //tunnel_title.setText(js.getInt("TunnelType") == 0 ? "SSH/INJECT" : "SSH/SSL");
                setIcon(iv, getIcon(name));
            } catch (Exception e) {
                Toast.makeText(getContext(), "Network Adapter" + e.getMessage(), 1).show();
            }

            // TODO: Implement this method
            return v;
        }
		private int getIcon(String name)
		{
			if (name.contains("gtm")) {
				return(R.drawable.ic_globe);
			} else if (name.contains("omantel")) {
				return(R.drawable.ic_omantel);
			} else if (name.contains("gp")) {
				return(R.drawable.gphone);
			} else if (name.contains("lebara")) {
				return(R.drawable.ic_lebara);
			} else if (name.contains("tnt")) {
				return(R.drawable.ic_tnt);
			} else if (name.contains("vargin")) {
				return(R.drawable.ic_vargin);
			} else if (name.contains("facebook")) {
				return(R.drawable.ic_facebook);
			} else if (name.contains("google")) {
				return(R.drawable.ic_google);
			} else if (name.contains("youtube")) {
				return(R.drawable.ic_youtube);
			} else if (name.contains("instagram")) {
				return(R.drawable.ic_instagram);
			} else if (name.contains("iflix")) {
				return(R.drawable.ic_iflix);
			} else if (name.contains("snapchat")) {
				return(R.drawable.ic_snapchat);
			} else if (name.contains("twitter")) {
				return(R.drawable.ic_twitter);
			} else if (name.contains("neflix")) {
				return(R.drawable.ic_netflix);
			} else if (name.contains("mobile legends")) {
				return(R.drawable.ic_ml);
			} else if (name.contains("du")) {
				return(R.drawable.ic_du);
			} else if(name.contains("etisalat")) {
				return(R.drawable.ic_eti);
			} else if (name.contains("wifi")) {
				return(R.drawable.ic_wifi);
			} else if (name.contains("whatsapp")) {
				return(R.drawable.ic_whatsapp);
			} else if (name.contains("tiktok")) {
				return(R.drawable.ic_tiktok);
			} else if (name.contains("viber")) {
				return(R.drawable.ic_viber);
			} else if(name.contains("airtel")) {
				return(R.drawable.ic_airtel);
			} else if(name.contains("jawwy")) {
				return(R.drawable.ic_jawwy);
			} else if(name.contains("digi")) {
				return(R.drawable.ic_digi);
			}  else if(name.contains("airtel")) {
				return(R.drawable.ic_airtel);
			}  else if(name.contains("pubg")) {
				return(R.drawable.ic_pubg);
			}  else if(name.contains("playstore")) {
				return(R.drawable.ic_playstore);
			}  else if(name.contains("skype")) {
				return(R.drawable.ic_skype);
			}  else if(name.contains("telegram")) {
				return(R.drawable.ic_telegram);
			}  else if(name.contains("vivobee")) {
				return(R.drawable.ic_vivobee);
			}   else if(name.contains("ooredoo")) {
				if (!name.contains("free")) {
					return(R.drawable.ic_ooredoo);
				}
				return(R.drawable.ic_ooredoo_free);
			}   else if(name.contains("viva")) {
				return(R.drawable.ic_viva);
			}  else if(name.contains("progresif")) {
				return(R.drawable.ic_progresif);
			}  else if(name.contains("jio")) {
				return(R.drawable.ic_jio);
			}  else if(name.contains("flexi")) {
				return(R.drawable.ic_flexi);
			}  else if(name.contains("vodaphone")) {
				return(R.drawable.ic_vodafone);
			}  else if(name.contains("mobily")) {
				if (name.contains("free")) {
					return(R.drawable.ic_mobily_free);
				}
				return(R.drawable.ic_mobily);
			} else if(name.contains("zain")) {
				if (name.contains("free")) {
					return(R.drawable.ic_zain_free);
				}
				return(R.drawable.ic_zain);
			} else if(name.contains("banglalink")) {
				return(R.drawable.ic_banglalink);
			}  else if(name.contains("dhiraagu")) {
				return(R.drawable.ic_dhiraagu);
			}  else if(name.contains("dst")) {
				return(R.drawable.ic_dst);
			}  else if(name.contains("friendi")) {
				return(R.drawable.ic_friendi);
			}  else if(name.contains("grameen")) {
				return(R.drawable.ic_grameenphone);
			}  else if(name.contains("imagine")) {
				return(R.drawable.ic_imagine);
			}  else if(name.contains("kuwait zain")) {
				return(R.drawable.ic_kuwait_zain);
			}  else if(name.contains("lebera")) {
				return(R.drawable.ic_lebara);
			}  else if(name.contains("omantel")) {
				return(R.drawable.ic_omantel);
			}  else if(name.contains("progresif")) {
				return(R.drawable.ic_progresif);
			}  else if(name.contains("vodafone")) {
				return(R.drawable.ic_vodafone);
			}  else if(name.contains("robi")) {
				return(R.drawable.ic_robi);
			}  else if(name.contains("singtel")) {
				return(R.drawable.ic_singtel);
			}  else if(name.contains("stc")) {
				if (name.contains("free")) {
					return(R.drawable.ic_stc_free);
				}
				return(R.drawable.ic_stc);
			}  else if(name.contains("vargin")) {
				return(R.drawable.ic_vargin);
			}  else if(name.contains("starhub")) {
				return(R.drawable.starhub);
			}  else {
				return(R.drawable.ic_app_icon);
			}
			// TODO: Implement this method
			//return 0;
		}
        
        public void setIcon(ImageView iv, int icon)
        {
            iv.setImageResource(icon);
        }
    }
}
