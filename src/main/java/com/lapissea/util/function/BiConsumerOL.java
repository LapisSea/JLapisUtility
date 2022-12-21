package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface BiConsumerOL<T>{
	
	void accept(T t, long l);
	
	@NotNull
	default BiConsumerOL<T> andThen(@NotNull BiConsumerOL<? super T> after){
		Objects.requireNonNull(after);
		
		return (l, r) -> {
			accept(l, r);
			after.accept(l, r);
		};
	}
}
