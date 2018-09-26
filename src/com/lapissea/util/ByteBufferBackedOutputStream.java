package com.lapissea.util;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferBackedOutputStream extends OutputStream{
	private final ByteBuffer buf;
	
	public ByteBufferBackedOutputStream(ByteBuffer buf){
		this.buf=buf;
	}
	
	@Override
	public void write(int b){
		buf.put((byte)b);
	}
	
	@Override
	public void write(@NotNull byte[] bytes, int off, int len){
		buf.put(bytes, off, len);
	}
	
}
