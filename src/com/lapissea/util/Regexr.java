package com.lapissea.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regexr{
	
	private static final List<Pattern> CACHE=new ArrayList<>();
	
	public Matcher match(@NotNull String regex, @NotNull CharSequence text){
		return getPattern(regex).matcher(text);
	}
	
	public static Pattern getPattern(@NotNull String regex){
		int id=Collections.binarySearch(CACHE, null, (e, a)->e.pattern().compareTo(regex));
		
		if(id>-1&&id<CACHE.size()){
			Pattern p=CACHE.get(id);
			if(!p.pattern().equals(regex)) p=compile(regex);
			return p;
		}
		return compile(regex);
	}
	
	private static Pattern compile(String regex){
		Pattern p=Pattern.compile(regex);
		CACHE.add(p);
		CACHE.sort(Comparator.comparing(Pattern::pattern));
		return p;
	}
	
}
