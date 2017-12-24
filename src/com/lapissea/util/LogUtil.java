package com.lapissea.util;


import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.function.Function;
import java.util.function.IntConsumer;

public class LogUtil{
	
	public static final class __{
		
		private static final long START_TIME=System.currentTimeMillis();
		
		protected static boolean DEBUG_ACTIVE=true, CLICKABLE;
		protected static boolean     DEBUG_INIT;
		protected static boolean     EXTERNAL_INIT;
		protected static IntConsumer EXTERNAL_STREAM_OUT;
		protected static IntConsumer EXTERNAL_STREAM_ERR;
		protected static Runnable    EXTERNAL_CLEAR;
		public static PrintStream OUT=System.out;
		
		public static void INIT(boolean active, boolean clickable, String fileLog){
			CLICKABLE=clickable;
			if(DEBUG_INIT) return;
			DEBUG_INIT=true;
			DEBUG_ACTIVE=active;
			if(!(DEBUG_ACTIVE=active)) return;
			
			Function<String, FileOutputStream> create=name->{
				File f=new File(name).getAbsoluteFile();
				try{
					f.getParentFile().mkdirs();
					return new FileOutputStream(f);
				}catch(Exception e){
					throw UtilL.uncheckedThrow(e);
				}
			};
			
			FileOutputStream fileRawOut=null, fileCsvOut=null;
			if(fileLog!=null){
				fileRawOut=create.apply(fileLog+".log");
				fileCsvOut=create.apply(fileLog+".csv");
				try{
					fileCsvOut.write("Type, Time, Thread, Class, Function, Line, Message\n".getBytes());
				}catch(IOException e){
					throw UtilL.uncheckedThrow(e);
				}
			}
			System.setOut(new PrintStream(new DebugHeaderStream(System.out, "OUT", fileRawOut, fileCsvOut)));
			System.setErr(new PrintStream(new DebugHeaderStream(System.err, "ERR", fileRawOut, fileCsvOut)));
			
			
		}
		
		private static final class DebugHeaderStream extends OutputStream{
			
			final OutputStream child;
			final byte[]       prefix;
			static boolean LAST_CH_ENDL=true;
			private static int MAX_SIZE_POINT, MAX_SIZE_THREAD;
			private static long             MAX_SIZE_TIM;
			private        FileOutputStream fileCsvOut;
			
			public DebugHeaderStream(OutputStream child, String prefix, FileOutputStream fileRawOut, FileOutputStream fileCsvOut){
				if(fileRawOut!=null) this.child=new SplitStream(child, fileRawOut);
				else this.child=child;
				this.prefix=prefix.getBytes();
				this.fileCsvOut=fileCsvOut;
			}
			
			@Override
			public void write(int b) throws IOException{
				if(b=='\r') return;
				
				if(LAST_CH_ENDL){//new line
					LAST_CH_ENDL=false;
					debugHeader(child);
				}
				
				if(b=='\n') LAST_CH_ENDL=true;
				
				child.write((char)b);
				if(fileCsvOut!=null){
					if(b=='"') fileCsvOut.write("\\\"".getBytes());
					else if(b=='\n') fileCsvOut.write("\"\n".getBytes());
					else fileCsvOut.write((char)b);
				}
			}
			
			private void debugHeader(OutputStream stream){
				long tim=System.currentTimeMillis();
				if(MAX_SIZE_TIM<tim-5000){
					MAX_SIZE_TIM=tim;
					MAX_SIZE_POINT=MAX_SIZE_THREAD=0;
				}
				
				StackTraceElement[] trace=Thread.currentThread().getStackTrace();
//				for(int i=0;i<trace.length;i++){
//					StackTraceElement stackTraceElement=trace[i];
//					OUT.println(i+" "+stackTraceElement);
//				}
				
				int    depth =trace.length;
				String module="java.base";
				
				while(!module.equals(trace[--depth].getModuleName())) ;//get out of system.out/err
				depth++;
				
				while(trace[depth].getClassName().equals(LogUtil.class.getName())) depth++;//get out of LogUtil if called called trough it
				
				StackTraceElement stack=trace[depth];
				
				
				String threadName=Thread.currentThread().getName();
				byte[] pointerBytes;
				
				String className=stack.getClassName();
				if(CLICKABLE) pointerBytes=stack.toString().getBytes();
				else{
					pointerBytes=(className.substring(className.lastIndexOf('.')+1)+'.'+stack.getMethodName()+'('+stack.getLineNumber()+')').getBytes();
				}
				
				if(MAX_SIZE_THREAD<threadName.length()){
					MAX_SIZE_THREAD=threadName.length();
					MAX_SIZE_TIM=tim;
				}
				if(MAX_SIZE_POINT<pointerBytes.length){
					MAX_SIZE_POINT=pointerBytes.length;
					MAX_SIZE_TIM=tim;
				}
				
				try{
					
					if(fileCsvOut!=null){
						
						long passed=System.currentTimeMillis()-START_TIME;
						
						int ms,s,min,h;
						
						s=(int)Math.floor(passed/1000D);
						ms=(int)(passed-s*1000);
						min=(int)Math.floor(s/60D);
						s-=min*60;
						h=(int)Math.floor(min/60D);
						min-=h*60;
						
						fileCsvOut.write(prefix);
						fileCsvOut.write(",\"".getBytes());
						fileCsvOut.write((h+":"+min+":"+s+"."+ms).getBytes());
						fileCsvOut.write("\",\"".getBytes());
						fileCsvOut.write(threadName.replace("\"", "\\\"").getBytes());
						fileCsvOut.write("\",".getBytes());
						fileCsvOut.write(className.getBytes());
						fileCsvOut.write(',');
						fileCsvOut.write(stack.getMethodName().getBytes());
						fileCsvOut.write(',');
						fileCsvOut.write(Integer.toString(stack.getLineNumber()).getBytes());
						fileCsvOut.write(",\"".getBytes());
					}
					
//					child.write('[');
//					child.write(prefix);
//					child.write(']');
					stream.write('[');
					stream.write(threadName.getBytes());
					for(int i=0, j=MAX_SIZE_THREAD-threadName.length();i<j;i++){
						stream.write(' ');
					}
					stream.write("] [".getBytes());
					stream.write(pointerBytes);
					for(int i=0, j=MAX_SIZE_POINT-pointerBytes.length;i<j;i++){
						stream.write(' ');
					}
					stream.write("]: ".getBytes());
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
		
		private static class SplitStream extends OutputStream{
			
			private final OutputStream s1, s2;
			
			public SplitStream(OutputStream s1, OutputStream s2){
				this.s1=s1;
				this.s2=s2;
			}
			
			@Override
			public void write(int b) throws IOException{
				s1.write(b);
				s2.write(b);
			}
		}
		
	}
	
	/**
	 * Note that this clears only external window.
	 */
	public static void clear(){
		__.EXTERNAL_CLEAR.run();
	}
	
	//================================================
	public static void print(){
		out("");
	}
	
	public static void println(){
		out("\n");
	}
	
	public static void print(Object obj){
		out(TextUtil.toString(obj));
	}
	
	public static void println(int obj){
		out(obj+"\n");
	}
	
	public static void println(float obj){
		out(obj+"\n");
	}
	
	public static void println(long obj){
		out(obj+"\n");
	}
	
	public static void println(double obj){
		out(obj+"\n");
	}
	
	public static void println(char obj){
		out(obj+"\n");
	}
	
	public static void println(byte obj){
		out(obj+"\n");
	}
	
	public static void println(boolean obj){
		out(obj+"\n");
	}
	
	public static void println(short obj){
		out(obj+"\n");
	}
	
	public static void println(String obj){
		out(obj+"\n");
	}
	
	public static void println(Object obj){
		out(TextUtil.toString(obj)+"\n");
	}
	
	public static void print(Object... objs){
		out(TextUtil.toString(objs));
	}
	
	public static void println(Object... objs){
		out(TextUtil.toString(objs)+"\n");
	}
	
	//================================================
	
	public static void printlnEr(){
		err("\n");
	}
	
	public static void printEr(){
		err("");
	}
	
	public static void printEr(Object obj){
		err(TextUtil.toString(obj));
	}
	
	public static void printlnEr(Object obj){
		err(TextUtil.toString(obj)+"\n");
	}
	
	public static void printEr(Object... objs){
		err(TextUtil.toString(objs));
	}
	
	public static void printlnEr(Object... objs){
		err(TextUtil.toString(objs)+"\n");
	}
	
	//================================================
	
	public static void printFunctionTrace(int count, CharSequence splitter){
		println(getFunctionTrace(count, splitter));
	}
	
	public static String getFunctionTrace(int count, CharSequence splitter){
		StringBuilder line=new StringBuilder();
		
		StackTraceElement[] trace=Thread.currentThread().getStackTrace();
		if(count>=trace.length) count=trace.length-1;
		for(int i=count+1;i>=2;i--){
			line.append(trace[i].getMethodName()).append('(').append(trace[i].getLineNumber()).append(')');
			if(i!=2) line.append(splitter);
		}
		return line.toString();
	}
	
	/**
	 * print fancy stuff and things
	 */
	public static void printWrappedEr(Object obj){
		printlnEr(TextUtil.wrappedString(obj));
	}
	
	/**
	 * print fancy stuff and things
	 */
	public static void printWrapped(Object obj){
		println(TextUtil.wrappedString(obj));
	}
	
	public static <T> T printlnAndReturn(T obj){
		println(obj);
		return obj;
	}
	
	public static void printStackTrace(String msg){
		printStackTrace(msg, Thread.currentThread().getStackTrace());
	}
	
	public static void printStackTrace(String msg, StackTraceElement[] a1){
		StringBuilder result=new StringBuilder();
		
		if(msg==null){
			DateFormat dateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Calendar   cal       =Calendar.getInstance();
			result.append("Invoke time: ").append(dateFormat.format(cal.getTime())).append("\n");
		}else result.append(msg).append("\n");
		
		int length=0;
		for(int i=2;i<a1.length;i++){
			StackTraceElement a=a1[i];
			String            s=a.toString();
			result.append(s).append("\n");
			length=Math.max(s.length(), length);
		}
		for(int b=0;b<=length/4;b++)
			result.append("_/\\_");
		
		printlnEr(result);
	}
	
	//================================================
	
	private static void out(String s){
//		if(__.DEBUG_ACTIVE&&!__.DEBUG_INIT) System.err.println("LOG UTILITY DID NOT INJECT DEBUG HEADER!");
		System.out.print(s);
	}
	
	private static void err(String s){
//		if(__.DEBUG_ACTIVE&&!__.DEBUG_INIT) System.err.println("LOG UTILITY DID NOT INJECT DEBUG HEADER!");
		System.err.print(s);
	}
	
}
