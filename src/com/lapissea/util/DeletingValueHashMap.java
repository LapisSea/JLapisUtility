package com.lapissea.util;

import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public abstract class DeletingValueHashMap<K, V, SELF extends DeletingValueHashMap<K, V, SELF>> extends AbstractMap<K, V>{
	
	public interface DeletingValueEntry<K, T>{
		K getKey();
		
		T get();
	}
	
	private final HashMap<K, DeletingValueEntry<K, V>> data;
	
	private final     ReferenceQueue<V> gcQueue=new ReferenceQueue<>();
	private transient Executor          stayAlive;
	
	private transient Keys     keys;
	private transient Values   values;
	private transient EntrySet entries;
	
	private transient long policy=-1;
	
	
	public DeletingValueHashMap(int initialCapacity, float loadFactor){
		data=new HashMap<>(initialCapacity, loadFactor);
		
	}
	
	public DeletingValueHashMap(int initialCapacity){
		this(initialCapacity, 0.75F);
	}
	
	public DeletingValueHashMap(){
		this(16);
	}
	
	public DeletingValueHashMap(Map<? extends K, ? extends V> m){
		this();
		m.forEach((k, v)->data.put(k, newNode(k, v)));
	}
	
	public SELF defineStayAlivePolicy(long seconds){
		return defineStayAlivePolicy(seconds, TimeUnit.SECONDS);
	}
	
	public SELF defineStayAlivePolicy(long delay, TimeUnit unit){
		if(delay<0) throw new IllegalArgumentException("ms less than 0");
		
		long newPolicy=unit.toNanos(delay);
		if(newPolicy==policy) return (SELF)this;
		policy=newPolicy;
		
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
		
		return (SELF)this;
	}
	
	private DeletingValueEntry<K, V> newNode(K key, V value){
		return newNode(key, value, gcQueue);
	}
	
	protected abstract DeletingValueEntry<K, V> newNode(K key, V value, ReferenceQueue<V> gcQueue);
	
	private void holdTo(V value){
		Executor stayAlive=this.stayAlive;
		if(stayAlive==null) return;
		
		stayAlive.execute(new Runnable(){
			V reference=value;//create reference, stop gc from yeeting it
			
			@Override
			public void run(){}
		});
	}
	
	public abstract SELF copy();
	
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public SELF clone(){
		return copy();
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
		DeletingValueEntry<K, V> ref;
		while((ref=(DeletingValueEntry<K, V>)gcQueue.poll())!=null){
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
		if(value==null) return false;
		for(DeletingValueEntry<K, V> e : data.values()){
			V v=e.get();
			if(v==null) continue;
			if(v.equals(value)) return true;
		}
		return false;
	}
	
	@Override
	public V get(Object key){
		DeletingValueEntry<K, V> ref=data.get(key);
		if(ref==null) return null;
		return ref.get();
	}
	
	
	@Override
	public V put(K key, V value){
		if(value==null) return remove(key);
		processQueue();
		holdTo(value);
		DeletingValueEntry<K, V> old=data.put(key, newNode(key, value));
		return old==null?null:old.get();
	}
	
	@Override
	public V remove(Object key){
		processQueue();
		DeletingValueEntry<K, V> old=data.remove(key);
		return old==null?null:old.get();
	}
	
	@Override
	public void clear(){
		processQueue();
		data.clear();
	}
	
	private class Values extends AbstractCollection<V>{
		
		@NotNull
		@Override
		public Iterator<V> iterator(){
			Iterator<DeletingValueEntry<K, V>> iter=data.values().iterator();
			return new Iterator<V>(){
				
				V next;
				
				@Override
				public boolean hasNext(){
					getNext();
					return next!=null;
				}
				
				private void getNext(){
					while(next==null&&iter.hasNext()){
						DeletingValueEntry<K, V> ref=iter.next();
						if(ref==null) continue;
						V val=ref.get();
						if(val==null){
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
			Iterator<Entry<K, DeletingValueEntry<K, V>>> iter=data.entrySet().iterator();
			return new Iterator<K>(){
				
				K next;
				
				@Override
				public boolean hasNext(){
					getNext();
					return next!=null;
				}
				
				private void getNext(){
					while(next==null&&iter.hasNext()){
						Entry<K, DeletingValueEntry<K, V>> ref=iter.next();
						if(ref==null) continue;
						if(ref.getValue()==null){
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
	
	private class EntrySet extends AbstractSet<Entry<K, V>>{
		
		@NotNull
		@Override
		public Iterator<Entry<K, V>> iterator(){
			Iterator<Entry<K, DeletingValueEntry<K, V>>> iter=data.entrySet().iterator();
			return new Iterator<Entry<K, V>>(){
				Entry<K, DeletingValueEntry<K, V>> next;
				
				@Override
				public boolean hasNext(){
					getNext();
					return next!=null;
				}
				
				private void getNext(){
					while(next==null&&iter.hasNext()){
						Entry<K, DeletingValueEntry<K, V>> n  =iter.next();
						DeletingValueEntry<K, V>           ref=n.getValue();
						if(ref==null||ref.get()==null){
							iter.remove();
							continue;
						}
						next=n;
					}
				}
				
				@Override
				public Entry<K, V> next(){
					getNext();
					Entry<K, DeletingValueEntry<K, V>> n=next;
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
							DeletingValueEntry<K, V> old=n.setValue(newNode(getKey(), value));
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
	
}
