package com.csp.operationlog.model;

/**
 * 操作日志详情模型
 * @author taoken
 */
public class OperationLogDetail {
	/** 主键id */
	private String id;
	/** 操作日志id */
	private String operationLogId;
	/** 字段名 */
	private String clmName;
	/** 字段描述 */
	private String clmComment;
	/** 旧值 */
	private String oldString;
	/** 新值 */
	private String newString;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOperationLogId() {
		return operationLogId;
	}
	public void setOperationLogId(String operationLogId) {
		this.operationLogId = operationLogId;
	}
	public String getClmName() {
		return clmName;
	}
	public void setClmName(String clmName) {
		this.clmName = clmName;
	}
	public String getClmComment() {
		return clmComment;
	}
	public void setClmComment(String clmComment) {
		this.clmComment = clmComment;
	}
	public String getOldString() {
		return oldString;
	}
	public void setOldString(String oldString) {
		this.oldString = oldString;
	}
	public String getNewString() {
		return newString;
	}
	public void setNewString(String newString) {
		this.newString = newString;
	}
}
