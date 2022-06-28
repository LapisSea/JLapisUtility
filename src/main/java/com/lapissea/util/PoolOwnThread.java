package com.lapissea.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.function.Supplier;

public class PoolOwnThread extends Thread{
	
	@NotNull
	public static <U> CompletableFuture<U> async(@NotNull Supplier<U> supplier){
		
		PoolOwnThread pool=get();
		if(pool!=null) return async(supplier, pool.getPool());
		
		return CompletableFuture.supplyAsync(supplier);
	}
	
	public static <U> CompletableFuture<U> async(@NotNull Supplier<U> supplier, Executor executor){
		return CompletableFuture.supplyAsync(supplier, executor);
	}
	public static CompletableFuture<Void> async(@NotNull Runnable runnable, Executor executor){
		return CompletableFuture.runAsync(runnable, executor);
	}
	
	@NotNull
	public static <U> CompletableFuture<Void> async(@NotNull Runnable runnable){
		
		PoolOwnThread pool=get();
		if(pool!=null) return CompletableFuture.runAsync(runnable, pool.getPool());
		
		return CompletableFuture.runAsync(runnable);
	}
	
	public static <U> U await(@NotNull CompletableFuture<U> supplier){
		return supplier.join();
	}
	
	public static PoolOwnThread get(){
		
		Thread th=Thread.currentThread();
		
		if(th instanceof PoolOwnThread) return (PoolOwnThread)th;
		
		if(th instanceof ForkJoinWorkerThread){
			ForkJoinPool pool=((ForkJoinWorkerThread)th).getPool();
			if(pool instanceof Pool) return ((Pool)pool).parent();
		}
		
		return null;
	}
	
	public class Pool extends ForkJoinPool{
		public Pool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode){
			super(parallelism, factory, handler, asyncMode);
		}
		
		public PoolOwnThread parent(){
			return PoolOwnThread.this;
		}
	}
	
	private Pool pool;
	
	public PoolOwnThread(Runnable runnable, String name){
		super(runnable, name);
		pool=new Pool(Runtime.getRuntime().availableProcessors(), pool->{
			ForkJoinWorkerThread worker=ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
			worker.setName(name+"-w"+worker.getPoolIndex());
			return worker;
		}, null, false);
		
	}
	
	public Pool getPool(){
		return pool;
	}
}
