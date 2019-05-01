package com.lapissea.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class AsynchronousBufferingInputStream extends InputStream{
	
	private static final int MAX_CHUNK         =2<<4;
	private static final int DEFAULT_CHUNK_SIZE=8192;
	
	private final LinkedList<ByteArrayInputStream> chunks  =new LinkedList<>();
	private       boolean                          doneReading;
	private final int                              chunkSize;
	private       int                              maxChunk=MAX_CHUNK;
	
	public AsynchronousBufferingInputStream(@NotNull InputStream in){
		this(in, DEFAULT_CHUNK_SIZE);
	}
	
	public AsynchronousBufferingInputStream(@NotNull InputStream in, int chunkSize){
		
		this.chunkSize=chunkSize;
		
		new Thread(()->{
			try{
				byte[] reader=new byte[chunkSize];
				while(!doneReading){
					
					if(isOverflow()){
						while(!doneReading&&isOverflow()){
							UtilL.sleep(0, 1000000/4);
						}
						LogUtil.Init.OUT.println(chunks.size()+" "+maxChunk);
						//buffer starved, need to buffer more
						if(chunks.isEmpty()){
							maxChunk<<=1;
						}
					}
					
					int l=in.read(reader);
					if(l<=0) break;
					
					byte[] chunk=new byte[l];
					System.arraycopy(reader, 0, chunk, 0, chunk.length);
					
					synchronized(chunks){
						if(doneReading) return;
						chunks.add(new ByteArrayInputStream(chunk));
					}
				}
				doneReading=true;
			}catch(IOException e){
				throw UtilL.uncheckedThrow(e);
			}
		}, "async file read").start();
	}
	
	private boolean isOverflow(){
		return chunks.size()>maxChunk;
	}
	
	private void sleep(){
		if(chunks.isEmpty()){
			if(doneReading){
				return;
			}
			UtilL.sleepWhile(()->{
				if(doneReading) return false;
				return chunks.isEmpty();
			});
		}
	}
	
	private ByteArrayInputStream getStream(){
		sleep();
		if(chunks.isEmpty()){
			return null;
		}
		ByteArrayInputStream stream=chunks.getFirst();
		
		if(stream.available()<=0){
			if(chunks.size()>2){
				synchronized(chunks){
					chunks.removeFirst();
				}
			}else chunks.removeFirst();
			
			return getStream();
		}
		return stream;
	}
	
	@Override
	public int available(){
		ByteArrayInputStream s=getStream();
		if(s==null){
			return 0;
		}
		
		return (chunks.size()-1)*chunkSize+s.available();
	}
	
	@Override
	public int read() throws IOException{
		InputStream s=getStream();
		return s==null?-1:s.read();
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException{
		if(off+len>b.length) throw new RuntimeException();
		if(len==0){
			return 0;
		}
		
		InputStream s=getStream();
		if(s==null){
			return 0;
		}
		
		int read=0;
		
		while(true){
			int toRead=Math.min(s.available(), len);
			if(toRead==0) break;
			
			s.read(b, off, toRead);
			
			read+=toRead;
			off+=toRead;
			len-=toRead;
			
			
			if(s.available()<=0){
				s=getStream();
				if(s==null) return len>0?-1:read;
			}
		}
		
		return len>0?-1:read;
	}
	
	@Override
	public void close(){
		doneReading=true;
		
		synchronized(chunks){
			chunks.clear();
		}
	}
	
	@Override
	public long skip(long n) throws IOException{
		InputStream s=getStream();
		if(s==null){
			return 0;
		}
		
		long remaining=n;
		while(remaining>0){
			remaining-=s.skip(n);
			if(remaining>0){
				s=getStream();
				if(s==null) break;
			}
		}
		return n-remaining;
	}
}
