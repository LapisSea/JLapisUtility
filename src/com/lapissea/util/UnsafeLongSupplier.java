package com.lapissea.util;

@FunctionalInterface
public interface UnsafeLongSupplier<E extends Throwable>{
	
	/**
	 * Gets a result.
	 *
	 * @return a result
	 */
	long getAsLong() throws E;
}
