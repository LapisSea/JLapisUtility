package com.lapissea.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ObjectSize{
	
	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Getter{}
	
	public static int sizeof(@Nullable Object[] obj){
		if(obj==null) return 0;
		int size=0;
		for(Object o : obj){
			size+=sizeof(o.getClass());
		}
		return size;
	}
	
	public static int sizeof(){
		try{
			return sizeof(Class.forName(Thread.currentThread().getStackTrace()[2].getClassName()));
		}catch(Throwable e){
			throw UtilL.uncheckedThrow(e);
		}
	}
	
	public static int sizeofPrimitive(@NotNull Class type){
		if(type.isPrimitive()){
			if(type==boolean.class) return Byte.SIZE;
			if(type==byte.class) return Byte.SIZE;
			if(type==char.class) return Character.SIZE;
			if(type==short.class) return Short.SIZE;
			if(type==int.class) return Integer.SIZE;
			if(type==long.class) return Long.SIZE;
			if(type==float.class) return Float.SIZE;
			if(type==double.class) return Double.SIZE;
		}else{
			if(type==Boolean.class) return Byte.SIZE;
			if(type==Byte.class) return Byte.SIZE;
			if(type==Character.class) return Character.SIZE;
			if(type==Short.class) return Short.SIZE;
			if(type==Integer.class) return Integer.SIZE;
			if(type==Long.class) return Long.SIZE;
			if(type==Float.class) return Float.SIZE;
			if(type==Double.class) return Double.SIZE;
		}
		return -1;
	}
	
	public static int sizeof(@NotNull Class type){
		
		int size=sizeofPrimitive(type);
		if(size==-1) size=0;
		else return size;
		
		do{
			if(type.isInterface()){
				for(Method m : type.getDeclaredMethods()){
					if(Modifier.isStatic(m.getModifiers())||!Modifier.isPublic(m.getModifiers())) continue;
					
					if(m.isAnnotationPresent(Getter.class)||(m.getName().startsWith("get")&&m.getName().length()>3&&Character.isUpperCase(m.getName().charAt(3)))){
						size+=sizeof(m.getReturnType());
					}
				}
			}else{
				for(Field f : type.getDeclaredFields()){
					if(Modifier.isStatic(f.getModifiers())) continue;
					
					size+=sizeof(f.getType());
				}
			}
		}while((type=type.getSuperclass())!=null);
		
		return size;
	}
	
}
