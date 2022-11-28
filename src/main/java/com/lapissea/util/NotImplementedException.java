package com.lapissea.util;

public class NotImplementedException extends RuntimeException{
	
	public static NotImplementedException infer(){
		StackTraceElement e=Thread.currentThread().getStackTrace()[2];
		return new NotImplementedException(e.getClassName().replace('$','.')+"#"+e.getMethodName()+" function not implemented!");
	}
	
	public NotImplementedException(){ }
	
	public NotImplementedException(String message){
		super(message);
	}
	
	public NotImplementedException(String message, Throwable cause){
		super(message, cause);
	}
	
	public NotImplementedException(Throwable cause){
		super(cause);
	}
}
