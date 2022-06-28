package com.lapissea.util;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LateInit<T>{
	
	private T       value;
	private boolean inited;
	
	public LateInit(Supplier<T> initializer){
		this(initializer, ForkJoinPool.commonPool());
	}
	
	public LateInit(Supplier<T> initializer, Executor executor){
		executor.execute(()->{
			try{
				value=initializer.get();
				inited=true;
			}catch(Throwable e){
				throw UtilL.uncheckedThrow(e);
			}
		});
	}
	
	public boolean isInited(){
		return inited;
	}
	
	public void block(){
		if(isInited()) return;
		UtilL.sleepUntil(this::isInited);
	}
	
	public T get(){
		block();
		return value;
	}
	
	public void ifInited(Consumer<T> action){
		if(isInited()) action.accept(value);
	}
	
	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		if(!(o instanceof LateInit)) return false;
		LateInit<?> lateInit=(LateInit<?>)o;
		return isInited()==lateInit.isInited()&&
		       Objects.equals(value, lateInit.value);
	}
	@Override
	public int hashCode(){
		int result=1;
		
		result=31*result+(value==null?0:value.hashCode());
		result=31*result+Boolean.hashCode(inited);
		
		return result;
	}
	@Override
	public String toString(){
		return isInited()?"LateInit("+TextUtil.toString(value)+")":"LateInit<...>";
	}
}
