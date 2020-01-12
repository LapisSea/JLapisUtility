package com.lapissea.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WeakValueHashMap<K, V> implements Map<K, V>{
	
	private final class WeakNode<T> extends WeakReference<T>{
		private final K key;
		
		private WeakNode(K key, T value, ReferenceQueue<T> queue){
			super(value, queue);
			this.key=key;
		}
		
		private K getKey(){
			return key;
		}
	}
	
	private final HashMap<K, WeakNode<V>> data;
	
	private final     ReferenceQueue<V> gcQueue=new ReferenceQueue<>();
	private transient Executor          stayAlive;
	
	private transient Keys     keys;
	private transient Values   values;
	private transient EntrySet entries;
	
	
	public WeakValueHashMap(int initialCapacity, float loadFactor){
		data=new HashMap<>(initialCapacity, loadFactor);
		
	}
	
	public WeakValueHashMap(int initialCapacity){
		this(initialCapacity, 0.75F);
	}
	
	public WeakValueHashMap(){
		this(16);
	}
	
	public WeakValueHashMap(Map<? extends K, ? extends V> m){
		this();
		m.forEach((k, v)->data.put(k, newNode(k, v)));
	}
	
	public <k extends K, v extends V> WeakValueHashMap(WeakValueHashMap<k, v> m){
		this((Map<k, v>)m);
	}
	
	
	public WeakValueHashMap<K, V> defineStayAlivePolicy(long seconds){
		return defineStayAlivePolicy(seconds, TimeUnit.SECONDS);
	}
	
	public WeakValueHashMap<K, V> defineStayAlivePolicy(long delay, TimeUnit unit){
		if(delay<0) throw new IllegalArgumentException("ms less than 0");
		boolean triggerMassHolding=stayAlive==null&&delay>0;
		
		ScheduledThreadPoolExecutor scheduler=new ScheduledThreadPoolExecutor(1);
		scheduler.setKeepAliveTime(10, TimeUnit.SECONDS);
		scheduler.allowCoreThreadTimeOut(true);
		
		if(delay>0) stayAlive=command->scheduler.schedule(command, delay, unit);
		else stayAlive=null;
		
		if(triggerMassHolding){
			for(V value : values()){
				holdTo(value);
			}
		}
		return this;
	}
	
	private WeakNode<V> newNode(K key, V value){
		return new WeakNode<>(key, value, gcQueue);
	}
	
	private void holdTo(V value){
		Executor stayAlive=this.stayAlive;
		if(stayAlive==null) return;
		
		stayAlive.execute(new Runnable(){
			V reference=value;//create reference, stop gc from yeeting it
			
			@Override
			public void run(){}
		});
	}
	
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public WeakValueHashMap<K, V> clone(){
		return new WeakValueHashMap<>(this);
	}
	
	@Override
	public int size(){
		processQueue();
		return data.size();
	}
	
	
	@Override
	public boolean isEmpty(){
		processQueue();
		return data.isEmpty();
	}
	
	@SuppressWarnings("unchecked")
	private void processQueue(){
		WeakNode<V> ref;
		while((ref=(WeakNode<V>)gcQueue.poll())!=null){
			remove(ref.getKey());
		}
	}
	
	
	@Override
	public boolean containsKey(Object key){
		processQueue();
		return data.containsKey(key);
	}
	
	@Override
	public boolean containsValue(Object value){
		processQueue();
		return data.containsValue(value);
	}
	
	@Override
	public V get(Object key){
		WeakNode<V> ref=data.get(key);
		if(ref==null) return null;
		V val=ref.get();
		if(val==null) processQueue();
		return val;
	}
	
	@Override
	public V put(K key, V value){
		if(value==null) return remove(key);
		holdTo(value);
		WeakNode<V> old=data.put(key, newNode(key, value));
		return old==null?null:old.get();
	}
	
	@Override
	public V remove(Object key){
		WeakNode<V> old=data.remove(key);
		return old==null?null:old.get();
	}
	
	@Override
	public void putAll(Map<? extends K, ? extends V> m){
		m.forEach(this::put);
	}
	
	@Override
	public void clear(){
		data.clear();
	}
	
	private class Values extends AbstractCollection<V>{
		
		@NotNull
		@Override
		public Iterator<V> iterator(){
			Iterator<WeakNode<V>> iter=data.values().iterator();
			return new Iterator<V>(){
				
				V next;
				
				@Override
				public boolean hasNext(){
					getNext();
					return next!=null;
				}
				
				private void getNext(){
					while(next==null&&iter.hasNext()){
						WeakNode<V> ref=iter.next();
						if(ref==null) continue;
						V val=ref.get();
						if(val==null){
							processQueue();
							continue;
						}
						next=val;
					}
				}
				
				@Override
				public V next(){
					getNext();
					V n=next;
					next=null;
					return n;
				}
			};
		}
		
		@Override
		public int size(){
			return data.size();
		}
	}
	
	private class Keys extends AbstractSet<K>{
		
		@NotNull
		@Override
		public Iterator<K> iterator(){
			Iterator<Entry<K, WeakNode<V>>> iter=data.entrySet().iterator();
			return new Iterator<K>(){
				
				K next;
				
				@Override
				public boolean hasNext(){
					getNext();
					return next!=null;
				}
				
				private void getNext(){
					while(next==null&&iter.hasNext()){
						Entry<K, WeakNode<V>> ref=iter.next();
						if(ref==null) continue;
						if(ref.getValue()==null){
							processQueue();
							continue;
						}
						next=ref.getKey();
					}
				}
				
				@Override
				public K next(){
					getNext();
					K n=next;
					next=null;
					return n;
				}
			};
		}
		
		@Override
		public int size(){
			return data.size();
		}
	}
	
	private class EntrySet extends AbstractSet<Map.Entry<K, V>>{
		
		@NotNull
		@Override
		public Iterator<Entry<K, V>> iterator(){
			Iterator<Entry<K, WeakNode<V>>> iter=data.entrySet().iterator();
			return new Iterator<Entry<K, V>>(){
				Entry<K, WeakNode<V>> next;
				
				@Override
				public boolean hasNext(){
					getNext();
					return next!=null;
				}
				
				private void getNext(){
					while(next==null&&iter.hasNext()){
						Entry<K, WeakNode<V>> n  =iter.next();
						WeakNode<V>           ref=n.getValue();
						if(ref==null||ref.get()==null){
							processQueue();
							continue;
						}
						next=n;
					}
				}
				
				@Override
				public Entry<K, V> next(){
					getNext();
					Entry<K, WeakNode<V>> n=next;
					next=null;
					
					return new Entry<K, V>(){
						@Override
						public K getKey(){
							return n.getKey();
						}
						
						@Override
						public V getValue(){
							return n.getValue().get();
						}
						
						@Override
						public V setValue(V value){
							holdTo(value);
							WeakNode<V> old=n.setValue(new WeakNode<>(getKey(), value, gcQueue));
							return old==null?null:old.get();
						}
					};
				}
			};
		}
		
		@Override
		public int size(){
			return data.size();
		}
	}
	
	
	@NotNull
	@Override
	public Set<K> keySet(){
		return keys==null?(keys=new Keys()):keys;
	}
	
	@NotNull
	@Override
	public Collection<V> values(){
		return values==null?(values=new Values()):values;
	}
	
	@NotNull
	@Override
	public Set<Entry<K, V>> entrySet(){
		return entries==null?(entries=new EntrySet()):entries;
	}
	
	@SuppressWarnings("ObjectInstantiationInEqualsHashCode")
	@Override
	public boolean equals(Object o){
		if(o==this)
			return true;
		
		if(!(o instanceof Map))
			return false;
		Map<?, ?> m=(Map<?, ?>)o;
		if(m.size()!=size())
			return false;
		
		try{
			for(Entry<K, V> e : entrySet()){
				K key  =e.getKey();
				V value=e.getValue();
				if(value==null){
					if(!(m.get(key)==null&&m.containsKey(key)))
						return false;
				}else{
					if(!value.equals(m.get(key)))
						return false;
				}
			}
		}catch(ClassCastException|NullPointerException unused){
			return false;
		}
		
		return true;
	}
	
	@SuppressWarnings("ObjectInstantiationInEqualsHashCode")
	@Override
	public int hashCode(){
		int h=0;
		for(Entry<K, V> entry : entrySet()){
			h+=entry.hashCode();
		}
		return h;
	}
}
