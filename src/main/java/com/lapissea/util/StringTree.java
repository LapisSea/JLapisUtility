package com.lapissea.util;

import java.util.*;

import static com.lapissea.util.TextUtil.*;
import static java.lang.Math.*;
import static java.util.Comparator.*;

public class StringTree<T>{
	
	private static final int
		PUT_FAIL            =0,
		PUT_SUCCESS         =1,
		PUT_SUCCESS_OPTIMIZE=2;
	
	private class Part extends ArrayList<Part>{
		protected String signature;
		
		@Nullable
		T obj;
		
		Part(String signature, T val){
			this(signature);
			obj=val;
		}
		
		Part(String signature){
			super(2);
			this.signature=signature;
		}
		
		boolean partMatch(@NotNull String key, int pos){
			return key.regionMatches(pos, signature, 0, signature.length());
		}
		
		@Nullable
		Part find(@NotNull String key, int pos){
			int partEnd=pos+signature.length();
			
			//nope key shorter than what the signature would require to match
			if(key.length()<partEnd) return null;
			
			boolean endHit =key.length()==partEnd;
			boolean matched=partMatch(key, pos);
			
			if(endHit){
				//end hit? then it must match or it's invalid
				return matched?this:null;
			}else{
				//if it's matched children have to be checked, and if it's not then there is no point in continuing
				if(!matched) return null;
			}
			
			//ok signature matches! Pass to children
			
			for(Part child : this){
				Part result=child.find(key, partEnd);
				
				if(result!=null){
					//Found it!
					return result;
				}
			}
			
			//no child parts match
			return null;
		}
		
		int put(@NotNull String key, int pos, T value){
			int partEnd=pos+signature.length();
			
			//nope key shorter than what the signature would require to match
			if(key.length()<partEnd) return PUT_FAIL;
			
			boolean endHit =key.length()==partEnd;
			boolean matched=partMatch(key, pos);
			
			if(endHit){
				//end hit? then it must set and success or fail
				if(matched){
					obj=value;
					return PUT_SUCCESS;
				}
				return PUT_FAIL;
			}else{
				//if it's matched children have to be checked, and if it's not then it's a fail
				if(!matched) return PUT_FAIL;
			}
			
			//ok signature matches! Pass to children
			return childPut(key, partEnd, value);
		}
		
		int childPut(@NotNull String key, int end, T value){
			
			if(isEmpty()){
				add(new Part(key.substring(end), value));
				return PUT_SUCCESS_OPTIMIZE;
			}
			
			for(Part child : this){
				switch(child.put(key, end, value)){
				case PUT_SUCCESS:
					return PUT_SUCCESS;
				
				case PUT_FAIL:
					break;
				
				case PUT_SUCCESS_OPTIMIZE:
					optimize();
					return PUT_SUCCESS;
				
				default:
					throw new RuntimeException();
				}
			}
			
			add(new Part(key.substring(end), value));
			return PUT_SUCCESS_OPTIMIZE;
		}
		
		@Override
		public boolean equals(@NotNull Object o){
			if(o==this) return true;
			if(!o.getClass().equals(Part.class)) return false;
			Part p=(Part)o;
			return p.signature.equals(o);
		}
		
		@NotNull
		@Override
		public String toString(){
			StringBuilder sb=new StringBuilder();
			root.displayTree(sb, 0);
			sb.setLength(max(sb.length()-1, 0));
			return sb.toString();
		}
		
		void optimize(){
			if(isEmpty()) return;
			while(true){
				
				Part compactPart=stream().map(childTest->{
					StringBuilder sb   =new StringBuilder(childTest.signature);
					List<Part>    build=new ArrayList<>();
					
					childFor:
					for(Part child : this){
						for(int j=0;j<sb.length();j++){
							if(sb.charAt(j)!=child.signature.charAt(j)){
								if(j==0) continue childFor;
								sb.setLength(j);
								break;
							}
						}
						build.add(child);
					}
					
					if(build.size()<=1) return null;
					
					Part p=new Part(sb.toString());
					p.addAll(build);
					return p;
					
				}).filter(Objects::nonNull).max(Comparator.comparingInt(List::size)).orElse(null);
				
				if(compactPart==null) break;
				
				for(Part part : compactPart){
					remove(part);
				}
				
				compactPart.forEach(e->e.signature=e.signature.substring(compactPart.signature.length()));
				compactPart.optimize();
				add(compactPart);
			}
			
			sort(comparing(a->a.signature));
		}
		
		void printSig(@NotNull StringBuilder sb){
			sb.append(signature);
			if(obj!=null) sb.append("=").append(TextUtil.toString(obj));
			sb.append(NEW_LINE);
		}
		
		void displayTree(@NotNull StringBuilder sb, int tabbing){
			for(int i=0;i<tabbing;i++){
				sb.append(' ');
			}
			
			printSig(sb);
			
			for(Part child : this){
				child.displayTree(sb, tabbing+signature.length());
			}
		}
	}
	
	private class PartRoot extends Part{
		PartRoot(){
			super("");
		}
		
		@Nullable
		@Override
		Part find(@NotNull String key, int pos){
			if(key.isEmpty()) return this;
			
			for(Part child : this){
				Part result=child.find(key, 0);
				
				if(result!=null){
					//Found it!
					return result;
				}
			}
			return null;
		}
		
		@Override
		int put(@NotNull String key, int pos, T value){
			if(key.isEmpty()){
				obj=value;
				return PUT_SUCCESS;
			}
			int code=childPut(key, 0, value);
			if(code==PUT_SUCCESS_OPTIMIZE){
				this.optimize();
				return PUT_SUCCESS;
			}else return code;
		}
		
		@Override
		public void displayTree(@NotNull StringBuilder sb, int tabbing){
			if(obj==null){
				for(Part child : this){
					child.displayTree(sb, tabbing);
				}
				return;
			}
			
			for(int i=0;i<tabbing;i++){
				sb.append(' ');
			}
			
			printSig(sb);
			
			for(Part child : this){
				child.displayTree(sb, tabbing);
			}
		}
	}
	
	@NotNull
	PartRoot root=new PartRoot();
	
	@Nullable
	private LinkedList<Part> unoptimized;
	
	public StringTree(){
	
	
	}
	
	public void clear(){
		root.clear();
		root.obj=null;
	}
	
	@Nullable
	public T get(@NotNull String key){
		optimize();
		Part part=root.find(key, 0);
		return part==null?null:part.obj;
	}
	
	public void put(@NotNull String key, T value){
		root.put(key, 0, value);
	}
	
	@NotNull
	public String displayTree(){
		optimize();
		StringBuilder sb=new StringBuilder();
		root.displayTree(sb, 0);
		sb.setLength(max(sb.length()-1, 0));
		return sb.toString();
	}
	
	protected void optimize(){
		if(unoptimized!=null){
			unoptimized.forEach(Part::optimize);
			unoptimized=null;
		}
	}
}
