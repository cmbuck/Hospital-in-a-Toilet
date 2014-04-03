package com.HiT.hospital_in_a_toilet11;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import com.HiT.hospital_in_a_toilet11.RGB_Wrapper;
import com.HiT.hospital_in_a_toilet11.RunningAverager;



public class Bluetooth {

	private static final int REQUEST_ENABLE_BT = 10;
	private TextView status_label;
	private static final String MAC_ADDRESS ="00:06:66:4E:3E:8D";
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    private InputStream mmInputStream;
    private OutputStream mmOutputStream;
    private RunningAverager rgb_hist;
    private TextView date_label;	
	
    private ArrayList<RGB_Wrapper> user_history;
    private static RGB_Wrapper last_collected;
	private SeekBar seekBar;
    private String base_url = "http://ec2-54-237-70-7.compute-1.amazonaws.com/";

    
	private void connect_to_btDevice() throws IOException
	{
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    Log.e("No BT on device", "hehe");
		}
		Log.e("LOL","what");
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
		if (mBluetoothAdapter.isDiscovering()) {
			Log.e("what", "Already Discovering...");
		    mBluetoothAdapter.cancelDiscovery();
		}
		boolean is_paired = false;
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		BluetoothDevice bt_device = null;
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        if(device.getAddress().equals(MAC_ADDRESS))
		        	{
		        		bt_device = device;
		        		is_paired = true;
		        		break;
		        	}
		        
		    }
		}
		
		if(is_paired)
		{
			//Start connection
			Log.e("Abhi", "Is Paired!");
			UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
	        BluetoothSocket mmSocket = bt_device.createRfcommSocketToServiceRecord(uuid);
	        mmSocket.connect();
	        mmOutputStream = mmSocket.getOutputStream();
	        mmInputStream = mmSocket.getInputStream();
	        beginListenForData();
			
		}
	}
	
	void beginListenForData()
    {
        final Handler handler = new Handler(); 
        final byte delimiter = 10; //This is the ASCII code for a newline character
        
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {                
               while(!Thread.currentThread().isInterrupted() && !stopWorker)
               {
                    try 
                    {
                        int bytesAvailable = mmInputStream.available();                        
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            if(seekBar.getProgress() == 0)
	                                        {
                                            	date_label.setText("Live/Capturing");
                                            	status_label.setText("Rx-ing..." + data);
	                                            String[] rgb = data.split(",");
	                                            Log.e("DATA",data);
	                                            try
		                                        {
	                                            	Float r = Float.parseFloat(rgb[0].toString());
		                                            Float g= Float.parseFloat(rgb[1].toString());
		                                            Float b= Float.parseFloat(rgb[2].toString());
		                                            RGB_Wrapper rgb_wrapped = new RGB_Wrapper(r,g,b);
		                                            RGB_Wrapper rgb_result = rgb_hist.get_element(rgb_wrapped);
		                                            changeColor(rgb_result);
		                                        
		                                            last_collected = rgb_result;
	                                            }
	                                            catch (NumberFormatException nfe)
	                                            {
	                                            	Log.e("EXCEPTION", "exception caught");
	                                            }
	                                            
                                            }
                                            
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } 
                    catch (IOException ex) 
                    {
                        stopWorker = true;
                    }
               }
            }
        });

        workerThread.start();
    }
    	
	private void discover_bt(BluetoothAdapter mBluetoothAdapter)
	{
		boolean is_discovering = mBluetoothAdapter.startDiscovery();
		if(is_discovering)
		{
			statusLabel.append("Discovering...");
		}

		// Create a BroadcastReceiver for ACTION_FOUND
		final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		    public void onReceive(Context context, Intent intent) {
		        String action = intent.getAction();
		        // When discovery finds a device
		        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		            // Get the BluetoothDevice object from the Intent
		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		            // Add the name and address to an array adapter to show in a ListView
		            Log.e("Device",device.getName() + "\n" + device.getAddress());
		            
		        }
		    }
		};
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
	}

	
}
