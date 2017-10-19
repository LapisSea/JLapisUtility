package com.lapissea.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ObjectSize{
	
	
	public static int sizeof(Object[] obj){
		if(obj==null) return 0;
		int size=0;
		for(Object o : obj){
			size+=sizeof(o.getClass());
		}
		return size;
	}
	
	public static int sizeof(Class type){
		
		if(type==boolean.class) return Byte.SIZE;
		if(type==Boolean.class) return Byte.SIZE;
		
		if(type==byte.class) return Byte.SIZE;
		if(type==Byte.class) return Byte.SIZE;
		
		if(type==char.class) return Character.SIZE;
		if(type==Character.class) return Character.SIZE;
		
		if(type==short.class) return Short.SIZE;
		if(type==Short.class) return Short.SIZE;
		
		if(type==int.class) return Integer.SIZE;
		if(type==Integer.class) return Integer.SIZE;
		
		if(type==long.class) return Long.SIZE;
		if(type==Long.class) return Long.SIZE;
		
		if(type==float.class) return Float.SIZE;
		if(type==Float.class) return Float.SIZE;
		
		if(type==double.class) return Double.SIZE;
		if(type==Double.class) return Double.SIZE;
		
		
		int size=0;
		do{
			if(type.isInterface()){
				for(Method m : type.getDeclaredMethods()){
					if(Modifier.isStatic(m.getModifiers())||!Modifier.isPublic(m.getModifiers())) continue;
					
					if(m.isAnnotationPresent(Getter.class)){
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