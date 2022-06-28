package com.lapissea.util;

public interface Immutable<SELF>{
	
	SELF clone();
	
	boolean isImmutable();
	
	default SELF immutable(){
		if(isImmutable()) return (SELF)this;
		return clone();
	}
	
}
