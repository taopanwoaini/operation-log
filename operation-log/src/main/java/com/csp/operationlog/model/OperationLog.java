package com.csp.operationlog.model;

import java.sql.Timestamp;
/**
 * 操作日志主信息模型
 * @author taoken
 */
public class OperationLog {
	public static void main(String[] args) {
		String a = null;
		System.out.println(a);
	}
	/** 主键id */
	private String id;
	/** 操作业务名 */
	private String name;
	/** 操作表名 */
	private String tableName;
	/** 操作表id */
	private String tableId;
	/** 操作类型,(添加ADD,删除DELETE,修改UPDATE)' */
	private String type;
	/** 操作人id */
	private String operatorId;
	/** 操作人名 */
	private String operatorName;
	/** 操作时间 */
	private Timestamp operationTime;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getTableId() {
		return tableId;
	}
	public void setTableId(String tableId) {
		this.tableId = tableId;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getOperatorId() {
		return operatorId;
	}
	public void setOperatorId(String operatorId) {
		this.operatorId = operatorId;
	}
	public String getOperatorName() {
		return operatorName;
	}
	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}
	public Timestamp getOperationTime() {
		return operationTime;
	}
	public void setOperationTime(Timestamp operationTime) {
		this.operationTime = operationTime;
	}
	
}
