package com.lapissea.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.function.IntConsumer;

public class LogUtil{
	
	public static final class __{
		
		protected static boolean		DEBUG_ACTIVE=true;
		protected static boolean		DEBUG_INIT;
		protected static boolean		EXTERNAL_INIT;
		protected static IntConsumer	EXTERNAL_STREAM_OUT;
		protected static IntConsumer	EXTERNAL_STREAM_ERR;
		protected static Runnable		EXTERNAL_CLEAR;
		public static PrintStream		OUT			=System.out;
		
		public static void INJECT_DEBUG_PRINT(boolean active){
			if(DEBUG_INIT) return;
			DEBUG_INIT=true;
			DEBUG_ACTIVE=active;
			if(!(DEBUG_ACTIVE=active)) return;
			
			System.setOut(new PrintStream(new DebugHeaderStream(System.out)));
			System.setErr(new PrintStream(new DebugHeaderStream(System.err)));
		}
		
		private static final class DebugHeaderStream extends OutputStream{
			
			final OutputStream child;
			
			static boolean LAST_CH_ENDL=true;
			
			public DebugHeaderStream(OutputStream child){
				this.child=child;
			}
			
			@Override
			public void write(int b) throws IOException{
				
				if(LAST_CH_ENDL){
					LAST_CH_ENDL=false;
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
				int depth=trace.length;
				String name;
				while(!(name=trace[--depth].getClassName())
						.equals(PrintStream.class.getName())&&
						!name.startsWith(LogUtil.class.getName())&&
						!name.startsWith(ArrayList.class.getName())
						){
//					OUT.println(depth+" "+name);
				}
				depth++;
				
//				OUT.println();
				StackTraceElement stack=trace[depth];
				String className=stack.getClassName();
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
			
			private final OutputStream s1,s2;
			
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
				File logFile=new File(file);
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
		out(UtilL.toString(obj));
	}
	
	public static void println(Object obj){
		out(UtilL.toString(obj)+"\n");
	}
	
	public static void print(Object...objs){
		out(UtilL.toString(objs));
	}
	
	public static void println(Object...objs){
		out(UtilL.toString(objs)+"\n");
	}
	
	//================================================
	
	public static void printlnEr(){
		err("\n");
	}
	
	public static void printEr(){
		err("");
	}
	
	public static void printEr(Object obj){
		err(UtilL.toString(obj));
	}
	
	public static void printlnEr(Object obj){
		err(UtilL.toString(obj)+"\n");
	}
	
	public static void printEr(Object...objs){
		err(UtilL.toString(objs));
	}
	
	public static void printlnEr(Object...objs){
		err(UtilL.toString(objs)+"\n");
	}
	
	//================================================
	
	public static void printFunctionTrace(int count, CharSequence splitter){
		println(getFunctionTrace(count, splitter));
	}
	
	public static String getFunctionTrace(int count, CharSequence splitter){
		StringBuilder line=new StringBuilder();
		
		StackTraceElement[] trace=Thread.currentThread().getStackTrace();
		if(count>=trace.length) count=trace.length-1;
		//		for(StackTraceElement stack:trace){
		//			stack.getMethodName()
		//		}
		for(int i=count+1;i>=2;i--){
			line.append(trace[i].getMethodName()).append('(').append(trace[i].getLineNumber()).append(')');
			if(i!=2) line.append(splitter);
		}
		return line.toString();
	}
	
	/**
	 * print fancy stuff and things
	 */
	public static void printWrapped(Object obj){
		String[] data=UtilL.toString(obj).split("\n");
		StringBuilder line=new StringBuilder();
		
		int length=0;
		for(int i=0;i<data.length;i++){
			String lin=data[i]=data[i].replaceFirst("\\s+$", "");
			length=Math.max(length, lin.length());
		}
		
		if(data.length>1){
			length+=4;
			for(int i=0;i<data.length;i++){
				String lin=data[i]="| "+data[i]+" |";
				int diff=length-lin.length();
				if(diff==0) continue;
				
				for(int j=0;j<diff;j++){
					if(j%2==0) lin+="=";
					else lin="="+lin;
				}
				
				data[i]=lin;
			}
		}
		
		line.append("<<");
		for(int i=0, j=length+4;i<j;i++){
			line.append('=');
		}
		line.append(">>");
		
		String lineS=line.toString();
		
		out(lineS+"\n");
		for(String lin:data)
			out("<<=="+lin+"==>>\n");
		out(lineS+"\n");
	}
	
	public static <T> T printlnAndReturn(T obj){
		println(obj);
		return obj;
	}
	
	public static void printStackTrace(String msg){
		StringBuilder result=new StringBuilder();
		
		StackTraceElement[] a1=Thread.currentThread().getStackTrace();
		
		if(msg==null){
			DateFormat dateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Calendar cal=Calendar.getInstance();
			result.append("Invoke time: ").append(dateFormat.format(cal.getTime())).append("\n");
		}else result.append(msg).append("\n");
		
		int length=0;
		for(int i=2;i<a1.length;i++){
			StackTraceElement a=a1[i];
			String s=a.toString();
			result.append(s).append("\n");
			length=Math.max(s.length(), length);
		}
		for(int b=0;b<length/4;b++)
			result.append("_/\\_");
		
		println(result);
	}
	
	//================================================
	
	private synchronized static void out(String s){
//		if(__.DEBUG_ACTIVE&&!__.DEBUG_INIT) System.err.println("LOG UTILITY DID NOT INJECT DEBUG HEADER!");
		System.out.print(s);
	}
	
	private synchronized static void err(String s){
//		if(__.DEBUG_ACTIVE&&!__.DEBUG_INIT) System.err.println("LOG UTILITY DID NOT INJECT DEBUG HEADER!");
		System.err.print(s);
	}
	
}
