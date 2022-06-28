package com.lapissea.util.function;

import java.util.Objects;
import java.util.function.Function;

public interface FunctionOL<T>{
	
	long apply(T t);
	
	default <V> FunctionOL<V> compose(Function<? super V, ? extends T> before){
		Objects.requireNonNull(before);
		return (V v)->apply(before.apply(v));
	}
	
	static <T> Function<T, T> identity(){
		return t->t;
	}
}
