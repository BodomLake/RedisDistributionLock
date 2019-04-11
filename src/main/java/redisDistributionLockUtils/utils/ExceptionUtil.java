package redisDistributionLockUtils.utils;

import redisDistributionLockUtils.exception.CustomRedisException;

public class ExceptionUtil {

	public static CustomRedisException geneException(CommonExceptionEnum enumeration){
		return new CustomRedisException(enumeration.toString());
	}

}
