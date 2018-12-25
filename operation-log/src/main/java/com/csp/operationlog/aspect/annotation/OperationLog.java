package com.csp.operationlog.aspect.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.csp.operationlog.aspect.enums.OperationType;

/**
 * 用来标注需要进行操作日志的服务函数上
 * @author taoken
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
	/** 业务名 */
	String name();
	/** 表名 */
	String table();
	/** id 在函数的字段名 */
	int idRef() default -1; 
	/** 需要记录的字段 */
	String[] cloum() default {};
	/** 操作类型 */
	OperationType type();
	/** 操作人 id 在函数的字段名*/
	int operatorIdRef();
	/** 操作人名称 在函数的字段名 */
	int operatorNameRef();
}
