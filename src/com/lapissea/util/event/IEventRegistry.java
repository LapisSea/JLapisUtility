package com.lapissea.util.event;

public interface IEventRegistry<SELF,EventType extends Event<SELF>>{
	
	boolean register(EventListener<EventType> listener);
	
	boolean unregister(EventListener<EventType> listener);
	
	void dispatch(EventType event);
	
	void asyncDispatch(EventType event);
}
