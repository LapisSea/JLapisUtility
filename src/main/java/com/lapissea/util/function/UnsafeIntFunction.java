package com.lapissea.util.function;

public interface UnsafeIntFunction<R, E extends Throwable>{
	
	R apply(int value) throws E;
}
