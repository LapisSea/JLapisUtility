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
	
	private static class RefHolder implements Runnable{
		@SuppressWarnings("FieldCanBeLocal")
		private final Object reference;
		public RefHolder(Object reference){
			this.reference=reference;
		}
		@Override
		public void run(){}
	}
	
	@Nullable
	private HashMap<K, DeletingValueEntry<K, V>> data;
	
	private           ReferenceQueue<V> gcQueue;
	private transient Executor          stayAlive;
	
	private transient Keys     keys;
	private transient Values   values;
	private transient EntrySet entries;
	
	private transient long policy=-1;
	
	public DeletingValueHashMap(){}
	
	public DeletingValueHashMap(Map<? extends K, ? extends V> m){
		if(!m.isEmpty()){
			data=new HashMap<>(m.size());
			m.forEach((k, v)->data.put(k, newNode(k, v)));
		}
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
		
		ScheduledThreadPoolExecutor scheduler=new ScheduledThreadPoolExecutor(1, r->{
			Thread t=new Thread(r, "value-keepalive");
			t.setDaemon(true);
			return t;
		});
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
		if(gcQueue==null) gcQueue=new ReferenceQueue<>();
		return newNode(key, value, gcQueue);
	}
	
	protected abstract DeletingValueEntry<K, V> newNode(K key, V value, ReferenceQueue<V> gcQueue);
	
	private void holdTo(V value){
		Executor stayAlive=this.stayAlive;
		if(stayAlive==null) return;
		
		stayAlive.execute(new RefHolder(value));
	}
	
	public abstract SELF copy();
	
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public SELF clone(){
		return copy();
	}
	
	@Override
	public int size(){
		if(data==null) return 0;
		return data.size();
	}
	
	
	@Override
	public boolean isEmpty(){
		return size()==0;
	}
	
	@SuppressWarnings("unchecked")
	private void processQueue(){
		if(gcQueue==null) return;
		
		DeletingValueEntry<K, V> ref;
		while((ref=(DeletingValueEntry<K, V>)gcQueue.poll())!=null){
			data.remove(ref.getKey());
		}
	}
	
	
	@Override
	public boolean containsKey(Object key){
		if(data==null) return false;
		DeletingValueEntry<K, V> e=data.get((K)key);
		return e!=null&&e.get()!=null;
	}
	
	@Override
	public boolean containsValue(Object value){
		if(data==null||value==null) return false;
		for(DeletingValueEntry<K, V> e : data.values()){
			V v=e.get();
			if(v==null) continue;
			if(v.equals(value)) return true;
		}
		return false;
	}
	
	@Override
	public V get(Object key){
		if(data==null) return null;
		DeletingValueEntry<K, V> ref=data.get(key);
		if(ref==null) return null;
		return ref.get();
	}
	
	
	@Override
	public V put(K key, V value){
		if(value==null) return remove(key);
		if(data==null) data=new HashMap<>();
		else processQueue();
		holdTo(value);
		DeletingValueEntry<K, V> old=data.put(key, newNode(key, value));
		return old==null?null:old.get();
	}
	
	@Override
	public V remove(Object key){
		if(data==null) return null;
		processQueue();
		DeletingValueEntry<K, V> old=data.remove(key);
		return old==null?null:old.get();
	}
	
	@Override
	public void clear(){
		processQueue();
		if(data!=null) data.clear();
	}
	
	private class Values extends AbstractCollection<V>{
		
		@NotNull
		@Override
		public Iterator<V> iterator(){
			if(data==null) return Collections.emptyIterator();
			
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
					if(n==null) throw new NoSuchElementException();
					next=null;
					return n;
				}
			};
		}
		
		@Override
		public int size(){
			return DeletingValueHashMap.this.size();
		}
	}
	
	private class Keys extends AbstractSet<K>{
		
		@NotNull
		@Override
		public Iterator<K> iterator(){
			if(data==null) return Collections.emptyIterator();
			
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
						DeletingValueEntry<K, V> entry=ref.getValue();
						if(entry==null||entry.get()!=null){
							continue;
						}
						next=ref.getKey();
					}
				}
				
				@Override
				public K next(){
					getNext();
					K n=next;
					if(n==null) throw new NoSuchElementException();
					next=null;
					return n;
				}
			};
		}
		
		@Override
		public int size(){
			return DeletingValueHashMap.this.size();
		}
	}
	
	private class EntrySet extends AbstractSet<Entry<K, V>>{
		
		@NotNull
		@Override
		public Iterator<Entry<K, V>> iterator(){
			if(data==null) return Collections.emptyIterator();
			
			Iterator<Entry<K, DeletingValueEntry<K, V>>> iter=data.entrySet().iterator();
			return new Iterator<Entry<K, V>>(){
				Entry<K, V> next;
				
				@Override
				public boolean hasNext(){
					getNext();
					return next!=null;
				}
				
				private void getNext(){
					while(next==null&&iter.hasNext()){
						Entry<K, DeletingValueEntry<K, V>> n  =iter.next();
						DeletingValueEntry<K, V>           ref=n.getValue();
						
						if(ref==null){
							continue;
						}
						V val=ref.get();
						if(val==null){
							continue;
						}
						
						next=new Entry<K, V>(){
							@Override
							public K getKey(){
								return n.getKey();
							}
							
							@Override
							public V getValue(){
								return val;
							}
							
							@Override
							public V setValue(V value){
								holdTo(value);
								DeletingValueEntry<K, V> old=n.setValue(newNode(getKey(), value));
								return old==null?null:old.get();
							}
						};
					}
				}
				
				@Override
				public Entry<K, V> next(){
					getNext();
					Entry<K, V> n=next;
					if(n==null) throw new NoSuchElementException();
					next=null;
					
					return n;
				}
			};
		}
		
		@Override
		public int size(){
			return DeletingValueHashMap.this.size();
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
