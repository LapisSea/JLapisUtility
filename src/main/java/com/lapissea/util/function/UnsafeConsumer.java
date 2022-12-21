package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

public interface UnsafeConsumer<T, E extends Throwable>{
	
	void accept(T t) throws E;
	
	@NotNull
	default <Ex extends E> UnsafeConsumer<T, E> andThen(@NotNull UnsafeConsumer<? super T, Ex> after){
		Objects.requireNonNull(after);
		return (T t) -> {
			accept(t);
			after.accept(t);
		};
	}
	
	@NotNull
	default UnsafeConsumer<T, E> andThen(@NotNull Consumer<? super T> after){
		Objects.requireNonNull(after);
		return (T t) -> {
			accept(t);
			after.accept(t);
		};
	}
}

