package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface TriIntConsumer{
	
	void accept(int a, int b, int c);
	
	/**
	 * Returns a composed {@code BiConsumer} that performs, in sequence, this
	 * operation followed by the {@code after} operation. If performing either
	 * operation throws an exception, it is relayed to the caller of the
	 * composed operation.  If performing this operation throws an exception,
	 * the {@code after} operation will not be performed.
	 *
	 * @param after the operation to perform after this operation
	 * @return a composed {@code TriIntConsumer} that performs in sequence this
	 * operation followed by the {@code after} operation
	 * @throws NullPointerException if {@code after} is null
	 */
	@NotNull
	default TriIntConsumer andThen(@NotNull TriIntConsumer after){
		Objects.requireNonNull(after);
		
		return (a, b, c) -> {
			accept(a, b, c);
			after.accept(a, b, c);
		};
	}
}
