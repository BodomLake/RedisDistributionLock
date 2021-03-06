package redisDistributionLockUtils.lock;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.concurrent.TimeUnit;


public class RedisDistributionLock {

	//设置时间
	private static final Logger logger = LoggerFactory.getLogger(RedisDistributionLock.class);

	//key的TTL,一天
	private static final int finalDefaultTTLwithKey = 24 * 3600;

	//锁默认超时时间,20秒
	private static final long defaultExpireTime = 20 * 1000;

	private static final boolean Success = true;

	@Resource( name = "redisTemplate")
	private RedisTemplate<String, String> redisTemplateForGeneralize;

	/**
	 * 加锁,锁默认超时时间20秒
	 * @param resource
	 * @return
	 */
	public boolean lock(String resource) {
		return this.lock(resource, defaultExpireTime);
	}

	/**
	 * 加锁,同时设置锁超时时间
	 * @param key 分布式锁的key
	 * @param expireTime 单位是ms
	 * @return
	 */

	public boolean lock(String key, long expireTime) {

		logger.debug("redis lock debug, start. key:[{}], expireTime:[{}]",key,expireTime);
		long now = Instant.now().toEpochMilli();
		long lockExpireTime = now + expireTime;

		//setnx
		boolean executeResult = redisTemplateForGeneralize.opsForValue().setIfAbsent(key,String.valueOf(lockExpireTime));
		logger.debug("redis lock debug, setnx. key:[{}], expireTime:[{}], executeResult:[{}]", key, expireTime,executeResult);

		//取锁成功,为key设置expire
		if (executeResult == Success) {
			redisTemplateForGeneralize.expire(key,finalDefaultTTLwithKey, TimeUnit.SECONDS);
			return true;
		}
		//没有取到锁,继续流程
		else{
			Object valueFromRedis = this.getKeyWithRetry(key, 3);
			// 避免获取锁失败,同时对方释放锁后,造成NPE
			if (valueFromRedis != null) {
				//已存在的锁超时时间
				long oldExpireTime = Long.parseLong((String)valueFromRedis);
				logger.debug("redis lock debug, key already seted. key:[{}], oldExpireTime:[{}]",key,oldExpireTime);

				//当前时间 大于 锁过期时间,锁已经超时,重新取锁
				if (now >= oldExpireTime) {

					logger.debug("redis lock debug, lock time expired. key:[{}], oldExpireTime:[{}], now:[{}]", key, oldExpireTime, now);
					String valueFromRedis2 = redisTemplateForGeneralize.opsForValue().getAndSet(key, String.valueOf(lockExpireTime));
					long currentExpireTime = Long.parseLong(valueFromRedis2);

					//判断currentExpireTime与oldExpireTime是否相等
					if(currentExpireTime == oldExpireTime){
						//相等,则取锁成功
						logger.debug("redis lock debug, getSet. key:[{}], currentExpireTime:[{}], oldExpireTime:[{}], lockExpireTime:[{}]", key, currentExpireTime, oldExpireTime, lockExpireTime);
						redisTemplateForGeneralize.expire(key, finalDefaultTTLwithKey, TimeUnit.SECONDS);
						return true;
					}else{
						//不相等,取锁失败
						return false;
					}
				}
			}
			else {
				//锁已经被释放掉了
				logger.warn("redis lock,lock have been release. key:[{}]", key);
				return false;
			}
		}
		return false;
	}

	//重试获取Key的value
	private Object getKeyWithRetry(String key, int retryTimes) {
		int failTime = 0;
		while (failTime < retryTimes) {
			try {
				return redisTemplateForGeneralize.opsForValue().get(key);
			} catch (Exception e) {
				failTime++;
				if (failTime >= retryTimes) {
					throw new RuntimeException(e.getMessage());
				}
			}
		}
		return null;
	}

	/**
	 * 解锁
	 * @param key
	 * @return
	 */
	public boolean unlock(String key) {
		logger.debug("redis unlock debug, start. resource:[{}].",key);
		redisTemplateForGeneralize.delete(key);
		return Success;
	}
}
