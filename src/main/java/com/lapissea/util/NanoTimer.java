package com.lapissea.util;

import java.util.ArrayList;
import java.util.List;

public interface NanoTimer{
	
	class Staged implements NanoTimer{
		
		private final List<Stage> stages  = new ArrayList<>();
		private       long        start;
		private       long        total;
		private       boolean     started = false;
		
		private static class Stage{
			private final String name;
			private final long   time;
			private Stage(String name, long time){
				this.name = name;
				this.time = time;
			}
		}
		
		private long   lastStageTime;
		private String lastStage;
		
		public Staged(){
			this("<Start>");
		}
		public Staged(String firstStageName){
			lastStage = firstStageName;
		}
		
		public void stage(String nextStageName){
			long t = now();
			stages.add(new Stage(nextStageName, t));
			lastStage = nextStageName;
		}
		
		@Override
		public void start(){
			started = true;
			lastStageTime = start = now();
			stages.add(new Stage(lastStage, start));
		}
		
		@Override
		public void end(){
			long t = now();
			if(!started) throw new IllegalStateException("Not started");
			total = t - start;
			stages.add(new Stage("<End>", t));
		}
		
		
		@Override
		public long ns(){
			return total;
		}
		
		@Override
		public double ms(){
			return toMs(ns());
		}
		
		@Override
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
		
		
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append("Total: ").append(toMs(total)).append(", ");
			long lastT = 0;
			for(Stage stage : stages){
				if(sb.length() == 0){
					sb.append(stage.name);
				}else{
					sb.append(" > ").append(toMs(stage.time - lastT)).append(" > ");
				}
				lastT = stage.time;
			}
			
			return sb.toString();
		}
	}
	
	class Avg implements NanoTimer{
		
		@NotNull
		private final long[] data;
		private       long   start;
		private       int    pos = 99, count;
		private boolean started = false;
		
		public Avg(){
			this(100);
		}
		public Avg(int buffSize){
			data = new long[buffSize];
		}
		
		@Override
		public void start(){
			started = true;
			start = now();
		}
		
		@Override
		public void end(){
			long t = now();
			if(!started) throw new IllegalStateException("Not started");
			pos = (pos + 1)%data.length;
			data[pos] = t - start;
			if(count<data.length) count++;
			
		}
		
		public long nsAvrg100(){
			if(count == 0) return -1;
			long sum = 0;
			for(int i = 0; i<count; i++){
				sum += data[i];
			}
			return sum/count;
		}
		
		public double msAvrg100(){
			if(count == 0) return -1;
			return toMs(nsAvrg100());
		}
		
		public double sAvrg100(){
			if(count == 0) return -1;
			return toS(nsAvrg100());
		}
		
		@Override
		public long ns(){
			return data[pos];
		}
		
		@Override
		public double ms(){
			return toMs(ns());
		}
		
		@Override
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
	
	class Simple implements NanoTimer{
		
		private long    last;
		private long    start;
		private boolean started = false;
		
		public Simple(){
		}
		
		@Override
		public void start(){
			started = true;
			start = now();
		}
		
		@Override
		public void end(){
			long t = now();
			if(!started) throw new IllegalStateException("Not started");
			last = t - start;
			
		}
		
		@Override
		public long ns(){
			return last;
		}
		
		@Override
		public double ms(){
			return toMs(ns());
		}
		
		@Override
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
		
		@Override
		public String toString(){
			return "{" + ms() + "}";
		}
	}
	
	
	void start();
	
	void end();
	
	long ns();
	
	double ms();
	
	double s();
	
}
