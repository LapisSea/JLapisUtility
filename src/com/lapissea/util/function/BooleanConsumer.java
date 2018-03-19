package com.lapissea.util.function;

import java.util.Objects;

public interface BooleanConsumer{
	
	void accept(boolean t);
	
	default BooleanConsumer andThen(BooleanConsumer after){
		Objects.requireNonNull(after);
		return b->{ accept(b); after.accept(b); };
	}
}
