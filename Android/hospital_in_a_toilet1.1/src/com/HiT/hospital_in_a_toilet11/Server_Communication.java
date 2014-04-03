package com.HiT.hospital_in_a_toilet11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.SeekBar;

import com.HiT.hospital_in_a_toilet11.RGB_Wrapper;


/* Implements Server Communications */


public class Server_Communication {
	
    private ArrayList<RGB_Wrapper> user_history;
	private SeekBar seekBar;
    private String base_url = "http://ec2-54-237-70-7.compute-1.amazonaws.com/";
    private String android_id;
	
    public Server_Communication(String android_id){
    	this.android_id = android_id;
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
	
	public void register_on_server()
	{
		String request = base_url + "new_user.php?username=" + android_id + "&weight=60";
		connect_to_server(request, false);
	}

	public void send_to_server(RGB_Wrapper rgb)
	{
		String request = base_url + "receive_data.php?username=" + android_id +
				"&R=" + rgb.getR() + "&G=" +rgb.getG() + "&B=" +rgb.getB();
		connect_to_server(request, false);
	}
	
	public void receive_from_server()
	{
		String request = base_url + "get_data.php?username=" + android_id +
				"&number=5";
		connect_to_server(request,true);
	}

	
}
