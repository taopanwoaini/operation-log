package com.csp.operationlog.service;

/**
 * 操作日志服务
 * @author taoken
 */
public interface OperationLogService {
	
	/**
	 * 添加操作日志记录
	 * @param professionalName  业务名
	 * @param logTableName      表名
	 * @param opratorId         操作人id
	 * @param operatorName      操作人名
	 * @param addedBean         添加的bean
	 */
	public void logForAdd(String professionalName,String logTableName,String opratorId,String operatorName,Object addedBean);
	
	/**
	 * 删除操作日志记录
	 * @param professionalName 业务名
	 * @param logTableName     表名
	 * @param primaryKeyId     主键id
	 * @param opratorId        操作人id
	 * @param operatorName     操作人名称
	 * @param beforDeledBean   删除前的bean
	 */
	public void logForDel(String professionalName,String logTableName,String primaryKeyId,String opratorId,String operatorName,Object beforDeledBean);
	
	/**
	 * 更新操作日志记录
	 * @param professionalName  业务名
	 * @param logTableName      表名
	 * @param primaryKeyId      主键id
	 * @param opratorId         操作人id
	 * @param operatorName      操作人名称
	 * @param beforUpedBean     更新前的bean
	 * @param afterUpedBean     更新好的bean
	 */
	public void logForUpd(String professionalName,String logTableName,String primaryKeyId,String opratorId,String operatorName,Object beforUpedBean,Object afterUpedBean);

}
