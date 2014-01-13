package fi.tol.android.RTDAReceiver;

import android.bluetooth.BluetoothDevice;

public class DeviceItem 
{
	private int btDeviceNo;
	private BluetoothDevice btDevice;
	private boolean isAllowedDevice;
	private boolean isUselessDevice;
	private boolean isCurrentDevice;
	private String enter_time;
	private String leave_time;
    private boolean inScope;
    private int btMajorDeviceClass;
    
    public void setBTdeviceNo(int no)
    {
    	btDeviceNo = no;
    }
    public void setBluetoothDevice(BluetoothDevice device)
    {
    	btDevice = device;
    }
    public void setIsUseless(boolean isUseless)
    {
    	isUselessDevice = isUseless;
    }
    public void setIsAllowed(boolean isAllowed)
    {
    	isAllowedDevice = isAllowed;
    }
    public void setIsCurrent(boolean isCurrent)
    {
    	isCurrentDevice = isCurrent;
    }
    public void setIsInScope(boolean isInScope)
    {
    	inScope = isInScope;
    }
    public void setEnter_time(String eTime)
    {
    	enter_time = eTime;
    }
    public void setLeave_time(String lTime)
    {
    	leave_time = lTime;
    }
    public void setBTMajorDeviceClass(int majorClass)
    {
    	btMajorDeviceClass = majorClass;
    }
    
    public int getBTDeviceNo()
    {
    	return btDeviceNo;
    }
    public BluetoothDevice getBluetoothDevice()
    {
    	return btDevice;
    }
    public boolean getIsUseless()
    {
    	return isUselessDevice;
    }
    public boolean getIsAllowed()
    {
    	return isAllowedDevice;
    }
    public boolean getIsCurrent()
    {
    	return isCurrentDevice;
    }
    public boolean getIsInScope()
    {
    	return inScope;
    }
    public String getEnter_time()
    {
    	return enter_time;
    }
    public String getLeave_time()
    {
    	return leave_time;
    }
    public int getBTMajorDeviceClass()
    {
    	return btMajorDeviceClass;
    }
}
