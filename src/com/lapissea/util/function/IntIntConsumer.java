package com.lapissea.util.function;

import java.util.Objects;

public interface IntIntConsumer{
	
	void accept(int i, int value);
	
	default IntIntConsumer andThen(IntIntConsumer after){
		Objects.requireNonNull(after);
		
		return (l, r)->{
			accept(l, r);
			after.accept(l, r);
		};
	}
}