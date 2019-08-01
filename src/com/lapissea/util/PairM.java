package com.lapissea.util;

import java.util.Map;

public class PairM<Obj1, Obj2>{
	
	public Obj1 obj1;
	public Obj2 obj2;
	
	public PairM(){}
	
	public PairM(@NotNull Map.Entry<Obj1, Obj2> e){
		this(e.getKey(), e.getValue());
	}
	
	public PairM(Obj1 obj1, Obj2 obj2){
		this.obj1=obj1;
		this.obj2=obj2;
	}
	
	public Object get(boolean firstObj){
		return firstObj?obj1:obj2;
	}
	
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof PairM)) return false;
		return ((PairM<?, ?>)obj).obj1.equals(obj1)&&((PairM<?, ?>)obj).obj2.equals(obj2);
	}
	
	@NotNull
	@Override
	public String toString(){
		return getClass().getSimpleName()+"{"+TextUtil.toString(obj1)+", "+TextUtil.toString(obj2)+"}";
	}
	
	@Override
	public int hashCode(){
		int hash=1;
		if(obj1!=null) hash=hash*31+obj1.hashCode();
		if(obj2!=null) hash=hash*31+obj2.hashCode();
		return hash;
	}
	
	public Obj1 get1(){
		return obj1;
	}
	
	public Obj2 get2(){
		return obj2;
	}
}
