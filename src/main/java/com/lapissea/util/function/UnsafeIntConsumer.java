package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;

public interface UnsafeIntConsumer<E extends Throwable>{
	
	void accept(int l) throws E;
	
	@NotNull
	default <Ex extends E> UnsafeIntConsumer<E> andThen(@NotNull UnsafeIntConsumer<Ex> after){
		Objects.requireNonNull(after);
		return (t) -> {
			accept(t);
			after.accept(t);
		};
	}
	
}
