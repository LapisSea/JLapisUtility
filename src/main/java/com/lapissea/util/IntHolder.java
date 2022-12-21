package com.lapissea.util;

import java.util.Objects;

public class IntHolder{
	public int num;
	
	public IntHolder(){ }
	
	public IntHolder(int num){
		this.num = num;
	}
	
	public int getNum(){
		return num;
	}
	
	public void setNum(int num){
		this.num = num;
	}
	
	@Override
	public String toString(){
		return TextUtil.toString(num);
	}
	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if(!(o instanceof IntHolder)) return false;
		IntHolder that = (IntHolder)o;
		return Objects.equals(getNum(), that.getNum());
	}
	@Override
	public int hashCode(){
		return num;
	}
}
