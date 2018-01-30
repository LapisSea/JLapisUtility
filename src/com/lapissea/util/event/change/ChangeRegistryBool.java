package com.lapissea.util.event.change;

import com.lapissea.util.event.Event;
import com.lapissea.util.event.EventListener;
import com.lapissea.util.event.EventRegistry;

import java.util.Objects;

public class ChangeRegistryBool extends EventRegistry<ChangeRegistryBool, ChangeRegistryBool.ValueChange>{
	
	public class ValueChange extends Event<ChangeRegistryBool>{
		public final boolean bool;
		
		public ValueChange(boolean bool){
			super(ChangeRegistryBool.this);
			this.bool=bool;
		}
		
	}
	
	private boolean object;
	
	@SafeVarargs
	public ChangeRegistryBool(boolean object, EventListener<ValueChange>... listener){
		this(listener);
		this.object=object;
	}
	
	@SafeVarargs
	public ChangeRegistryBool(EventListener<ValueChange>... listener){
		for(EventListener<ValueChange> l : listener){
			register(l);
		}
	}
	
	public ChangeRegistryBool(){}
	
	public ChangeRegistryBool(boolean object){
		this.object=object;
	}
	
	public void set(boolean object){
		if(Objects.equals(this.object, object)) return;
		dispatch(new ValueChange(object));
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
