package com.lapissea.util;

import java.util.SplittableRandom;

public class Rand{
	
	private static final SplittableRandom RAND=new SplittableRandom();
	
	
	public static boolean b(){
		return RAND.nextBoolean();
	}
	
	public static boolean b(float chance){
		return !(chance==0)&&(chance==1||f()<chance);
	}
	
	@NotNull
	public static boolean[] bs(int count){
		boolean[] arr=new boolean[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=b();
		}
		return arr;
	}
	
	@NotNull
	public static boolean[] bs(int count, float chance){
		boolean[] arr=new boolean[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=b(chance);
		}
		return arr;
	}
	
	
	public static float f(){
		return (RAND.nextInt()>>>8)*0x1.0p-24F;
	}
	
	public static float f(float scale){
		return f()*scale;
	}
	
	@NotNull
	public static float[] fs(int count){
		float[] arr=new float[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=f();
		}
		return arr;
	}
	
	@NotNull
	public static float[] fs(int count, float chance){
		float[] arr=new float[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=f(chance);
		}
		return arr;
	}
	
	
	public static double d(){
		return RAND.nextDouble();
	}
	
	public static double d(double scale){
		return d()*scale;
	}
	
	@NotNull
	public static double[] ds(int count){
		double[] arr=new double[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=d();
		}
		return arr;
	}
	
	@NotNull
	public static double[] ds(int count, double chance){
		double[] arr=new double[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=d(chance);
		}
		return arr;
	}
	
	
	public static int i(int max){
		return RAND.nextInt(max);
	}
	
	public static int i(int min, int max){
		return min+i(max-min);
	}
	
	public static short i(short max){
		return (short)i((int)max);
	}
	
	public static short i(short min, short max){
		return (short)(min+i(max-min));
	}
	
	public static byte i(byte max){
		return (byte)i((int)max);
	}
	
	public static byte i(byte min, byte max){
		return (byte)(min+i(max-min));
	}
	
	@NotNull
	public static int[] is(int count, int max){
		int[] arr=new int[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=i(max);
		}
		return arr;
	}
	
	@NotNull
	public static int[] is(int count, int min, int max){
		int[] arr=new int[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=i(min, max);
		}
		return arr;
	}
	
	@NotNull
	public static short[] is(int count, short max){
		short[] arr=new short[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=i(max);
		}
		return arr;
	}
	
	@NotNull
	public static short[] is(int count, short min, short max){
		short[] arr=new short[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=i(min, max);
		}
		return arr;
	}
	
	@NotNull
	public static byte[] is(int count, byte max){
		byte[] arr=new byte[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=i(max);
		}
		return arr;
	}
	
	@NotNull
	public static byte[] is(int count, byte min, byte max){
		byte[] arr=new byte[count];
		for(int i=0;i<arr.length;i++){
			arr[i]=i(min, max);
		}
		return arr;
	}
	
}
