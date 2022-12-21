package test;

import com.lapissea.util.ConsoleColors;
import com.lapissea.util.LogUtil;
import com.lapissea.util.UtilL;

public class Test{
	
	public static void main(String[] args){
		LogUtil.Init.attach(LogUtil.Init.USE_TIME_DELTA|LogUtil.Init.USE_TABULATED_HEADER|LogUtil.Init.USE_CALL_THREAD);
		LogUtil.println("hi. wierd letters: ˘°€|~^˘°˛`˙´đđšćž¨|\uD83D\uDE0A");
		LogUtil.println("wooo");
		LogUtil.println("hi "+ConsoleColors.BLUE+"wooo"+ConsoleColors.RESET);
		LogUtil.println("wooo");
		LogUtil.println("wooo");
		LogUtil.println("wooo");
		LogUtil.println("wooo");
		UtilL.sleep(0.5);
		LogUtil.println("wooo");
		UtilL.sleep(0.5);
		LogUtil.println("wooo");
		UtilL.sleep(0.5);
		LogUtil.println("sleep 0.5");
		UtilL.sleep(2);
		LogUtil.println("sleep 2");
		UtilL.sleep(1001);
		LogUtil.println("sleep 1001");
		
		for(int i = 0; i<10; i++){
			UtilL.sleep(10);
			LogUtil.println("sleep 10");
		}
		
		LogUtil.println("Multiline\nstring\nok enough");
	}
	
}
