package com.lapissea.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.*;

import static com.lapissea.util.UtilL.*;
import static java.lang.reflect.Modifier.*;

public class TextUtil{
	
	public static final class CustomToString{
		private final Map<Predicate<Class>, Function<Object, String>> overrides=new LinkedHashMap<>();
		private final Function<Object, String>                        fallback;
		
		public CustomToString(Function<Object, String> fallback){
			this.fallback=fallback;
		}
		
		public <T> void register(@NotNull Class<T> type, @NotNull Function<T, String> funct){
			register(t->t==type||instanceOf(t, type), funct);
		}
		
		@SuppressWarnings("unchecked")
		public <T> void register(@NotNull Predicate<Class<T>> canStringify, @NotNull Function<T, String> funct){
			overrides.put((Predicate<Class>)((Object)canStringify), (Function<Object, String>)funct);
		}
		
		public String toString(Object obj){
			if(obj==null) return "null";
			
			Class<?> type=obj.getClass();
			
			return overrides.entrySet()
			                .stream()
			                .filter(e->e.getKey().test(type))
			                .findAny()
			                .map(Map.Entry::getValue)
			                .orElse(fallback)
			                .apply(obj);
		}
	}
	
	public static final String  NEW_LINE       =System.lineSeparator();
	public static       boolean JSON_NULL_PRINT=false;
	
	public static final CustomToString CUSTOM_TO_STRINGS  =new CustomToString(TextUtil::enhancedToString);
	public static final CustomToString IN_TABLE_TO_STRINGS=new CustomToString(CUSTOM_TO_STRINGS::toString);
	
	@Deprecated
	public static <T> void registerCustomToString(@NotNull Class<T> type, @NotNull Function<T, String> funct){
		CUSTOM_TO_STRINGS.register(type, funct);
	}
	
	@Deprecated
	public static <T> void registerCustomToString(@NotNull Predicate<Class<T>> canStringify, @NotNull Function<T, String> funct){
		CUSTOM_TO_STRINGS.register(canStringify, funct);
	}
	
	static{
		CUSTOM_TO_STRINGS.register(CharSequence.class, CharSequence::toString);
		IN_TABLE_TO_STRINGS.register(CharSequence.class, CharSequence::toString);
		
		CUSTOM_TO_STRINGS.register(UtilL::isArray, TextUtil::unknownArrayToString);
		
		CUSTOM_TO_STRINGS.register(FloatBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("FloatBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i));
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		CUSTOM_TO_STRINGS.register(IntBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("IntBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i));
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		CUSTOM_TO_STRINGS.register(ByteBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("ByteBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i)&0xFF);
				
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		CUSTOM_TO_STRINGS.register(LongBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("LongBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i));
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		CUSTOM_TO_STRINGS.register(ShortBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("ShortBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i));
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		CUSTOM_TO_STRINGS.register(LinkedList.class, list->{
			StringBuilder print=new StringBuilder("[");
			list.forEach(e->print.append(toString(e)));
			print.append(']');
			return print.toString();
		});
		CUSTOM_TO_STRINGS.register(Stream.class, stream->toString((Object)stream.toArray()));
		CUSTOM_TO_STRINGS.register(IntStream.class, stream->toString(stream.toArray()));
		CUSTOM_TO_STRINGS.register(DoubleStream.class, stream->toString(stream.toArray()));
		CUSTOM_TO_STRINGS.register(LongStream.class, stream->toString(stream.toArray()));
		CUSTOM_TO_STRINGS.register(AbstractCollection.class, col->{
			Iterator<?> it=col.iterator();
			if(!it.hasNext())
				return "[]";
			
			StringBuilder sb=new StringBuilder();
			sb.append('[');
			for(;;){
				Object e=it.next();
				sb.append(e==col?"(this Collection)":TextUtil.toString(e));
				if(!it.hasNext())
					return sb.append(']').toString();
				sb.append(',').append(' ');
			}
		});
	}
	
	@NotNull
	public static String toStringArray(@Nullable Object[] arr){
		if(arr==null) return "null";
		StringBuilder print=new StringBuilder("[");
		
		for(int i=0;i<arr.length;i++){
			Object a=arr[i];
			if(isArray(a)) print.append(unknownArrayToString(a));
			else print.append(toString(a));
			if(i!=arr.length-1) print.append(", ");
		}
		
		return print.append("]").toString();
	}
	
	public static Map<String, Object> mapObjectValues(Object o){
		Map<String, Object> map=new LinkedHashMap<>();
		mapObjectValues(o, map::put);
		return map;
	}
	
	public static void mapObjectValues(Object o, @NotNull BiConsumer<String, Object> push){
		if(o==null) return;
		
		Class c=o.getClass();
		for(Method m : c.getMethods()){
			if(m.getParameterCount()!=0) continue;
			if(isPrivate(m.getModifiers())||isProtected(m.getModifiers())||isStatic(m.getModifiers())) continue;
			if(m.getReturnType()==Void.TYPE) continue;
			
			String name=m.getName();
			if(name.length()<=4) continue;
			if(m.getDeclaringClass()==Object.class) continue;
			
			int prefix;
			
			if((m.getReturnType()==Boolean.class||m.getReturnType()==boolean.class)&&name.startsWith("is")){
				prefix=2;
			}else{
				if(!name.startsWith("get")) continue;
				prefix=3;
			}
			
			char ch=name.charAt(prefix);
			if(!Character.isAlphabetic(ch)||!Character.isUpperCase(ch)) continue;
			name=firstToLoverCase(name.substring(prefix));
			
			try{
				m.setAccessible(true);
				push.accept(name, m.invoke(o));
			}catch(Throwable e){
				push.accept(name, "<read error>");
			}
		}
		
		for(Field f : c.getFields()){
			if(isPrivate(f.getModifiers())||isProtected(f.getModifiers())||isStatic(f.getModifiers())) continue;
			String name=f.getName();
			
			try{
				f.setAccessible(true);
				push.accept(name, f.get(o));
			}catch(ReflectiveOperationException e){
				e.printStackTrace();
			}
		}
	}
	
	@NotNull
	private static String unknownArrayToString(@Nullable Object arr){
		if(arr==null) return "null";
		if(arr instanceof boolean[]) return Arrays.toString((boolean[])arr);
		if(arr instanceof float[]) return Arrays.toString((float[])arr);
		if(arr instanceof byte[]){
			byte[] ba=(byte[])arr;
			
			int iMax=ba.length-1;
			
			StringBuilder b=new StringBuilder();
			b.append('[');
			for(int i=0;;i++){
				b.append(ba[i]&0xFF);
				if(i==iMax) return b.append(']').toString();
				b.append(", ");
			}
		}
		if(arr instanceof int[]) return Arrays.toString((int[])arr);
		if(arr instanceof long[]) return Arrays.toString((long[])arr);
		if(arr instanceof short[]) return Arrays.toString((short[])arr);
		if(arr instanceof char[]) return Arrays.toString((char[])arr);
		if(arr instanceof double[]) return Arrays.toString((double[])arr);
		if(arr instanceof Object[]) return toStringArray((Object[])arr);
		return "ERR: "+arr;
	}
	
	@NotNull
	public static String toString(@Nullable Object... objs){
		if(objs==null) return "null";
		StringBuilder print=new StringBuilder();
		
		for(int i=0;i<objs.length;i++){
			Object a=objs[i];
			if(isArray(a)) print.append(unknownArrayToString(a));
			else print.append(toString(a));
			if(i!=objs.length-1) print.append(" ");
		}
		
		return print.toString();
	}
	
	@NotNull
	public static String toString(@Nullable Object obj){
		return CUSTOM_TO_STRINGS.toString(obj);
	}
	
	
	private static final Stack<Object> JSON_CALL_STACK=new Stack<>();
	
	public static String toNamedJson(Object o){
		
		JSON_CALL_STACK.push(o);
		try{
			if(o==null) return "null";
			if(o instanceof Number) return o.toString();
			
			Class c=o.getClass();
			
			if(c==Boolean.class) return o.toString();
			
			Map<String, String> data=new HashMap<>();
			
			mapObjectValues(o, (name, obj)->{
				if(!JSON_NULL_PRINT&&obj==null) return;
				if(JSON_CALL_STACK.contains(obj)) obj="<circular reference of "+obj.getClass().getSimpleName()+">";
				if(obj instanceof String){
					obj="\""+((String)obj).replace("\n", "\\n").replace("\r", "\\r")+'"';
				}
				data.put(name, toString(obj));
			});
			
			if(data.isEmpty()) data.put("hash", o.hashCode()+"");
			
			return c.getSimpleName()+"{"+data.entrySet().stream().map(Object::toString).collect(Collectors.joining(", "))+"}";
			
		}finally{
			JSON_CALL_STACK.pop();
		}
		
	}
	
	private static final Stack<Object> PRETTY_JSON_CALL_STACK=new Stack<>();
	private static final String        TAB                   ="    ";
	private static final int           MAX_SINGLE_LINE_ARRAY =150;
	
	public static String toNamedPrettyJson(Object o){
		return toNamedPrettyJson(o, false);
	}
	
	public static String toNamedPrettyJson(Object o, boolean tryTabulatingArrays){
		if(o==null) return "null";
		
		if(PRETTY_JSON_CALL_STACK.contains(o)) return "<circular reference of "+o.getClass().getSimpleName()+">";
		if(o instanceof CharSequence) return o.toString();
		if(o instanceof Number) return o.toString();
		if(o instanceof Class) return ((Class)o).getSimpleName();
		
		Class c=o.getClass();
		
		if(c==Boolean.class) return o.toString();
		
		PRETTY_JSON_CALL_STACK.push(o);
		try{
			
			Function<String, String> tabulate      =s->s.replace("\n", "\n"+TAB);
			Function<Object, String> lazyTabbedJson=o1->tabulate.apply(overridesToString(o1.getClass())&&!(o1 instanceof Collection)?o1.toString():toNamedPrettyJson(o1, tryTabulatingArrays));
			
			
			if(o.getClass().isArray()){
				o=Arrays.asList((Object[])o);
			}
			
			if(o instanceof Collection){
				Collection<?> l=(Collection)o;
				
				tabulator:
				if(tryTabulatingArrays){
					if(l.isEmpty()) break tabulator;
					
					Class<?> unifyingClass=l.stream()
					                        .filter(Objects::nonNull)
					                        .map(o1->(Class)o1.getClass())
					                        .reduce(UtilL::findClosestCommonSuper)
					                        .orElse(Object.class);
					
					if(unifyingClass==Object.class) break tabulator; //not same classes so can't tabulate
					if(overridesToString(unifyingClass)) break tabulator; //overrides to string so it's better not to generate table
					
					return "{{\n"+toTable(" "+unifyingClass.getSimpleName()+" ", l.stream().map(TextUtil::mapObjectValues).collect(Collectors.toList()))+"}}";
				}
				
				String contents=l.stream().map(lazyTabbedJson).collect(Collectors.joining(",\n"+TAB));
				if(contents.length()<MAX_SINGLE_LINE_ARRAY) return tabulate.apply(toString(o));
				
				return "[\n"+TAB+contents+"\n]";
			}
			
			if(o instanceof Map){
				Map<?, ?> l=(Map)o;
				
				StringBuilder sb=new StringBuilder("[\n");
				
				for(Map.Entry o1 : l.entrySet()){
					sb.append(TAB).append(toString(o1.getKey())).append(lazyTabbedJson.apply(o1.getValue())).append(",\n");
				}
				
				return sb.append(']').toString();
			}
			
			Map<String, String> data=new HashMap<>();
			
			mapObjectValues(o, (name, obj)->{
				if(!JSON_NULL_PRINT&&obj==null) return;
				if(PRETTY_JSON_CALL_STACK.contains(obj)) obj="<circular reference of "+obj.getClass().getSimpleName()+">";
				if(obj instanceof String){
					obj="\""+((String)obj).replace("\n", "\\n").replace("\r", "\\r")+'"';
				}
				data.put(name, lazyTabbedJson.apply(obj));
			});
			
			if(data.isEmpty()) data.put("hash", o.hashCode()+"");
			
			return c.getSimpleName()+"{\n"+TAB+data.entrySet().stream().map(ob->tabulate.apply(ob.toString())).collect(Collectors.joining(",\n"+TAB))+"\n}";
			
		}finally{
			PRETTY_JSON_CALL_STACK.pop();
		}
		
	}
	
	public static boolean TABLE_BOOLEAN_TO_CHECK=true;
	
	public static String toTable(Iterable<?> rows){
		return toTable("", rows);
	}
	
	public static String toTable(String title, Iterable<?> rows){
		List<Map<?, ?>> rowsExplicit=new ArrayList<>(rows instanceof Collection?((Collection<?>)rows).size():16);
		for(Object obj : rows) rowsExplicit.add(mapObjectValues(obj));
		return toTable(title, rowsExplicit);
	}
	
	public static String toTable(Collection<? extends Map<?, ?>> rows){
		return toTable("", rows);
	}
	
	@SuppressWarnings("AutoBoxing")
	public static String toTable(String title, Collection<? extends Map<?, ?>> rows){
		Function<Object, String> toSingleLineString=o->{
			if("null".equals(o)) return "";
			
			String        nonTabbed=IN_TABLE_TO_STRINGS.toString(o);
			StringBuilder tabbed   =new StringBuilder(nonTabbed.length()+4);
			
			for(int i=0;i<nonTabbed.length();i++){
				char c=nonTabbed.charAt(i);
				switch(c){
				case '\n':
					tabbed.append("\\n");
					break;
				case '\r':
					tabbed.append("\\r");
					break;
				case '\t':
					tabbed.append(stringFill((tabbed.length()+1)%4, ' '));
					break;
				default:
					tabbed.append(c);
					break;
				}
			}
			
			return tabbed.toString();
		};
		
		List<Map<String, String>> safe=rows.stream().map(row->{
			Map<String, String> map=new LinkedHashMap<>();
			
			if(row!=null) row.forEach((k, v)->{
				if(v==null||"null".equals(v)) return;
				
				String s=toSingleLineString.apply(k);
				if(k instanceof CharSequence&&s.startsWith("\"")&&s.endsWith("\"")){
					s=s.substring(1, s.length()-1);
				}
				map.put(s, toSingleLineString.apply(v));
			});
			
			return map;
		}).collect(Collectors.toList());
		
		if(TABLE_BOOLEAN_TO_CHECK){
			Map<String, Boolean> booleanColumns=new HashMap<>();
			
			for(Map<String, String> row : safe){
				row.forEach((k, v)->{
					if(!booleanColumns.getOrDefault(k, Boolean.TRUE)) return;
					booleanColumns.put(k, v.isEmpty()||v.equals("true")||v.equals("false"));
				});
			}
			
			for(Map<String, String> row : safe){
				row.entrySet().forEach(e->{
					if(booleanColumns.get(e.getKey())){
						switch(e.getValue()){
						case "true":
							e.setValue("√");
							break;
						case "false":
							e.setValue("x");
							break;
						}
					}
				});
			}
		}
		
		Map<String, Integer> columnWidths=new LinkedHashMap<>();
		
		for(Map<String, String> row : safe){
			row.forEach((k, v)->{
				Integer max   =columnWidths.get(k);
				int     newLen=Math.max(k.length(), v.length())+2;
				if(max==null||max<newLen) columnWidths.put(k, newLen);
			});
		}
		
		if(columnWidths.isEmpty()) columnWidths.put("", 0);
		
		int width=columnWidths.values().stream().mapToInt(i->i+1).sum()+1;
		
		int titleRequiredWidth=title.length()+4;
		while(titleRequiredWidth>width){
			int diff       =titleRequiredWidth-width;
			int columnCount=columnWidths.size();
			
			if(diff>columnCount){
				int toAdd=diff/columnCount;
				for(Map.Entry<String, Integer> e : columnWidths.entrySet()){
					e.setValue(e.getValue()+toAdd);
				}
				width+=toAdd*columnCount;
			}else{
				columnWidths.entrySet().stream().limit(diff).forEach(e->e.setValue(e.getValue()+1));
				width+=diff;
			}
		}
		
		BiFunction<Integer, String, String> printCell=(len, val)->{
			if(val==null||val.equals("null")) return TextUtil.stringFill(len, ' ');
			int diff=len-val.length();
			if(diff==0) return val;
			
			int l;
			if(UtilL.isNumeric(val)) l=diff>1?diff-1:diff;
			else if(val.startsWith("[")&&val.endsWith("]")) l=Math.min(diff, 1);
			else l=diff/2;
			
			int r=diff-l;
			
			return TextUtil.stringFill(l, ' ')+val+TextUtil.stringFill(r, ' ');
		};
		
		char lineChar='=';
		
		String line=stringFill(width, lineChar);
		
		StringBuilder result=new StringBuilder((line.length()+1)*safe.size());
		
		int titleFreeSpace=width-title.length();
		
		int l=titleFreeSpace/2;
		int r=titleFreeSpace-l;
		for(int i=0;i<l;i++) result.append(lineChar);
		result.append(title);
		for(int i=0;i<r;i++) result.append(lineChar);
		result.append('\n');
		
		columnWidths.forEach((name, len)->result.append('|').append(printCell.apply(len, name)));
		result.append("|\n");
		
		result.append(line).append('\n');
		
		for(Map<String, String> row : safe){
			
			columnWidths.forEach((name, len)->result.append('|').append(printCell.apply(len, row.get(name))));
			result.append("|\n");
		}
		
		result.append(line);
		
		return result.toString();
	}
	
	private static final Map<Class<?>, Boolean> CLASS_CACHE=new HashMap<>();
	
	@SuppressWarnings("AutoBoxing")
	public static synchronized boolean overridesToString(Class<?> cl){
		return CLASS_CACHE.computeIfAbsent(cl, c->{
			try{
				return c.getMethod("toString").getDeclaringClass()!=Object.class;
			}catch(NoSuchMethodException e){
				return Boolean.TRUE;
			}
		});
	}
	
	public static String enhancedToString(Object o){
		if(o==null) return "null";
		
		if(overridesToString(o.getClass())) return o.toString();
		return toNamedJson(o);
	}
	
	@NotNull
	public static String plural(@NotNull String word, int count){
		return count==1?word:plural(word);
	}
	
	public static String plural(@NotNull String word){
		switch(word.charAt(word.length()-1)){
		case 's':
		case 'x':
			return word+"es";
		case 'h':{
			switch(word.charAt(word.length()-2)){
			case 's':
			case 'c':
				return word+"es";
			}
		}
		default:
			return word+"s";
		}
	}
	
	@NotNull
	public static String stringFill(int length, char c){
		if(length==0) return "";
		
		char[] ch=new char[length];
		Arrays.fill(ch, c);
		return new String(ch);
	}
	
	public static String wrappedString(Object obj){
		return wrappedString(splitByChar(toString(obj), '\n'));
	}
	
	public static String wrappedString(@Nullable String... lines){
		if(lines==null||lines.length==0) return "";
		
		StringBuilder result =new StringBuilder();
		StringBuilder padding=new StringBuilder();
		
		int width=Arrays.stream(lines).mapToInt(String::length).max().orElse(0)+2;
		result.append('/');
		for(int i=0;i<width;i++) result.append('-');
		result.append("\\\n");
		
		for(String line : lines){
			int diff=width-line.length();
			int l   =diff/2;
			int r   =diff-l;
			
			result.append('|');
			
			if(padding.length()>l) padding.setLength(l);
			else while(padding.length()<l) padding.append(' ');
			result.append(padding);
			
			result.append(line);
			
			if(padding.length()>r) padding.setLength(l);
			else while(padding.length()<r) padding.append(' ');
			result.append(padding);
			
			result.append("|\n");
		}
		
		result.append('\\');
		for(int i=0;i<width;i++) result.append('-');
		result.append('/');
		
		return result.toString();
	}
	
	@NotNull
	public static List<String> wrapLongString(@NotNull String str, int width){
		List<String>  result=new ArrayList<>(2);
		StringBuilder line  =new StringBuilder();
		
		for(int i=0;i<str.length();i++){
			char c=str.charAt(i);
			
			if(line.length() >= width){
				
				int lastSpace=line.length()-1;
				while(lastSpace>0&&Character.isWhitespace(line.charAt(lastSpace-1))) lastSpace--;
				while(lastSpace>0&&!Character.isWhitespace(line.charAt(lastSpace-1))) lastSpace--;
				
				if(lastSpace!=0){
					if(Character.isWhitespace(c)){
						int possibleSplit=line.length()-lastSpace;
						if(possibleSplit>width) return wrapLongString(str, possibleSplit);
						
						String lastWord=line.substring(lastSpace);
						line.setLength(lastSpace);
						result.add(line.toString());
						line.setLength(0);
						line.append(lastWord);
					}else{
						String overflowLine=line.toString();
						result.add(overflowLine.substring(0, lastSpace).trim());
						line.setLength(0);
						line.append(overflowLine.substring(lastSpace));
					}
				}
			}
			if(c=='\n'){
				result.add(line.toString());
				line.setLength(0);
			}else line.append(c);
		}
		String lastLine=line.toString();
		if(lastLine.length()>width) return wrapLongString(str, lastLine.length());
		result.add(lastLine);
		
		return result;
	}
	
	public static String join(@NotNull Collection<?> data, String s){
		Iterator<?> i=data.iterator();
		if(!i.hasNext()) return "";
		
		StringBuilder result=new StringBuilder();
		
		while(true){
			result.append(i.next());
			if(i.hasNext()) result.append(s);
			else return result.toString();
		}
	}
	
	@Nullable
	public static String firstToLoverCase(@Nullable String string){
		if(string==null||string.isEmpty()) return string;
		
		char[] c=string.toCharArray();
		c[0]=Character.toLowerCase(c[0]);
		return new String(c);
	}
	
	@Nullable
	public static String firstToUpperCase(@Nullable String string){
		if(string==null||string.isEmpty()) return string;
		
		char[] c=string.toCharArray();
		c[0]=Character.toUpperCase(c[0]);
		return new String(c);
	}
	
	private static final char[] HEX_ARRAY="0123456789ABCDEF".toCharArray();
	
	public static String bytesToHex(@NotNull byte[] bytes){
		char[] hexChars=new char[bytes.length*2];
		for(int j=0;j<bytes.length;j++){
			int v=bytes[j]&0xFF;
			hexChars[j*2]=HEX_ARRAY[v >>> 4];
			hexChars[j*2+1]=HEX_ARRAY[v&0x0F];
		}
		return new String(hexChars);
	}
	
	@NotNull
	public static byte[] hexStringToByteArray(@NotNull String s){
		int    len =s.length();
		byte[] data=new byte[len/2];
		for(int i=0;i<len;i+=2){
			data[i/2]=(byte)((Character.digit(s.charAt(i), 16)<<4)+Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
	
	@NotNull
	public static String[] splitByChar(@NotNull String src, char toSplit){
		return splitByChar(src, toSplit, 0, src.length());
	}
	
	@NotNull
	public static String[] splitByChar(@NotNull String src, char toSplit, int start){
		return splitByChar(src, toSplit, start, src.length());
	}
	
	@NotNull
	public static String[] splitByChar(@NotNull String src, char toSplit, int start, int end){
		
		int count=1;
		
		for(int i=start;i<end;i++){
			if(src.charAt(i)==toSplit) count++;
		}
		
		String[] result=new String[count];
		int      pos   =start;
		
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<count;i++){
			
			sb.setLength(0);
			while(pos<end){
				char c=src.charAt(pos++);
				if(c==toSplit) break;
				sb.append(c);
			}
			
			result[i]=sb.toString();
		}
		
		return result;
	}
	
}
