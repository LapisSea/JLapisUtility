package com.lapissea.util.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class IntEventRegistry{
	
	private List<IntConsumer> listeners;
	
	public boolean register(IntConsumer listener){
		if(listeners == null) listeners = new ArrayList<>(2);
		else if(listeners.contains(listener)) return false;
		listeners.add(listener);
		return true;
	}
	
	public boolean unregister(IntConsumer listener){
		return listeners != null && listeners.remove(listener);
	}
	
	public void dispatch(int obj){
		if(listeners == null) return;
		for(IntConsumer listener : listeners){
			listener.accept(obj);
		}
	}
}
