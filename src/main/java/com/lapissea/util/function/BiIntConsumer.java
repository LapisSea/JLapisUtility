package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface BiIntConsumer<T>{
	
	void accept(int i, T t);
	
	/**
	 * Returns a composed {@code BiConsumer} that performs, in sequence, this
	 * operation followed by the {@code after} operation. If performing either
	 * operation throws an exception, it is relayed to the caller of the
	 * composed operation.  If performing this operation throws an exception,
	 * the {@code after} operation will not be performed.
	 *
	 * @param after the operation to perform after this operation
	 * @return a composed {@code BiIntConsumer} that performs in sequence this
	 * operation followed by the {@code after} operation
	 * @throws NullPointerException if {@code after} is null
	 */
	@NotNull
	default BiIntConsumer<T> andThen(@NotNull BiIntConsumer<? super T> after){
		Objects.requireNonNull(after);
		
		return (l, r) -> {
			accept(l, r);
			after.accept(l, r);
		};
	}
}
