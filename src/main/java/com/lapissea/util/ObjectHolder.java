package com.lapissea.util;

import java.util.Objects;

public class ObjectHolder<T>{
	public T obj;
	
	public ObjectHolder(){ }
	
	public ObjectHolder(T obj){
		this.obj = obj;
	}
	
	public T getObj(){
		return obj;
	}
	
	public void setObj(T obj){
		this.obj = obj;
	}
	
	@Override
	public String toString(){
		return TextUtil.toString(obj);
	}
	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if(!(o instanceof ObjectHolder)) return false;
		ObjectHolder<?> that = (ObjectHolder<?>)o;
		return Objects.equals(getObj(), that.getObj());
	}
	@Override
	public int hashCode(){
		return obj == null? 0 : obj.hashCode();
	}
}
