package com.lapissea.util;

public class ObjectHolder<T>{
	public T obj;
	
	public ObjectHolder(){}
	
	public ObjectHolder(T obj){
		this.obj=obj;
	}
	
	public T getObj(){
		return obj;
	}
	
	public void setObj(T obj){
		this.obj=obj;
	}
}
