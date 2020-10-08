package com.lapissea.util.function;

@FunctionalInterface
public interface UnsafeLongFunction<R, E extends Throwable>{
	
	/**
	 * Applies this function to the given argument.
	 *
	 * @param value the function argument
	 * @return the function result
	 */
	R apply(long value) throws E;
}
