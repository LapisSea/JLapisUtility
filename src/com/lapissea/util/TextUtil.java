package com.lapissea.util;

import java.nio.FloatBuffer;
import java.util.*;
import java.util.function.Function;

import static com.lapissea.util.UtilL.*;

public class TextUtil{
	
	private static final Map<Class<Object>, Function<Object, String>> CUSTOM_TO_STRINGS=new HashMap<>();
	@SuppressWarnings("unchecked")
	public static <T> void __REGISTER_CUSTOM_TO_STRING(Class<T> type, Function<T, String> funct){
		CUSTOM_TO_STRINGS.put((Class<Object>)type, (Function<Object, String>)funct);
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
	
	public static String toString(Object... objs){
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
			Class                    type=obj.getClass();
			Function<Object, String> fun =CUSTOM_TO_STRINGS.get(type);
			if(fun!=null) print.append(fun.apply(obj));
			else{
				Map.Entry<Class<Object>, Function<Object, String>> ent=CUSTOM_TO_STRINGS.entrySet().stream().filter(e->instanceOf(e.getKey(), type)).findFirst().orElse(null);
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
		StringBuilder line=new StringBuilder();
		
		int length=0;
		for(int i=0;i<lines.length;i++){
			String lin=lines[i]=lines[i].replaceFirst("\\s+$", "");
			length=Math.max(length, lin.length());
		}
		
		if(lines.length>1){
			length+=4;
			for(int i=0;i<lines.length;i++){
				String lin =lines[i]="| "+lines[i]+" |";
				int    diff=length-lin.length();
				if(diff==0) continue;
				
				for(int j=0;j<diff;j++){
					if(j%2==0) lin+="=";
					else lin="="+lin;
				}
				
				lines[i]=lin;
			}
		}
		
		line.append("<<");
		for(int i=0, j=length+4;i<j;i++){
			line.append('=');
		}
		line.append(">>");
		
		String lineS=line.toString();
		
		StringBuilder res=new StringBuilder(lineS+"\n");
		for(String lin : lines) res.append("<<==").append(lin).append("==>>\n");
		res.append(lineS);
		return res.toString();
	}
	
	public static List<String> wrapLongString(String str, int width){
		List<String>  result=new ArrayList<>(2);
		StringBuilder line  =new StringBuilder();
		
		int x=0;
		
		for(int i=0;i<str.length();i++){
			char c=str.charAt(i);
			if(Character.isWhitespace(c)){
				if(x>=width){
					x=0;
					result.add(line.toString());
					line.setLength(0);
					while(i+1<str.length()&&Character.isWhitespace(c=str.charAt(i+1))&&c!='\n') i++;
					continue;
				}
			}
			line.append(c);
			if(c=='\n'){
				x=0;
				result.add(line.toString());
				line.setLength(0);
			}else x++;
		}
		if(x!=0) result.add(line.toString());
		
		return result;
	}
}
