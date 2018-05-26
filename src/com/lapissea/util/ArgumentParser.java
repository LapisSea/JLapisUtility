package com.lapissea.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ArgumentParser{
	
	private final Map<String, Object> data=new HashMap<>();
	
	public ArgumentParser(@NotNull String[] args){
		for(String arg : args){
			if(arg.startsWith("--")){
				int pos=arg.indexOf("=");
				data.put(arg.substring(2, pos), arg.substring(pos+1));
			}
		}
	}
	
	public boolean hasValue(@NotNull String key){
		return data.containsKey(key);
	}
	
	@Nullable
	public String getString(@NotNull String key){
		return getString(key, null);
	}
	
	public void setString(@NotNull String key, String val){
		data.put(key, val);
	}
	
	@Nullable
	public String getString(@NotNull String key, @Nullable String def){
		Object o=data.get(key);
		if(o==null) return def;
		return o.toString();
	}
	
	public int getInt(@NotNull String key){
		return getInt(key, -1);
	}
	
	public int getInt(@NotNull String key, int def){
		Object o=data.get(key);
		if(o==null) return def;
		if(o instanceof Integer) return (int)o;
		data.put(key, o=Integer.parseInt(o.toString()));
		return (int)o;
	}
	
	public void setInt(@NotNull String key, int val){
		data.put(key, val);
	}
	
	public boolean getBoolean(@NotNull String key){
		return getBoolean(key, false);
	}
	
	public boolean getBoolean(@NotNull String key, boolean def){
		Object o=data.get(key);
		if(o==null) return def;
		if(o instanceof Boolean) return (boolean)o;
		data.put(key, o=Boolean.parseBoolean(o.toString()));
		return (boolean)o;
	}
	
	public void setBoolean(@NotNull String key, boolean val){
		data.put(key, val);
	}
	
	@Override
	public String toString(){
		return data.entrySet().stream().map(e->"--"+escape(e.getKey())+"="+escape(TextUtil.toString(e.getValue()))).collect(Collectors.joining(" "));
	}
	
	private String escape(String in){
		in=in.replaceAll("\"", "\\\"");
		
		if(in.chars().anyMatch(Character::isWhitespace)){
			in='"'+in+'"';
		}
		return in;
	}
}
