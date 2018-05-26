package com.lapissea.util;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;

import static com.lapissea.util.UtilL.*;

public class LogUtil{
	private static final long START_TIME=System.currentTimeMillis();
	
	@SuppressWarnings("PointlessBitwiseExpression")
	public static class Init{
		
		
		private Init(){}
		
		public static final int CREATE_OUTPUT_LOG     =1<<0;
		public static final int CREATE_OUTPUT_CSV     =1<<1;
		public static final int USE_CALL_POS          =1<<2;
		public static final int USE_CALL_POS_CLICKABLE=1<<3;
		public static final int USE_CALL_THREAD       =1<<4;
		public static final int DISABLED              =1<<5;
		public static final int USE_TABULATED_HEADER  =1<<6;
		
		public static PrintStream OUT=System.out;
		public static PrintStream ERR=System.err;
		
		/**
		 * @param flags Use or operator on defined fields to get a desired effect.
		 * @see #CREATE_OUTPUT_LOG
		 * @see #CREATE_OUTPUT_CSV
		 * @see #USE_CALL_POS
		 * @see #USE_CALL_POS_CLICKABLE
		 * @see #DISABLED
		 */
		public static void attach(int flags){
			attach(flags, "debug/log");
		}
		
		/**
		 * @param flags   Use or operator on defined fields to get a desired effect.
		 * @param fileLog Pass a string to a path where the log/csv file needs to be (without the file extension).
		 * @see #CREATE_OUTPUT_LOG
		 * @see #CREATE_OUTPUT_CSV
		 * @see #USE_CALL_POS
		 * @see #USE_CALL_POS_CLICKABLE
		 * @see #DISABLED
		 */
		public static void attach(int flags, @NotNull String fileLog){
			if(checkFlag(flags, DISABLED)) return;
			
			
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
			
			if(checkFlag(flags, CREATE_OUTPUT_LOG)) fileRawOut=create.apply(fileLog+".log");
			if(checkFlag(flags, CREATE_OUTPUT_CSV)){
				try{
					fileCsvOut=create.apply(fileLog+".csv");
					fileCsvOut.write("Type, Time, Thread, Class, Function, Line, Message\n".getBytes());
				}catch(IOException e){
					throw UtilL.uncheckedThrow(e);
				}
			}
			
			class P extends PrintStream{
				
				public P(@NotNull OutputStream out){
					super(out);
				}
				
				@Override
				public void write(@NotNull byte[] buf, int off, int len){
					super.write(buf, off, len);
					flush();
				}
				
				@Override
				public void print(String s){
					super.print(s);
				}
			}
			
			Function<StackTraceElement, String> header=getHeader(flags);
			
			System.setOut(new P(new DebugHeaderStream(System.out, "OUT", fileRawOut, fileCsvOut, header)));
			System.setErr(new P(new DebugHeaderStream(System.err, "ERR", fileRawOut, fileCsvOut, header)));
			
		}
		
		private static Function<StackTraceElement, String> getHeader(int flags){
			
			boolean tabulated=checkFlag(flags, USE_TABULATED_HEADER);
			
			if(checkFlag(flags, USE_CALL_THREAD)){
				Tabulator threadTab=new Tabulator(tabulated);
				
				if(checkFlag(flags, USE_CALL_POS_CLICKABLE)){
					
					Tabulator stackTab=new Tabulator(tabulated);
					threadTab.onGrow=stackTab::reduce;
					
					return stack->{
						String threadSt="["+Thread.currentThread().getName()+"] ";
						String stackSt ="["+stack.toString()+"]";
						
						return threadSt+threadTab.getTab(threadSt.length())+
						       stackSt+stackTab.getTab(stackSt.length())+": ";
					};
				}
				
				if(checkFlag(flags, USE_CALL_POS)){
					
					Tabulator stackTab=new Tabulator(tabulated);
					threadTab.onGrow=stackTab::reduce;
					
					return stack->{
						String methodName=stack.getMethodName();
						if(methodName.startsWith("lambda$")) methodName=methodName.substring(7);
						String className=stack.getClassName();
						
						String threadSt="["+Thread.currentThread().getName()+"] ";
						String stackSt ="["+className.substring(className.lastIndexOf('.')+1)+'.'+methodName+':'+stack.getLineNumber()+"]";
						
						return threadSt+threadTab.getTab(threadSt.length())+stackSt+stackTab.getTab(stackSt.length())+": ";
					};
				}
				
				return stack->{
					String threadSt="["+Thread.currentThread().getName()+"]";
					
					return threadSt+threadTab.getTab(threadSt.length())+": ";
				};
			}else{
				
				if(checkFlag(flags, USE_CALL_POS_CLICKABLE)){
					
					Tabulator stackTab=new Tabulator(tabulated);
					
					return stack->{
						String stackSt="["+stack.toString()+"]";
						return stackSt+stackTab.getTab(stackSt.length())+": ";
					};
				}
				
				if(checkFlag(flags, USE_CALL_POS)){
					
					Tabulator stackTab=new Tabulator(tabulated);
					
					return stack->{
						String methodName=stack.getMethodName();
						if(methodName.startsWith("lambda$")) methodName=methodName.substring(7);
						String className=stack.getClassName();
						
						String stackSt="["+className.substring(className.lastIndexOf('.')+1)+'.'+methodName+':'+stack.getLineNumber()+"]";
						
						return stackSt+stackTab.getTab(stackSt.length())+": ";
					};
				}
				
				return stack->"";
			}
		}
		
		@Deprecated
		public static void destroy(){
			detach();
		}
		
		public static void detach(){
			System.setOut(OUT);
			System.setErr(ERR);
		}
		
		private static final class Tabulator{
			
			private final List<PairM<Integer, Long>> sizeTimeTable=new ArrayList<>();
			
			public IntConsumer onGrow=i->{};
			int     prevSize;
			boolean tabulated;
			
			public Tabulator(boolean tabulated){
				this.tabulated=tabulated;
			}
			
			public String getTab(int size){
				if(!tabulated) return "";
				
				long tim=System.currentTimeMillis();
				
				int max=0;
				
				Iterator<PairM<Integer, Long>> i=sizeTimeTable.iterator();
				while(i.hasNext()){
					PairM<Integer, Long> p=i.next();
					if(p.obj2+1000<tim) i.remove();
					else max=Math.max(max, p.obj1);
				}
				if(size>max) onGrow.accept(size-max);
				
				sizeTimeTable.add(new PairM<>(size, tim));
				
				return TextUtil.stringFill(Math.max(0, max-size), ' ');
			}
			
			
			public void reduce(int amount){
				if(!tabulated) return;
				Iterator<PairM<Integer, Long>> i=sizeTimeTable.iterator();
				while(i.hasNext()){
					PairM<Integer, Long> p=i.next();
					p.obj1-=amount;
					if(p.obj1<0) i.remove();
				}
			}
			
		}
		
		private static final class DebugHeaderStream extends OutputStream{
			
			@Nullable
			private final OutputStream  child;
			@NotNull
			private final byte[]        prefix;
			private       byte[]        data;
			private final StringBuilder lineBuild  =new StringBuilder();
			private       boolean       needsHeader=true;
			
			private final FileOutputStream                    fileCsvOut;
			private final Function<StackTraceElement, String> header;
			
			private final CharsetDecoder decoder=StandardCharsets.UTF_8.newDecoder();
			
			public DebugHeaderStream(OutputStream child, @NotNull String prefix, @Nullable FileOutputStream fileRawOut, FileOutputStream fileCsvOut, Function<StackTraceElement, String> header){
				this.header=header;
				if(fileRawOut!=null) this.child=new SplitStream(child, fileRawOut);
				else this.child=child;
				this.prefix=prefix.getBytes();
				this.fileCsvOut=fileCsvOut;
			}
			
			@Override
			public void flush() throws IOException{
				if(needsHeader){
					needsHeader=false;
					
					try{
						debugHeader();
					}catch(Exception e){
						detach();
						e.printStackTrace();
						System.exit(0);
					}
				}
				
				byte[] data=lineBuild.toString().getBytes();
				try{
					for(char b : decoder.decode(ByteBuffer.wrap(data)).array()){
						put(b);
					}
				}catch(CharacterCodingException e){
					for(byte b : data){
						put(b);
					}
				}
				
				lineBuild.setLength(0);
				child.flush();
			}
			
			private void put(int b) throws IOException{
				if(b=='\n') needsHeader=true;
				child.write(b);
			}
			
			@Override
			public void write(int b) throws IOException{
				if(b=='\r') return;
				
				if(b=='\n'){
					lineBuild.append(System.lineSeparator());
				}else{
					
					lineBuild.append((char)b);
				}
				
				if(fileCsvOut!=null){
					if(b=='"') fileCsvOut.write("\"\"".getBytes());
					else if(b=='\n') fileCsvOut.write("\"\n".getBytes());
					else fileCsvOut.write((char)b);
				}
			}
			
			private void debugHeader() throws IOException{
				
				StackTraceElement stack=getCallStack();
				if(stack==null) return;
				
				if(fileCsvOut!=null) writeCvs(stack);
				
				child.write(header.apply(stack).getBytes());
			}
			
			private StackTraceElement getCallStack(){
				StackTraceElement[] trace=Thread.currentThread().getStackTrace();
				
				int    depth=trace.length;
				String name;
				//noinspection StatementWithEmptyBody
				while(!(name=trace[--depth].getClassName())
					       .equals(PrintStream.class.getName())&&
				      !name.startsWith(LogUtil.class.getName())&&
				      !(name.startsWith("java.util")&&trace[depth].getMethodName().equals("forEach"))) ;
				depth++;
				
				if(depth<0||depth >= trace.length) return null;
				return trace[depth];
			}
			
			private void writeCvs(@NotNull StackTraceElement stack) throws IOException{
				
				long passed=System.currentTimeMillis()-START_TIME;
				
				int ms, s, min, h;
				
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
				fileCsvOut.write(Thread.currentThread().getName().replace("\"", "\"\"").getBytes());
				fileCsvOut.write("\",".getBytes());
				fileCsvOut.write(stack.getClassName().getBytes());
				fileCsvOut.write(',');
				fileCsvOut.write(stack.getMethodName().getBytes());
				fileCsvOut.write(',');
				fileCsvOut.write(Integer.toString(stack.getLineNumber()).getBytes());
				fileCsvOut.write(",\"".getBytes());
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
		if(count >= trace.length) count=trace.length-1;
		for(int i=count+1;i >= 2;i--){
			line.append(trace[i].getMethodName()).append('(').append(trace[i].getLineNumber()).append(')');
			if(i!=2) line.append(splitter);
		}
		return line.toString();
	}
	
	/**
	 * print fancy stuff and things
	 *
	 * @param obj object to print
	 */
	public static void printWrappedEr(Object obj){
		printlnEr(TextUtil.wrappedString(obj));
	}
	
	/**
	 * print fancy stuff and things
	 *
	 * @param obj object to print
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
	
	public static void printStackTrace(@Nullable String msg, @NotNull StackTraceElement[] a1){
		StringBuilder result=new StringBuilder();
		
		if(msg==null){
			result.append("Invoke time: ").append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())).append("\n");
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
		System.out.print(s);
	}
	
	private static void err(String s){
		System.err.print(s);
	}
	
}
