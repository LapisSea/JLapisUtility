package com.lapissea.util;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.function.IntConsumer;

public class LogUtil{
	
	public static final class __{
		
		protected static boolean DEBUG_ACTIVE=true;
		protected static boolean     DEBUG_INIT;
		protected static boolean     EXTERNAL_INIT;
		protected static IntConsumer EXTERNAL_STREAM_OUT;
		protected static IntConsumer EXTERNAL_STREAM_ERR;
		protected static Runnable    EXTERNAL_CLEAR;
		public static PrintStream OUT=System.out;
		
		public static void INJECT_DEBUG_PRINT(boolean active){
			if(DEBUG_INIT) return;
			DEBUG_INIT=true;
			DEBUG_ACTIVE=active;
			if(!(DEBUG_ACTIVE=active)) return;
			
			System.setOut(new PrintStream(new DebugHeaderStream(System.out, "OUT")));
			System.setErr(new PrintStream(new DebugHeaderStream(System.err, "ERR")));
		}
		
		private static final class DebugHeaderStream extends OutputStream{
			
			final OutputStream child;
			final byte[]       prefix;
			static boolean LAST_CH_ENDL=true;
			
			public DebugHeaderStream(OutputStream child, String prefix){
				this.child=child;
				this.prefix=("["+prefix+"]").getBytes();
			}
			
			@Override
			public void write(int b) throws IOException{
				if(b=='\r') return;
				
				if(LAST_CH_ENDL){
					LAST_CH_ENDL=false;
					child.write(prefix);
					debugHeader(child);
				}
				
				if(b=='\n') LAST_CH_ENDL=true;
				
				child.write((char)b);
			}
			
			private static void debugHeader(OutputStream stream){
				StackTraceElement[] trace=Thread.currentThread().getStackTrace();
//				for(StackTraceElement stackTraceElement:trace){
//					OUT.println(stackTraceElement.getClassName());
//				}
				int    depth=trace.length;
				String name;
				while(!(name=trace[--depth].getClassName())
					       .equals(PrintStream.class.getName())&&
				      !name.startsWith(LogUtil.class.getName())&&
				      !name.startsWith(ArrayList.class.getName())){
//					OUT.println(depth+" "+name);
				}
				depth++;

//				OUT.println();
				StackTraceElement stack    =trace[depth];
				String            className=stack.getClassName();
				try{
					stream.write('[');
					stream.write(Thread.currentThread().getName().getBytes());
					stream.write("] [".getBytes());
					stream.write(className.substring(className.lastIndexOf('.')+1).getBytes());
					stream.write(':');
					stream.write(Integer.toString(stack.getLineNumber()).getBytes());
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
		
		public static void INJECT_FILE_LOG(String file){
			try{
				File             logFile=new File(file);
				FileOutputStream fileOut=new FileOutputStream(logFile);
				
				if(logFile.exists()) logFile.delete();
				else{
					logFile.getParentFile().mkdirs();
					logFile.createNewFile();
				}
				System.setErr(new PrintStream(new SplitStream(System.err, fileOut)));
				System.setOut(new PrintStream(new SplitStream(System.out, fileOut)));
				
			}catch(Exception e){
				e.printStackTrace();
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
