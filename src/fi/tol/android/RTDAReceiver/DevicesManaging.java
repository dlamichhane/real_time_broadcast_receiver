package fi.tol.android.RTDAReceiver;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

public class DevicesManaging {
	private ArrayList<DeviceItem> devicesList;
	private BluetoothAdapter localAdapter;
	
	/** File I/O*/
	private FileInputStream fileIn;
	private InputStreamReader inReader;
	private BufferedReader bfReader;
	private FileOutputStream fileOut;
	private OutputStreamWriter outWriter;
	private BufferedWriter bfWriter;
	private String lineTemp;
	private File logFile;
	private FileOutputStream logFileOut;
	private OutputStreamWriter logOutWriter;
	private BufferedWriter logBFWriter;
	private int canvasState = RTDAReceiver.ON_OPEN_STATE;
	private int allowedDeviceNo = 0;
	private int deviceClass;
	
	/** App folder path in phone SDcard*/
	private String sdPath;
	
	private Time time = new Time();
	
	private static final String TAG = "DeviceManaging";
	private int result;
	
	/** Constructor, initial members*/
	public DevicesManaging(BluetoothAdapter adapter)
	{
		Log.i("RTDA", "DevicesManaging class constructor");
		devicesList = new ArrayList<DeviceItem>();
		localAdapter = adapter;
		
		
		sdPath = Environment.getExternalStorageDirectory() + "/";
		File file = new File(sdPath + RTDAReceiver.appRootFolder + "/" + RTDAReceiver.allowedFileName);
		if(file.exists())
		{
			readAllowedDevicesFile(); //read allowed devices info from .txt file into allowedDevices list
		}
		
		time.setToNow();
		setLogFileName();
	}
	
	/** Read allowed devices info from .txt file into allowedDevices list*/
	public void readAllowedDevicesFile()
	{
		Log.i("RTDA", "read allowed devices from .txt file");
		lineTemp = "";
		String[] tempArr = {};//Split temp
		String address = "";
		try //Open the allowed devices file
		{
			File file = new File(sdPath + RTDAReceiver.appRootFolder + "/" + RTDAReceiver.allowedFileName);
			fileIn = new FileInputStream(file);
			inReader = new InputStreamReader(fileIn);
			bfReader = new BufferedReader(inReader);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try 
		{
			while((lineTemp=bfReader.readLine()) != null) //Read one line
			{
				tempArr = lineTemp.split(" "); //Split the line with " "
				address = tempArr[(tempArr.length-1)]; //Get BT address info
				deviceClass = Integer.parseInt(tempArr[(tempArr.length -2)]);
				//Get BluetoothDevice according to the address
				allowedDeviceNo = Integer.parseInt(tempArr[0]);
				BluetoothDevice deviceItem = localAdapter.getRemoteDevice(address); 
				addAllowedDevice(deviceItem);
			}
			bfReader.close(); //close the file
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/** Write allowed devices info into .txt file when stop teaching BT device*/
	public void writeAllowedDevicesFile()
	{
		String allowedDeviceInfo = "";
		try //Open allowed Devices file
		{
			File file = new File(sdPath + RTDAReceiver.appRootFolder + "/" + RTDAReceiver.allowedFileName);
			fileOut = new FileOutputStream(file); //only allow this App access it, overwrite mode
			outWriter = new OutputStreamWriter(fileOut);
			bfWriter = new BufferedWriter(outWriter);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try 
		{
			for(int i = 0; i < devicesList.size(); i++)
			{
				if(devicesList.get(i).getIsAllowed())
				{
					allowedDeviceInfo += devicesList.get(i).getBTDeviceNo() + " " 
						+ devicesList.get(i).getBluetoothDevice().getName() + " "
						+ devicesList.get(i).getBluetoothDevice().getBluetoothClass().getMajorDeviceClass()
						+ " " + devicesList.get(i).getBluetoothDevice().getAddress() + " " + "\n";
				}
			}
			bfWriter.write(allowedDeviceInfo);
			bfWriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void addAllowedDevice(BluetoothDevice device)
	{
		Log.i("RTDA", "add allowed device");
		if(!isExist(device.getAddress()))
		{
			DeviceItem deviceItem = new DeviceItem();
			deviceItem.setBluetoothDevice(device);
			deviceItem.setBTdeviceNo(allowedDeviceNo);
			deviceItem.setIsAllowed(true);
			deviceItem.setIsUseless(false);
			if(canvasState == RTDAReceiver.ON_OPEN_STATE)
			{
				deviceItem.setBTMajorDeviceClass(deviceClass);
				deviceItem.setIsCurrent(false);
				deviceItem.setIsInScope(false);
			}
			else if(canvasState == RTDAReceiver.ON_START_STATE)
			{
				deviceItem.setBTMajorDeviceClass(device.getBluetoothClass().getMajorDeviceClass());
				deviceItem.setIsCurrent(true);
				deviceItem.setIsInScope(true);
				allowedDeviceNo ++;
			}
			devicesList.add(deviceItem);
		}
		else
		{
			for(int i = 0; i < devicesList.size(); i++)
			{
				if(devicesList.get(i).getBluetoothDevice().getAddress().
						equalsIgnoreCase(device.getAddress()) &&
						!devicesList.get(i).getIsUseless())
				{
					devicesList.get(i).setIsCurrent(true);
					devicesList.get(i).setIsInScope(true);
				}
			}
		}
	}
	public void addUselessDevice(BluetoothDevice device)
	{
		Log.i("RTDA", "add useless device");
		if(!isExist(device.getAddress()))
		{
			DeviceItem deviceItem = new DeviceItem();
			deviceItem.setBluetoothDevice(device);
			deviceItem.setBTMajorDeviceClass(device.getBluetoothClass().getMajorDeviceClass());
			deviceItem.setIsAllowed(false);
			deviceItem.setIsUseless(true);
			deviceItem.setIsCurrent(true);
			deviceItem.setIsInScope(true);
			devicesList.add(deviceItem);
		}
		else
		{
			for(int i = 0; i < devicesList.size(); i++)
			{
				if(devicesList.get(i).getBluetoothDevice().getAddress().
						equalsIgnoreCase(device.getAddress()))
				{
					devicesList.get(i).setIsCurrent(true);
					devicesList.get(i).setIsInScope(true);
				}
			}
		}
	}
	
	public void clearAllowedDevices()
	{
		Log.i("RTDA", "clear list");
		allowedDeviceNo = 1;
		@SuppressWarnings("unchecked")
		ArrayList<DeviceItem> temp = (ArrayList<DeviceItem>) devicesList.clone();
		devicesList.clear();
		for(int i = 0; i < temp.size(); i ++)
		{
			if(!temp.get(i).getIsAllowed())
			{
				devicesList.add(temp.get(i));
			}
		}
	}
	
	/** Return allowed devices list*/
	public ArrayList<DeviceItem> returnDevicesList()
	{
		return devicesList;
	}
	
	public void setIsCurrentFalse()
	{
		for(int i = 0; i < devicesList.size(); i++)
		{
			devicesList.get(i).setIsCurrent(false);
		}
	}
	public void setIsInScopeFalse()
	{
		for(int i = 0; i < devicesList.size(); i++)
		{
			devicesList.get(i).setIsInScope(false);
		}
	}
	
	public void recordEnterTime(BluetoothDevice device)
	{
		time.setToNow();
		String timeString = setTimeString(time);
		for(int i = 0; i < devicesList.size(); i++)
		{
			if(devicesList.get(i).getBluetoothDevice().getAddress().equalsIgnoreCase(device.getAddress())
					&& devicesList.get(i).getIsAllowed())
			{
				if(!devicesList.get(i).getIsInScope())//new discovered device
				{
					devicesList.get(i).setIsCurrent(true);
					devicesList.get(i).setIsInScope(true);
					devicesList.get(i).setEnter_time(timeString);
					Log.i("RTDA", "Enter Time:" + timeString);
					devicesList.get(i).setLeave_time(null);
					BTEvent e = new BTEvent(BTEvent.Event.ENTER);
					e.setBtBeaconAddress(devicesList.get(i).getBluetoothDevice().getAddress());
					e.setTime(timeString);
					new SendDataToServer().execute(e);

					
				}
				else if(devicesList.get(i).getIsInScope() && !devicesList.get(i).getIsCurrent())
				{
					devicesList.get(i).setIsCurrent(true);
					devicesList.get(i).setIsInScope(true);
				}
			}
		}
	}
	
	
	public void recordLeaveTime()
	{
		time.setToNow();
		String timeString = setTimeString(time);
		String recordInfo = "";
		for(int i = 0; i < devicesList.size(); i++)
		{
			if(devicesList.get(i).getIsAllowed() &&
					!devicesList.get(i).getIsInScope() && 
					devicesList.get(i).getEnter_time() != null &&
					devicesList.get(i).getLeave_time() == null)
			{
				devicesList.get(i).setLeave_time(timeString);
				recordInfo += "Bluetooth Device NO.: " + devicesList.get(i).getBTDeviceNo() +
					"; Bluetooth Name: " + devicesList.get(i).getBluetoothDevice().getName() +
					"; Bluetooth Address: " + devicesList.get(i).getBluetoothDevice().getAddress() +
					"; Enter Time: " + devicesList.get(i).getEnter_time() +
					"; Leave Time: " + devicesList.get(i).getLeave_time() + ";\n";
				
				try {
					logFileOut = new FileOutputStream(logFile, true);
					logOutWriter = new OutputStreamWriter(logFileOut);
					logBFWriter = new BufferedWriter(logOutWriter);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} //only allow this App access it, overwrite mode
				
				try 
				{
					logBFWriter.write(recordInfo);
					logBFWriter.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	
	public void checkIsInScope()
	{
		for(int i = 0; i < devicesList.size(); i++)
		{
			if(devicesList.get(i).getIsInScope() && !devicesList.get(i).getIsCurrent())
			{
				devicesList.get(i).setIsInScope(false);
			}
		}
	}
			
	public boolean isAnyAllowedDevices()
	{
		boolean exist = false;
		for(int i = 0; i < devicesList.size(); i++)
		{
			if(devicesList.get(i).getIsAllowed())
			{
				exist = true;
				break;
			}
		}
		return exist;
	}
	
	/** Check is a BT device already exist in list*/
	public boolean isExist(String btAddress)
	{
		boolean exist = false;
		for(int i = 0; i < devicesList.size(); i++)
		{
			if(devicesList.get(i).getBluetoothDevice().getAddress().equalsIgnoreCase(btAddress))
			{
				exist = true;
				break;
			}
		}
		return exist;
	}
	
	public void printList()
	{
		String listInfo = "Name		Address		Allowed		Useless		Current		InScope		\n";
		for(int i = 0; i < devicesList.size(); i++)
		{
			listInfo += devicesList.get(i).getBluetoothDevice().getName() + "; " +
				devicesList.get(i).getBluetoothDevice().getAddress() + "; " +
				devicesList.get(i).getIsAllowed() +";" +
				devicesList.get(i).getIsUseless() +";" +
				devicesList.get(i).getIsCurrent() + "; " +
				devicesList.get(i).getIsInScope() + "; \n";
			Log.i("RTDA", listInfo);
		}
	}
	public void setCanvasState(int state)
    {
    	canvasState = state;
    }
	public void setLogFileName()
	{
		int i = 1;
		boolean createFile = false;
		String logSubFileName = sdPath + RTDAReceiver.appRootFolder + "/" + RTDAReceiver.appLogFolder + 
		"/" + time.year + "_" + (time.month + 1) + "_" + time.monthDay;
		String fileName =logSubFileName + "/" + time.year + "_" + (time.month + 1) +
			"_" + time.monthDay + "_(";
		do
		{
			logFile = new File(fileName + i + ").txt");
			if(logFile.exists())
			{
				i ++;
			}
			else
			{
				createFile = true;
			}
		}while(!createFile);
	}
	public String setTimeString(Time time)
	{
		String timeStr = time.year + "-" + (time.month + 1) + "-" + time.monthDay + " " +
			time.hour + ":" + time.minute + ":" + time.second + "(" + time.timezone + ")";
		return timeStr;
	}
	
	/*	Converts InputStream type to String
	 * 	@param	is	InputStream to be converted
	 * 	@return A String of what was converted
	 */
	private StringBuilder inputStreamToString(InputStream is){
        String line = "";
        StringBuilder total = new StringBuilder();
        
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        try{
	        while ((line = rd.readLine()) != null) { 
	            total.append(line); 
	        }
        }catch(IOException e){
        	Log.d(TAG, "onputStreamToString IOE");
        }
        
        return total;
    }
	
	
	/**
	 * @author Olli Anttila
	 *
	 */
	private class SendDataToServer extends AsyncTask<BTEvent, Void, Integer> {
	     protected Integer doInBackground(BTEvent... event) {
	    	 HttpClient httpclient = new DefaultHttpClient();
	         HttpPost httppost = new HttpPost("http://poman.fi/RTDAsaver/test.php");
	         HttpResponse response;

	         
	         
	         try {
	        	 List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        	 nameValuePairs.add(new BasicNameValuePair("name", localAdapter.getName()));
	        	 nameValuePairs.add(new BasicNameValuePair("mobileAddress", localAdapter.getAddress()));
		         nameValuePairs.add(new BasicNameValuePair("beaconAddress", event[0].getBtBeaconAddress()));
		         nameValuePairs.add(new BasicNameValuePair("time", event[0].getTime()));
		         nameValuePairs.add(new BasicNameValuePair("event", event[0].getEvent().toString()));
		         
		         httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		         
	             // Execute HTTP Post Request
	             response = httpclient.execute(httppost);
	             
	             String message = inputStreamToString(response.getEntity().getContent()).toString();
	             int statusCode = response.getStatusLine().getStatusCode();
	             Log.d(TAG, "Status code: " + Integer.toString(statusCode));
	             Log.d(TAG, "Message: " + message);
	             Log.d(TAG, "try part done");

	         } catch (ClientProtocolException e) {
	         	Log.d(TAG, "ClientProtocolException");
	         } catch (IOException e) {
	         	Log.d(TAG, "IOException");
	         	e.printStackTrace();
	         }
			return 10;
	         
	     }

	     protected void onProgressUpdate(Integer... progress) {
	         
	     }

	     protected void onPostExecute(Integer result) {
	    	 Log.i("INFO", "Sending information to server, complete.");
	     }
	 }

}
