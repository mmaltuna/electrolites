package org.microlites;

import java.util.Stack;

import org.microlites.data.Data;
import org.microlites.data.DataHolder;
import org.microlites.data.DataManager;
import org.microlites.data.bluetooth.BluetoothManager;
import org.microlites.data.filereader.FileManager;
import org.microlites.data.generator.GeneratorManager;
import org.microlites.data.usb.DeviceManager;
import org.microlites.view.AnimationThread;
import org.microlites.view.ECGView;
import org.microlites.view.dynamic.DynamicViewThread;
import org.microlites.view.still.StaticViewThread;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MicrolitesActivity extends Activity implements OnGestureListener {
	public static final byte MODE_NONE		= 0xF;		// No mode (Start Menu) 
	public static final byte MODE_BLUETOOTH = 0x1;		// Bluetooth Constant
	public static final byte MODE_FILELOG	= 0x2;		// Log Constant
	public static final byte MODE_USB		= 0x3;		// USB Constant
	public static final byte MODE_GEN		= 0x4;		// Generator Constant
	
	public static final byte ST_MENU		= 0x1;		// Main Menu
	public static final byte ST_INMANAGER	= 0x2;		// Manager Operating
	public static final byte ST_RUNNING		= 0x3;		// Visualiz. Running
	
	GestureDetector gestureScanner;				// Gesture Detector
	
	public static MicrolitesActivity instance;	// Reference to this Activity
	
	byte currentMode;							// Current App Mode
	ECGView currentView;						// Reference to current View
	AnimationThread currentViewThread;			// Reference to View Thread
	DataManager currentManager;					// Reference to Data Manager
	View menuView;								// Refernece to initial Menu
	byte currentState; 							// Application current state
	
    /** Called when the activity is first created,
     * 	when the screen is rotated or when the
     *  application is paused and resumed by the system
     *  or the user.
     * */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set main layout
    	setContentView(R.layout.main);
    	
    	// Store reference to this instance
        MicrolitesActivity.instance = this;
        // Instantiate Gesture Detector
        gestureScanner = new GestureDetector(this);
        // Initialize references
        currentMode = MODE_NONE;
        currentView = null;
        currentViewThread = null;
        currentManager = null;
        menuView = null;
        currentState = ST_MENU;
        
        // Testing Area
        viewStack = new Stack<View>();
        
        // Restore view if application has been restored
    	if (savedInstanceState != null) {
    		if (savedInstanceState.containsKey("currentMode")) {
    			byte mode = savedInstanceState.getByte("currentMode");
    			if (mode != MODE_NONE)
    				initVisualization(mode, 0, null);
    		}
        }
        
        // Create Main Menu Button Handlers
    	if (currentMode == MODE_NONE) {
    		SeekBar bar = (SeekBar) findViewById(R.id.seekBar1);
    		bar.setProgress(50);
    		
    		bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				//@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				//@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				//@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					int max = seekBar.getMax();
					Float w = 1 + progress/((float) max)*2;
					if (w - Math.floor(w.doubleValue()) > 0.5)
						w = (float) (Math.floor(w.doubleValue()) + 0.5);
					else
						w = android.util.FloatMath.floor((float) w.doubleValue());
						
					((TextView) findViewById(R.id.textView1)).setText("Ancho en segundos: "+w);
					Data.getInstance().viewWidth = w;
				}
			});
    		
	        Button start = (Button) findViewById(R.id.startBluetoothButton);
	        start.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					MicrolitesActivity.instance.initVisualization(MODE_BLUETOOTH, 0, null);
				}
			});
	        
	        Button usb = (Button) findViewById(R.id.startUsbButton);
	        usb.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					MicrolitesActivity.instance.initVisualization(MODE_USB, 0, null);
				}
			});
	        
	        Button logButton = (Button) findViewById(R.id.startLogButton);
	        logButton.setOnClickListener(new OnClickListener() {
				//@Override
				public void onClick(View v) {
					initVisualization(MODE_FILELOG, 0, null);
				}
	        });
    	}
    }
    
    public void initVisualization(byte mode, int phase, ECGView v) {
    	switch (phase) {
    	case 0: 
			// Phase 0 - Init
    		currentState = ST_INMANAGER;
    		System.out.println("AppStatus: The manager takes control");
    		
			// 1. Create Manager
    		switch (mode) {
				case MODE_BLUETOOTH:
					currentManager = new BluetoothManager();
				break;
				case MODE_FILELOG:
					currentManager = new FileManager();
				break;
				case MODE_USB:
					currentManager = new DeviceManager(this);
				break;
				case MODE_GEN:
					currentManager = new GeneratorManager();
				break;
				default:
					System.out.println("Modo no reconocido: " + mode);
			}
    		
    		currentMode = mode;
    		
			break;
    	case 1:
    		// Phase 1 - Dynamic Surface
    		System.out.println("AppStatus: Dynamic Surface Creation");
    		
			// 2. Add new Dynamic Surface Holder
			currentView = new ECGView(getApplicationContext(), null, this);
			currentView.notifyAboutCreation = mode;
	        // content.addView(currentView);
			pushView(currentView);
	        
	        // 3. Wait for dynamicHolder to call this again
	        break;
    	case 2: 
			// Phase 2 - Surface available, start the magic!
    		currentState = ST_RUNNING;
    		System.out.println("AppStatus: Visualization Running");
    		
    		// Show zoom, shrink buttons
    		setViewControlButtons(true);
    		
    		Data d = Data.getInstance();
    		d.resetView();
    		
    		// 1. Instantiate viewthread
			switch (mode) {
				case MODE_BLUETOOTH:
				case MODE_USB:
				case MODE_GEN:
					d.currentViewThread = new DynamicViewThread(d.currentViewHolder, currentView);
				break;
				case MODE_FILELOG:
					d.currentViewThread = new StaticViewThread(d.currentViewHolder, currentView);
				break;
				default:
					System.out.println("Modo no reconocido: " + mode);
			}
    		currentView.setThread(d.currentViewThread);

    		// 2. Start reception thread
    		currentManager.configure((DataHolder) Data.getInstance().currentViewThread);
			currentManager.start();
			break;
    	}
    }
        
    public void endCurrentManagerOperation() {
    	if (currentManager != null) {
    		currentManager = null;
    	}
    	setViewControlButtons(false);
		currentView = null;
    	currentMode = MODE_NONE;
    	currentState = ST_MENU;
		System.out.println("AppStatus: Returned to Main Menu");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    	/*if (currentView instanceof ECGView) {
    		outState.putString("currentView", "ECGView");
    		if (currentViewThread != null)
    			currentViewThread.saveYourData(outState);
    	}
    	else
    		outState.remove("currentView");*/
    	outState.putByte("currentMode", currentMode);
    	// TODO: Save actual state
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	if (currentManager != null)
    		currentManager.stop();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me)
    {
        return gestureScanner.onTouchEvent(me);
    }
    
    //@Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
    	float x = e1.getX(0);
    	float y = e1.getY(0);
    	if (currentView != null)
    		currentView.handleScroll(distanceX, distanceY, x, y);
        return true;
    }

	//@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return true;
	}

	//@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return true;
	}

	//@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
	}

	//@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		// Data.getInstance().pause = !Data.getInstance().pause;
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Data.getInstance().appMenu = menu;
		
		menu.add("Acercar vista");
		menu.add("Alejar vista");
		menu.add("Acerca de");
		menu.add("Salir");

		switch (currentState) {
		case ST_MENU:
		case ST_INMANAGER:
			menu.getItem(0).setVisible(false);
			menu.getItem(1).setVisible(false);
			break;
		case ST_RUNNING:
			break;
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		Data d = Data.getInstance();
	    if (item.getTitle().equals("Acercar vista")) {
	    	d.yScaleTopValue /= 1.5f;
	    } else if (item.getTitle().equals("Alejar vista")) {
	    	d.yScaleTopValue *= 1.5f; 
	    } else if (item.getTitle().equals("Salir")){
	    	finish();
	    /*} else if (item.getTitle().equals("Zero/NotZero")) {
	    	d.generateZeros = !d.generateZeros;
	    	
	    	if (d.generateZeros) {
	    		if (Math.random() < 0.5)
	    			d.generateHeight -= 500;
	    		else
	    			d.generateHeight += 500;
	    	}*/
	    } else if (item.getTitle().equals("Acerca de")) {
	    	showAboutMenu();
	    } else {
	    	return super.onOptionsItemSelected(item);
	    }
	    
        return true;
	}
	
	Stack<View> viewStack;
		
	OnClickListener addLayoutListener;
	
	public View pushView(View v) {
		// 1. Get reference to main content panel
    	LinearLayout content = (LinearLayout) findViewById(R.id.contentPanel);
    	View last = content.getChildAt(0);
        // Pushing old to stack
        viewStack.push(last);
    	
    	// 2. Clear it
		content.removeAllViews();
		
		// 3. Add new View
        content.addView(v);
        
        return last;
	}
	
	public View popView() {
		if (!viewStack.isEmpty()) {
			// 1. Get reference to main content panel
			LinearLayout content = (LinearLayout) findViewById(R.id.contentPanel);
			View last = content.getChildAt(0);
			
			// 2. Clear it
			content.removeAllViews();
			
			// 3. Add last View
		    content.addView(viewStack.pop());
		    // Pushing old to stack
		    return last;
		} else {
			return null;
		}
	}
	
	public View getCurrentView() {
		return ((LinearLayout)findViewById(R.id.contentPanel)).getChildAt(0);
	}
	
	public DataManager getCurrentManager() {
		return currentManager;
	}
	
	@Override
	public void onBackPressed() {
		if (viewStack.isEmpty()) {
    		System.out.println("AppStatus: Goodbye!");
			finish();
		} else {
			if (currentManager !=  null)
				currentManager.back();
			else
				popView();
		}
	}
	
	public void setViewControlButtons(boolean on) {
		if (Data.getInstance().appMenu == null)
			return;
		MenuItem a = Data.getInstance().appMenu.getItem(0), b = Data.getInstance().appMenu.getItem(1);
		if (a == null || b == null)
			return;
		
		a.setVisible(on);
		a.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		b.setVisible(on);
		b.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}
	
	protected void showAboutMenu() {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.about_dialog);
		dialog.setTitle("Acerca de");

		LinearLayout labout = (LinearLayout) dialog
				.findViewById(R.id.layout_about);
		labout.setGravity(Gravity.CENTER);
		labout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.cancel();
			}
		});

		TextView text = (TextView) dialog.findViewById(R.id.textA);
		text.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		text.setPadding(30, 0, 0, 0);
		text.setText("microlites 1.2");
		
		text = (TextView) dialog.findViewById(R.id.textB);
		text.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);
		text.setPadding(20, 0, 0, 0);
		text.setText("Pablo Fernández\nRafa de la Hoz\nMiguel Márquez");
		
		text = (TextView) dialog.findViewById(R.id.textC);
		text.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		text.setPadding(35, 2, 0, 0);
		text.setText("2011/2012");
		
		ImageView image = (ImageView) dialog.findViewById(R.id.im_android);
		image.setImageResource(R.drawable.ecg_icon);
		
		dialog.show();
	}
}