package fi.tol.android.RTDAReceiver;

import java.io.File;

import android.app.Activity;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RTDAReceiver extends Activity {
	/** Components on the UI */
	public static final int ON_OPEN_STATE = 1;
	public static final int ON_START_STATE = 2;
	public static final int ON_RUN_STATE = 3;
	private int canvasState;
	
	private Button btnStart;
	private Button btnStop;
	private Button btnRun;
	//private Button btnReport;
	private TextView textWelcomeInfo;
	private BTDeviceCanvas btDevicesCanvas; //Self defined Canvas component to draw BT devices
	
	/** System Time*/
	private Time time = new Time();
    
	/** Member classes, manage allowed devices list and recording*/
	private DevicesManaging deviceMag;
    
    /** Bluetooth device detecting*/
	private BroadcastReceiver btReceiver; //Listener for discovering a new BT device
	private BluetoothAdapter localBTAdapter; //Local Bluetooth adapter
	private String localBTAddress;	//Local Bluetooth address
	
	/** Create folders in phone SDCard*/
	public static final String appRootFolder = "RTDA";
	public static final String appLogFolder = "Logs";
	public static final String allowedFileName = "allowed_devices.txt";
	private String sdPath;
	
	
	
	/** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Log.i("RTDA", "On Create");
        
        /** Initial members and UI components*/
        initComponents();
		
        /** register Bluetooth BroadcastReceiver*/
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(btReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(btReceiver, filter);
    }
    
    /** Called after onCreate(). */
    public void onStart()
    {
    	Log.i("RTDA", "On Start");
    	super.onStart();
    	
    	localBTAdapter.startDiscovery();
    }
    
    /** Initial members and UI components*/
    public void initComponents()
	{
    	Log.i("RTDA", "call initComponents() function");
    	openBTAdapter(); //Get local BT adapter and turn on Bluetooth
    	localBTAddress = localBTAdapter.getAddress(); //Get local Bluetooth address
    	time.setToNow(); //Get system time;
    	canvasState = ON_OPEN_STATE;
    	
		textWelcomeInfo = (TextView) findViewById(R.id.welcome_info);
		btDevicesCanvas = (BTDeviceCanvas) findViewById(R.id.bt_device_canvas);
		btDevicesCanvas.setCanvasState(ON_OPEN_STATE);
		
		deviceMag = new DevicesManaging(localBTAdapter);
		setWelcomeInfo();
		
		/**Create App Home folder and Logs folder*/
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			sdPath = Environment.getExternalStorageDirectory() + "/";
			String logSubFileName = sdPath + appRootFolder + "/" + appLogFolder +
			"/" + time.year + "_" + (time.month+1) + "_" + time.monthDay;
			if(!isFileExist(sdPath + appRootFolder))
			{
				Log.i("RTDA", "create App home folder");
				File dir = new File(sdPath + appRootFolder);
				dir.mkdir();
				File logDir = new File(sdPath + appRootFolder + "/" + appLogFolder);
				logDir.mkdir();
			}
			if(!isFileExist(logSubFileName))
			{
				File logSubDir = new File(logSubFileName);
				logSubDir.mkdir();
			}
		}
		
		/**Click to start teaching BT devices*/
		btnStart = (Button) findViewById(R.id.button_start);
		this.btnStart.setOnClickListener(new OnClickListener(){
			public void onClick(final View v)
			{
				Log.i("RTDA", "Start button is clicked");
				canvasState = ON_START_STATE;
				btDevicesCanvas.setCanvasState(ON_START_STATE);
				deviceMag.setCanvasState(ON_START_STATE);
				btnStop.setEnabled(true);
				btnRun.setEnabled(false);
				//btnReport.setEnabled(false);
				deviceMag.clearAllowedDevices();
				localBTAdapter.cancelDiscovery();
			}
		});
		
		/**Click to stop teaching BT devices*/
		btnStop = (Button) findViewById(R.id.button_stop);
		this.btnStop.setOnClickListener(new OnClickListener(){
			public void onClick(final View v)
			{
				Log.i("RTDA", "Stop button is clicked");
				if(deviceMag.isAnyAllowedDevices())
				{
					btnRun.setEnabled(true);
				}
				btnStop.setEnabled(false);
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				{
					deviceMag.writeAllowedDevicesFile();
				}
				localBTAdapter.cancelDiscovery();
			}
		});
		btnStop.setEnabled(false);
		
		/**Click to recording routes*/
		btnRun = (Button) findViewById(R.id.button_run);
		this.btnRun.setOnClickListener(new OnClickListener(){
			public void onClick(final View v)
			{
				Log.i("RTDA", "Run button is clicked");
				
				canvasState = ON_RUN_STATE;
				btDevicesCanvas.setCanvasState(ON_RUN_STATE);
				deviceMag.setCanvasState(ON_RUN_STATE);
				btnStart.setEnabled(true);
				//btnReport.setEnabled(true);
				btnStop.setEnabled(false);
				
				deviceMag.setIsCurrentFalse();
				deviceMag.setIsInScopeFalse();
				localBTAdapter.cancelDiscovery();
			}
		});
		if(deviceMag.isAnyAllowedDevices())
		{
			btnRun.setEnabled(true);
		}
		else
		{
			btnRun.setEnabled(false);
		}
		
		/**Click to save records to .txt file*/
		/*btnReport = (Button) findViewById(R.id.button_report);
		this.btnReport.setOnClickListener(new OnClickListener(){
			public void onClick(final View v)
			{
				Log.i("RTDA", "Report button is clicked");
				//newThread = null;
			}
		});
		btnReport.setEnabled(false);*/
		
		/** Bluetooth BroadcastReceiver*/
		
		btReceiver = new BroadcastReceiver()
    	{
    		public void onReceive(Context context, Intent intent)
    		{
    			Log.i("RTDA", "Bluetooth Broadcast on receive");
    			String action = intent.getAction();
    			
    			// When discover a BT device
    			if (BluetoothDevice.ACTION_FOUND.equals(action))
    			{
    				Log.i("RTDA", "find Bluetooth device");
    				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    				switch(canvasState)
    				{
    				case ON_OPEN_STATE: //Open the application, it's in open state
    					deviceMag.addUselessDevice(device);
    					break;
    				case ON_START_STATE: //Start teaching BT device
    					deviceMag.addAllowedDevice(device);
    					break;
    				case ON_RUN_STATE: //Recording routes
    					deviceMag.recordEnterTime(device);
    					break;
    				}
    				deviceMag.printList();//testing
    				btDevicesCanvas.setBTDevicesList(deviceMag.returnDevicesList());
    				btDevicesCanvas.invalidate();
    			}
    			
    			// One discovery is finished
    			else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
    			{
    				Log.i("RTDA", "Bluetooth inquiry finished");
    				deviceMag.checkIsInScope();//all suitable
    				if(canvasState == ON_RUN_STATE)
    				{
    					deviceMag.recordLeaveTime();
    				}
    				deviceMag.printList();//testing
    				btDevicesCanvas.setBTDevicesList(deviceMag.returnDevicesList());
    				btDevicesCanvas.invalidate();
    				deviceMag.setIsCurrentFalse();
    				localBTAdapter.startDiscovery();
    			}
    		}
    	};
    	
    	Log.i("RTDA", "onCreate done");
	}
    
    /**Set welcome information in the UI*/
    public void setWelcomeInfo()
	{
    	Log.i("RTDA", "set welcome information");
    	String welInfo = "Welcome, Today is: " + time.monthDay + "/" +
    		(time.month + 1) + "/" + time.year + "!";
		textWelcomeInfo.setText(welInfo);
	}
    
    /**Get local BT adapter and turn on Bluetooth*/
    public void openBTAdapter()
	{
		Log.i("INFO", "open Bluetooth Device Adapter.");
    	localBTAdapter = BluetoothAdapter.getDefaultAdapter();
    	if (localBTAdapter == null)
		{
    		Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
		}    	
		if(!localBTAdapter.isEnabled())
		{
			localBTAdapter.enable();
		}
	}
    
    /** Check if files or directories exist*/
    public boolean isFileExist(String fileName)
	{
    	Log.i("RTDA", "check is file exist");
    	File logFile = new File(fileName);
		return logFile.exists();
	}
    
    public String getLocalBTAddress(){
    	return localBTAddress;
    }
}