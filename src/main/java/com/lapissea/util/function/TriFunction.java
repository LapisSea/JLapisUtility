package com.lapissea.util.function;

import java.util.function.Function;

@FunctionalInterface
public interface TriFunction<A, B, C, R>{
	
	R apply(A a, B b, C c);
	
	static <T> Function<T, T> identity(){
		return t->t;
	}
}
