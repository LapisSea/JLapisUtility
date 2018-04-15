package com.lapissea.util;

import java.util.HashMap;
import java.util.Map;

public class ArgumentParser{
	
	private final Map<String, Object> data=new HashMap<>();
	
	public ArgumentParser(String[] args){
		for(String arg : args){
			if(arg.startsWith("--")){
				int pos=arg.indexOf("=");
				data.put(arg.substring(2, pos), arg.substring(pos+1));
			}
		}
	}
	
	public boolean hasValue(String key){
		return data.containsKey(key);
	}
	
	public String getString(String key){
		return getString(key, null);
	}
	
	public String getString(String key, String def){
		Object o=data.get(key);
		if(o==null) return def;
		return o.toString();
	}
	
	public int getInt(String key){
		return getInt(key, -1);
	}
	
	public int getInt(String key, int def){
		Object o=data.get(key);
		if(o==null) return def;
		if(o instanceof Integer) return (int)o;
		data.put(key, o=Integer.parseInt(o.toString()));
		return (int)o;
	}
	
	public boolean getBoolean(String key){
		return getBoolean(key, false);
	}
	
	public boolean getBoolean(String key, boolean def){
		Object o=data.get(key);
		if(o==null) return def;
		if(o instanceof Boolean) return (boolean)o;
		data.put(key, o=Boolean.parseBoolean(o.toString()));
		return (boolean)o;
	}
}
