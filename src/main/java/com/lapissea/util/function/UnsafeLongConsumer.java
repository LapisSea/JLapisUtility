package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;

public interface UnsafeLongConsumer<E extends Throwable>{
	
	void accept(long l) throws E;
	
	@NotNull
	default <Ex extends E> UnsafeLongConsumer<E> andThen(@NotNull UnsafeLongConsumer<Ex> after){
		Objects.requireNonNull(after);
		return (t)->{
			accept(t);
			after.accept(t);
		};
	}
	
}
