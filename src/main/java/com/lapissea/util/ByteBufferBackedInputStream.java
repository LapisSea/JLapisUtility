package com.lapissea.util;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferBackedInputStream extends InputStream{
	
	private final ByteBuffer buf;
	private final int        pos;
	
	public ByteBufferBackedInputStream(ByteBuffer buf){
		this.buf = buf;
		pos = buf.position();
	}
	
	@Override
	public int read(){
		if(!buf.hasRemaining()){
			return -1;
		}
		return buf.get()&0xFF;
	}
	
	@Override
	public int read(@NotNull byte[] bytes, int off, int len){
		if(!buf.hasRemaining()){
			return -1;
		}
		
		len = Math.min(len, buf.remaining());
		buf.get(bytes, off, len);
		return len;
	}
	
	@Override
	public void close(){
		buf.position(pos);
	}
}
