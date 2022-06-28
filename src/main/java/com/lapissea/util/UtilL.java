package com.lapissea.util;

import com.lapissea.util.function.IntIntConsumer;
import com.lapissea.util.function.UnsafeConsumer;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.lapissea.util.UtilL.OS.LINUX;
import static com.lapissea.util.UtilL.OS.MACOS;
import static com.lapissea.util.UtilL.OS.WINDOWS;
import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("unused")
public class UtilL{
	
	
	public static final double SQRT2D  =Math.sqrt(2);
	public static final float  SQRT2F  =(float)SQRT2D;
	public static final byte[] NO_BYTES=new byte[0];
	
	public static final int MS=1_000;
	public static final int NS=1_000_000;
	
	
	public static boolean isArray(@Nullable Object object){
		if(object==null) return false;
		return object instanceof Class?((Class)object).isArray():object.getClass().isArray();
	}
	
	public static boolean TRUE(){
		return true;
	}
	
	public static void sleep(long millis){
		try{
			Thread.sleep(millis);
		}catch(InterruptedException ignored){}
	}
	
	public static void sleep(long millis, float nanoUnit){
		sleep(millis, (int)(NS*nanoUnit));
	}
	
	public static void sleep(float millis){
		sleep((long)millis, (millis%1));
	}
	
	public static void sleep(double millis){
		sleep((long)millis, (float)(millis%1));
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
		
		T[] a=(T[])UtilL.array(list.get(0).getClass(), 1);
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
	
	@NotNull
	@SuppressWarnings("unchecked")
	public static <T> T[] array(T[] arr, int newLength){
		return (T[])array(arr.getClass().getComponentType(), newLength);
	}
	
	/**
	 * left instanceof right
	 */
	public static boolean instanceOf(@NotNull Class<?> left, @NotNull Class<?> right){
		if(left==right) return true;
		return right.isAssignableFrom(left);
	}
	
	public static boolean instanceOfObj(@Nullable Object left, @NotNull Class<?> right){
		return left!=null&&instanceOf(left.getClass(), right);
	}
	
	@NotNull
	public static <T> T deserialize(@NotNull String s){
		byte[] data=s.getBytes(UTF_8);
		try(ObjectInputStream ois=new ObjectInputStream(new ByteArrayInputStream(data))){
			//noinspection unchecked
			return (T)ois.readObject();
		}catch(Exception e){
			throw uncheckedThrow(e);
		}
	}
	
	@NotNull
	public static String serialize(@NotNull Serializable o) throws IOException{
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		ObjectOutputStream    oos =new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		//		return Base64.getEncoder().encodeToString(baos.toByteArray());
		return new String(baos.toByteArray(), UTF_8);
	}
	
	public static void closeSilenty(@NotNull Closeable closeable){
		try{
			closeable.close();
		}catch(IOException ignored){}
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
	public static Field getDeepDeclaredField(@NotNull Class<?> type, String name) throws NoSuchFieldException{
		
		try{
			return type.getDeclaredField(name);
		}catch(NoSuchFieldException e){
			if(type==Object.class) throw e;
			return getDeepDeclaredField(type.getSuperclass(), name);
		}
	}
	
	@NotNull
	public static String compress(@NotNull String data){
		try{
			return new String(compress(data.getBytes(UTF_8)));
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	@NotNull
	public static byte[] compress(@Nullable byte[] data){
		try{
			if(data==null||data.length==0) return NO_BYTES;
			ByteArrayOutputStream obj=new ByteArrayOutputStream();
			try(GZIPOutputStream gzip=new GZIPOutputStream(obj)){
				gzip.write(data);
			}
			return obj.toByteArray();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	@NotNull
	public static String decompress(@Nullable final String compressed){
		return new String(decompress(compressed.getBytes(UTF_8)), UTF_8);
	}
	
	@NotNull
	public static byte[] decompress(@Nullable final byte[] compressed){
		try{
			if(compressed==null||compressed.length==0) return ZeroArrays.ZERO_BYTE;
			
			if(isCompressed(compressed)){
				try(GZIPInputStream gis=new GZIPInputStream(new ByteArrayInputStream(compressed))){
					ByteArrayOutputStream builder=new ByteArrayOutputStream();
					byte[]                buff   =new byte[Math.min(2048, compressed.length)];
					
					int read;
					while((read=gis.read(buff, 0, buff.length))>=0){
						builder.write(buff, 0, read);
					}
					
					return builder.toByteArray();
				}
			}
			
			return compressed;
			
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static boolean isCompressed(byte[] compressed){
		return compressed.length>1&&compressed[0]==(byte)GZIPInputStream.GZIP_MAGIC&&compressed[1]==(byte)(GZIPInputStream.GZIP_MAGIC>>8);
	}
	
	
	public static Class<?> findObjectClosestCommonSuper(@NotNull Collection<?> objects){
		return findObjectClosestCommonSuper(objects.stream());
	}
	
	public static Class<?> findObjectClosestCommonSuper(@NotNull Stream<?> objects){
		return findClosestCommonSuper(objects.filter(Objects::nonNull)
		                                     .map(Object::getClass));
	}
	
	public static Class<?> findClosestCommonSuper(@NotNull Collection<Class<?>> classes){
		return findClosestCommonSuper(classes.stream());
	}
	
	public static Class<?> findClosestCommonSuper(@NotNull Stream<Class<?>> classes){
		return classes.reduce(UtilL::findClosestCommonSuper)
		              .orElse(Object.class);
	}
	
	@NotNull
	public static Class<?> findClosestCommonSuper(@NotNull Class<?> a, @NotNull Class<?> b){
		if(a==b) return a;
		if(a==Object.class||b==Object.class) return Object.class;
		Class<?> s=a;
		while(!s.isAssignableFrom(b)){
			s=s.getSuperclass();
		}
		return s;
	}
	
	@NotNull
	public static byte[] readAll(@NotNull InputStream is){
		ByteArrayOutputStream buffer;
		try{
			buffer=new ByteArrayOutputStream();
			
			int    nRead;
			byte[] data=new byte[1024];
			
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
		BufferedReader b=new BufferedReader(new InputStreamReader(stream, UTF_8));
		
		for(String line;(line=b.readLine())!=null;){
			cons.accept(line);
		}
		
	}
	
	public static <T> void forEach(@NotNull T[] ts, @NotNull Consumer<T> consumer){
		for(T t : ts) consumer.accept(t);
	}
	
	public static void sleepUntil(@NotNull BooleanSupplier checkUntil){
		sleepUntil(checkUntil, 1);
	}
	
	public static void sleepUntil(@NotNull BooleanSupplier checkUntil, long ms){
		while(!checkUntil.getAsBoolean()) sleep(ms);
	}
	
	public static void sleepUntil(@NotNull BooleanSupplier checkUntil, long ms, int ns){
		while(!checkUntil.getAsBoolean()) sleep(ms, ns);
	}
	
	public static void sleepUntil(@NotNull BooleanSupplier checkUntil, long ms, float nsUnit){
		while(!checkUntil.getAsBoolean()) sleep(ms, (int)(NS*nsUnit));
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
	
	public static void sleepWhile(@NotNull BooleanSupplier checkWhile, long ms, float nsUnit){
		while(checkWhile.getAsBoolean()) sleep(ms, (int)(NS*nsUnit));
	}
	
	public static <T> T waitForNotNull(@NotNull Supplier<T> getter, long ms, float nsUnit){
		while(true){
			T val=getter.get();
			if(val!=null) return val;
			sleep(ms, (int)(NS*nsUnit));
		}
	}
	
	public static <T> T waitForNotNull(@NotNull Supplier<T> getter, long ms){
		while(true){
			T val=getter.get();
			if(val!=null) return val;
			sleep(ms);
		}
	}
	
	public static <T> T waitForNotNull(@NotNull Supplier<T> getter){
		return waitForNotNull(getter, 1);
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
		public int read(@NotNull byte[] b) throws IOException{
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
	public static <In, Out> Out[] convert(@NotNull Collection<In> in, IntFunction<Out[]> newArray, @NotNull Function<In, Out> converter){
		Out[] out=newArray.apply(in.size());
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
	
	public static <In, Out> Out[] convert(@NotNull In[] in, @NotNull Out[] dest, @NotNull Function<In, Out> converter){
		for(int i=0;i<in.length;i++){
			dest[i]=converter.apply(in[i]);
		}
		return dest;
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
	public static RuntimeException exitWithErrorMsg(Object... msg){
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
		for(int i=7;i>=0;i--){
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
		
		Class<?> at =a.getClass().getComponentType();
		Class<?> bt =b.getClass().getComponentType();
		Class<?> typ=findClosestCommonSuper(at, bt);
		
		T[] c=array((Class<T>)typ, aLen+bLen);
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
		switch(getOS()){
			case WINDOWS:
				path=System.getenv("APPDATA"); break;
			case LINUX:
				path=System.getProperty("user.home"); break;
			case MACOS:
				path=System.getProperty("user.home")+"/Library/"; break;
			default:
				path=System.getProperty("user.dir"); break;
		}
		
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
			ret|=(int)bytes[i+offset]&0xFF;
		}
		return ret;
	}
	
	public enum OS{
		WINDOWS("dll"), LINUX("so"), MACOS("jnilib");
		
		public final String nativeLibExtension;
		
		OS(String nativeLibExtension){this.nativeLibExtension=nativeLibExtension;}
	}
	
	private static OS OS;
	
	public static synchronized OS getOS(){
		if(OS==null){
			String os=System.getProperty("os.name").toLowerCase();
			
			if(os.contains("win")) OS=WINDOWS;
			else if(os.contains("linux")) OS=LINUX;
			else if(os.contains("mac")) OS=MACOS;
		}
		return OS;
	}
	
	public static boolean isInJar(Class<?> clazz){
		return new File(clazz.getProtectionDomain()
		                     .getCodeSource()
		                     .getLocation()
		                     .getPath()
		)
			       .getName()
			       .endsWith(".jar");
	}
	
	public static <T extends Comparable<T>> int addRemainSorted(List<T> list, T value){
		if(list.isEmpty()){
			list.add(value);
			return 0;
		}
		
		if(value.compareTo(list.get(0))<0){
			list.add(0, value);
			return 0;
		}
		if(value.compareTo(list.get(list.size()-1))>0){
			list.add(value);
			return list.size();
		}
		
		int lo=0;
		int hi=list.size()-1;
		
		while(lo<=hi){
			int mid=(hi+lo)/2;
			
			int comp=value.compareTo(list.get(mid));
			if(comp<0){
				hi=mid-1;
			}else if(comp>0){
				lo=mid+1;
			}else{
				list.add(mid, value);
				return mid;
			}
		}
		
		list.add(lo, value);
		return lo;
	}
	
	public static void Assert(boolean condition){
		if(!condition){
			throw new AssertionError();
		}
	}
	
	public static void Assert(boolean condition, Object... message){
		if(!condition) throw new AssertionError(TextUtil.toString(message));
	}
	
	public static void Assert(boolean condition, String message){
		if(!condition) throw new AssertionError(message);
	}
	
	public static RuntimeException sysExit(int exitCode) throws RuntimeException{
		System.exit(0);
		throw new RuntimeException();
	}
	
	public static <K extends Enum<K>, V> EnumMap<K, V> generateEnumMap(@NotNull Class<K> enumType, @NotNull Function<K, V> generator){
		EnumMap<K, V> map=new EnumMap<>(enumType);
		
		for(K k : enumType.getEnumConstants()){
			map.put(k, generator.apply(k));
		}
		
		return map;
	}
	
	private static final Pattern NUMERIC_PATTERN=Pattern.compile("-?\\d+(\\.\\d+)?");
	
	public static boolean isNumeric(CharSequence str){
		return NUMERIC_PATTERN.matcher(str).matches();
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Annotation> Class<T> getAnnotationInterface(T annotation){
		return getAnnotationInterface((Class<T>)annotation.getClass());
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Annotation> Class<T> getAnnotationInterface(Class<? extends T> proxyClass){
		return (Class<T>)Arrays.stream(proxyClass.getInterfaces())
		                       .filter(c->instanceOf(proxyClass, Annotation.class))
		                       .findAny()
		                       .orElseThrow(()->new IllegalArgumentException(proxyClass+" not an annotation"));
	}
	
	public static <T> T sysPropertyByClass(Class<?> targetClass, String varName, T defaultValue, Function<String, T> map){
		return sysPropertyByClass(targetClass, varName).map(map).orElse(defaultValue);
	}
	
	public static Optional<String> sysPropertyByClass(Class<?> targetClass, String varName){
		return Optional.ofNullable(System.getProperty(targetClass.getName()+(varName==null||varName.isEmpty()?"":"."+varName)));
	}
}
