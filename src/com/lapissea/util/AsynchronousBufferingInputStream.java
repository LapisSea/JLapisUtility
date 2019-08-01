package com.lapissea.util;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;


public class AsynchronousBufferingInputStream extends InputStream{
	
	
	private static final double SLEEP_START                   =0.1;
	private static final int    DEFAULT_CHUNK_SIZE            =1024*10;
	private static final int    DEFAULT_MAX_MEMORY_CONSUMPTION=DEFAULT_CHUNK_SIZE*64;
	
	/**
	 * Use as compiler warning for pointless call
	 */
	@Deprecated
	public static AsynchronousBufferingInputStream makeAsync(AsynchronousBufferingInputStream stream){
		return makeAsync(stream, DEFAULT_CHUNK_SIZE, DEFAULT_MAX_MEMORY_CONSUMPTION);
	}
	
	/**
	 * Use as compiler warning for pointless call
	 */
	@Deprecated
	public static AsynchronousBufferingInputStream makeAsync(AsynchronousBufferingInputStream stream, int chunkSize, int maxMemoryConsumption){
		return stream;
	}
	
	public static AsynchronousBufferingInputStream makeAsync(InputStream stream){
		return makeAsync(stream, DEFAULT_CHUNK_SIZE, DEFAULT_MAX_MEMORY_CONSUMPTION);
	}
	
	public static AsynchronousBufferingInputStream makeAsync(InputStream stream, int chunkSize, int maxMemoryConsumption){
		if(stream==null) return null;
		if(stream instanceof AsynchronousBufferingInputStream) return (AsynchronousBufferingInputStream)stream;
		return new AsynchronousBufferingInputStream(stream, chunkSize, maxMemoryConsumption);
	}
	
	private static class Chunk{
		final byte[] data;
		int pos;
		
		private Chunk(byte[] data){this.data=data;}
		
		public int left(){
			return data.length-pos;
		}
		
		public boolean noData(){
			return data.length<=pos;
		}
		
		public int read(){
			return data[pos++]&0xFF;
		}
		
		public synchronized int read(byte[] b, int off, int len){
			
			if(pos >= data.length) return -1;
			
			int avail=data.length-pos;
			if(len>avail) len=avail;
			
			if(len<=0) return 0;
			
			System.arraycopy(data, pos, b, off, len);
			pos+=len;
			return len;
		}
		
		public int skip(int n){
			int toSkip=Math.min(n, left());
			pos+=toSkip;
			return toSkip;
		}
	}
	
	private static class Block{
		Deque<Chunk> chunks=new LinkedList<>();
		int          byteCount;
		
		private void pop(){
			Chunk removed=chunks.removeFirst();
			byteCount-=removed.data.length;
		}
		
		private void push(byte[] data){
			chunks.addLast(new Chunk(data));
			byteCount+=data.length;
		}
		
		private Chunk get(){
			return chunks.getFirst();
		}
		
		private int size(){
			return chunks.size();
		}
		
		public boolean isEmpty(){
			return chunks.isEmpty();
		}
		
		public int getByteCount(){
			return byteCount;
		}
	}
	
	private Block bufferActive=new Block();
	private Block bufferBack  =new Block();
	
	
	private boolean reading=true;
	private double  starveCounter;
	private double  overflowCounter;
	
	private AsynchronousBufferingInputStream(@NotNull InputStream in, int chunkSize, int maxMemoryConsumption){
		
		if(chunkSize<=0) throw new IllegalArgumentException("chunkSize("+chunkSize+") can not be equal or less to 0");
		if(maxMemoryConsumption<=0) throw new IllegalArgumentException("maxMemoryConsumption("+maxMemoryConsumption+") can not be equal or less to 0");
		if(maxMemoryConsumption%chunkSize!=0) throw new IllegalArgumentException("maxMemoryConsumption("+maxMemoryConsumption+") must be dividable chunkSize("+chunkSize+")");
		
		int chunkCountLimit=maxMemoryConsumption/chunkSize;
		new Thread(()->readSource(in, chunkSize, chunkCountLimit), "async file read").start();
	}
	
	private void readSource(@NotNull InputStream in, int chunkSize, int chunkCountLimit){
		try{
			
			while(reading){
				{
					double sleep=SLEEP_START;
					double t    =System.nanoTime();
					while(true){
						synchronized(swapLock){
							if(bufferActive.size()+bufferBack.size()<chunkCountLimit) break;
						}
						UtilL.sleep(sleep);
						sleep*=1.5;
						if(sleep>2) sleep=Math.sqrt(sleep);
					}
					if(sleep!=SLEEP_START){
						overflowCounter+=(System.nanoTime()-t)/UtilL.NS;
					}
				}
				
				byte[] bb  =new byte[chunkSize];
				int    read=in.read(bb);
				
				if(read<=0) break;
				
				if(read!=bb.length) bb=Arrays.copyOfRange(bb, 0, read);
				
				push(bb);
			}
		}catch(Throwable e){
			e.printStackTrace();
			UtilL.closeSilenty(in);
			reading=false;
		}finally{
			UtilL.closeSilenty(in);
			reading=false;
		}
	}
	
	private final Object swapLock=new Object();
	
	private void push(byte[] data){
		synchronized(swapLock){
			bufferBack.push(data);
		}
	}
	
	//no need to synchronize pop as flip and pop will always be on same thread
	private void pop(){
		bufferActive.pop();
	}
	
	private void flip(){
		synchronized(swapLock){
			Block q=bufferActive;
			bufferActive=bufferBack;
			bufferBack=q;
		}
	}
	
	@Nullable
	private Chunk getStream(){
		
		if(bufferActive.isEmpty()){
			if(!bufferBack.isEmpty()) flip();
			else{
				if(!reading) return null;
				
				{
					double sleep=SLEEP_START;
					double t    =System.nanoTime();
					while(bufferBack.isEmpty()){
						if(!reading) break;
						
						UtilL.sleep(sleep);
						sleep*=1.5F;
						if(sleep>2) sleep=Math.sqrt(sleep);
					}
					if(sleep!=SLEEP_START){
						starveCounter+=(System.nanoTime()-t)/UtilL.NS;
					}
				}
				
				if(bufferBack.isEmpty()) return null;
				
				flip();
			}
		}
		
		Chunk result=bufferActive.get();
		
		if(result.noData()){
			pop();
			
			if(!bufferActive.isEmpty()) return bufferActive.get();
			return getStream();
		}
		
		return result;
	}
	
	public double getStarveCounter(){
		return starveCounter;
	}
	
	public double getOverflowCounter(){
		return overflowCounter;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////// API-OVERRIDES ////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public synchronized int available(){
		return bufferActive.getByteCount()+bufferBack.getByteCount();
	}
	
	@Override
	public synchronized int read(){
		Chunk s=getStream();
		return s==null?-1:s.read();
	}
	
	@Override
	public synchronized int read(byte[] b, int off, int len){
		if(off+len>b.length) throw new RuntimeException();
		if(len==0) return 0;
		
		Chunk s   =getStream();
		int   read=0;
		
		while(s!=null&&len>0){
			int r=s.read(b, off, len);
			
			read+=r;
			off+=r;
			len-=r;
			
			if(s.noData()) s=getStream();
		}
		return read==0?-1:read;
	}
	
	@Override
	public synchronized void close(){
		reading=false;
	}
	
	@Override
	public synchronized long skip(long n){
		
		long skipped=0;
		while(skipped<n){
			Chunk s=getStream();
			if(s==null) break;
			skipped+=s.skip((int)(n-skipped));
		}
		
		return skipped;
	}
}
