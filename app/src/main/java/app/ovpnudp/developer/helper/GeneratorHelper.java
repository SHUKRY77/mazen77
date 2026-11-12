package app.ovpnudp.developer.helper;

import android.content.*;
import androidx.appcompat.app.*;

import app.ovpnudp.developer.view.*;

public class GeneratorHelper implements RenzGenerator.GeneratorListener
{

	@Override
	public void onGeneratePayload(String payload)
	{
		listener.onGenerate(payload);
		// TODO: Implement this method
	}

	@Override
	public void onGeneratorClose()
	{
		listener.onCancel();
		// TODO: Implement this method
	}
	
	public interface GeneratorListener
	{
		void onCancel();
		void onGenerate(String payload);
	}
	private Context context;
	private GeneratorListener listener;
	private AlertDialog ab;
	public GeneratorHelper(Context context)
	{
		this.context = context;
	}
	public void setCancelListener(GeneratorListener GeneratorListener)
	{
		listener = GeneratorListener;
	}

	public void show()
	{
		RenzGenerator gen = new RenzGenerator(context);
		gen.setGeneratorListener(this);
		gen.show();
	}
}
