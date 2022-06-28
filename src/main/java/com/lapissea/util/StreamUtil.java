package com.lapissea.util;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class StreamUtil{
	
	public static <Out, Arg1, Arg2> Stream<Out> join1X(Arg2[] in, Arg1 arg1, BiFunction<Arg1, Arg2, Out> mapper){
		return join1X(Arrays.stream(in), arg1, mapper);
	}
	
	public static <Out, Arg1, Arg2> Stream<Out> join1X(Stream<Arg2> in, Arg1 arg1, BiFunction<Arg1, Arg2, Out> mapper){
		return in.map(arg2->mapper.apply(arg1, arg2));
	}
	
	public static <Out, Arg1, In> Stream<Out> join1(In[] in, Arg1 arg1, BiFunction<In, Arg1, Out> mapper){
		return join1(Arrays.stream(in), arg1, mapper);
	}
	public static <Out, Arg1, In> Stream<Out> join1(Stream<In> in, Arg1 arg1, BiFunction<In, Arg1, Out> mapper){
		return in.map(in1->mapper.apply(in1, arg1));
	}
	
}
