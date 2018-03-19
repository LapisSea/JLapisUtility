package com.lapissea.util.event.change;

import com.lapissea.util.event.LongEventRegistry;

import java.util.Objects;
import java.util.function.LongConsumer;

public class ChangeRegistryLong extends LongEventRegistry{
	
	private long object;
	
	public ChangeRegistryLong(long object, LongConsumer... listener){
		this(listener);
		this.object=object;
	}
	
	public ChangeRegistryLong(LongConsumer... listener){
		for(LongConsumer l : listener){
			register(l);
		}
	}
	
	public ChangeRegistryLong(){ }
	
	public ChangeRegistryLong(long object){
		this.object=object;
	}
	
	public void set(long object){
		if(Objects.equals(this.object, object)) return;
		dispatch(object);
		this.object=object;
	}
	
	public long get(){
		return object;
	}
	
	@Override
	public String toString(){
		return getClass().getSimpleName()+"{val="+object+"}";
	}
}
