package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;

public interface UnsafeConsumerOL<T, E extends Throwable>{
	
	void accept(T t, long l) throws E;
	
	@NotNull
	default <Ex extends E> UnsafeConsumerOL<T, E> andThen(@NotNull UnsafeConsumerOL<? super T, Ex> after){
		Objects.requireNonNull(after);
		return (T t, long l) -> {
			accept(t, l);
			after.accept(t, l);
		};
	}
	
}
