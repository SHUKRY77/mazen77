package app.ovpnudp.developer;

import android.app.*;
import android.view.*;
import app.sylnetdev.jhinuk.vpn.pro.R;

import android.widget.*;

import app.ovpnudp.developer.activities.*;


public class ServerSelectDialog
{
	private ListView mListView;
	private View v;
	private OpenVPNClient context;
	private AlertDialog mDialog;

	
	public ServerSelectDialog(final OpenVPNClient context)
	{
		this.context = context;
		v = LayoutInflater.from(context).inflate(R.layout.server_dialog, null);
		mDialog = new AlertDialog.Builder(context).create();
		mDialog.setView(v);

		
		mListView = (ListView)findViewById(R.id.server_list_dialog);
		mListView.setAdapter(context.mServerAdapter);
		mListView.postDelayed(new Runnable() {

				@Override
				public void run()
				{
					mListView.smoothScrollToPosition(context.mServerAdapter.getServerPosition());
					context.mServerAdapter.notifyDataSetChanged();
					// TODO: Implement this method
				}


			}, 500);
		
	}
	public void show()
	{
		mDialog.show();
	}
	private View findViewById(int id)
	{
		// TODO: Implement this method
		return v.findViewById(id);
	}
	public void closeDialog()
	{
		mDialog.dismiss();
	}
}
