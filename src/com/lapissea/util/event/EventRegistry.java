package com.lapissea.util.event;

import java.util.ArrayList;
import java.util.List;

public class EventRegistry<SELF, EventType extends Event<SELF>> implements IEventRegistry<SELF, EventType>{
	
	private List<EventListener<EventType>> listeners;
	
	@Override
	public boolean register(EventListener<EventType> listener){
		if(listeners==null) listeners=new ArrayList<>(2);
		else if(listeners.contains(listener)) return false;
		listeners.add(listener);
		return true;
	}
	
	@Override
	public boolean unregister(EventListener<EventType> listener){
		return listeners!=null&&listeners.remove(listener);
	}
	
	@Override
	public void dispatch(EventType event){
		if(listeners==null) return;
		for(EventListener<EventType> listener : listeners){
			listener.onEvent(event);
		}
	}
	
	@Override
	public void asyncDispatch(EventType event){
		if(listeners!=null) listeners.parallelStream().forEach(l->l.onEvent(event));
	}
	
	
}
