package com.lapissea.util;

import java.util.AbstractList;
import java.util.function.Consumer;

public class ArrayViewList<T> extends AbstractList<T>{
	private T[] source;
	
	public static <T> PairM<Consumer<T[]>, ArrayViewList<T>> create(){
		return create((T[])null);
	}
	
	public static <T> PairM<Consumer<T[]>, ArrayViewList<T>> create(T[] source){
		ArrayViewList<T> arr=new ArrayViewList<>(source);
		return new PairM<>(arr::setSource, arr);
	}
	
	@NotNull
	public static <T> ArrayViewList<T> create(Consumer<Consumer<T[]>> setterConsumer){
		return create(null, setterConsumer);
	}
	
	@NotNull
	public static <T> ArrayViewList<T> create(T[] source, @Nullable Consumer<Consumer<T[]>> setterConsumer){
		ArrayViewList<T> arr=new ArrayViewList<>(source);
		if(setterConsumer!=null) setterConsumer.accept(arr::setSource);
		return arr;
	}
	
	protected ArrayViewList(T[] source){
		this.source=source;
	}
	
	private void setSource(T[] source){
		this.source=source;
	}
	
	@Override
	public T get(int index){
		return source[index];
	}
	
	@Override
	public int size(){
		return source.length;
	}
}
