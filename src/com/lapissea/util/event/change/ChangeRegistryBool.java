package com.lapissea.util.event.change;

import com.lapissea.util.event.BoolEventRegistry;
import com.lapissea.util.function.BooleanConsumer;

import java.util.Objects;

public class ChangeRegistryBool extends BoolEventRegistry{
	
	private boolean object;
	
	public ChangeRegistryBool(boolean object, BooleanConsumer... listener){
		this(listener);
		this.object=object;
	}
	
	public ChangeRegistryBool(BooleanConsumer... listener){
		for(BooleanConsumer l : listener){
			register(l);
		}
	}
	
	public ChangeRegistryBool(){}
	
	public ChangeRegistryBool(boolean object){
		this.object=object;
	}
	
	public void set(boolean object){
		if(Objects.equals(this.object, object)) return;
		dispatch(object);
		this.object=object;
	}
	
	public boolean get(){
		return object;
	}
	
	@Override
	public String toString(){
		return getClass().getSimpleName()+"{val="+object+"}";
	}
}
