package com.lapissea.util.event.change;

import com.lapissea.util.event.EventRegistry;

import java.util.Objects;
import java.util.function.Consumer;

public class ChangeRegistry<ObjectType> extends EventRegistry<ObjectType>{
	
	private ObjectType object;
	
	@SafeVarargs
	public ChangeRegistry(ObjectType object, Consumer<ObjectType>... listener){
		this(listener);
		this.object=object;
	}
	
	@SafeVarargs
	public ChangeRegistry(Consumer<ObjectType>... listener){
		for(Consumer<ObjectType> l : listener){
			register(l);
		}
	}
	
	public ChangeRegistry(){ }
	
	public ChangeRegistry(ObjectType object){
		this.object=object;
	}
	
	public void set(ObjectType object){
		if(Objects.equals(this.object, object)) return;
		this.object=object;
		dispatch(object);
	}
	
	public ObjectType get(){
		return object;
	}
	
	@Override
	public String toString(){
		return getClass().getSimpleName()+"{val="+object+"}";
	}
}
