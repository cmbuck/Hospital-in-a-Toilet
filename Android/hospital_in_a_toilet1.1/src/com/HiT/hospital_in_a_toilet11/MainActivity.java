package com.HiT.hospital_in_a_toilet11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.HiT.hospital_in_a_toilet11.RGB_Wrapper;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;

public class MainActivity extends ActionBarActivity implements
		ActionBar.TabListener {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a {@link FragmentPagerAdapter}
	 * derivative, which will keep every loaded fragment in memory. If this
	 * becomes too memory intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

    private ArrayList<RGB_Wrapper> user_history;
    private static RGB_Wrapper last_collected;
	private SeekBar seekBar;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			if(position == 0)
			{
				Fragment fragment = new TrendsSectionFragment();
				Bundle args = new Bundle();
				args.putInt(TrendsSectionFragment.ARG_SECTION_NUMBER, position + 1);
				fragment.setArguments(args);
				return fragment;
			}
			else
			{
				Fragment fragment = new LiveSectionFragment();
				Bundle args = new Bundle();
				args.putInt(LiveSectionFragment.ARG_SECTION_NUMBER, position + 1);
				fragment.setArguments(args);
				return fragment;
			}
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return new String("Trends");
			case 1:
				return new String("Live");
			}
			return null;
		}
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
	

}
