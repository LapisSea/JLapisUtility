package com.lapissea.util;

public class NanoTimer{
	
	private long   start;
	@NotNull
	private long[] data=new long[100];
	private int    pos =99, count;
	private boolean started=false;
	
	public void start(){
		started=true;
		start=now();
	}
	
	public void end(){
		long t=now();
		if(!started) throw new IllegalStateException("Not started");
		pos=(pos+1)%100;
		data[pos]=t-start;
		if(count<100) count++;
		
	}
	
	public long nsAvrg100(){
		if(count==0) return -1;
		long sum=0;
		for(int i=0;i<count;i++){
			sum+=data[i];
		}
		return sum/count;
	}
	
	public double msAvrg100(){
		if(count==0) return -1;
		return toMs(nsAvrg100());
	}
	
	public double sAvrg100(){
		if(count==0) return -1;
		return toS(nsAvrg100());
	}
	
	public long ns(){
		return data[pos];
	}
	
	public double ms(){
		return toMs(ns());
	}
	
	public double s(){
		return toS(ns());
	}
	
	private static long now(){
		return System.nanoTime();
	}
	
	private static double toMs(long nano){
		return Math.round(nano/1000D)/1000D;
	}
	
	private static double toS(long nano){
		return Math.round(nano/10000000D)/100D;
	}
	
}
