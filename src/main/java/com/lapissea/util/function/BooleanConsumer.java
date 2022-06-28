package com.lapissea.util.function;

import com.lapissea.util.NotNull;

import java.util.Objects;

public interface BooleanConsumer{
	
	void accept(boolean t);
	
	@NotNull
	default BooleanConsumer andThen(@NotNull BooleanConsumer after){
		Objects.requireNonNull(after);
		return b->{ accept(b); after.accept(b); };
	}
}
