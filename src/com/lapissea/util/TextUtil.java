package com.lapissea.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lapissea.util.UtilL.*;
import static java.lang.reflect.Modifier.*;

public class TextUtil{
	
	public static final String  NEW_LINE       =System.lineSeparator();
	public static       boolean JSON_NULL_PRINT=false;
	
	private static final Map<Predicate<Class>, Function<Object, String>> CUSTOM_TO_STRINGS=new LinkedHashMap<>();
	
	public static <T> void registerCustomToString(@NotNull Class<T> type, @NotNull Function<T, String> funct){
		registerCustomToString(t->t==type||instanceOf(t, type), funct);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> void registerCustomToString(@NotNull Predicate<Class<T>> type, @NotNull Function<T, String> funct){
		CUSTOM_TO_STRINGS.put((Predicate<Class>)((Object)type), (Function<Object, String>)funct);
	}
	
	static{
		registerCustomToString(UtilL::isArray, TextUtil::unknownArrayToString);
		
		registerCustomToString(FloatBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("FloatBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i));
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		registerCustomToString(IntBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("IntBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i));
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		registerCustomToString(ByteBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("ByteBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i)&0xFF);
				
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		registerCustomToString(LongBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("LongBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i));
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		registerCustomToString(ShortBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("ShortBuffer[");
			for(int i=0;i<buffer.limit();){
				print.append(buffer.get(i));
				if(++i<buffer.limit()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		registerCustomToString(LinkedList.class, list->{
			StringBuilder print=new StringBuilder("[");
			list.forEach(e->print.append(toString(e)));
			print.append(']');
			return print.toString();
		});
		registerCustomToString(Stream.class, stream->toString((Object)stream.toArray()));
		registerCustomToString(AbstractCollection.class, col->{
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
	
	public static void mapObjectValues(@NotNull Object o, BiConsumer<String, Object> push){
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
			}catch(ReflectiveOperationException e){
				e.printStackTrace();
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
		if(obj==null) return "null";
		if(obj instanceof CharSequence) return obj.toString();
		
		Class<?> type=obj.getClass();
		
		return CUSTOM_TO_STRINGS.entrySet()
		                        .stream()
		                        .filter(e->e.getKey().test(type))
		                        .findAny()
		                        .map(Map.Entry::getValue)
		                        .orElse(TextUtil::enhancedToString)
		                        .apply(obj);
	}
	
	private static final Map<Class, Boolean> CLASS_CACHE=new HashMap<>();
	
	public static String toNamedJson(Object o){
		return _toNamedJson(o);
	}
	
	private static final Stack<Object> JSON_CALL_STACK=new Stack<>();
	
	private static synchronized String _toNamedJson(Object o){
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
				data.put(name, toString(obj));
			});
			
			if(data.isEmpty()) data.put("hash", o.hashCode()+"");
			
			return c.getSimpleName()+"{"+data.entrySet().stream().map(Object::toString).collect(Collectors.joining(", "))+"}";
			
		}finally{
			JSON_CALL_STACK.pop();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static String enhancedToString(Object o){
		if(o==null) return "null";
		
		boolean overridesToString=CLASS_CACHE.computeIfAbsent(o.getClass(), c->{
			try{
				return c.getMethod("toString").getDeclaringClass()!=Object.class;
			}catch(NoSuchMethodException e){
				return Boolean.TRUE;
			}
		});
		
		if(overridesToString) return o.toString();
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
