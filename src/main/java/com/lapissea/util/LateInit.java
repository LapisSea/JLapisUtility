package com.lapissea.util;

import com.lapissea.util.function.UnsafeSupplier;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LateInit<T, E extends Throwable>{
	
	public static class Safe<T> extends LateInit<T, RuntimeException>{
		public Safe(Supplier<T> initializer){
			super(initializer::get);
		}
		public Safe(Supplier<T> initializer, Executor executor){
			super(initializer::get, executor);
		}
	}
	
	private T result;
	private E err;
	
	public LateInit(UnsafeSupplier<T, E> initializer){
		this(initializer, ForkJoinPool.commonPool());
	}
	
	public LateInit(UnsafeSupplier<T, E> initializer, Executor executor){
		executor.execute(() -> {
			try{
				result = Objects.requireNonNull(initializer.get());
			}catch(Throwable e){
				err = (E)e;
			}finally{
				synchronized(LateInit.this){
					LateInit.this.notifyAll();
				}
			}
		});
	}
	
	public boolean isInitialized(){
		return result != null || err != null;
	}
	
	public void block(){
		while(!isInitialized()){
			synchronized(LateInit.this){
				try{
					LateInit.this.wait(1000);
				}catch(InterruptedException e){
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	public T get() throws E{
		if(!isInitialized()) block();
		if(err != null) throw err;
		return result;
	}
	
	public void ifInited(Consumer<T> action) throws E{
		if(isInitialized()){
			if(err != null) throw err;
			action.accept(result);
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if(!(o instanceof LateInit)) return false;
		LateInit<?, ?> lateInit = (LateInit<?, ?>)o;
		return isInitialized() == lateInit.isInitialized() &&
		       Objects.equals(result, lateInit.result) &&
		       Objects.equals(err, lateInit.err);
	}
	
	@Override
	public String toString(){
		if(!isInitialized()) return "LateInit<...>";
		if(err != null) return "LateInit<failed: " + err + ">";
		return "LateInit(" + TextUtil.toString(result) + ")";
	}
}
