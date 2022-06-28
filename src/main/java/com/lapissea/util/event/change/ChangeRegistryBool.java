package com.lapissea.util.event.change;

import com.lapissea.util.NotNull;
import com.lapissea.util.event.BoolEventRegistry;
import com.lapissea.util.function.BooleanConsumer;

public class ChangeRegistryBool extends BoolEventRegistry{
	
	private boolean object;
	
	public ChangeRegistryBool(boolean object, BooleanConsumer... listener){
		this(listener);
		this.object=object;
	}
	
	public ChangeRegistryBool(@NotNull BooleanConsumer... listener){
		for(BooleanConsumer l : listener){
			register(l);
		}
	}
	
	public ChangeRegistryBool(){}
	
	public ChangeRegistryBool(boolean object){
		this.object=object;
	}
	
	public void set(boolean object){
		if(this.object==object) return;
		this.object=object;
		dispatch(object);
	}
	
	public boolean get(){
		return object;
	}
	
	@NotNull
	@Override
	public String toString(){
		return getClass().getSimpleName()+"{val="+object+"}";
	}
}
