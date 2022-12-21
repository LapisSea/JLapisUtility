package com.lapissea.util;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static com.lapissea.util.UtilL.uncheckedThrow;

public class NativeUtils{
	
	public static void loadLibrary(@NotNull File libPath){
		loadLibrary(libPath, "");
	}
	
	public static void loadLibrary(@NotNull File libPath, @NotNull String inJarFolder){
		String path = fileToPath(libPath);
		loadLibrary0(path, () -> NativeUtils.class.getClassLoader().getResourceAsStream((inJarFolder.isEmpty()? "" : inJarFolder + "/") + new File(path).getName()));
	}
	
	public static void loadLibrary(@NotNull File libPath, @NotNull Supplier<InputStream> source){
		loadLibrary0(fileToPath(libPath), source);
	}
	
	private static void loadLibrary0(String path, Supplier<InputStream> source){
		try{
			System.load(path);
		}catch(Throwable e){
			try{
				new File(path).getParentFile().mkdirs();
				try(InputStream in = source.get()){
					if(in == null) throw new RuntimeException("Missing native lib");
					Files.copy(in, Paths.get(path));
				}
				System.load(path);
			}catch(Throwable e1){
				throw uncheckedThrow(e.initCause(e1));
			}
		}
	}
	
	private static String fileToPath(File file){
		return file.getAbsolutePath() + "." + UtilL.getOS().nativeLibExtension;
	}
}
