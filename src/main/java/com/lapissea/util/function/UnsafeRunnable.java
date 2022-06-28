package com.lapissea.util.function;

public interface UnsafeRunnable<E extends Throwable>{
	
	void run() throws E;
}
