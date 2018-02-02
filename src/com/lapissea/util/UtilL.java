package com.lapissea.util;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class UtilL{
	
	
	public static final double SQRT2D=Math.sqrt(2);
	public static final float  SQRT2F=(float)SQRT2D;
	
	
	public static boolean isArray(Object object){
		return object!=null&&object.getClass().isArray();
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
	
	public static <T> T[] array(List<T> list){
		if(list.isEmpty()) return null;
		
		T[] a=(T[])UtilL.array(list.get(0).getClass(), list.size());
		return list.toArray(a);
	}
	
	public static <T> T[] array(List<T> list, T[] arr){
		if(list.isEmpty()) return null;
		
		T[] a;
		if(arr.length==list.size()) a=arr;
		else a=(T[])UtilL.array(list.get(0).getClass(), list.size());
		return list.toArray(a);
	}
	
	public static <K, V> void doAndClear(Map<K, V> collection, BiConsumer<K, V> toDo){
		if(collection.isEmpty()) return;
		collection.forEach(toDo);
		collection.clear();
	}
	
	public static <T> void doAndClear(Collection<T> collection, Consumer<T> toDo){
		if(collection.isEmpty()) return;
		for(T t : collection){
			toDo.accept(t);
		}
		collection.clear();
	}
	
	public static void startDaemonThread(Runnable run, String name){
		Thread t=new Thread(run, name);
		t.setDaemon(true);
		t.start();
	}
	
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
		Object o   =null;
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
		ObjectOutputStream    oos =new ObjectOutputStream(baos);
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
	
	public static List<Field> getAllFields(Class<?> type){
		return getAllFields(new ArrayList<>(), type);
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
			ByteArrayOutputStream obj =new ByteArrayOutputStream();
			GZIPOutputStream      gzip=new GZIPOutputStream(obj);
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
				GZIPInputStream gis           =new GZIPInputStream(new ByteArrayInputStream(compressed));
				BufferedReader  bufferedReader=new BufferedReader(new InputStreamReader(gis, "UTF-8"));
				
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
	
	public static <T> void iterate(Object data, Class<T> type, Consumer<T> consumer){
		if(data==null) return;
		
		if(instanceOf(data, type)) consumer.accept((T)data);
		else if(data instanceof Iterable){
			((Iterable<?>)data).forEach(mat->consumer.accept((T)mat));
		}else if(data.getClass().isArray()){
			Class<?> typeTx=data.getClass().getComponentType();
			
			if(instanceOf(typeTx, type)){
				for(T tx : (T[])data){
					consumer.accept(tx);
				}
			}
		}
	}
	
	public static byte[] readAll(InputStream is){
		ByteArrayOutputStream buffer;
		try{
			buffer=new ByteArrayOutputStream();
			
			int    nRead;
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
	
	public static void fileLines(InputStream stream, UnsafeConsumer<String, IOException> cons) throws IOException{
		BufferedReader b=new BufferedReader(new InputStreamReader(stream, "ISO-8859-1"));
		
		for(String line;(line=b.readLine())!=null;){
			cons.accept(line);
		}
		
	}
	
	public static <T> void forEach(T[] ts, Consumer<T> consumer){
		for(T t : ts) consumer.accept(t);
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
	
	public static <T extends Throwable> T uncheckedThrow(Throwable t) throws T{
		if(true) throw (T)t;
		return (T)t;
	}
	
	public static <T> Stream<T> stream(Iterable<T> it){
		return stream(it, false);
	}
	
	public static <T> Stream<T> stream(Iterable<T> it, boolean parallel){
		return StreamSupport.stream(it.spliterator(), false);
	}
	
	public static <T> Stream<T> stream(Iterator<T> it){
		return stream(it, false);
	}
	
	public static <T> Stream<T> stream(Iterator<T> it, boolean parallel){
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.NONNULL), parallel);
	}
	
	public static <In, Out> Out[] convert(Collection<In> in, Class<Out> outType, Function<In, Out> converter){
		Out[]        out =array(outType, in.size());
		int          i   =0;
		Iterator<In> iter=in.iterator();
		while(iter.hasNext()){
			out[i++]=converter.apply(iter.next());
		}
		return out;
	}
	
	public static <Out> Out[] convert(int[] in, Class<Out> outType, IntFunction<Out> converter){
		Out[] out=array(outType, in.length);
		for(int i=0;i<in.length;i++){
			out[i]=converter.apply(in[i]);
		}
		return out;
	}
	
	public static <In, Out> Out[] convert(In[] in, Class<Out> outType, Function<In, Out> converter){
		Out[] out=array(outType, in.length);
		for(int i=0;i<in.length;i++){
			out[i]=converter.apply(in[i]);
		}
		return out;
	}
	
	public static <In, Out> Out[] convert(In[] in, IntFunction<Out[]> array, Function<In, Out> converter){
		Out[] out=array.apply(in.length);
		for(int i=0;i<in.length;i++){
			out[i]=converter.apply(in[i]);
		}
		return out;
	}
	
	public static <T> boolean contains(T[] array, T what){
		if(what==null){
			for(T t : array){
				if(t==null) return true;
			}
			return false;
		}
		for(T t : array){
			if(t!=null&&t.equals(what)) return true;
		}
		return false;
	}
	
	public static int indexOf(int[] array, int what){
		return indexOf(array, 0, what);
	}
	
	public static int indexOf(int[] array, int start, int what){
		for(;start<array.length;start++){
			if(array[start]==what) return start;
		}
		return -1;
	}
	
	public static boolean contains(int[] array, int what){
		for(int t : array){
			if(t==what) return true;
		}
		return false;
	}
	
	public static <T extends Throwable> T exitWithErrorMsg(Object... msg) throws T{
		String msg0=TextUtil.toString(msg);
		LogUtil.printlnEr(msg0);
		System.exit(-1);
		return (T)new Exception(msg0);
	}
	
	public static byte[] longToBytes(long l){
		byte[] dest=new byte[8];
		return longToBytes(dest, l);
	}
	
	public static byte[] longToBytes(byte[] dest, long l){
		return longToBytes(dest, 0, l);
	}
	
	public static byte[] longToBytes(byte[] dest, int destStart, long l){
		for(int i=7;i>=0;i--){
			dest[destStart+i]=(byte)(l&0xFF);
			l>>=8;
		}
		return dest;
	}
	
	public static long bytesToLong(byte[] b){
		return bytesToLong(0, b);
	}
	
	public static long bytesToLong(int start, byte[] b){
		long result=0;
		for(int i=0;i<8;i++){
			result<<=8;
			result|=(b[start+i]&0xFF);
		}
		return result;
	}
}
