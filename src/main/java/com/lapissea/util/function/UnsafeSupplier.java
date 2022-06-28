package com.lapissea.util.function;

public interface UnsafeSupplier<T, E extends Throwable>{
	
	T get() throws E;
}
