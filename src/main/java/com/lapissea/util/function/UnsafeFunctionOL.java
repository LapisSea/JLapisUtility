package com.lapissea.util.function;

public interface UnsafeFunctionOL<T, E extends Throwable>{
	
	long apply(T t) throws E;
	
}
