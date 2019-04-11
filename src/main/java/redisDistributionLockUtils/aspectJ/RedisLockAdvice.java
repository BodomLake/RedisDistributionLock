package redisDistributionLockUtils.aspectJ;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import redisDistributionLockUtils.lock.RedisDistributionLock;
import redisDistributionLockUtils.lock.RedisLockAnnoation;
import redisDistributionLockUtils.utils.*;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Method;

@Component("redisLockAdvice")
@Aspect
public class RedisLockAdvice {

	private static final Logger logger = LoggerFactory.getLogger(RedisLockAdvice.class);

	@Named("")

	@Resource
	private RedisDistributionLock redisDistributionLock;

	@Around("@annotation(redisDistributionLockUtils.lock.RedisLockAnnoation)")
	public Object processAround(ProceedingJoinPoint pjp) throws Throwable {

		//获取 被切入点(是我自定义的RedisLockAnnoation) 的对象实例,通过反射抽取出想要的参数
		String methodName = pjp.getSignature().getName();
		Class<?> classTarget = pjp.getTarget().getClass();
		Class<?>[] par = ((MethodSignature) pjp.getSignature()).getParameterTypes();
		Method objMethod = classTarget.getMethod(methodName, par);
		RedisLockAnnoation redisLockAnnoation = objMethod.getDeclaredAnnotation(RedisLockAnnoation.class);

		//拼装分布式锁的key
		String[] keys = redisLockAnnoation.keys();
		Object[] args = pjp.getArgs();
		Object arg = args[0];
		StringBuilder temp = new StringBuilder();
		temp.append(redisLockAnnoation.keyPrefix());

		//遍历,Method类抽取出
		for (String key : keys) {
			String getMethod = "get" + StringUtils.capitalize(key);
			temp.append(MethodUtils.invokeExactMethod(pjp,getMethod,args)).append("_");
		}

		//redisKey就是锁在redis中的名字(可以这么说)
		String redisKey = StringUtils.removeEnd(temp.toString(), "_");

		//执行分布式锁的逻辑
		if (redisLockAnnoation.isSpin()) {
			//阻塞锁
			int lockRetryTime = 0;
			try {
				while (!redisDistributionLock.lock(redisKey, redisLockAnnoation.expireTime())) {
					if (lockRetryTime++ > redisLockAnnoation.retryTimes()) {
						logger.error("lock exception. key:{}, lockRetryTime:{}", redisKey, lockRetryTime);
						throw ExceptionUtil.geneException(CommonExceptionEnum.SYSTEM_ERROR);
					}
					ThreadUtil.holdXms(redisLockAnnoation.waitTime());
				}
				return pjp.proceed();
			} finally {
				redisDistributionLock.unlock(redisKey);
			}
		} else {
			//非阻塞锁
			try {
				if (!redisDistributionLock.lock(redisKey)) {
					logger.error("lock exception. key:{}", redisKey);
					throw ExceptionUtil.geneException(CommonExceptionEnum.SYSTEM_ERROR);
				}
				return pjp.proceed();
			} finally {
				redisDistributionLock.unlock(redisKey);
			}
		}
	}
}
