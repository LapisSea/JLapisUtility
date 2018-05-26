package com.lapissea.util;

import com.lapissea.util.function.IntIntConsumer;
import com.lapissea.util.function.UnsafeConsumer;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class UtilL{
	
	
	public static final double SQRT2D  =Math.sqrt(2);
	public static final float  SQRT2F  =(float)SQRT2D;
	public static final byte[] NO_BYTES=new byte[0];
	
	
	public static boolean isArray(@Nullable Object object){
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
	
	public static <T> Stream<T> stream(@NotNull Enumeration<T> e){
		return StreamSupport.stream(
			new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED){
				
				@Override
				public boolean tryAdvance(@NotNull Consumer<? super T> action){
					if(e.hasMoreElements()){
						action.accept(e.nextElement());
						return true;
					}
					return false;
				}
				
				@Override
				public void forEachRemaining(@NotNull Consumer<? super T> action){
					while(e.hasMoreElements()){
						action.accept(e.nextElement());
					}
				}
			}, false);
	}
	
	@Deprecated
	@SuppressWarnings("unchecked")
	public static <T> T[] array(@NotNull List<T> list){
		if(list.isEmpty()) return null;
		
		T[] a=(T[])UtilL.array(list.get(0).getClass(), list.size());
		return list.toArray(a);
	}
	
	@Deprecated
	@SuppressWarnings("unchecked")
	public static <T> T[] array(@NotNull List<T> list, @NotNull T[] arr){
		if(list.isEmpty()) return null;
		
		T[] a;
		if(arr.length==list.size()) a=arr;
		else a=(T[])UtilL.array(list.get(0).getClass(), list.size());
		return list.toArray(a);
	}
	
	public static <K, V> void doAndClear(@NotNull Map<K, V> collection, @NotNull BiConsumer<K, V> toDo){
		if(collection.isEmpty()) return;
		collection.forEach(toDo);
		collection.clear();
	}
	
	public static <T> void doAndClear(@NotNull Collection<T> collection, @NotNull Consumer<T> toDo){
		if(collection.isEmpty()) return;
		for(T t : collection){
			toDo.accept(t);
		}
		collection.clear();
	}
	
	public static void startDaemonThread(@NotNull Runnable run, @NotNull String name){
		Thread t=new Thread(run, name);
		t.setDaemon(true);
		t.start();
	}
	
	@NotNull
	@SuppressWarnings("unchecked")
	public static <T> T[] array(Class<T> componentType, int length){
		return (T[])Array.newInstance(componentType, length);
	}
	
	public static boolean instanceOf(@NotNull Class<?> left, @NotNull Class<?> right){
		if(left==right) return true;
		try{
			left.asSubclass(right);
			return true;
		}catch(Exception ignored){}
		return false;
	}
	
	public static boolean instanceOf(@Nullable Object left, @NotNull Class<?> right){
		return left!=null&&instanceOf(left.getClass(), right);
	}
	
	public static boolean instanceOf(@NotNull Class<?> left, @NotNull Object right){
		return instanceOf(left, right.getClass());
	}
	
	public static boolean instanceOf(@NotNull Object left, @NotNull Object right){
		return instanceOf(left.getClass(), right.getClass());
	}
	
	@NotNull
	public static <T> T fromString(@NotNull String s){
		//		byte[] data=Base64.getDecoder().decode(s);
		byte[] data=s.getBytes();
		try(ObjectInputStream ois=new ObjectInputStream(new ByteArrayInputStream(data))){
			return (T)ois.readObject();
		}catch(Exception e){
			throw uncheckedThrow(e);
		}
	}
	
	@NotNull
	public static String toString(@NotNull Serializable o) throws IOException{
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		ObjectOutputStream    oos =new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		//		return Base64.getEncoder().encodeToString(baos.toByteArray());
		return new String(baos.toByteArray());
	}
	
	public static void closeSilenty(@NotNull Closeable closeable){
		try{
			closeable.close();
		}catch(IOException e){}
	}
	
	@NotNull
	public static List<Field> getAllFields(@NotNull Class<?> type){
		return getAllFields(new ArrayList<>(), type);
	}
	
	@NotNull
	public static List<Field> getAllFields(@NotNull List<Field> fields, @NotNull Class<?> type){
		fields.addAll(Arrays.asList(type.getDeclaredFields()));
		
		if(type.getSuperclass()!=null){
			getAllFields(fields, type.getSuperclass());
		}
		
		return fields;
	}
	
	@NotNull
	public static String compress(@NotNull String data){
		try{
			return new String(compress(data.getBytes("UTF-8")));
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	@NotNull
	public static byte[] compress(@Nullable byte[] data){
		try{
			if(data==null||data.length==0) return NO_BYTES;
			ByteArrayOutputStream obj =new ByteArrayOutputStream();
			GZIPOutputStream      gzip=new GZIPOutputStream(obj);
			gzip.write(data);
			gzip.close();
			return obj.toByteArray();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	@NotNull
	public static String decompress(@Nullable final byte[] compressed){
		try{
			StringBuilder outStr=new StringBuilder();
			if(compressed==null||compressed.length==0) return "";
			
			if(isCompressed(compressed)){
				GZIPInputStream gis           =new GZIPInputStream(new ByteArrayInputStream(compressed));
				BufferedReader  bufferedReader=new BufferedReader(new InputStreamReader(gis, "UTF-8"));
				
				String line;
				while((line=bufferedReader.readLine())!=null){
					outStr.append(line);
				}
				
				return outStr.toString();
			}
			
			return new String(compressed);
			
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static boolean isCompressed(final byte[] compressed){
		return compressed[0]==(byte)GZIPInputStream.GZIP_MAGIC&&compressed[1]==(byte)(GZIPInputStream.GZIP_MAGIC >> 8);
	}
	
	@NotNull
	public static Class<?> findClosestCommonSuper(@NotNull Class<?> a, @NotNull Class<?> b){
		while(!a.isAssignableFrom(b))
			a=a.getSuperclass();
		return a;
	}
	
	public static void iterate(Object data, @NotNull Consumer<Object> consumer){
		iterate(data, Object.class, consumer);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> void iterate(@Nullable Object data, @NotNull Class<T> type, @NotNull Consumer<T> consumer){
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
	
	@NotNull
	public static byte[] readAll(@NotNull InputStream is){
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
	
	public static boolean emptyOrNull(@Nullable String string){
		return string==null||string.isEmpty();
	}
	
	public static void runWhile(@NotNull BooleanSupplier when, @NotNull Runnable what){
		while(when.getAsBoolean())
			what.run();
	}
	
	public static void runWhileThread(@NotNull String threadName, @NotNull BooleanSupplier when, @NotNull Runnable what){
		new Thread(()->runWhile(when, what), threadName).start();
	}
	
	public static void fileLines(@NotNull InputStream stream, @NotNull UnsafeConsumer<String, IOException> cons) throws IOException{
		BufferedReader b=new BufferedReader(new InputStreamReader(stream, "ISO-8859-1"));
		
		for(String line;(line=b.readLine())!=null;){
			cons.accept(line);
		}
		
	}
	
	public static <T> void forEach(@NotNull T[] ts, @NotNull Consumer<T> consumer){
		for(T t : ts) consumer.accept(t);
	}
	
	public static void sleepWhile(@NotNull BooleanSupplier checkWhile){
		sleepWhile(checkWhile, 1);
	}
	
	public static void sleepWhile(@NotNull BooleanSupplier checkWhile, long ms){
		while(checkWhile.getAsBoolean()) sleep(ms);
	}
	
	public static void sleepWhile(@NotNull BooleanSupplier checkWhile, long ms, int ns){
		while(checkWhile.getAsBoolean()) sleep(ms, ns);
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
		public int read(@NotNull byte b[]) throws IOException{
			return parent.read(b);
		}
		
		@Override
		public int read(@NotNull byte[] b, int off, int len) throws IOException{
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
	
	public static String before(@NotNull String toSubstring, char marker){
		int pos=toSubstring.lastIndexOf(marker);
		if(pos==-1) return null;
		return toSubstring.substring(0, pos);
	}
	
	public static String after(@NotNull String toSubstring, char marker){
		int pos=toSubstring.lastIndexOf(marker);
		if(pos==-1) return null;
		return toSubstring.substring(pos+1);
	}
	
	@NotNull
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> RuntimeException uncheckedThrow(Throwable throwable) throws T{
		throw (T)throwable;
	}
	
	public static <T> Stream<T> stream(@NotNull Iterable<T> it){
		return stream(it, false);
	}
	
	public static <T> Stream<T> stream(@NotNull Iterable<T> it, boolean parallel){
		return StreamSupport.stream(it.spliterator(), false);
	}
	
	public static <T> Stream<T> stream(@NotNull Iterator<T> it){
		return stream(it, false);
	}
	
	public static <T> Stream<T> stream(@NotNull Iterator<T> it, boolean parallel){
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.NONNULL), parallel);
	}
	
	@NotNull
	public static <In, Out> Out[] convert(@NotNull Collection<In> in, Class<Out> outType, @NotNull Function<In, Out> converter){
		Out[] out=array(outType, in.size());
		int   i  =0;
		for(In anIn : in){
			out[i++]=converter.apply(anIn);
		}
		return out;
	}
	
	@NotNull
	public static <Out> Out[] convert(@NotNull int[] in, Class<Out> outType, @NotNull IntFunction<Out> converter){
		Out[] out=array(outType, in.length);
		for(int i=0;i<in.length;i++){
			out[i]=converter.apply(in[i]);
		}
		return out;
	}
	
	@NotNull
	public static <In, Out> Out[] convert(@NotNull In[] in, Class<Out> outType, @NotNull Function<In, Out> converter){
		Out[] out=array(outType, in.length);
		for(int i=0;i<in.length;i++){
			out[i]=converter.apply(in[i]);
		}
		return out;
	}
	
	public static <In, Out> Out[] convert(@NotNull In[] in, @NotNull IntFunction<Out[]> array, @NotNull Function<In, Out> converter){
		Out[] out=array.apply(in.length);
		for(int i=0;i<in.length;i++){
			out[i]=converter.apply(in[i]);
		}
		return out;
	}
	
	public static <Out> Out[] convert(@NotNull int[] in, @NotNull IntFunction<Out[]> array, @NotNull IntFunction<Out> converter){
		Out[] out=array.apply(in.length);
		for(int i=0;i<in.length;i++){
			out[i]=converter.apply(in[i]);
		}
		return out;
	}
	
	public static <T> boolean contains(@NotNull T[] array, @Nullable T what){
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
	
	public static int indexOf(@NotNull int[] array, int what){
		return indexOf(array, 0, what);
	}
	
	public static int indexOf(@NotNull int[] array, int start, int what){
		for(;start<array.length;start++){
			if(array[start]==what) return start;
		}
		return -1;
	}
	
	public static boolean contains(@NotNull int[] array, int what){
		for(int t : array){
			if(t==what) return true;
		}
		return false;
	}
	
	@NotNull
	public static <T extends Throwable> RuntimeException exitWithErrorMsg(Object... msg) throws T{
		String msg0=TextUtil.toString(msg);
		LogUtil.printlnEr(msg0);
		System.exit(-1);
		return new RuntimeException(msg0);
	}
	
	@NotNull
	public static byte[] longToBytes(long l){
		byte[] dest=new byte[8];
		return longToBytes(dest, l);
	}
	
	@NotNull
	public static byte[] longToBytes(@NotNull byte[] dest, long l){
		return longToBytes(dest, 0, l);
	}
	
	@NotNull
	public static byte[] longToBytes(@NotNull byte[] dest, int destStart, long l){
		for(int i=7;i >= 0;i--){
			dest[destStart+i]=(byte)(l&0xFF);
			l >>= 8;
		}
		return dest;
	}
	
	public static long bytesToLong(@NotNull byte[] b){
		return bytesToLong(0, b);
	}
	
	public static long bytesToLong(int start, @NotNull byte[] b){
		long result=0;
		for(int i=0;i<8;i++){
			result<<=8;
			result|=(b[start+i]&0xFF);
		}
		return result;
	}
	
	@NotNull
	public static <In1, In2, Out> List<Out> combine(@NotNull List<In1> in1, @NotNull List<In2> in2, @NotNull BiFunction<In1, In2, Out> converter){
		int       size=Math.min(in1.size(), in2.size());
		List<Out> out =new ArrayList<>(size);
		for(int i=0;i<size;i++){
			out.add(converter.apply(in1.get(i), in2.get(i)));
		}
		return out;
	}
	
	@NotNull
	public static <In1, In2, Out> Out[] combine(@NotNull In1[] in1, @NotNull In2[] in2, @NotNull IntFunction<Out[]> array, @NotNull BiFunction<In1, In2, Out> converter){
		int   size=Math.min(in1.length, in2.length);
		Out[] out =array.apply(size);
		for(int i=0;i<size;i++){
			out[i]=converter.apply(in1[i], in2[i]);
		}
		return out;
	}
	
	@NotNull
	@SuppressWarnings("unchecked")
	public static <T> T[] concatenate(@NotNull T[] a, @NotNull T[] b){
		int aLen=a.length;
		int bLen=b.length;
		
		T[] c=array((Class<T>)a.getClass().getComponentType(), aLen+bLen);
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		
		return c;
	}
	
	
	public static void parallelFor(@NotNull int[] array, int threads, @NotNull IntIntConsumer consumer){
		if(threads<=1){
			for(int i=0;i<array.length;i++){
				consumer.accept(i, array[i]);
			}
			return;
		}
		int chunkSize=array.length/threads;
		
		IntStream.range(0, threads).parallel().forEach(chunkId->{
			for(int i=chunkId*chunkSize, j=Math.min(i+chunkSize, array.length);i<j;i++){
				consumer.accept(i, array[i]);
			}
		});
	}
	
	
	public static boolean checkFlag(byte flags, byte flag){
		return (flags&flag)==flag;
	}
	
	public static boolean checkFlag(short flags, short flag){
		return (flags&flag)==flag;
	}
	
	public static boolean checkFlag(int flags, int flag){
		return (flags&flag)==flag;
	}
	
	public static boolean checkFlag(long flags, long flag){
		return (flags&flag)==flag;
	}
	
	@NotNull
	public static UUID hashMD5(@NotNull ByteBuffer input){
		try{
			MessageDigest md5=MessageDigest.getInstance("MD5");
			md5.update(input);
			return UUID.nameUUIDFromBytes(md5.digest());
		}catch(NoSuchAlgorithmException e){
			throw uncheckedThrow(e);
		}
	}
	
	@NotNull
	public static UUID hashMD5(@NotNull String input){
		return hashMD5(input.getBytes());
	}
	
	@NotNull
	public static UUID hashMD5(@NotNull byte[] input){
		try{
			MessageDigest md5=MessageDigest.getInstance("MD5");
			return UUID.nameUUIDFromBytes(md5.digest(input));
		}catch(NoSuchAlgorithmException e){
			throw uncheckedThrow(e);
		}
	}
	
	@NotNull
	public static String getAppData(){
		String path;
		String OS=System.getProperty("os.name").toUpperCase();
		if(OS.contains("WIN")) path=System.getenv("APPDATA");
		else if(OS.contains("MAC")) path=System.getProperty("user.home")+"/Library/";
		else if(OS.contains("NUX")) path=System.getProperty("user.home");
		else path=System.getProperty("user.dir");
		
		return path+"/";
	}
	
	public static <T> T any(T t1, T t2){
		return t1!=null?t1:t2;
	}
	
	@NotNull
	public static String fileExtension(@NotNull String fileName){
		int i=fileName.lastIndexOf('.');
		int p=Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		if(i>p) return fileName.substring(i+1);
		return "";
	}
	
	public static int bytesToInt(byte[] bytes, int offset){
		int ret=0;
		for(int i=0;i<4&&i+offset<bytes.length;i++){
			ret<<=8;
			ret|=(int)bytes[i]&0xFF;
		}
		return ret;
	}
	
}
