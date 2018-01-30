package com.lapissea.util.event.change;

import com.lapissea.util.event.Event;
import com.lapissea.util.event.EventListener;
import com.lapissea.util.event.EventRegistry;

import java.util.Objects;

public class ChangeRegistryLong extends EventRegistry<ChangeRegistryLong, ChangeRegistryLong.ValueChange>{
	
	public class ValueChange extends Event<ChangeRegistryLong>{
		public final long bool;
		
		public ValueChange(long bool){
			super(ChangeRegistryLong.this);
			this.bool=bool;
		}
		
	}
	
	private long object;
	
	@SafeVarargs
	public ChangeRegistryLong(long object, EventListener<ValueChange>... listener){
		this(listener);
		this.object=object;
	}
	
	@SafeVarargs
	public ChangeRegistryLong(EventListener<ValueChange>... listener){
		for(EventListener<ValueChange> l : listener){
			register(l);
		}
	}
	public ChangeRegistryLong(){ }
	
	public ChangeRegistryLong(long object){
		this.object=object;
	}
	
	public void set(long object){
		if(Objects.equals(this.object, object)) return;
		dispatch(new ValueChange(object));
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
