package com.electrolites.ecg;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.electrolites.data.Data;
import com.electrolites.services.BluetoothService;
import com.electrolites.services.DataService;
import com.electrolites.services.FileParserService;
import com.electrolites.services.RandomGeneratorService;

public class ElectrolitesActivity extends Activity {

	static final int DIALOG_EXIT_ID = 0;
	static final int DIALOG_ABOUT_ID = 1;
	static final int DIALOG_LOAD_ID = 2;
	
	 // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_NAME = 2;
    public static final int MESSAGE_TOAST = 3;
    
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

	private Data data;

	private Button start;
	private StartListener startListener;
	private Button more;
	private MoreListener moreListener;
	private Button up;
	private UpListener upListener;
	private Button down;
	private DownListener downListener;
	private Button plus;
	private PlusListener plusListener;
	private Button less;
	private LessListener lessListener;

	private TextView hRate;
	private TextView display;

	private EditText id;
	private EditText name;

	private LinearLayout lSuperior;
	private LinearLayout lInferior;

	Intent intentDePferv;

	private ECGView ecgView;

	@Override
	public void onSaveInstanceState(Bundle saveInstanceState) {
		saveInstanceState.putBoolean("ecgVisible",
				ecgView.getVisibility() == View.VISIBLE);
		super.onSaveInstanceState(saveInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState.getBoolean("ecgVisible"))
			ecgView.setVisibility(View.VISIBLE);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		data = Data.getInstance();
		data.app = getApplication();
		data.activity = this;

		/* CUTRESY! */
		Button auto = (Button) findViewById(R.id.b_auto);
		auto.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (data.autoScroll)
					((Button) findViewById(R.id.b_auto))
							.setText("Auto\n[ OFF ]");
				else
					((Button) findViewById(R.id.b_auto))
							.setText("Auto\n[ ON ]");
				data.autoScroll = !data.autoScroll;
			}
		});

		start = (Button) findViewById(R.id.b_start);
		start.setEnabled(true);
		startListener = new StartListener();
		start.setOnClickListener(startListener);

		more = (Button) findViewById(R.id.b_reset);
		moreListener = new MoreListener();
		more.setOnClickListener(moreListener);

		up = (Button) findViewById(R.id.b_up);
		up.setEnabled(true);
		upListener = new UpListener();
		up.setOnClickListener(upListener);

		down = (Button) findViewById(R.id.b_down);
		down.setEnabled(true);
		downListener = new DownListener();
		down.setOnClickListener(downListener);

		plus = (Button) findViewById(R.id.b_plus);
		plus.setEnabled(true);
		plusListener = new PlusListener();
		plus.setOnClickListener(plusListener);

		less = (Button) findViewById(R.id.b_less);
		less.setEnabled(true);
		lessListener = new LessListener();
		less.setOnClickListener(lessListener);

		ecgView = (ECGView) findViewById(R.id.v_eCGView);
		ecgView.setVisibility(View.INVISIBLE);

		lSuperior = (LinearLayout) findViewById(R.id.l_superior);
		lSuperior.setBackgroundColor(Color.DKGRAY);

		lInferior = (LinearLayout) findViewById(R.id.l_inferior);
		lInferior.setBackgroundColor(Color.DKGRAY);

		id = (EditText) findViewById(R.id.e_id);
		id.setTextColor(Color.GRAY);
		name = (EditText) findViewById(R.id.e_name);
		name.setTextColor(Color.GRAY);

		hRate = (TextView) findViewById(R.id.t_hRate);
		hRate.setBackgroundColor(Color.DKGRAY);
		hRate.setGravity(Gravity.CENTER);
		hRate.setTextSize(30f);
		hRate.setTextColor(Color.RED);
		hRate.setText("60 bpr");

		display = (TextView) findViewById(R.id.t_display);
		display.setBackgroundColor(Color.DKGRAY);
		display.setGravity(Gravity.CENTER);
		display.setTextSize(30f);
		display.setTextColor(Color.GRAY);
		display.setText("Disconected");
		
		//Hiper Cutresy
		data.mHandler = mHandler;
		
	}

	// Menu Items

	// Called when a dialog is first created
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_EXIT_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to exit?")
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									ElectrolitesActivity.this.finish();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
			break;
		case (DIALOG_ABOUT_ID):
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.about_dialog);
			dialog.setTitle("About");

			LinearLayout labout = (LinearLayout) dialog
					.findViewById(R.id.layout_about);
			labout.setOnClickListener(new AboutListener());

			TextView text = (TextView) dialog.findViewById(R.id.text);
			text.setGravity(Gravity.CENTER);
			text.setText("Electrolites V0.2");
			ImageView image = (ImageView) dialog.findViewById(R.id.im_android);

			image.setImageResource(R.drawable.icon);

			break;
		case (DIALOG_LOAD_ID):
			final String[] items = logTitles();

			builder = new AlertDialog.Builder(this);
			builder.setTitle("Choose a log file");
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					Toast.makeText(getApplication(), items[item],
							Toast.LENGTH_SHORT).show();
					data.toLoad = items[item] + ".txt";
					dialog.cancel();
					
					intentDePferv = new Intent(getApplication(),
							BluetoothService.class);
					stopService(intentDePferv);
					
					intentDePferv = new Intent(getApplication(),
							FileParserService.class);
					intentDePferv.setAction(DataService.RETRIEVE_DATA);
					getApplication().startService(intentDePferv);

					ecgView.setVisibility(View.VISIBLE);
					start.setEnabled(true);
					display.setText("NOT ARRITMIA DETECTED");
				}
			});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.about:
			showDialog(DIALOG_ABOUT_ID);
			return true;
		case R.id.exit:
			showDialog(DIALOG_EXIT_ID);
			return true;
		case R.id.load:
			showDialog(DIALOG_LOAD_ID);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Este metodo no est� muy bien aqu�, ya lo pondr� en su sitio cuando
	// funcione
	public String[] logTitles() {
		File root = Environment.getExternalStorageDirectory();
		File path = new File(root, "/Download/");

		String[] list = path.list();
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < list.length; i++) {
			if (list[i].endsWith(".txt"))
				result.add(list[i].substring(0, list[i].length() - 4));
		}

		list = result.toArray(new String[result.size()]);

		return list;
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (BluetoothService.DEBUG)
					Log.i(BluetoothService.TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothService.STATE_CONNECTED:
					display.setText("Conected to: ");
					display.append(data.conected);
					break;
				case BluetoothService.STATE_CONNECTING:
					display.setText("Conecting...");
					break;
				case BluetoothService.STATE_LISTEN:
					display.setText("Disconected");
					break;
				case BluetoothService.STATE_NONE:
					display.setText("Disconected");
					start.setEnabled(true);
					break;
				}
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				data.conected = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + data.conected,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	// AboutDialog Listener

	class AboutListener implements OnClickListener {

		public void onClick(View v) {
			dismissDialog(DIALOG_ABOUT_ID);
		}
	}

	// Main Layout Listeners
	class StartListener implements OnClickListener {

		public void onClick(View v) {

			/*
			 * intentDePferv = new Intent(getApplication(),
			 * FileParserService.class);
			 * intentDePferv.setAction(DataService.RETRIEVE_DATA);
			 * getApplication().startService(intentDePferv);
			 * 
			 * start.setEnabled(false); ecgView.setVisibility(View.VISIBLE);
			 */

			intentDePferv = new Intent(getApplication(), BluetoothService.class);
			intentDePferv.setAction(DataService.START_RUNNING);
			intentDePferv.putExtra("deviceName", data.conected);
			getApplication().startService(intentDePferv);

			intentDePferv.setAction(DataService.RETRIEVE_DATA);
			getApplication().startService(intentDePferv);

			start.setEnabled(false);
			ecgView.setVisibility(View.VISIBLE);
			
		}
	}

	class MoreListener implements OnClickListener {
		public void onClick(View v) {

			intentDePferv = new Intent(getApplication(),
					RandomGeneratorService.class);
			intentDePferv.setAction(DataService.RETRIEVE_DATA);
			getApplication().startService(intentDePferv);

		}
	}

	class UpListener implements OnClickListener {

		public void onClick(View v) {
			if (ecgView.getVisibility() != View.VISIBLE)
				return;

			if (data.getDrawBaseHeight() < 0.1)
				up.setEnabled(false);
			else {
				data.setDrawBaseHeight(data.getDrawBaseHeight() - 0.1f);
				down.setEnabled(true);
			}
		}
	}

	class DownListener implements OnClickListener {
		public void onClick(View v) {
			if (ecgView.getVisibility() != View.VISIBLE)
				return;

			if (data.getDrawBaseHeight() >= 1)
				down.setEnabled(false);
			else {
				data.setDrawBaseHeight(data.getDrawBaseHeight() + 0.1f);
				up.setEnabled(true);
			}
		}
	}

	class PlusListener implements OnClickListener {

		public void onClick(View v) {
			if (ecgView.getVisibility() != View.VISIBLE)
				return;

			data.setWidhtScale(data.getWidhtScale() + 0.5f);
			less.setEnabled(true);
		}
	}

	class LessListener implements OnClickListener {
		public void onClick(View v) {
			if (ecgView.getVisibility() != View.VISIBLE)
				return;

			if (data.getWidhtScale() < 0.5f)
				less.setEnabled(false);
			else {
				data.setWidhtScale(data.getWidhtScale() - 0.5f);
			}
		}
	}

}