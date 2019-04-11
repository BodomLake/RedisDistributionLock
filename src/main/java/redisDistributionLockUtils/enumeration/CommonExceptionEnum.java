package redisDistributionLockUtils.enumeration;

public enum CommonExceptionEnum {

	SYSTEM_ERROR("The System part of Redis carshed with a error!");

	private final String name;

	private CommonExceptionEnum(String str){
		this.name = str;
	}



}
