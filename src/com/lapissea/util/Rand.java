package com.lapissea.util;

import java.util.Random;

public class Rand{
	
	private static final Random RAND=new Random();
	
	public static boolean b(){
		return RAND.nextBoolean();
	}
	
	public static int i(int min, int max){
		return min+i(max-min);
	}
	
	public static int i(int max){
		return RAND.nextInt(max);
	}
	
	public static float f(){
		return RAND.nextFloat();
	}
	
}
