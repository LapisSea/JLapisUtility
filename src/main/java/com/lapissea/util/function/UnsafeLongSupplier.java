package com.lapissea.util.function;

@FunctionalInterface
public interface UnsafeLongSupplier<E extends Throwable>{
	
	/**
	 * Gets a result.
	 *
	 * @return a result
	 */
	long getAsLong() throws E;
}
