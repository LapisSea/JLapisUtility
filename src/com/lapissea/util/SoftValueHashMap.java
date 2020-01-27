package com.lapissea.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Map;

public class SoftValueHashMap<K, V> extends DeletingValueHashMap<K, V, SoftValueHashMap<K, V>>{
	
	private final class SoftEntry<T> extends SoftReference<T> implements DeletingValueEntry<K, T>{
		private final K key;
		
		private SoftEntry(K key, T value, ReferenceQueue<T> queue){
			super(value, queue);
			this.key=key;
		}
		
		@Override
		public K getKey(){
			return key;
		}
	}
	
	public SoftValueHashMap(int initialCapacity, float loadFactor){
		super(initialCapacity, loadFactor);
	}
	
	public SoftValueHashMap(int initialCapacity){
		super(initialCapacity);
	}
	
	public SoftValueHashMap(){
	}
	
	public SoftValueHashMap(Map<? extends K, ? extends V> m){
		super(m);
	}
	
	@Override
	protected DeletingValueEntry<K, V> newNode(K key, V value, ReferenceQueue<V> gcQueue){
		return new SoftEntry<>(key, value, gcQueue);
	}
	
	@Override
	public SoftValueHashMap<K, V> copy(){
		return new SoftValueHashMap<>(this);
	}
}
