package com.lapissea.util.function;

import java.util.Objects;

public interface TriConsumer<A, B, C>{
	
	void accept(A a, B b, C c);
	
	default TriConsumer<A, B, C> andThen(TriConsumer<A, B, C> after){
		Objects.requireNonNull(after);
		return (a, b, c)->{
			accept(a, b, c);
			after.accept(a, b, c);
		};
	}
}
