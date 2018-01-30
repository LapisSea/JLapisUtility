package com.lapissea.util.event;

public interface EventListener<EventType>{
	
	void onEvent(EventType event);
	
}
