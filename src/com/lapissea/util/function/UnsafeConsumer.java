package com.lapissea.util.function;

import java.util.Objects;
import java.util.function.Consumer;

public interface UnsafeConsumer<T,E extends Throwable>{
	
	void accept(T t) throws E;
	
	default <Ex extends E> UnsafeConsumer<T,E> andThen(UnsafeConsumer<? super T,Ex> after){
		Objects.requireNonNull(after);
		return (T t)->{
			accept(t);
			after.accept(t);
		};
	}
	
	default UnsafeConsumer<T,E> andThen(Consumer<? super T> after){
		Objects.requireNonNull(after);
		return (T t)->{
			accept(t);
			after.accept(t);
		};
	}
}
