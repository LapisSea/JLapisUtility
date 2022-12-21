package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface BiConsumerLO<T>{
	
	void accept(long l, T t);
	
	@NotNull
	default BiConsumerLO<T> andThen(@NotNull BiConsumerLO<? super T> after){
		Objects.requireNonNull(after);
		
		return (l, r) -> {
			accept(l, r);
			after.accept(l, r);
		};
	}
}

