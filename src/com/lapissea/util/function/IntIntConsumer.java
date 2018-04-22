package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;

public interface IntIntConsumer{
	
	void accept(int i, int value);
	
	@NotNull
	default IntIntConsumer andThen(@NotNull IntIntConsumer after){
		Objects.requireNonNull(after);
		
		return (l, r)->{
			accept(l, r);
			after.accept(l, r);
		};
	}
}
