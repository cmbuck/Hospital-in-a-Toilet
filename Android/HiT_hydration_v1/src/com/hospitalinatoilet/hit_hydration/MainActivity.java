package com.hospitalinatoilet.hit_hydration;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;





import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ColorMatrixColorFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AnalogClock;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class MainActivity extends Activity implements OnClickListener, OnSeekBarChangeListener{
	
	private static final int REQUEST_ENABLE_BT = 10;
	private EditText statusLabel;

	private ImageView imageView;

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
    
    private String base_url = "http://ec2-54-237-70-7.compute-1.amazonaws.com/";
    private ArrayList<RGB_Wrapper> user_history;
    private static RGB_Wrapper last_collected;
	private SeekBar seekBar;
	
	private TextView recommendation;
	
	/*RECOMMENDATION*/
	private String rec_OK = "Doing Ok! You're probably well hydrated. Drink water as normal.";
	private String rec_FINE = "You're just fine. You could stand to drink a little water now, maybe a small glass of water.";
	private String rec_DRINK1 = "Drink about 1/2 bottle of water (1/4 liter) within the hour, or drink a whole bottle (1/2 liter) of water if you're outside " +
			"and/or sweating.";
	private String rec_DRINK2 = "Drink about 1/2 bottle of water (1/4 liter) right now, or drink a whole bottle (1/2 liter)" +
			"of water if you're outside and/or sweating.";
	private String rec_DRINK3 = "Drink 2 bottles of water right now (1 liter). If your urine is darker than this and/or red " +
			"or brown, then dehydration may not be your problem. See a doctor.";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		imageView = (ImageView) findViewById(R.id.imageView1);		
		seekBar = (SeekBar) findViewById(R.id.seekBar1);
		seekBar.setMax(1);
		seekBar.setOnSeekBarChangeListener(this);
		user_history = new ArrayList<RGB_Wrapper>();
		status_label = (TextView) findViewById(R.id.statusLabel);
		date_label = (TextView) findViewById(R.id.dateLabel);
		recommendation = (TextView) findViewById(R.id.recommendation);
		rgb_hist = new RunningAverager(5);
		try {
			connect_to_btDevice();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		register_on_server();
		receive_from_server();	
		
	}
	
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
	private void changeColor(RGB_Wrapper rgb)
	{
		float d = rgb.getR();
		float e = rgb.getG();
		float f = rgb.getB();
		float[] filter_float = new float[]{d/255f, 0, 0, 0, 0,   0, e/255f, 0, 0, 0,    0, 0, f/255f, 0, 0,      0, 0, 0, 1f, 0}; 
		ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(filter_float);
		imageView.setColorFilter(colorFilter);
		
	}

	public void register_on_server()
	{
		String android_id = Secure.getString(getBaseContext().getContentResolver(),
                Secure.ANDROID_ID); 
		String request = base_url + "new_user.php?username=" + android_id + "&weight=60";
		connect_to_server(request, false);
	}

	public void send_to_server(RGB_Wrapper rgb)
	{
		String android_id = Secure.getString(getBaseContext().getContentResolver(),
                Secure.ANDROID_ID); 
		String request = base_url + "receive_data.php?username=" + android_id +
				"&R=" + rgb.getR() + "&G=" +rgb.getG() + "&B=" +rgb.getB();
		connect_to_server(request, false);
	}
	
	public void receive_from_server()
	{
		String android_id = Secure.getString(getBaseContext().getContentResolver(),
                Secure.ANDROID_ID); 
		String request = base_url + "get_data.php?username=" + android_id +
				"&number=5";
		connect_to_server(request,true);
	}
	
	public void connect_to_server(final String request, final boolean toAdd) {
		Thread trd = new Thread(new Runnable() {
			@Override
			public void run() {
				
				HttpClient httpClient = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(request);

				// Making HTTP Request
				try {
					HttpResponse response = httpClient.execute(httpGet);

					// writing response to log
					InputStream in = response.getEntity().getContent();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(in));
					StringBuilder str = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						str.append(line);
					}
					in.close();

					Log.d("HTML", str.toString());
					if(toAdd){
					try {
						final JSONObject json = new JSONObject(str.toString());
						
						for(Integer i=0;i<json.length();i++)
						{
							String res = json.getString(i.toString());
							final JSONObject json_2 = new JSONObject(res);
							String R = json_2.getString("R");
							String G = json_2.getString("G");
							String B = json_2.getString("B");
							String SENT_TIME = json_2.getString("SENT_TIME");
							Log.e("JSON", R + G + B +SENT_TIME);
							RGB_Wrapper rgb = new RGB_Wrapper(Float.parseFloat(R),Float.parseFloat(G),Float.parseFloat(B));
							rgb.setDate(SENT_TIME);
							user_history.add(rgb);
							
						}
						seekBar.setMax(user_history.size());
						
						Log.d("SIZE",String.valueOf(seekBar.getMax()));
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					}

				} catch (ClientProtocolException e) {
					// writing exception to log
					e.printStackTrace();

				} catch (IOException e) {
					// writing exception to log
					e.printStackTrace();
				}
			}
		});
		trd.start();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case R.id.dialog_send:
			DialogFragment fragment = LoginDialog.newInstance();
			fragment.show(getFragmentManager().beginTransaction(), "dialog");
			return true;
		}
		return false;
	}
	@Override
	public void onClick(View v) {
		Log.e("Abhi","btn clk");
//		changeColor(new RGB_Wrapper(Float.parseFloat(red.getText().toString()), Float.parseFloat(green.getText().toString()), 
//				Float.parseFloat(blue.getText().toString())));
		
	}
	
	public static class LoginDialog extends DialogFragment {
		public static LoginDialog newInstance() {
			LoginDialog dialog = new LoginDialog();
			return dialog;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			// Get the layout inflater
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View inflatedView = inflater.inflate(R.layout.dialog_submit, null);
			
			
			builder.setView(inflatedView);

			builder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							((MainActivity) getActivity()).send_to_server(last_collected);
							((MainActivity) getActivity()).recommendation.setText("Recommendation:\n" + ((MainActivity) getActivity()).get_recommendation(last_collected.getR(),last_collected.getG(),last_collected.getB()));
						}
					});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							
						}
					});
			return builder.create();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if(progress == 0)
		{
			status_label.setText("Receiving...");
			date_label.setText("Live");
			this.recommendation.setText("Recommendation: Still recording");
		}
		else if(user_history.size()>0)
		{
			RGB_Wrapper rgb = user_history.get(progress - 1);
			Date dt = new Date();
			try {
				dt = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss", Locale.ENGLISH).parse(rgb.getDate());
				long diff = ((new Date().getTime() - dt.getTime())/(1000*60*60)) % 24;
				date_label.setText(String.valueOf(diff) + " hour(s) ago");
				date_label.append("\n" + rgb.getDate());
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			status_label.setText("Rx-ed From Server");
			recommendation.setText("Recommendation:\n" + get_recommendation(rgb.getR(),rgb.getG(),rgb.getB()));
			changeColor(rgb);
		}
			
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	
	private String get_recommendation(float r, float g, float b)
	{
		float bright = (r + g + b) /3;
		if(bright>173)
		{
			return rec_OK;
		}
		if(bright>155)
		{
			return rec_FINE;
		}
		if(bright>121)
		{
			return rec_DRINK1;
		}
		if(bright>101)
		{
			return rec_DRINK2;
		}
		if(bright>50)
		{
			return rec_DRINK3;
		}
		return "You need to DRINK A LOT OF WATER. NOW.";
		
	}
	
	

}



