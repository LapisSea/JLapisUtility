package com.lapissea.util.event.change;

import com.lapissea.util.event.Event;
import com.lapissea.util.event.EventListener;
import com.lapissea.util.event.EventRegistry;

import java.util.Objects;

public class ChangeRegistry<ObjectType> extends EventRegistry<ChangeRegistry<ObjectType>, ChangeRegistry.ValueChange<ObjectType>>{
	
	public static class ValueChange<ObjectType> extends Event<ChangeRegistry<ObjectType>>{
		public final ObjectType object;
		
		ValueChange(ChangeRegistry<ObjectType> source, ObjectType object){
			super(source);
			this.object=object;
		}
		
	}
	
	private ObjectType object;
	
	@SafeVarargs
	public ChangeRegistry(ObjectType object, EventListener<ValueChange<ObjectType>>... listener){
		this(listener);
		this.object=object;
	}
	
	@SafeVarargs
	public ChangeRegistry(EventListener<ValueChange<ObjectType>>... listener){
		for(EventListener<ValueChange<ObjectType>> l : listener){
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
		dispatch(new ValueChange<>(this, object));
	}
	
	public ObjectType get(){
		return object;
	}
	
	@Override
	public String toString(){
		return getClass().getSimpleName()+"{val="+object+"}";
	}
}
