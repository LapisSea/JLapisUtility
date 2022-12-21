package com.lapissea.util.event.change;

import com.lapissea.util.NotNull;
import com.lapissea.util.event.IntEventRegistry;

import java.util.function.IntConsumer;

public class ChangeRegistryInt extends IntEventRegistry{
	
	private int object;
	
	public ChangeRegistryInt(int object, IntConsumer... listener){
		this(listener);
		this.object = object;
	}
	
	public ChangeRegistryInt(@NotNull IntConsumer... listener){
		for(IntConsumer l : listener){
			register(l);
		}
	}
	
	public ChangeRegistryInt(){ }
	
	public ChangeRegistryInt(int object){
		this.object = object;
	}
	
	public void set(int object){
		if(this.object == object) return;
		this.object = object;
		dispatch(object);
	}
	
	public int get(){
		return object;
	}
	
	@NotNull
	@Override
	public String toString(){
		return getClass().getSimpleName() + "{val=" + object + "}";
	}
}
