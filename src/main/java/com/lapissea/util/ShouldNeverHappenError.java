package com.lapissea.util;

public class ShouldNeverHappenError extends RuntimeException{
	
	public ShouldNeverHappenError(){
	}
	
	public ShouldNeverHappenError(String message){
		super(message);
	}
	
	public ShouldNeverHappenError(String message, Throwable cause){
		super(message, cause);
	}
	
	public ShouldNeverHappenError(Throwable cause){
		super(cause);
	}
	
	public ShouldNeverHappenError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace){
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
