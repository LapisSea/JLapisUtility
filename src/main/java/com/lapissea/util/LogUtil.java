package com.lapissea.util;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.lapissea.util.TextUtil.IN_TABLE_TO_STRINGS;
import static com.lapissea.util.TextUtil.JSON_NULL_PRINT;
import static com.lapissea.util.TextUtil.mapObjectValues;
import static com.lapissea.util.TextUtil.stringFill;
import static com.lapissea.util.UtilL.checkFlag;

@SuppressWarnings({"rawtypes", "unused"})
public class LogUtil{
	
	private static final Map<String, String> SKIP_METHODS = new ConcurrentHashMap<>();
	private static final Set<String>         SKIP_CLASSES = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public static void registerSkipMethod(Method method){
		SKIP_METHODS.put(method.getName(), method.getDeclaringClass().getName());
	}
	
	public static void registerSkipClass(Class<?> type){
		SKIP_CLASSES.add(type.getName());
	}
	
	static{
		registerSkipClass(PrintStream.class);
		registerSkipClass(LogUtil.class);
	}
	
	private static final long START_TIME = System.currentTimeMillis();
	
	@SuppressWarnings("PointlessBitwiseExpression")
	public static class Init{
		
		private Init(){ }
		
		private static boolean attached;
		
		public static final int USE_CALL_POS           = 1<<0;
		public static final int USE_CALL_POS_CLICKABLE = 1<<1;
		public static final int USE_CALL_THREAD        = 1<<2;
		public static final int DISABLED               = 1<<3;
		public static final int USE_TABULATED_HEADER   = 1<<4;
		public static final int USE_TIME_DELTA         = 1<<5;
		
		public static PrintStream OUT = System.out;
		public static PrintStream ERR = System.err;
		
		static{
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				synchronized(System.out){
					synchronized(System.err){
						detach();
						System.out.flush();
						System.err.flush();
					}
				}
			}, LogUtil.class.getSimpleName() + "-Flush"));
		}
		
		/**
		 * @param flags Use or operator on defined fields to get a desired effect.
		 * @see #USE_CALL_POS
		 * @see #USE_CALL_POS_CLICKABLE
		 * @see #DISABLED
		 */
		public static void attach(int flags){
			if(attached){
				detach();
			}
			if(checkFlag(flags, DISABLED)) return;
			
			attached = true;
			
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
			
			BiFunction<Boolean, StackTraceElement, String> header = getHeader(flags);
			
			System.setOut(new P(new DebugHeaderStream(System.out, false, header)));
			System.setErr(new P(new DebugHeaderStream(System.err, true, header)));
			
		}
		
		private static final Pattern KOTLIN_COMPANION_ANNOTATION = Pattern.compile("\\$Proxy[0-9]+]");
		
		private static String filterClassName(String className){
			
			//check and remove companion class
			String companionMarker = "$Companion";
			
			int    end = className.indexOf(companionMarker) + companionMarker.length();
			String s   = className.length()>=end? className.substring(0, end) : className;
			
			if(s.endsWith(companionMarker)){
				try{
					Class comp = Class.forName(s);
					if(Arrays.stream(comp.getAnnotations()).anyMatch(a -> a.getClass().getSimpleName().contains("$Proxy"))){
						s = s.substring(0, s.lastIndexOf(companionMarker));
					}
					return s;
				}catch(ClassNotFoundException e){ }
			}
			
			return className;
		}
		
		private static BiFunction<Boolean, StackTraceElement, String> getHeader(int flags){
			
			boolean tabulated = checkFlag(flags, USE_TABULATED_HEADER);
			
			TabPart part = null;
			
			if(checkFlag(flags, USE_CALL_THREAD)){
				part = new TabPart(s -> "[" + Thread.currentThread().getName() + "]", null, tabulated);
			}
			
			if(checkFlag(flags, USE_CALL_POS_CLICKABLE)){
				part = new TabPart(s -> "[" + s.toString() + "]", part, tabulated);
			}else if(checkFlag(flags, USE_CALL_POS)){
				part = new TabPart(stack -> {
					String methodName = stack.getMethodName();
					if(methodName.startsWith("lambda$")) methodName = methodName.substring(7);
					
					String className = filterClassName(stack.getClassName());
					
					int clip = Math.max(className.lastIndexOf('.'), className.lastIndexOf('$')) + 1;
					className = className.substring(clip);
					
					return "[" + className + '.' + methodName + ':' + stack.getLineNumber() + "]";
				}, part, tabulated);
			}
			
			if(checkFlag(flags, USE_TIME_DELTA)){
				part = new TabPart(new Function<StackTraceElement, String>(){
					long last = -1;
					
					@Override
					public String apply(StackTraceElement s){
						long t = System.nanoTime();
						if(last == -1){
							last = t;
							return "[--]";
						}
						long delta = t - last;
						last = t;
						
						if(delta<UtilL.NS){
							int smol = (int)Math.round((delta/(double)UtilL.NS)*10);
							if(smol == 0) return "[--]";
							String p = String.valueOf(smol/10F).substring(1);
							return "[" + p + "ms]";
						}
						
						double deltaUnit = delta/(double)UtilL.NS;
						String unit      = "ms";
						reduce:
						{
							if(deltaUnit>1000){
								deltaUnit /= 1000;
								unit = "s";
							}else break reduce;
							if(deltaUnit>60){
								deltaUnit /= 60;
								unit = "min";
							}else break reduce;
							if(deltaUnit>60){
								deltaUnit /= 60;
								unit = "hours";
							}else break reduce;
							if(deltaUnit>24){
								deltaUnit /= 24;
								unit = "days";
							}else break reduce;
						}
						return "[" + Math.round(deltaUnit*10)/10 + unit + "]";
					}
				}, part, tabulated);
			}
			
			if(part == null) return (b, s) -> "";
			TabPart finalPart = part;
			return (b, s) -> finalPart.output(b, 1, s);
		}
		
		@Deprecated
		public static void destroy(){
			detach();
		}
		
		public static void detach(){
			if(!attached) return;
			if(OUT == null) return;
			System.setOut(OUT);
			System.setErr(ERR);
			OUT = null;
			ERR = null;
		}
		
		@SuppressWarnings({"AutoBoxing", "AutoUnboxing"})
		private static final class TabPart{
			
			private       Map<Integer, Long>                  sizeTimeTable = new HashMap<>();
			private final Function<StackTraceElement, String> toStr;
			private final boolean                             tabulated;
			private final TabPart                             next;
			
			public TabPart(Function<StackTraceElement, String> toStr, TabPart next, boolean tabulated){
				this.toStr = toStr;
				this.next = next;
				this.tabulated = tabulated;
			}
			
			public String getTab(int size){
				if(!tabulated) return "";
				
				long tim = System.nanoTime();
				
				sizeTimeTable.entrySet().removeIf(e -> e.getValue() + 500*1000_000<tim);
				
				int max = sizeTimeTable.keySet().stream().mapToInt(i -> i).max().orElse(0);
				if(size>max && next != null){
					next.reduce(size - max);
				}
				
				sizeTimeTable.put(size, tim);
				
				int tabSize   = Math.max(0, max - size);
				int totalSize = tabSize + size;
				
				return TextUtil.stringFill(tabSize, ' ');
			}
			
			
			public void reduce(int amount){
				if(!tabulated) return;
				int max = sizeTimeTable.keySet().stream().mapToInt(i -> i).max().orElse(0);
				sizeTimeTable =
					sizeTimeTable.entrySet()
					             .stream()
					             .filter(e -> e.getKey()>=amount)
					             .collect(Collectors.toMap(e -> e.getKey() - amount, Map.Entry::getValue));
				
				if(amount>max && next != null){
					next.reduce(amount - max);
				}
			}
			
			public String output(boolean color, int i, StackTraceElement stackTraceElement){
				String str = toStr.apply(stackTraceElement);
				String tab = getTab(str.length());
				
				String col;
				if(color){
					if(i%2 == 0) col = ConsoleColors.BLACK_BRIGHT;
					else col = ConsoleColors.RESET;
				}else col = "";
				
				return col + str + tab + (next == null? ": " + ConsoleColors.RESET : next.output(color, i + 1, stackTraceElement));
			}
		}
		
		private static final class DebugHeaderStream extends OutputStream{
			
			private static final class LineBuild extends ByteArrayOutputStream{
				private byte[] buf(){
					return buf;
				}
			}
			
			private final OutputStream child;
			private       LineBuild    lineBuild   = new LineBuild();
			private       boolean      needsHeader = true;
			private final boolean      err;
			
			private final BiFunction<Boolean, StackTraceElement, String> header;
			
			private final StringBuilder sb = new StringBuilder();
			
			public DebugHeaderStream(OutputStream child, boolean err, BiFunction<Boolean, StackTraceElement, String> header){
				this.err = err;
				this.header = header;
				this.child = child;
			}
			
			@Override
			public synchronized void flush() throws IOException{
				StringBuilder startCol = null;
				startColor:
				if(!err){
					Reader reader = reader();
					int    c;
					while((c = reader.read()) != -1){
						if(Character.isWhitespace((char)c)) continue;
						if(c == '\033'){
							if(reader.read() != '[') break startColor;
							StringBuilder col = new StringBuilder("\033[");
							while(true){
								int c1 = reader.read();
								col.append((char)c1);
								if(c1 != ';' && !(c1>='0' && c1<='9')){
									if(c1 == 'm'){
										startCol = col;
									}
									break startColor;
								}
							}
						}else break startColor;
					}
				}
				sb.setLength(0);
				if(startCol != null) sb.append(startCol);
				String h = header(!err && startCol == null);
				sb.append(h);
				
				Reader reader = reader();
				while(true){
					int bi = reader.read();
					if(bi == -1) break;
					char b = (char)bi;
					
					if(needsHeader){
						needsHeader = false;
						for(int i = 0; i<h.length(); i++){
							sb.append(' ');
						}
					}
					sb.append(b);
					if(b == '\n'){
						needsHeader = true;
					}
				}
				
				if(lineBuild.buf().length>lineBuild.size()*2) lineBuild = new LineBuild();
				lineBuild.reset();
				child.write(sb.toString().getBytes());
				child.flush();
				if(sb.capacity()>sb.length()*2) sb.trimToSize();
				
				TABLE_LAST_FLAG = TABLE_FLAG;
				if(TABLE_FLAG){
					TABLE_FLAG = false;
				}else{
					TABLE_COLUMNS.clear();
				}
			}
			
			private Reader reader(){
				return new InputStreamReader(new ByteBufferBackedInputStream(ByteBuffer.wrap(lineBuild.buf(), 0, lineBuild.size())));
			}
			
			private String header(boolean color){
				if(!needsHeader) return "";
				needsHeader = false;
				
				try{
					return debugHeader(color);
				}catch(Exception e){
					detach();
					e.printStackTrace();
					throw UtilL.sysExit(-1);
				}
			}
			
			@Override
			public void write(int b){
				if(b == '\r') return;
				
				if(b == '\n'){
					for(int i = 0; i<TextUtil.NEW_LINE.length(); i++){
						lineBuild.write(TextUtil.NEW_LINE.charAt(i));
					}
				}else{
					lineBuild.write(b);
				}
			}
			
			private String debugHeader(boolean color){
				StackTraceElement stack = getCallStack();
				boolean           shouldPrint;
				shouldPrint = !stack.getClassName().equals(Throwable.class.getName() + "$WrappedPrintStream");
				if(shouldPrint) shouldPrint = !stack.getClassName().equals(ThreadGroup.class.getName());
				
				if(shouldPrint){
					return header.apply(color, stack);
				}
				return "";
			}
			
			private StackTraceElement getCallStack(){
				StackTraceElement[] trace = Thread.currentThread().getStackTrace();
				
				int depth = 0;
				
				for(int i = depth; i<trace.length; i++){
					String clazz  = trace[i].getClassName();
					String method = trace[i].getMethodName();
					if(clazz.equals(PrintStream.class.getName()) &&
					   (method.equals("print") || method.equals("println"))){
						depth = i;
						break;
					}
				}
				
				for(int i = depth; i<trace.length; i++){
					String clazz = trace[i].getClassName();
					if(clazz.equals(LogUtil.class.getName())){
						depth = i;
						break;
					}
				}
				
				depth--;
				
				while(true){
					depth++;
					String stackClassName = trace[depth].getClassName();
					if(stackClassName.startsWith("java.util")) continue;
					if(stackClassName.startsWith("java.lang")) continue;
					if(SKIP_CLASSES.contains(stackClassName)) continue;
					String cName = SKIP_METHODS.get(trace[depth].getMethodName());
					if(stackClassName.equals(cName)) continue;
					if(trace[depth].getMethodName().startsWith("forEach")) continue;
					break;
				}
				
				return trace[depth];
			}
		}
		
		private static class SplitStream extends OutputStream{
			
			private final OutputStream s1, s2;
			
			public SplitStream(OutputStream s1, OutputStream s2){
				this.s1 = s1;
				this.s2 = s2;
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
		out(obj + "\n");
	}
	
	public static void println(float obj){
		out(obj + "\n");
	}
	
	public static void println(long obj){
		out(obj + "\n");
	}
	
	public static void println(double obj){
		out(obj + "\n");
	}
	
	public static void println(char obj){
		out(obj + "\n");
	}
	
	public static void println(byte obj){
		out(obj + "\n");
	}
	
	public static void println(boolean obj){
		out(obj + "\n");
	}
	
	public static void println(short obj){
		out(obj + "\n");
	}
	
	public static void println(String obj){
		out(obj + "\n");
	}
	
	public static void println(Object obj){
		out(TextUtil.toString(obj) + "\n");
	}
	
	public static void print(Object... objs){
		out(TextUtil.toString(objs));
	}
	
	public static void println(Object... objs){
		out(TextUtil.toString(objs) + "\n");
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
		err(TextUtil.toString(obj) + "\n");
	}
	
	public static void printEr(Object... objs){
		err(TextUtil.toString(objs));
	}
	
	public static void printlnEr(Object... objs){
		err(TextUtil.toString(objs) + "\n");
	}
	
	//================================================
	
	public static void printFunctionTrace(int count, CharSequence splitter){
		println(getFunctionTrace(count, splitter));
	}
	
	public static String getFunctionTrace(int count, CharSequence splitter){
		StringBuilder line = new StringBuilder();
		
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		if(count>=trace.length) count = trace.length - 1;
		for(int i = count + 1; i>=2; i--){
			line.append(trace[i].getMethodName()).append('(').append(trace[i].getLineNumber()).append(')');
			if(i != 2) line.append(splitter);
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
		StringBuilder result = new StringBuilder();
		
		if(msg == null){
			result.append("Invoke time: ").append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())).append("\n");
		}else result.append(msg).append("\n");
		
		int length = 0;
		for(int i = 2; i<a1.length; i++){
			StackTraceElement a = a1[i];
			String            s = a.toString();
			result.append(s).append("\n");
			length = Math.max(s.length(), length);
		}
		for(int b = 0; b<=length/4; b++)
			result.append("_/\\_");
		
		printlnEr(result);
	}
	
	//================================================
	
	private static class TableColumn{
		final String name;
		int width;
		
		public TableColumn(String name){
			this.name = name;
			this.width = name.length();
		}
		
		void gibWidth(int w){
			width = Math.max(width, w);
		}
		
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder(width + 2);
			sb.append(' ').append(name);
			for(int i = 0; i<width - name.length(); i++){
				sb.append(' ');
			}
			sb.append(' ');
			return sb.toString();
		}
	}
	
	private static final List<TableColumn> TABLE_COLUMNS   = new ArrayList<>(1);
	private static       boolean           TABLE_FLAG      = false;
	private static       boolean           TABLE_LAST_FLAG = false;
	
	private static boolean notKotlinData(Class c){
		List<Method> functs    = new ArrayList<>(Arrays.asList(c.getDeclaredMethods()));
		Field[]      fs        = c.getDeclaredFields();
		int          compCount = (int)functs.stream().filter(m -> m.getName().matches("component[0-9]+")).count();
		
		
		if(fs.length != compCount) return true;
		
		for(Field f : fs){
			String n = f.getName();
			if(!functs.removeIf(t -> t.getName().equals("get" + TextUtil.firstToUpperCase(n)))) return true;
		}
		
		return false;
	}
	
	//returns null if object type is suboptimal
	private static final List<Function<Object, Map<String, String>>> OBJECT_SCANNERS = new CopyOnWriteArrayList<>(Arrays.asList(
		map -> map instanceof Map? ((Map<?, ?>)map).entrySet().stream().collect(Collectors.toMap((Map.Entry e) -> IN_TABLE_TO_STRINGS.toString(e.getKey()), (Map.Entry e) -> IN_TABLE_TO_STRINGS.toString(e.getValue()))) : null,
		//kotlin object
		row -> {
			Class<?> c = row.getClass();
			if(notKotlinData(c)) return null;
			
			Field[]                       fs    = c.getDeclaredFields();
			LinkedHashMap<String, String> table = new LinkedHashMap<>(fs.length);
			for(Field f : fs){
				f.setAccessible(true);
				try{
					table.put(f.getName(), IN_TABLE_TO_STRINGS.toString(f.get(row)));
				}catch(IllegalAccessException ignored){ }
			}
			
			return table;
		},
		//regular object
		o -> {
			LinkedHashMap<String, String> data = new LinkedHashMap<>();
			
			mapObjectValues(o, (name, obj) -> {
				if(!JSON_NULL_PRINT && obj == null) return;
				data.put(name, IN_TABLE_TO_STRINGS.toString(obj));
			});
			
			if(data.isEmpty()) data.put("hash", o.hashCode() + "");
			
			return data;
		}
	));
	
	public static void registerCustomObjectScannerRaw(Function<Object, Map<String, String>> scanner){
		OBJECT_SCANNERS.add(scanner);
	}
	public static void registerCustomObjectScanner(Function<Object, Map<String, Object>> scanner){
		OBJECT_SCANNERS.add(o -> {
			Map<String, Object> map = scanner.apply(o);
			for(Map.Entry<String, Object> e : map.entrySet()){
				if(e.getValue() instanceof String) continue;
				e.setValue(TextUtil.toString(e.getValue()));
			}
			//noinspection unchecked
			return (Map<String, String>)(Object)map;
		});
	}
	
	private static Map<String, String> objectToMap(Object row){
		for(Function<Object, Map<String, String>> objectScanner : OBJECT_SCANNERS){
			Map<String, String> mapped = objectScanner.apply(row);
			if(mapped != null) return mapped;
		}
		throw new RuntimeException();
	}
	
	public static void printTable(Object row){
		
		Class c = row.getClass();
		
		if(c.isArray()){
			printTable((Object[])row);
			return;
		}
		
		if(row instanceof Iterable){
			List<Map<String, String>> collective = new ArrayList<>();
			Set<String>               names      = new HashSet<>();
			
			for(Object r : ((Iterable)row)){
				
				Map<String, String> mapped = objectToMap(r);
				names.addAll(mapped.keySet());
				
				collective.add(mapped);
				
				for(Map<String, String> ro : collective){
					for(String name : names){
						ro.putIfAbsent(name, "null");
					}
				}
				
				
			}
			
			if(collective.size()>1){
				int[] growthProtection = new int[names.size()];
				
				for(Map<String, String> r : collective){
					Iterator<String> iter = r.values().iterator();
					
					for(int i = 0; i<growthProtection.length; i++){
						int len = iter.next().length();
						if(growthProtection[i]<len){
							growthProtection[i] = len;
						}
					}
				}
				
				Iterator<Map.Entry<String, String>> iter = collective.get(0).entrySet().iterator();
				
				for(int max : growthProtection){
					Map.Entry<String, String> e = iter.next();
					
					int len = e.getValue().length();
					if(max>len){
						e.setValue(e.getValue() + stringFill(max - len, ' '));
					}
				}
			}
			
			for(Map<String, String> r : collective){
				printTable(r);
			}
			return;
		}
		
		printTable(objectToMap(row));
		
	}
	
	public static void printTable(@NotNull Object... row){
		
		synchronized(System.out){
			if(row.length%2 != 0) throw new IllegalArgumentException();
			
			Map<Object, Object> table = new LinkedHashMap<>();
			for(int i = 0, j = row.length/2; i<j; i++){
				table.put(row[i*2], row[i*2 + 1]);
			}
			printTable(table);
		}
	}
	
	public static void printTable(@NotNull Object[] rowNames, @NotNull Object... rowValues){
		synchronized(System.out){
			assert rowNames.length == rowValues.length;
			
			Map<Object, Object> table = new LinkedHashMap<>();
			for(int i = 0, j = rowNames.length; i<j; i++){
				table.put(rowNames[i], rowValues[i]);
			}
			printTable(table);
		}
	}
	
	public static void printTable(String keyName, String valueName, Map<?, ?> data){
		if(data.isEmpty()) return;
		
		List<String> keys  = new ArrayList<>(data.size()), values = new ArrayList<>(data.size());
		int[]        sizes = {keyName.length(), valueName.length()};
		
		data.forEach((key, value) -> {
			String k, v;
			keys.add(k = TextUtil.toString(key));
			values.add(v = TextUtil.toString(value));
			
			sizes[0] = Math.max(sizes[0], k.length());
			sizes[1] = Math.max(sizes[1], v.length());
		});
		String k = keys.get(0);
		if(k.length()<sizes[0]){
			keys.set(0, k + TextUtil.stringFill(sizes[0] - k.length(), ' '));
		}
		String v = values.get(0);
		if(v.length()<sizes[1]){
			values.set(0, v + TextUtil.stringFill(sizes[1] - v.length(), ' '));
		}
		
		Map<String, String> row = new LinkedHashMap<>(2);
		
		for(int i = 0; i<data.size(); i++){
			row.put(keyName, keys.get(i));
			row.put(valueName, values.get(i));
			printTable(row);
			row.clear();
		}
		
	}
	
	public static void printTable(Map<?, ?> row){
		if(!Init.attached) Init.attach(0);
		synchronized(System.out){
			Map<String, String> rowSafe = new LinkedHashMap<>(row.size());
			
			Function<Object, String> toString = o -> TextUtil.toTableString(o).replace("\n", "\\n");
			row.forEach((k, v) -> {
				String val = toString.apply(v);
				
				//resolve tabs to local table space
				if(val.indexOf('\t') != -1){
					
					StringBuilder sb = new StringBuilder(val.length() + 20);
					
					int pos = 0;
					for(char c : val.toCharArray()){
						
						if(c != '\t'){
							sb.append(c);
							pos++;
							continue;
						}
						
						int requiredPos = (pos/4 + 1)*4;
						
						while(pos<requiredPos){
							sb.append(' ');
							pos++;
						}
						
					}
					
					val = sb.toString();
				}
				rowSafe.put(toString.apply(k), val);
			});
			
			if(TABLE_COLUMNS.stream().noneMatch(s -> rowSafe.containsKey(s.name))) TABLE_COLUMNS.clear();
			
			
			rowSafe.forEach((k, v) ->
				                TABLE_COLUMNS
					                .stream()
					                .filter(c -> c.name.equals(k))
					                .findAny()
					                .orElseGet(() -> {
						                TableColumn c = new TableColumn(k);
						                TABLE_COLUMNS.add(c);
						                TABLE_LAST_FLAG = false;
						                return c;
					                })
					                .gibWidth(v.length()));
			
			StringBuilder sb = new StringBuilder();
			if(!TABLE_LAST_FLAG){//first row
				
				String names = TABLE_COLUMNS.stream()
				                            .map(TextUtil::toTableString)
				                            .collect(Collectors.joining("|"));
				
				StringBuilder lines = new StringBuilder(names.length() + 3);
				lines.append('|');
				for(int i = 0; i<names.length(); i++){
					lines.append('=');
				}
				lines.append("|\n");
				
				sb.append(lines);
				sb.append("|").append(names).append("|\n");
				sb.append(lines);
				
			}
			
			sb.append('|');
			for(TableColumn column : TABLE_COLUMNS){
				int    left = column.width;
				String val  = rowSafe.get(column.name);
				sb.append(' ');
				if(val != null){
					left -= val.length();
					sb.append(val);
				}
				sb.append(' ');
				while(left-->0) sb.append(' ');
				sb.append('|');
			}
			sb.append('\n');
			
			TABLE_FLAG = true;
			out(sb.toString());
		}
	}
	
	//================================================
	
	
	public static class Val<T>{
		
		public interface Getter<T>{
			double get(T obj);
		}
		
		private final String    name;
		private final char      identifier;
		private final Getter<T> getter;
		
		public Val(String name, char identifier, Getter<T> getter){
			this.name = name;
			this.identifier = identifier;
			this.getter = getter;
		}
	}
	
	@SafeVarargs
	public static <T> void printGraph(T[] data, int height, boolean snapBottom, Val<T>... values){
		printGraph(data, height, -1, snapBottom, values);
	}
	
	@SafeVarargs
	public static <T> void printGraph(T[] data, int height, int width, boolean snapBottom, Val<T>... values){
		printGraph(Arrays.asList(data), height, width, snapBottom, values);
	}
	
	@SafeVarargs
	public static <T> void printGraph(Collection<T> data, int height, boolean snapBottom, Val<T>... values){
		printGraph(data, height, -1, snapBottom, values);
	}
	
	@SafeVarargs
	public static <T> void printGraph(Collection<T> data, int height, int width, boolean snapBottom, Val<T>... values){
		
		if(height<=0) throw new IllegalArgumentException("Height must be greater than 0");
		if(width<=0){
			if(width != -1) throw new IllegalArgumentException("Width must be greater than 0 or -1 for auto width");
			width = data.size();
		}
		
		//collect
		double[][] collected = new double[data.size()][values.length];
		{
			int i = 0;
			for(T datum : data){
				double[] line = collected[i];
				for(int y = 0; y<line.length; y++){
					line[y] = values[y].getter.get(datum);
				}
				i++;
			}
		}
		//normalize
		double[] min = new double[values.length];
		double[] max = new double[values.length];
		Arrays.fill(min, Double.MAX_VALUE);
		Arrays.fill(max, Double.MIN_VALUE);
		
		for(double[] line : collected){
			for(int j = 0; j<line.length; j++){
				double val = line[j];
				if(val<min[j]) min[j] = val;
				if(val>max[j]) max[j] = val;
			}
		}
		
		if(!snapBottom){
			for(int i = 0; i<min.length; i++){
				if(min[i]>0 && max[i]>0) min[i] = 0;
			}
		}
		
		for(double[] line : collected){
			for(int j = 0; j<line.length; j++){
				double unit = (line[j] - min[j])/(max[j] - min[j]);
				line[j] = unit;
			}
		}
		
		//render
		
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		
		for(int i = 0; i<collected.length - 1; i++){
			double[] current = collected[i];
			double[] next    = collected[i + 1];
			
			for(int j = 0; j<current.length; j++){
				double yCurrent = (1 - current[j])*height;
				double xCurrent = i/(double)collected.length*width;
				double yNext    = (1 - next[j])*height;
				double xNext    = (i + 1)/(double)collected.length*width;
				
				int id = j + 1;
				g.setColor(new Color(id, id, id));
				g.draw(new Line2D.Double(xCurrent, yCurrent, xNext, yNext));
			}
		}
		
		g.dispose();
		
		StringBuilder sb = new StringBuilder(2 + (width + 2)*(height + 1) + 2);
		
		sb.append("A\n");
		for(int y = 0; y<height; y++){
			sb.append('|');
			for(int x = 0; x<width; x++){
				int id = img.getRGB(x, y)&0xFF;
				sb.append(id == 0? ' ' : values[id - 1].identifier);
			}
			sb.append('\n');
		}
		for(int i = 0; i<width + 1; i++){
			sb.append('-');
		}
		sb.append(">\n");

//		LogUtil.println(2+(width+2)*(height+1)+2, sb.toString().length());
		out(sb.toString());
		println(IntStream.range(0, values.length).mapToObj(i -> values[i].name + " '" + values[i].identifier + "' - (" + min[i] + " - " + max[i] + ")").collect(Collectors.joining(", ")));
		
	}
	
	//================================================
	
	private static void out(String s){
		System.out.print(s);
	}
	
	private static void err(String s){
		System.err.print(s);
	}
	
}
