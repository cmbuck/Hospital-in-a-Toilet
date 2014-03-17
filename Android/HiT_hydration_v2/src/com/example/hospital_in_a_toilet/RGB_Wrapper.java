package com.example.hospital_in_a_toilet;

public class RGB_Wrapper {
	private float R;
	private float G;
	private float B;
	private String date;
	
	public RGB_Wrapper(float R, float G, float B)
	{
		this.R = R;
		this.G = G;
		this.B = B;
	}
	public float getR() {
		return R;
	}
	public void setR(float r) {
		R = r;
	}
	public float getG() {
		return G;
	}
	public void setG(float g) {
		G = g;
	}
	public float getB() {
		return B;
	}
	public void setB(float b) {
		B = b;
	}
	public void divide(float div)
	{
		this.R /=div;
		this.B /=div;
		this.G /=div;
	}
	public void add(RGB_Wrapper rgb)
	{
		this.R += rgb.getR();
		this.B += rgb.getB();
		this.G += rgb.getG();
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	
	
}
