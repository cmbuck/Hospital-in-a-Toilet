package com.hospitalinatoilet.hit_hydration;

import java.util.ArrayList;

import android.util.Log;

public class RunningAverager {
	private ArrayList<RGB_Wrapper> rgb_vals;
	private int size;

	
	public RunningAverager(int size)
	{
		this.rgb_vals = new ArrayList<RGB_Wrapper>();
		this.size= size;
		
	}
	
	public RGB_Wrapper get_element(RGB_Wrapper aRGB)
	{
		if(rgb_vals.size()<size)
		{
			Log.e("fsd",Integer.toString(rgb_vals.size(),10));
			rgb_vals.add(aRGB);
		}
		else
		{
			Log.e("fsd",Integer.toString(rgb_vals.size(),10));
			rgb_vals.remove(0);
			rgb_vals.add(aRGB);
		}
		return average();
		
	}
	
	public RGB_Wrapper average()
	{
		RGB_Wrapper sum= new RGB_Wrapper(0,0,0);
		for(RGB_Wrapper a: rgb_vals)
		{
			sum.add(a);
		}
		sum.divide(rgb_vals.size());
		return sum;
	}
	

}
