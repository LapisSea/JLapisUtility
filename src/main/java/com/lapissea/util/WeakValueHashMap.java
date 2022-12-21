package com.lapissea.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;

public class WeakValueHashMap<K, V> extends DeletingValueHashMap<K, V, WeakValueHashMap<K, V>>{
	
	private final class WeakEntry<T> extends WeakReference<T> implements DeletingValueEntry<K, T>{
		private final K key;
		
		private WeakEntry(K key, T value, ReferenceQueue<T> queue){
			super(value, queue);
			this.key = key;
		}
		
		@Override
		public K getKey(){
			return key;
		}
	}
	
	public WeakValueHashMap(){ }
	
	public WeakValueHashMap(Map<? extends K, ? extends V> m){
		super(m);
	}
	
	@Override
	protected DeletingValueEntry<K, V> newNode(K key, V value, ReferenceQueue<V> gcQueue){
		return new WeakEntry<>(key, value, gcQueue);
	}
	
	@Override
	public WeakValueHashMap<K, V> copy(){
		return new WeakValueHashMap<>(this);
	}
}
