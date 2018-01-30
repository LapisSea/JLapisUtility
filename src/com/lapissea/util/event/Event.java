package com.lapissea.util.event;

public class Event<SourceType>{
	
	protected SourceType source;
	
	public Event(SourceType source){this.source=source;}
	
	
	public SourceType getSource(){
		return source;
	}
	
}
