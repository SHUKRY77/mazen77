package app.ovpnudp.developer.fragment;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import app.sylnetdev.jhinuk.vpn.pro.R;
import app.ovpnudp.developer.activities.OpenVPNApplication;
import app.ovpnudp.developer.interfaces.OnByteCountListener;
import app.ovpnudp.developer.utils.StatisticsGraphData;

/**
 * Created by arne on 19.05.17.
 */

public class GraphFragment extends Fragment implements OnByteCountListener
{
	private int mColourIn;
	//private int mColourOut;
	private int mColourPoint;
	private Handler mHandler;
	private LineChart chart;
	private Thread mGraphThread;
	private boolean isRunning = false;
	private TextView mStatsDuration;
	private TextView mBytesSent;
	private TextView mBytesRecv;
	private StatisticsGraphData.DataTransferStats mGraphStats;

	private Timer timer;

	@Override
	public void onByteCount(long bytes_in, long bytes_out)
	{
		mGraphStats.addBytesReceived(bytes_in);
		mGraphStats.addBytesSent(bytes_out);
		// TODO: Implement this method
	}
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
	{
		mGraphStats = StatisticsGraphData.getStatisticData().getDataTransferStats();

		View v = inflater.inflate(R.layout.graph, container, false);
		OpenVPNApplication.setByteCountListener(this);

		chart = (LineChart)v.findViewById(R.id.chart);
		mStatsDuration = (TextView)v.findViewById(R.id.stats_duration);
		mBytesSent = (TextView)v.findViewById(R.id.sent_bytes);
		mBytesRecv = (TextView)v.findViewById(R.id.recv_bytes);

		mColourIn = getActivity().getResources().getColor(R.color.graph_color);
		//mColourOut = getActivity().getResources().getColor(R.color.dataOut);
		mColourPoint = getActivity().getResources().getColor(android.R.color.white);

		chart.getDescription().setEnabled(false);
		chart.setBackgroundColor(Color.parseColor("#00000000"));
		chart.setDrawGridBackground(false);

		XAxis xAxis = chart.getXAxis();
		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setDrawGridLines(false);
		xAxis.setDrawAxisLine(true);
		xAxis.setTextColor(Color.BLACK);
		xAxis.setEnabled(false);
		xAxis.setDrawLabels(false);

		xAxis.setValueFormatter(new IAxisValueFormatter() {


			@Override
			public String getFormattedValue(float value, AxisBase axis) {
				return String.format(Locale.getDefault(), "%.0f\u2009s ago", (axis.getAxisMaximum() - value) / 10);

			}
		});
		xAxis.setLabelCount(5);

		YAxis yAxis = chart.getAxisLeft();
		yAxis.setLabelCount(10, true);
		yAxis.setTextColor(Color.BLACK);
		yAxis.setDrawGridLines(false);
		yAxis.setDrawAxisLine(false);
		yAxis.enableGridDashedLine(5.0f,5.0f,1.0f);
		yAxis.setAxisMaxValue(3072.0f);
		yAxis.setAxisMinValue(0.0f);
		//yAxis.setAxisLineColor(android.R.color.transparent);
		//final Resources res = getActivity().getResources();
		yAxis.setValueFormatter(new IAxisValueFormatter() {
			@Override
			public String getFormattedValue(float value, AxisBase axis)
			{
				return mGraphStats.byteCountNoDisplaySize((long) value, true);
			}
		});

		chart.getAxisRight().setEnabled(false);

		yAxis.setAxisMinimum(0f);
		//yAxis.resetAxisMaximum();
		yAxis.setLabelCount(10);
		chart.getViewPortHandler().setMaximumScaleX(5.0f);
		chart.getViewPortHandler().setMaximumScaleX(5.0f);
		chart.getDescription().setEnabled(false);
		chart.getLegend().setEnabled(false);
		chart.setNoDataText("No Graph Data");
		chart.invalidate();

		mHandler = new Handler();
		updateSshByteCount();
		return v;
	}

	@Override
	public void onResume() {
		//updateSshByteCount();
		super.onResume();
	}

	private void updateSshByteCount()
	{
		isRunning = true;
		mGraphThread = new Thread(new Runnable() {

			@Override
			public void run()
			{
				while (isRunning) {
					if (getActivity() == null) {
						return;
					}
					mGraphStats.addBytesReceived(mGraphStats.getBytesReceived());

					getActivity().runOnUiThread(new Runnable() {

						@Override
						public void run()
						{
							addEntry();
							// TODO: Implement this method
						}


					});
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {

					}
				}
				// TODO: Implement this method
			}


		});
		mGraphThread.start();

		timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run()
			{
				if (getActivity() == null) {
					return;
				}

				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run()
					{
						if (mGraphStats.isConnected()) {
							mBytesSent.setText(mGraphStats.byteCountToDisplaySize(mGraphStats.getBytesSent(), true));
							mBytesRecv.setText(mGraphStats.byteCountToDisplaySize(mGraphStats.getBytesReceived(), true));
							mStatsDuration.setText(mGraphStats.elapsedTimeToDisplay(mGraphStats.getElapsedTime()));
						}
						// TODO: Implement this method
					}


				});
				// TODO: Implement this method
			}


		}, 0, 1000);
		// TODO: Implement this method
	}
	private void addEntry()
	{
		LineData lineData = getSshDataSet(0);
		if (lineData.getDataSetByIndex(0).getEntryCount() < 3)
			chart.setData(null);
		else
			chart.setData(lineData);

		chart.invalidate();
	}
	private LineData getSshDataSet(int period)
	{
		List<Entry> dataIn = new ArrayList<>();
		//List<Entry> dataOut = new ArrayList<>();
		mGraphStats = StatisticsGraphData.getStatisticData().getDataTransferStats();
		ArrayList<Long>  listByteIn = mGraphStats.getFastReceivedSeries();
		//ArrayList<Long> listByteOut = mGraphStats.getFastSentSeries();


		for (int i = 0; i < listByteIn.size(); i++) {
			dataIn.add(new Entry(i, mGraphStats.render_byteCount(listByteIn.get(i), true)));
		}
        /*for (int i = 0; i < listByteOut.size(); i++) {
         dataOut.add(new Entry(i, listByteOut.get(i)));
         }*/

		List<ILineDataSet> dataSets = new ArrayList<>();


		LineDataSet indata = new LineDataSet(dataIn, "");
		//LineDataSet outdata = new LineDataSet(dataOut, getActivity().getString(R.string.data_out));

		setLineDataAttributes(indata, mColourIn);
		//setLineDataAttributes(outdata, mColourOut);

		dataSets.add(indata);
		//dataSets.add(outdata);

		return new LineData(dataSets);
	}
	public IAxisValueFormatter getIAxisValueFormatter(final int position)
	{
		return new IAxisValueFormatter() {

			@Override
			public String getFormattedValue(float value, AxisBase axis)
			{

				return String.format(Locale.getDefault(), "%.0f\u2009s ago", (axis.getAxisMaximum() - value) / 10);

			}
		};
	}
	private void setLineDataAttributes(LineDataSet dataSet, int colour) {
		dataSet.setLineWidth(1);
		dataSet.setCircleRadius(1);
		dataSet.setDrawCircles(false);
		dataSet.setCircleColor(mColourPoint);
		dataSet.setDrawFilled(false);
		dataSet.setFillAlpha(100);
		//dataSet.setFillColor(colour);
		dataSet.setColor(colour);
		dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
		dataSet.setValueTextColor(Color.WHITE);
		dataSet.setDrawValues(false);
	}
	@Override
	public void onDestroy()
	{
		if (timer != null) {
			timer.cancel();
		}

		// TODO: Implement this method
		super.onDestroy();
	}
}
