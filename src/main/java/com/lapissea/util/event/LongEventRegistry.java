package com.lapissea.util.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

public class LongEventRegistry{
	private List<LongConsumer> listeners;
	
	public boolean register(LongConsumer listener){
		if(listeners==null) listeners=new ArrayList<>(2);
		else if(listeners.contains(listener)) return false;
		listeners.add(listener);
		return true;
	}
	
	public boolean unregister(LongConsumer listener){
		return listeners!=null&&listeners.remove(listener);
	}
	
	public void dispatch(long obj){
		if(listeners==null) return;
		for(LongConsumer listener : listeners){
			listener.accept(obj);
		}
	}
}
