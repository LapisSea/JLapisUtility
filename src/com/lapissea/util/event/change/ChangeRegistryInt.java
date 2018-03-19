package com.lapissea.util.event.change;

import com.lapissea.util.event.IntEventRegistry;

import java.util.Objects;
import java.util.function.IntConsumer;

public class ChangeRegistryInt extends IntEventRegistry{
	
	private int object;
	
	public ChangeRegistryInt(int object, IntConsumer... listener){
		this(listener);
		this.object=object;
	}
	
	public ChangeRegistryInt(IntConsumer... listener){
		for(IntConsumer l : listener){
			register(l);
		}
	}
	
	public ChangeRegistryInt(){ }
	
	public ChangeRegistryInt(int object){
		this.object=object;
	}
	
	public void set(int object){
		if(Objects.equals(this.object, object)) return;
		dispatch(object);
		this.object=object;
	}
	
	public int get(){
		return object;
	}
	
	@Override
	public String toString(){
		return getClass().getSimpleName()+"{val="+object+"}";
	}
}
