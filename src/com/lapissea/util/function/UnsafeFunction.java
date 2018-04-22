package com.lapissea.util.function;

import com.lapissea.util.NotNull;

public interface UnsafeFunction<T, R, E extends Throwable>{
	
	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 */
	@NotNull
	R apply(T t) throws E;
}
