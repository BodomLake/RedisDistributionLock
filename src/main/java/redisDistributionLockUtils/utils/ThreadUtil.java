package redisDistributionLockUtils.utils;

public class ThreadUtil {

	public static void holdXms(int seconds){
		try {
			Thread.currentThread().wait((long) seconds);
		}catch (InterruptedException inter){
			inter.printStackTrace();
		}
	}

}
