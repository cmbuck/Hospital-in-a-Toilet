package com.example.hospital_in_a_toilet111;


import java.io.IOException;
import java.io.InputStream;
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
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LiveSectionFragment extends Fragment implements OnClickListener {
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	public static final String ARG_SECTION_NUMBER = "section_number";
	private static final int REQUEST_ENABLE_BT = 10;
	private EditText statusLabel;

	private TextView status_label;
	private static final String MAC_ADDRESS ="00:06:66:4E:3E:8D";
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    private InputStream mmInputStream;
    private RunningAverager rgb_hist;
    private TextView date_label;	
    private TextView recommendation;
    private ArrayList<RGB_Wrapper> user_history;
	private Button start_btn;
	private Button stop_btn;
	private boolean is_stopped = true;
    private static RGB_Wrapper last_collected;
    
    /*RECOMMENDATION*/
	private String rec_OK = "Doing Ok! You're probably well hydrated. Drink water as normal.";
	private String rec_FINE = "You're just fine. You could stand to drink a little water now, maybe a small glass of water.";
	private String rec_DRINK1 = "Drink about 1/2 bottle of water (1/4 liter) within the hour, or drink a whole bottle (1/2 liter) of water if you're outside " +
			"and/or sweating.";
	private String rec_DRINK2 = "Drink about 1/2 bottle of water (1/4 liter) right now, or drink a whole bottle (1/2 liter)" +
			"of water if you're outside and/or sweating.";
	private String rec_DRINK3 = "Drink 2 bottles of water right now (1 liter). If your urine is darker than this and/or red " +
			"or brown, then dehydration may not be your problem. See a doctor.";
	private String rec_ZOMG = "You need to DRINK A LOT OF WATER. NOW.";
	private String[] recommendations = {rec_ZOMG, rec_DRINK3, rec_DRINK2, rec_DRINK1, rec_FINE, rec_OK}; 


	public LiveSectionFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_live,
				container, false);
		user_history = new ArrayList<RGB_Wrapper>();
		status_label = (TextView) rootView.findViewById(R.id.statusLabel);
		recommendation = (TextView) rootView.findViewById(R.id.recommendation);
		start_btn = (Button) rootView.findViewById(R.id.button1);
		start_btn.setOnClickListener(this);
		stop_btn = (Button) rootView.findViewById(R.id.button2);
		stop_btn.setOnClickListener(this);
		last_collected = new RGB_Wrapper(0,0,0);
		date_label = (TextView) rootView.findViewById(R.id.dateLabel);
		rgb_hist = new RunningAverager(5);	
		return rootView;
	}

    
	private void connect_to_btDevice() throws IOException
	{
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    Log.e("No BT on device", "No BT on device");
		}
		Log.e("LOL","what");
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
		if (mBluetoothAdapter.isDiscovering()) {
			Log.e("Already Discovering...", "Already Discovering...");
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
                                              	if(!is_stopped){
	                                        		date_label.setText("Live/Capturing");
	                                            	status_label.setText("Rx-ing..." + data);
	//	                                            String[] rgb = data.split(",");
		                                            Log.e("DATA",data);
	//	                                            try
	//		                                        {
	//	                                            	Float r = Float.parseFloat(rgb[0].toString());
	//		                                            Float g= Float.parseFloat(rgb[1].toString());
	//		                                            Float b= Float.parseFloat(rgb[2].toString());
	//		                                            RGB_Wrapper rgb_wrapped = new RGB_Wrapper(r,g,b);
	//		                                            RGB_Wrapper rgb_result = rgb_hist.get_element(rgb_wrapped);
	//		                                           // changeColor(rgb_result);
	//		                                        
	//		                                            last_collected = rgb_result;
	//	                                            }
	//	                                            catch (NumberFormatException nfe)
	//	                                            {
	//	                                            	Log.e("EXCEPTION", "exception caught");
	//	                                            }
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
		getActivity().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
	}

	@Override
	public void onClick(View v) {
		if(this.start_btn == v)
		{
			Log.e("Abhi","start click");
			this.is_stopped  = false;
			status_label.setText("");
			date_label.setText("Live");
			this.recommendation.setText("Recommendation: Still recording");
			this.recommendation.setText("");
			try {
				stopWorker = false;
				connect_to_btDevice();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		else if(this.stop_btn == v)
		{
			Log.e("Abhi","stop clk");
			this.is_stopped = true;
			if(last_collected.getR() == 0 && last_collected.getG() == 0 && last_collected.getB() == 0)
			{
				this.date_label.setText("Unsuccessful capture; Try Again");
				this.status_label.setText("Rx-ed from device");
			}
			else
			{
				this.date_label.setText("Successfully captured");
				this.status_label.setText("Rx-ed from device");
				Integer rec_id = get_recommendation(last_collected.getR(),last_collected.getG(),last_collected.getB());
				this.recommendation.setText("Hydration Status (Scale 0-5): " + rec_id.toString()+"\n Recommendation:\n" + recommendations[rec_id]);
			}	
			this.workerThread.interrupt();
		}		
	}
	
	private Integer get_recommendation(float r, float g, float b)
	{
		float bright = (r + g + b) /3;
		if(bright>173)
		{
			return 5;
		}
		if(bright>155)
		{
			return 4;
		}
		if(bright>121)
		{
			return 3;
		}
		if(bright>101)
		{
			return 2;
		}
		if(bright>50)
		{
			return 1;
		}
		return 0;
		
	}



}
