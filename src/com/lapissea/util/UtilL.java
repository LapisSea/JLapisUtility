package com.lapissea.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class UtilL{
	
	private static final Map<Class<Object>,Function<Object,String>> CUSTOM_TO_STRINGS=new HashMap<>();
	
	public static final double	SQRT2D	=Math.sqrt(2);
	public static final float	SQRT2F	=(float)SQRT2D;
	
	@SuppressWarnings("unchecked")
	protected static <T> void __REGISTER_CUSTOM_TO_STRING(Class<T> type, Function<T,String> funct){
		CUSTOM_TO_STRINGS.put((Class<Object>)type, (Function<Object,String>)funct);
	}
	
	public static String toStringArray(Object[] arr){
		if(arr==null) return "null";
		StringBuilder print=new StringBuilder("[");
		
		for(int i=0;i<arr.length;i++){
			Object a=arr[i];
			if(isArray(a)) print.append(unknownArrayToString(a));
			else if(a instanceof FloatBuffer) print.append(floatBufferToString((FloatBuffer)a));
			else print.append(toString(a));
			if(i!=arr.length-1) print.append(", ");
		}
		
		return print.append("]").toString();
	}
	
	public static String toString(Object...objs){
		if(objs==null) return "null";
		StringBuilder print=new StringBuilder();
		
		for(int i=0;i<objs.length;i++){
			Object a=objs[i];
			if(isArray(a)) print.append(unknownArrayToString(a));
			else if(a instanceof FloatBuffer) print.append(floatBufferToString((FloatBuffer)a));
			else print.append(toString(a));
			if(i!=objs.length-1) print.append(" ");
		}
		
		return print.toString();
	}
	
	public static String toString(Object obj){
		if(obj==null) return "null";
		
		StringBuilder print=new StringBuilder();
		
		if(isArray(obj)) print.append(unknownArrayToString(obj));
		else if(obj instanceof FloatBuffer) print.append(floatBufferToString((FloatBuffer)obj));
		else{
			Class<?> type=obj.getClass();
			Function<Object,String> fun=CUSTOM_TO_STRINGS.get(type);
			if(fun!=null) print.append(fun.apply(obj));
			else{
				Entry<Class<Object>,Function<Object,String>> ent=CUSTOM_TO_STRINGS.entrySet().stream().filter(e->instanceOf(e.getKey(), type)).findFirst().orElse(null);
				if(ent!=null) print.append(ent.getValue().apply(obj));
				else print.append(obj.toString());
			}
		}
		
		return print.toString();
	}
	
	public static boolean isArray(Object object){
		return object!=null&&object.getClass().isArray();
	}
	
	public static String floatBufferToString(FloatBuffer buff){
		if(buff==null) return "null";
		
		StringBuilder print=new StringBuilder("Buffer{");
		
		buff=buff.duplicate();
		buff.limit(buff.capacity());
		if(buff.capacity()>0){
			int j=0;
			print.append(buff.get(j));
			for(j=1;j<buff.capacity();j++)
				print.append(", ").append(buff.get(j));
		}
		print.append('}');
		return print.toString();
	}
	
	private static String unknownArrayToString(Object arr){
		if(arr==null) return "null";
		if(arr instanceof boolean[]) return Arrays.toString((boolean[])arr);
		if(arr instanceof float[]) return Arrays.toString((float[])arr);
		if(arr instanceof byte[]) return Arrays.toString((byte[])arr);
		if(arr instanceof int[]) return Arrays.toString((int[])arr);
		if(arr instanceof long[]) return Arrays.toString((long[])arr);
		if(arr instanceof short[]) return Arrays.toString((short[])arr);
		if(arr instanceof char[]) return Arrays.toString((char[])arr);
		if(arr instanceof double[]) return Arrays.toString((double[])arr);
		if(arr instanceof Object[]) return toStringArray((Object[])arr);
		return "ERR: "+arr;
	}
	
	public static boolean TRUE(){
		return true;
	}
	
	public static void sleep(long millis){
		try{
			Thread.sleep(millis);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void sleep(long millis, int nanos){
		try{
			Thread.sleep(millis, nanos);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static <T> Stream<T> stream(Enumeration<T> e){
		return StreamSupport.stream(
				new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED){
					
					@Override
					public boolean tryAdvance(Consumer<? super T> action){
						if(e.hasMoreElements()){
							action.accept(e.nextElement());
							return true;
						}
						return false;
					}
					
					@Override
					public void forEachRemaining(Consumer<? super T> action){
						while(e.hasMoreElements()){
							action.accept(e.nextElement());
						}
					}
				}, false);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] array(List<T> list){
		if(list.isEmpty()) return null;
		
		T[] a=(T[])UtilL.array(list.get(0).getClass(), list.size());
		return list.toArray(a);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] array(List<T> list, T[] arr){
		if(list.isEmpty()) return null;
		
		T[] a;
		if(arr.length==list.size()) a=arr;
		else a=(T[])UtilL.array(list.get(0).getClass(), list.size());
		return list.toArray(a);
	}
	
	public static <K,V> void doAndClear(Map<K,V> collection, BiConsumer<K,V> toDo){
		if(collection.isEmpty()) return;
		collection.forEach(toDo);
		collection.clear();
	}
	
	public static <T> void doAndClear(Collection<T> collection, Consumer<T> toDo){
		if(collection.isEmpty()) return;
		for(T t:collection){
			toDo.accept(t);
		}
		collection.clear();
	}
	
	public static void startDaemonThread(Runnable run, String name){
		Thread t=new Thread(run, name);
		t.setDaemon(true);
		t.start();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] array(Class<T> componentType, int length){
		return (T[])Array.newInstance(componentType, length);
	}
	
	public static boolean instanceOf(Class<?> left, Class<?> right){
		if(left==right) return true;
		try{
			left.asSubclass(right);
			return true;
		}catch(Exception ignored){}
		return false;
	}
	
	public static boolean instanceOf(Object left, Class<?> right){
		return left!=null&&instanceOf(left.getClass(), right);
	}
	
	public static boolean instanceOf(Class<?> left, Object right){
		return instanceOf(left, right.getClass());
	}
	
	public static boolean instanceOf(Object left, Object right){
		return instanceOf(left.getClass(), right.getClass());
	}
	
	public static Serializable fromString(String s){
		//		byte[] data=Base64.getDecoder().decode(s);
		byte[] data=s.getBytes();
		Object o=null;
		try{
			ObjectInputStream ois=new ObjectInputStream(new ByteArrayInputStream(data));
			try{
				o=ois.readObject();
			}catch(ClassNotFoundException e){
				e.printStackTrace();
			}
			ois.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		return (Serializable)o;
	}
	
	public static String toString(Serializable o) throws IOException{
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		ObjectOutputStream oos=new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		//		return Base64.getEncoder().encodeToString(baos.toByteArray());
		return new String(baos.toByteArray());
	}
	
	public static void closeSilenty(Closeable closeable){
		try{
			closeable.close();
		}catch(IOException e){}
	}
	
	public static String stringFill(int length, char c){
		char[] ch=new char[length];
		Arrays.fill(ch, c);
		return new String(ch);
	}
	
	public static List<Field> getAllFields(Class<?> type){
		return getAllFields(new ArrayList<Field>(), type);
	}
	
	public static List<Field> getAllFields(List<Field> fields, Class<?> type){
		fields.addAll(Arrays.asList(type.getDeclaredFields()));
		
		if(type.getSuperclass()!=null){
			getAllFields(fields, type.getSuperclass());
		}
		
		return fields;
	}
	
	public static String compress(String data){
		try{
			return new String(compress(data.getBytes("UTF-8")));
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] compress(byte[] data){
		try{
			if(data==null||data.length==0) return null;
			ByteArrayOutputStream obj=new ByteArrayOutputStream();
			GZIPOutputStream gzip=new GZIPOutputStream(obj);
			gzip.write(data);
			gzip.close();
			return obj.toByteArray();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static String decompress(final byte[] compressed){
		try{
			String outStr="";
			if(compressed==null||compressed.length==0) return "";
			if(isCompressed(compressed)){
				GZIPInputStream gis=new GZIPInputStream(new ByteArrayInputStream(compressed));
				BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(gis, "UTF-8"));
				
				String line;
				while((line=bufferedReader.readLine())!=null){
					outStr+=line;
				}
			}else{
				outStr=new String(compressed);
			}
			return outStr;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static boolean isCompressed(final byte[] compressed){
		return compressed[0]==(byte)GZIPInputStream.GZIP_MAGIC&&compressed[1]==(byte)(GZIPInputStream.GZIP_MAGIC>>8);
	}
	
	public static Class<?> findClosestCommonSuper(Class<?> a, Class<?> b){
		while(!a.isAssignableFrom(b))
			a=a.getSuperclass();
		return a;
	}
	
	public static void iterate(Object data, Consumer<Object> consumer){
		iterate(data, Object.class, consumer);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> void iterate(Object data, Class<T> type, Consumer<T> consumer){
		if(data==null) return;
		
		if(instanceOf(data, type)) consumer.accept((T)data);
		else if(data instanceof Iterable){
			((Iterable<?>)data).forEach(mat->consumer.accept((T)mat));
		}else if(data.getClass().isArray()){
			Class<?> typeTx=data.getClass().getComponentType();
			
			if(instanceOf(typeTx, type)){
				for(T tx:(T[])data){
					consumer.accept(tx);
				}
			}
		}
	}
	
	public static byte[] readAll(InputStream is){
		ByteArrayOutputStream buffer;
		try{
			buffer=new ByteArrayOutputStream();
			
			int nRead;
			byte[] data=new byte[16384];
			
			while((nRead=is.read(data, 0, data.length))!=-1){
				buffer.write(data, 0, nRead);
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		return buffer.toByteArray();
	}
	
	public static boolean emptyOrNull(String string){
		return string==null||string.isEmpty();
	}
	
	public static void runWhile(BooleanSupplier when, Runnable what){
		while(when.getAsBoolean())
			what.run();
	}
	
	public static void runWhileThread(String threadName, BooleanSupplier when, Runnable what){
		new Thread(()->runWhile(when, what), threadName).start();
	}
	
	public static void fileLines(InputStream stream, Consumer<String> cons) throws IOException{
		StringBuilder lineBuild=new StringBuilder();
		int ch;
		
		while(true){
			String line;
			while(true){
				ch=stream.read();
				if(ch==-1){
					line=lineBuild.toString();
					lineBuild.setLength(0);
					break;
				}
				if(ch=='\n'){
					ch=stream.read();
					line=lineBuild.toString();
					lineBuild.setLength(0);
					if(ch!='\r'&&ch!='\n') lineBuild.append((char)ch);
					break;
				}
				lineBuild.append((char)ch);
			}
			cons.accept(line);
			
			if(ch==-1) return;
		}
	}
	
	public static class InputStreamSilent extends InputStream{
		
		public final InputStream parent;
		
		public InputStreamSilent(InputStream parent){
			this.parent=parent;
		}
		
		@Override
		public void close(){
			closeSilenty(parent);
		}
		
		@Override
		public int read() throws IOException{
			return parent.read();
		}
		
		@Override
		public int read(byte b[]) throws IOException{
			return parent.read(b);
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException{
			return parent.read(b, off, len);
		}
		
		@Override
		public int available() throws IOException{
			return parent.available();
		}
		
		@Override
		public long skip(long n) throws IOException{
			return parent.skip(n);
		}
		
		@Override
		public boolean markSupported(){
			return parent.markSupported();
		}
		
		@Override
		public synchronized void mark(int readlimit){
			parent.mark(readlimit);
		}
		
		@Override
		public synchronized void reset() throws IOException{
			parent.reset();
		}
		
		@Override
		public String toString(){
			return parent.toString();
		}
	}
	
	public static InputStreamSilent silentClose(InputStream resource){
		return new InputStreamSilent(resource);
	}
	
	public static String before(String toSubstring, char marker){
		int pos=toSubstring.lastIndexOf(marker);
		if(pos==-1) return null;
		return toSubstring.substring(0, pos);
	}
	
	public static String after(String toSubstring, char marker){
		int pos=toSubstring.lastIndexOf(marker);
		if(pos==-1) return null;
		return toSubstring.substring(pos+1);
	}
	
	@SuppressWarnings({"unchecked","unused"})
	public static <T extends Throwable> T uncheckedThrow(Throwable t) throws T{
		if(true) throw(T)t;
		return (T)t;
	}
}
