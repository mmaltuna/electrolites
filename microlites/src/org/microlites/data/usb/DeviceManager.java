package org.microlites.data.usb;

import java.util.HashMap;
import java.util.Iterator;

import org.microlites.MicrolitesActivity;
import org.microlites.R;
import org.microlites.data.DataHolder;
import org.microlites.data.DataManager;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class DeviceManager implements DataManager {

	public final static String TAG = "DeviceManager";
	public static final String ACTION_USB_PERMISSION = "com.electrolites.ecg.USB_PERMISSION";
	
	private Activity activity;	// Referencia a la actividad que la invoca
	private Context context;
	private BroadcastReceiver usbReceiver;
	
	private UsbManager usbManager;
	
	private UsbInterface interf = null;
	private UsbEndpoint endpointRead = null;
	private UsbEndpoint endpointWrite = null;
	private UsbDeviceConnection connection = null;

	private boolean forceClaim = true;
	
	private UsbComThread thread;	// Hilo que se encargar?? de la comunicaci??n con el accesorio
	
	private DataHolder dataHolder;					// DataHolder Instance
	
	public DeviceManager(Activity activity) {
		this.activity = activity;
		context = activity.getApplicationContext();
		usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		System.out.println("USB Device Manager created");
		
		buildMenu();
	}
	
	public void start() {
		usbReceiver = new BroadcastReceiver() {
		    public void onReceive(Context context, Intent intent) {
		        String action = intent.getAction();
		        
		        if (DeviceManager.ACTION_USB_PERMISSION.equals(action)) {
		        	synchronized (this) {
		        		UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		    
		        		Toast.makeText(context, "Device found.", Toast.LENGTH_SHORT);
		        		
		        		if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
		        			Toast.makeText(context, "Device permission granted.", Toast.LENGTH_SHORT);
		                    if (device != null) {
		                    	Toast.makeText(context, "Opening device...", Toast.LENGTH_SHORT);
		                    	openDevice(device);
		                    }
		                }
		                else {
		                	Toast.makeText(context, "Device permission denied.", Toast.LENGTH_SHORT);
		                }
		        	}
		       }
		        
		       if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
		    	   UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
	    		   String output = "Device " + device.toString() + " detached.";
	    		   Toast.makeText(context, output, Toast.LENGTH_SHORT);
	    		   stop();
		       }
		    }
		};
		
		System.out.println("USB Permission Intent Created");
		PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        activity.registerReceiver(usbReceiver, filter);
        
        UsbDevice device = null;
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        System.out.println("Obtaining USB Devices List... Size: " +  deviceList.size());
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            device = deviceIterator.next();
        }
        
        if (device != null) {
        	usbManager.requestPermission(device, permissionIntent);
        	System.out.println("USB Device Permisson Requested");
        } else { 
        	Toast.makeText(context, "No devices attached.", Toast.LENGTH_SHORT).show();
        	System.out.println("Usable USB Device Not Found");
        }
	}
	
	//@Override
	public void stop() {
		// Terminamos la actividad del thread de comunicaci??n
		System.out.println("USB Device Manager Stopping it's operation");
		if (thread != null) {
			thread.stop = true;
			thread.interrupt();
			// thread.halt();
			try {
				thread.join();
				System.out.println("Thread Joined!");
			} catch (InterruptedException ex) {
				ex.printStackTrace();
				System.out.println("Thread Join Failed!");
			}
		}
		
		System.out.println("USB Device Comm Thread stopped");
		
		// Cerramos el descriptor de archivo del accesorio
		if (connection != null) {
			connection.releaseInterface(interf);
			connection.close();
		}
		
		System.out.println("USB Device Connection Closed & Released");
		
		try {
			activity.unregisterReceiver(usbReceiver);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
		System.out.println("USB Device Receiver Unregistered");
		
		MicrolitesActivity.instance.popView();
		MicrolitesActivity.instance.endCurrentManagerOperation();
		
		System.out.println("USB Manager Is Off!");
	}
	
	public void openDevice(UsbDevice device) {
        if (device.getInterfaceCount() != 1) {
        	Log.w(TAG, "could not find interface.");
            return;
        }
		interf = device.getInterface(0);
		
		if (interf.getEndpointCount() != 2) {
			Log.w(TAG, "could not find the endpoints.");
	        return;
	    }
		
		//El MSP tiene 2 endpoints, y su orden es relevante pero al preguntar no tiene 
		//porque devolverlos en el mismo orden siempre, 
		UsbEndpoint endpoint1 = interf.getEndpoint(0);
		UsbEndpoint endpoint2 = interf.getEndpoint(1);
			
		if (endpoint1.getType() == UsbConstants.USB_ENDPOINT_XFER_INT){
			if (endpoint1.getDirection() == UsbConstants.USB_DIR_IN){
				endpointRead = endpoint1;
			}
			else if (endpoint1.getDirection() == UsbConstants.USB_DIR_OUT){
				endpointWrite = endpoint1;
			}
		}
		if (endpoint2.getType() == UsbConstants.USB_ENDPOINT_XFER_INT){
			if (endpoint2.getDirection() == UsbConstants.USB_DIR_IN){
				endpointRead = endpoint2;
			}
			else if (endpoint2.getDirection() == UsbConstants.USB_DIR_OUT){
				endpointWrite = endpoint2;
			}
		}
		if ((endpointRead == null) || (endpointWrite == null)){
			Log.w(TAG, "endpoint is not interrupt type");
			return;
		}
		
		
		connection = usbManager.openDevice(device); 
		//connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT); //do in another thread
		
	    if (connection != null) {		    
			connection.claimInterface(interf, forceClaim);
	        thread = new UsbComThread(interf, endpointRead, endpointWrite, connection, dataHolder);
	        thread.start();
	    }
	}

	//@Override
	public void configure(DataHolder dataHolder) {
		this.dataHolder = dataHolder;
	}

	public void back() {
		this.stop();
	}
	
	/** Builds Bluetooth Configuration Menu and pushes it into the View Stack
	*/ 
	private void buildMenu() {
		// Store a reference to the activity instance
		MicrolitesActivity act = MicrolitesActivity.instance;
		
		// Display Bluetooth Settings dialog
		final Dialog dialog = new Dialog(act);
		
		dialog.setTitle(R.string.usbSettingsTitle);
		dialog.setContentView(R.layout.usbconfiglayout);
		dialog.setOwnerActivity(act);
		dialog.show();
		
		// Set button listener
		Button b = (Button) dialog.findViewById(R.id.usbSettingsStart);
		b.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				
				
				if (preConnect()) {
					// Close dialog
					dialog.cancel();
					
					// Begin visualization
					MicrolitesActivity.instance.initVisualization(MicrolitesActivity.MODE_USB, 1, null);
				} else {
					Animation shake = AnimationUtils.loadAnimation(MicrolitesActivity.instance.getApplicationContext(), R.anim.shake);
	                
					TextView tv = (TextView) dialog.findViewById(R.id.usbSettingsText);
					tv.setText("El receptor USB no está disponible.\nPor favor, conéctelo antes de continuar.");
					LinearLayout l = (LinearLayout) dialog.findViewById(R.id.usbSettingsLayout);
					l.startAnimation(shake);
				}
			}
		});
		
		b = (Button) dialog.findViewById(R.id.usbSettingsCancel);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dialog.cancel();
			}
		});
	}
	
	protected boolean preConnect() {
		UsbDevice device = null;
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        System.out.println("Comprobando disponibilidad de accesorios USB...");
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        boolean found = false;
        while(deviceIterator.hasNext() && device == null) {
            device = deviceIterator.next();
            if (device != null) {
            	found = true;
            	break;
            }
        }
        
        if (found)
        	System.out.println("Accesorios USB encontrados.");
        else
        	System.err.println("No hay ningún accesorios USB disponible.");
        
        return found;
    }
}



