package com.lapissea.util.event.change;

import com.lapissea.util.event.Event;
import com.lapissea.util.event.EventListener;
import com.lapissea.util.event.EventRegistry;

import java.util.Objects;

public class ChangeRegistryInt extends EventRegistry<ChangeRegistryInt, ChangeRegistryInt.ValueChange>{
	
	public class ValueChange extends Event<ChangeRegistryInt>{
		public final int bool;
		
		public ValueChange(int bool){
			super(ChangeRegistryInt.this);
			this.bool=bool;
		}
		
	}
	
	private int object;
	
	@SafeVarargs
	public ChangeRegistryInt(int object, EventListener<ValueChange>... listener){
		this(listener);
		this.object=object;
	}
	
	@SafeVarargs
	public ChangeRegistryInt(EventListener<ValueChange>... listener){
		for(EventListener<ValueChange> l : listener){
			register(l);
		}
	}
	public ChangeRegistryInt(){ }
	
	public ChangeRegistryInt(int object){
		this.object=object;
	}
	
	public void set(int object){
		if(Objects.equals(this.object, object)) return;
		dispatch(new ValueChange(object));
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
