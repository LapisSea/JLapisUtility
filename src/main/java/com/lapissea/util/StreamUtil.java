package com.lapissea.util;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil{
	
	public static <Out, Arg1, Arg2> Stream<Out> join1X(Arg2[] in, Arg1 arg1, BiFunction<Arg1, Arg2, Out> mapper){
		return join1X(Arrays.stream(in), arg1, mapper);
	}
	
	public static <Out, Arg1, Arg2> Stream<Out> join1X(Stream<Arg2> in, Arg1 arg1, BiFunction<Arg1, Arg2, Out> mapper){
		return in.map(arg2 -> mapper.apply(arg1, arg2));
	}
	
	public static <Out, Arg1, In> Stream<Out> join1(In[] in, Arg1 arg1, BiFunction<In, Arg1, Out> mapper){
		return join1(Arrays.stream(in), arg1, mapper);
	}
	public static <Out, Arg1, In> Stream<Out> join1(Stream<In> in, Arg1 arg1, BiFunction<In, Arg1, Out> mapper){
		return in.map(in1 -> mapper.apply(in1, arg1));
	}
	
	public static <T> Stream<T> stream(@NotNull Enumeration<T> e){
		return StreamSupport.stream(
			new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED){
				
				@Override
				public boolean tryAdvance(@NotNull Consumer<? super T> action){
					if(e.hasMoreElements()){
						action.accept(e.nextElement());
						return true;
					}
					return false;
				}
				
				@Override
				public void forEachRemaining(@NotNull Consumer<? super T> action){
					while(e.hasMoreElements()){
						action.accept(e.nextElement());
					}
				}
			}, false);
	}
	
	public static <T> Stream<T> stream(@NotNull Iterable<T> it){
		return stream(it, false);
	}
	
	public static <T> Stream<T> stream(@NotNull Iterable<T> it, boolean parallel){
		return StreamSupport.stream(it.spliterator(), false);
	}
	
	public static <T> Stream<T> stream(@NotNull Iterator<T> it){
		return stream(it, false);
	}
	
	public static <T> Stream<T> stream(@NotNull Iterator<T> it, boolean parallel){
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.NONNULL), parallel);
	}
	
}
