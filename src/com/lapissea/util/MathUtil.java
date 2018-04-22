package com.lapissea.util;

public class MathUtil{
	
	public static final float SQRT2=(float)Math.sqrt(2);
	
	public static double snap(double value, double min, double max){
		if(min>max) return value;
		if(value<min) return min;
		if(value>max) return max;
		return value;
	}
	
	public static float snap(float value, float min, float max){
		if(min>max) return value;
		if(value<min) return min;
		if(value>max) return max;
		return value;
	}
	
	public static int snap(int value, int min, int max){
		if(min>max) return value;
		if(value<min) return min;
		if(value>max) return max;
		return value;
	}
	
	public static int snapToArray(int value, @NotNull Object[] arr){
		return snap(value, 0, arr.length-1);
	}
	
	public static double sq(double var){
		return var*var;
	}
	
	public static int sq(int var){
		return var*var;
	}
	
	public static float sq(float var){
		return var*var;
	}
	
	public static float sqrt(float value){
		return (float)Math.sqrt(value);
	}
	
	public static int max(int i1, int i2, int i3){
		return Math.max(i1, Math.max(i2, i3));
	}
	
	public static int max(int i1, int i2, int i3, int i4){
		return Math.max(i1, max(i2, i3, i4));
	}
	
	public static float max(float i1, float i2, float i3){
		return Math.max(i1, Math.max(i2, i3));
	}
	
	public static float max(float i1, float i2, float i3, float i4){
		return Math.max(i1, max(i2, i3, i4));
	}
	
	public static double lengthSquared(double x, double y){
		return x*x+y*y;
	}
	
	public static double length(double x, double y){
		return Math.sqrt(x*x+y*y);
	}
	
	public static double length(double x, double y, double z){
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	public static double length(double x, double y, double z, double w){
		double lengthSquared=x*x+y*y+z*z+w*w;
		if(lengthSquared==0)return 0;
		
		return Math.sqrt(lengthSquared);
	}
	
	public static float length(float x, float y, float z, float w){
		double lengthSquared=x*x+y*y+z*z+w*w;
		if(lengthSquared==0)return 0;
		return (float)Math.sqrt(lengthSquared);
	}
	
	public static int min(int i1, int i2, int i3){
		return Math.min(i1, Math.min(i2, i3));
	}
	
	public static int min(int i1, int i2, int i3, int i4){
		return Math.min(i1, min(i2, i3, i4));
	}
	
	public static float min(float i1, float i2, float i3){
		return Math.min(i1, Math.min(i2, i3));
	}
	
	public static float min(float i1, float i2, float i3, float i4){
		return Math.min(i1, min(i2, i3, i4));
	}
}
