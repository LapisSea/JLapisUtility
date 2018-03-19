package com.lapissea.util.event;

import com.lapissea.util.function.BooleanConsumer;

import java.util.ArrayList;
import java.util.List;

public class BoolEventRegistry{
	
	private List<BooleanConsumer> listeners;
	
	public boolean register(BooleanConsumer listener){
		if(listeners==null) listeners=new ArrayList<>(2);
		else if(listeners.contains(listener)) return false;
		listeners.add(listener);
		return true;
	}
	
	public boolean unregister(BooleanConsumer listener){
		return listeners!=null&&listeners.remove(listener);
	}
	
	public void dispatch(boolean obj){
		if(listeners==null) return;
		for(BooleanConsumer listener : listeners){
			listener.accept(obj);
		}
	}
}
