package com.lapissea.util.function;

import java.util.Objects;

@FunctionalInterface
public interface UnsafeBiConsumer<T, U, E extends Throwable>{
	
	void accept(T t, U u) throws E;
	
	/**
	 * Returns a composed {@code BiConsumer} that performs, in sequence, this
	 * operation followed by the {@code after} operation. If performing either
	 * operation throws an exception, it is relayed to the caller of the
	 * composed operation.  If performing this operation throws an exception,
	 * the {@code after} operation will not be performed.
	 *
	 * @param after the operation to perform after this operation
	 * @return a composed {@code BiConsumer} that performs in sequence this
	 * operation followed by the {@code after} operation
	 * @throws NullPointerException if {@code after} is null
	 */
	default UnsafeBiConsumer<T, U, E> andThen(UnsafeBiConsumer<? super T, ? super U, ? extends E> after){
		Objects.requireNonNull(after);
		
		return (l, r)->{
			accept(l, r);
			after.accept(l, r);
		};
	}
}
