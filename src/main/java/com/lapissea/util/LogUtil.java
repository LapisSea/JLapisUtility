package com.lapissea.util;


import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.lapissea.util.TextUtil.*;
import static com.lapissea.util.UtilL.*;

@SuppressWarnings("rawtypes")
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
		
		static{
			
			Runtime.getRuntime().addShutdownHook(new Thread(()->{
				synchronized(System.out){
					synchronized(System.err){
						detach();
						System.out.flush();
						System.err.flush();
					}
				}
			}, LogUtil.class.getSimpleName()+"-Flush"));
		}
		
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
					//noinspection ResultOfMethodCallIgnored
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
		
		private static final Pattern KOTLIN_COMPANION_ANNOTATION=Pattern.compile("\\$Proxy[0-9]+]");
		
		private static String filterClassName(String className){
			
			//check and remove companion class
			String companionMarker="$Companion";
			
			int    end=className.indexOf(companionMarker)+companionMarker.length();
			String s  =className.length()>=end?className.substring(0, end):className;
			
			if(s.endsWith(companionMarker)){
				try{
					Class comp=Class.forName(s);
					if(Arrays.stream(comp.getAnnotations()).anyMatch(a->a.getClass().getSimpleName().contains("$Proxy"))){
						s=s.substring(0, s.lastIndexOf(companionMarker));
					}
					return s;
				}catch(ClassNotFoundException e){ }
			}
			
			return className;
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
						String className=filterClassName(stack.getClassName());
						String threadSt ="["+Thread.currentThread().getName()+"] ";
						String stackSt  ="["+className.substring(className.lastIndexOf('.')+1)+'.'+methodName+':'+stack.getLineNumber()+"]";
						
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
						String className=filterClassName(stack.getClassName());
						
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
			if(OUT==null) return;
			System.setOut(OUT);
			System.setErr(ERR);
			OUT=null;
			ERR=null;
		}
		
		@SuppressWarnings({"AutoBoxing", "AutoUnboxing"})
		private static final class Tabulator{
			
			private final List<PairM<Integer, Long>> sizeTimeTable=new ArrayList<>();
			
			public IntConsumer onGrow=i->{};
			boolean tabulated;
			
			public Tabulator(boolean tabulated){
				this.tabulated=tabulated;
			}
			
			public synchronized String getTab(int size){
				if(!tabulated) return "";
				
				long tim=System.currentTimeMillis();
				
				int max=0;
				
				Iterator<PairM<Integer, Long>> i=sizeTimeTable.iterator();
				while(i.hasNext()){
					PairM<Integer, Long> p=i.next();
					if(p.obj2+1000<tim) i.remove();
					else max=Math.max(max, p.obj1);
				}
				if(size>max){
					onGrow.accept(size-max);
				}
				
				sizeTimeTable.add(new PairM<>(size, tim));
				
				int tabSize  =Math.max(0, max-size);
				int totalSize=tabSize+size;
				
				return TextUtil.stringFill(tabSize, ' ');
			}
			
			
			public void reduce(int amount){
				if(!tabulated) return;
				String                         s=sizeTimeTable.toString();
				Iterator<PairM<Integer, Long>> i=sizeTimeTable.iterator();
				while(i.hasNext()){
					PairM<Integer, Long> p=i.next();
					p.obj1-=amount;
					if(p.obj1<0){
						i.remove();
					}
				}
			}
			
		}
		
		private static final class DebugHeaderStream extends OutputStream{
			
			private final OutputStream  child;
			@NotNull
			private final byte[]        prefix;
			private final StringBuilder lineBuild  =new StringBuilder();
			private       boolean       needsHeader=true;
			
			private final FileOutputStream                    fileCsvOut;
			private final Function<StackTraceElement, String> header;
			
			public DebugHeaderStream(OutputStream child, @NotNull String prefix, @Nullable FileOutputStream fileRawOut, FileOutputStream fileCsvOut, Function<StackTraceElement, String> header){
				this.header=header;
				if(fileRawOut!=null) this.child=new SplitStream(child, fileRawOut);
				else this.child=child;
				this.prefix=prefix.getBytes();
				this.fileCsvOut=fileCsvOut;
			}
			
			@Override
			public void flush() throws IOException{
				header();
				
				for(int i=0;i<lineBuild.length();i++){
					char b=lineBuild.charAt(i);
					put(b);
				}
				
				lineBuild.setLength(0);
				child.flush();
				
				String s=LogUtil.class.getClassLoader().getClass().toString();
				
				TABLE_LAST_FLAG=TABLE_FLAG;
				if(TABLE_FLAG){
					TABLE_FLAG=false;
				}else{
					TABLE_COLUMNS.clear();
				}
				
			}
			
			private void header(){
				if(!needsHeader) return;
				needsHeader=false;
				
				try{
					debugHeader();
				}catch(Exception e){
					detach();
					e.printStackTrace();
					System.exit(0);
				}
			}
			
			private void put(int b) throws IOException{
				header();
				
				if(b=='\n') needsHeader=true;
				child.write(b);
				child.flush();
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
				
				boolean shouldPrint;
				
				shouldPrint=!stack.getClassName().equals(Throwable.class.getName()+"$WrappedPrintStream");
				if(shouldPrint) shouldPrint=!stack.getClassName().equals(ThreadGroup.class.getName());
				
				if(shouldPrint){
					child.write(header.apply(stack).getBytes());
				}
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
				
				if(depth<0||depth>=trace.length) return null;
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
		if(count>=trace.length) count=trace.length-1;
		for(int i=count+1;i>=2;i--){
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
	
	private static class TableColumn{
		final String name;
		int width;
		
		public TableColumn(String name){
			this.name=name;
			this.width=name.length();
		}
		
		void gibWidth(int w){
			width=Math.max(width, w);
		}
		
		@Override
		public String toString(){
			StringBuilder sb=new StringBuilder(width+2);
			sb.append(' ').append(name);
			for(int i=0;i<width-name.length();i++){
				sb.append(' ');
			}
			sb.append(' ');
			return sb.toString();
		}
	}
	
	private static final List<TableColumn> TABLE_COLUMNS  =new ArrayList<>(1);
	private static       boolean           TABLE_FLAG     =false;
	private static       boolean           TABLE_LAST_FLAG=false;
	
	private static boolean notKotlinData(Class c){
		List<Method> functs   =new ArrayList<>(Arrays.asList(c.getDeclaredMethods()));
		Field[]      fs       =c.getDeclaredFields();
		int          compCount=(int)functs.stream().filter(m->m.getName().matches("component[0-9]+")).count();
		
		
		if(fs.length!=compCount) return true;
		
		for(Field f : fs){
			String n=f.getName();
			if(!functs.removeIf(t->t.getName().equals("get"+TextUtil.firstToUpperCase(n)))) return true;
		}
		
		return false;
	}
	
	//returns null if object type is suboptimal
	private static final List<Function<Object, Map<String, String>>> OBJECT_SCANNERS=Arrays.asList(
		map->map instanceof Map?((Map<?, ?>)map).entrySet().stream().collect(Collectors.toMap((Map.Entry e)->IN_TABLE_TO_STRINGS.toString(e.getKey()), (Map.Entry e)->IN_TABLE_TO_STRINGS.toString(e.getValue()))):null,
		//kotlin object
		row->{
			Class<?> c=row.getClass();
			if(notKotlinData(c)) return null;
			
			Field[]                       fs   =c.getDeclaredFields();
			LinkedHashMap<String, String> table=new LinkedHashMap<>(fs.length);
			for(Field f : fs){
				f.setAccessible(true);
				try{
					table.put(f.getName(), IN_TABLE_TO_STRINGS.toString(f.get(row)));
				}catch(IllegalAccessException ignored){}
			}
			
			return table;
		},
		//regular object
		o->{
			LinkedHashMap<String, String> data=new LinkedHashMap<>();
			
			mapObjectValues(o, (name, obj)->{
				if(!JSON_NULL_PRINT&&obj==null) return;
				data.put(name, IN_TABLE_TO_STRINGS.toString(obj));
			});
			
			if(data.isEmpty()) data.put("hash", o.hashCode()+"");
			
			return data;
		}
	);
	
	private static Map<String, String> objectToMap(Object row){
		for(Function<Object, Map<String, String>> objectScanner : OBJECT_SCANNERS){
			Map<String, String> mapped=objectScanner.apply(row);
			if(mapped!=null) return mapped;
		}
		throw new RuntimeException();
	}
	
	public static void printTable(Object row){
		
		Class c=row.getClass();
		
		if(c.isArray()){
			printTable((Object[])row);
			return;
		}
		
		if(row instanceof Iterable){
			List<Map<String, String>> collective=new ArrayList<>();
			Set<String>               names     =new HashSet<>();
			
			for(Object r : ((Iterable)row)){
				
				Map<String, String> mapped=objectToMap(r);
				names.addAll(mapped.keySet());
				
				collective.add(mapped);
				
				for(Map<String, String> ro : collective){
					for(String name : names){
						ro.putIfAbsent(name, "null");
					}
				}
				
				
			}
			
			if(collective.size()>1){
				int[] growthProtection=new int[names.size()];
				
				for(Map<String, String> r : collective){
					Iterator<String> iter=r.values().iterator();
					
					for(int i=0;i<growthProtection.length;i++){
						int len=iter.next().length();
						if(growthProtection[i]<len){
							growthProtection[i]=len;
						}
					}
				}
				
				Iterator<Map.Entry<String, String>> iter=collective.get(0).entrySet().iterator();
				
				for(int max : growthProtection){
					Map.Entry<String, String> e=iter.next();
					
					int len=e.getValue().length();
					if(max>len){
						e.setValue(e.getValue()+stringFill(max-len, ' '));
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
			if(row.length%2!=0) throw new IllegalArgumentException();
			
			Map<Object, Object> table=new LinkedHashMap<>();
			for(int i=0, j=row.length/2;i<j;i++){
				table.put(row[i*2], row[i*2+1]);
			}
			printTable(table);
		}
	}
	
	public static void printTable(@NotNull Object[] rowNames, @NotNull Object... rowValues){
		synchronized(System.out){
			assert rowNames.length==rowValues.length;
			
			Map<Object, Object> table=new LinkedHashMap<>();
			for(int i=0, j=rowNames.length;i<j;i++){
				table.put(rowNames[i], rowValues[i]);
			}
			printTable(table);
		}
	}
	
	public static void printTable(String keyName, String valueName, Map<?, ?> data){
		if(data.isEmpty()) return;
		
		List<String> keys =new ArrayList<>(data.size()), values=new ArrayList<>(data.size());
		int[]        sizes={keyName.length(), valueName.length()};
		
		data.forEach((key, value)->{
			String k, v;
			keys.add(k=TextUtil.toString(key));
			values.add(v=TextUtil.toString(value));
			
			sizes[0]=Math.max(sizes[0], k.length());
			sizes[1]=Math.max(sizes[1], v.length());
		});
		String k=keys.get(0);
		if(k.length()<sizes[0]){
			keys.set(0, k+TextUtil.stringFill(sizes[0]-k.length(), ' '));
		}
		String v=values.get(0);
		if(v.length()<sizes[1]){
			values.set(0, v+TextUtil.stringFill(sizes[1]-v.length(), ' '));
		}
		
		Map<String, String> row=new LinkedHashMap<>(2);
		
		for(int i=0;i<data.size();i++){
			row.put(keyName, keys.get(i));
			row.put(valueName, values.get(i));
			printTable(row);
			row.clear();
		}
		
	}
	
	public static void printTable(Map<?, ?> row){
		synchronized(System.out){
			Map<String, String> rowSafe=new LinkedHashMap<>(row.size());
			
			Function<Object, String> toString=o->TextUtil.toTableString(o).replace("\n", "\\n");
			row.forEach((k, v)->{
				String val=toString.apply(v);
				
				//resolve tabs to local table space
				if(val.indexOf('\t')!=-1){
					
					StringBuilder sb=new StringBuilder(val.length()+20);
					
					int pos=0;
					for(char c : val.toCharArray()){
						
						if(c!='\t'){
							sb.append(c);
							pos++;
							continue;
						}
						
						int requiredPos=(pos/4+1)*4;
						
						while(pos<requiredPos){
							sb.append(' ');
							pos++;
						}
						
					}
					
					val=sb.toString();
				}
				rowSafe.put(toString.apply(k), val);
			});
			
			if(TABLE_COLUMNS.stream().noneMatch(s->rowSafe.containsKey(s.name))) TABLE_COLUMNS.clear();
			
			
			rowSafe.forEach((k, v)->
				                TABLE_COLUMNS
					                .stream()
					                .filter(c->c.name.equals(k))
					                .findAny()
					                .orElseGet(()->{
						                TableColumn c=new TableColumn(k);
						                TABLE_COLUMNS.add(c);
						                TABLE_LAST_FLAG=false;
						                return c;
					                })
					                .gibWidth(v.length()));
			
			StringBuilder sb=new StringBuilder();
			if(!TABLE_LAST_FLAG){//first row
				
				String names=TABLE_COLUMNS.stream()
				                          .map(TextUtil::toTableString)
				                          .collect(Collectors.joining("|"));
				
				StringBuilder lines=new StringBuilder(names.length()+3);
				lines.append('|');
				for(int i=0;i<names.length();i++){
					lines.append('=');
				}
				lines.append("|\n");
				
				sb.append(lines);
				sb.append("|").append(names).append("|\n");
				sb.append(lines);
				
			}
			
			sb.append('|');
			for(TableColumn column : TABLE_COLUMNS){
				int    left=column.width;
				String val =rowSafe.get(column.name);
				sb.append(' ');
				if(val!=null){
					left-=val.length();
					sb.append(val);
				}
				sb.append(' ');
				while(left-->0) sb.append(' ');
				sb.append('|');
			}
			sb.append('\n');
			
			TABLE_FLAG=true;
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
			this.name=name;
			this.identifier=identifier;
			this.getter=getter;
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
			if(width!=-1) throw new IllegalArgumentException("Width must be greater than 0 or -1 for auto width");
			width=data.size();
		}
		
		//collect
		double[][] collected=new double[data.size()][values.length];
		{
			int i=0;
			for(T datum : data){
				double[] line=collected[i];
				for(int y=0;y<line.length;y++){
					line[y]=values[y].getter.get(datum);
				}
				i++;
			}
		}
		//normalize
		double[] min=new double[values.length];
		double[] max=new double[values.length];
		Arrays.fill(min, Double.MAX_VALUE);
		Arrays.fill(max, Double.MIN_VALUE);
		
		for(double[] line : collected){
			for(int j=0;j<line.length;j++){
				double val=line[j];
				if(val<min[j]) min[j]=val;
				if(val>max[j]) max[j]=val;
			}
		}
		
		if(!snapBottom){
			for(int i=0;i<min.length;i++){
				if(min[i]>0&&max[i]>0) min[i]=0;
			}
		}
		
		for(double[] line : collected){
			for(int j=0;j<line.length;j++){
				double unit=(line[j]-min[j])/(max[j]-min[j]);
				line[j]=unit;
			}
		}
		
		//render
		
		BufferedImage img=new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g=img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		
		for(int i=0;i<collected.length-1;i++){
			double[] current=collected[i];
			double[] next   =collected[i+1];
			
			for(int j=0;j<current.length;j++){
				double yCurrent=(1-current[j])*height;
				double xCurrent=i/(double)collected.length*width;
				double yNext   =(1-next[j])*height;
				double xNext   =(i+1)/(double)collected.length*width;
				
				int id=j+1;
				g.setColor(new Color(id, id, id));
				g.draw(new Line2D.Double(xCurrent, yCurrent, xNext, yNext));
			}
		}
		
		g.dispose();
		
		StringBuilder sb=new StringBuilder(2+(width+2)*(height+1)+2);
		
		sb.append("A\n");
		for(int y=0;y<height;y++){
			sb.append('|');
			for(int x=0;x<width;x++){
				int id=img.getRGB(x, y)&0xFF;
				sb.append(id==0?' ':values[id-1].identifier);
			}
			sb.append('\n');
		}
		for(int i=0;i<width+1;i++){
			sb.append('-');
		}
		sb.append(">\n");

//		LogUtil.println(2+(width+2)*(height+1)+2, sb.toString().length());
		out(sb.toString());
		println(IntStream.range(0, values.length).mapToObj(i->values[i].name+" '"+values[i].identifier+"' - ("+min[i]+" - "+max[i]+")").collect(Collectors.joining(", ")));
		
	}
	
	//================================================
	
	private static void out(String s){
		System.out.print(s);
	}
	
	private static void err(String s){
		System.err.print(s);
	}
	
}
