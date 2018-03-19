package com.lapissea.util.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventRegistry<ObjType>{
	
	private List<Consumer<ObjType>> listeners;
	
	public boolean register(Consumer<ObjType> listener){
		if(listeners==null) listeners=new ArrayList<>(2);
		else if(listeners.contains(listener)) return false;
		listeners.add(listener);
		return true;
	}
	
	public boolean unregister(Consumer<ObjType> listener){
		return listeners!=null&&listeners.remove(listener);
	}
	
	public void dispatch(ObjType obj){
		if(listeners==null) return;
		for(Consumer<ObjType> listener : listeners){
			listener.accept(obj);
		}
	}
	
}
