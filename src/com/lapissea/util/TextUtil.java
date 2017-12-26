package com.lapissea.util;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Function;

import static com.lapissea.util.UtilL.*;

public class TextUtil{
	
	private static final Map<Class<Object>, Function<Object, String>> CUSTOM_TO_STRINGS=new HashMap<>();
	
	@SuppressWarnings("unchecked")
	public static <T> void __REGISTER_CUSTOM_TO_STRING(Class<T> type, Function<T, String> funct){
		CUSTOM_TO_STRINGS.put((Class<Object>)type, (Function<Object, String>)funct);
	}
	
	static{
		__REGISTER_CUSTOM_TO_STRING(FloatBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("FloatBuffer[");
			for(int i=0;i<buffer.capacity();){
				print.append(buffer.get(i));
				if(++i<buffer.capacity()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		__REGISTER_CUSTOM_TO_STRING(IntBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("IntBuffer[");
			for(int i=0;i<buffer.capacity();){
				print.append(buffer.get(i));
				if(++i<buffer.capacity()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
		__REGISTER_CUSTOM_TO_STRING(ByteBuffer.class, buffer->{
			StringBuilder print=new StringBuilder("ByteBuffer[");
			for(int i=0;i<buffer.capacity();){
				print.append(buffer.get(i));
				if(++i<buffer.capacity()) print.append(", ");
			}
			print.append(']');
			return print.toString();
		});
	}
	
	public static String toStringArray(Object[] arr){
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
	
	public static String toString(Object... objs){
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
	
	public static String toString(Object obj){
		if(obj==null) return "null";
		
		StringBuilder print=new StringBuilder();
		
		if(isArray(obj)) print.append(unknownArrayToString(obj));
		else{
			Class                    type=obj.getClass();
			Function<Object, String> fun =CUSTOM_TO_STRINGS.get(type);
			if(fun!=null) print.append(fun.apply(obj));
			else{
				Map.Entry<Class<Object>, Function<Object, String>> ent=
					CUSTOM_TO_STRINGS.entrySet().stream().filter(e->instanceOf(type, e.getKey()))
					                 .findFirst()
					                 .orElse(null);
				if(ent!=null) print.append(ent.getValue().apply(obj));
				else print.append(obj.toString());
			}
		}
		
		return print.toString();
	}
	
	public static String plural(String word, int count){
		if(count==1) return word;
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
	
	public static String stringFill(int length, char c){
		char[] ch=new char[length];
		Arrays.fill(ch, c);
		return new String(ch);
	}
	
	public static String wrappedString(Object obj){
		return wrappedString(toString(obj).split("\n"));
	}
	
	public static String wrappedString(String... lines){
		if(lines==null||lines.length==0) return "";
		
		StringBuilder result =new StringBuilder();
		StringBuilder padding=new StringBuilder();
		
		int width=Arrays.stream(lines).mapToInt(String::length).max().orElse(0)+2;
		result.append('/');
		for(int i=0;i<width;i++)result.append('-');
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
		for(int i=0;i<width;i++)result.append('-');
		result.append('/');
		
		return result.toString();
	}
	
	public static List<String> wrapLongString(String str, int width){
		List<String>  result=new ArrayList<>(2);
		StringBuilder line  =new StringBuilder();
		
		
		for(int i=0;i<str.length();i++){
			char c=str.charAt(i);
			
			if(line.length()>=width){
				
				if(Character.isWhitespace(c)){
					
					int lastSpace=line.length()-1;
					while(lastSpace>0&&Character.isWhitespace(line.charAt(lastSpace-1))) lastSpace--;
					while(lastSpace>0&&!Character.isWhitespace(line.charAt(lastSpace-1))) lastSpace--;
					
					if(lastSpace!=0){
						if(line.length()-lastSpace>width) return wrapLongString(str, line.length()-lastSpace);
						
						String lastWord=line.substring(lastSpace);
						line.setLength(lastSpace);
						result.add(line.toString());
						line.setLength(0);
						line.append(lastWord);
					}

//					result.add(line.toString());
//					line.setLength(0);
//					while(i+1<str.length()&&Character.isWhitespace(c=str.charAt(i+1))&&c!='\n') i++;
//					continue;
				}
			}
			line.append(c);
			if(c=='\n'){
				result.add(line.toString());
				line.setLength(0);
			}
		}
		result.add(line.toString());
		
		return result;
	}
	
	public static String join(Collection<? extends CharSequence> strings, String s){
		StringBuilder result=new StringBuilder();
		
		Iterator<? extends CharSequence> i=strings.iterator();
		
		while(i.hasNext()){
			result.append(i.next());
			if(i.hasNext()) result.append(s);
		}
		
		return result.toString();
	}
}