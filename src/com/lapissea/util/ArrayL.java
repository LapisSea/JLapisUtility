package com.lapissea.util;

public class ArrayL{
	
	public static int[] max(int[] array, int i, int val){
		if(array[i]<val) array[i]=val;
		return array;
	}
	
	public static int[] max(int[] array, int i, int val1, int val2){
		max(array, i, val1);
		max(array, i+1, val2);
		return array;
	}
	
}
