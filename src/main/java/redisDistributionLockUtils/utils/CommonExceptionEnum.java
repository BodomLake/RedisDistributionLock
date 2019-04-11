package redisDistributionLockUtils.utils;



public enum CommonExceptionEnum {

	SYSTEM_ERROR("System encounter a runtime error");

	public static String ERROR;

	private CommonExceptionEnum(String error){

	}

	public void setError(String ERROR){
		this.ERROR=ERROR;
	}
	public String getERROR(){
		return this.ERROR;
	}

}
