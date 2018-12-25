## 如何实现操作操作日志记录
### 为什么要记录操作日志?
```html
项目中的业务需求,需要针对用户的一些业务操作做操作记录,
也就是标题中的操场日志记录,最近做的项目也有这个需求,
我也是第一次写,相信有很多开发者也有遇到这个需求的,所以
在这里做一个简单的记录,只是提供一个思路参考,代码什么的
其实是次要的!
```
### 业务需求如下,记录用户的重要操作,记录除查询外,如增加,修改,和删除等操作
![在这里插入图片描述](https://img-blog.csdnimg.cn/20181225192909437.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3RfamluZGFv,size_16,color_FFFFFF,t_70)

### 实现思路
```html
首先我肯定是用aop了,在后面的使用发现,apo的实现适合大部分
的单表操作,但是多表更改,例如先加后改是没法实现的,所以我决定
提供两种实现方式,另外一种使用service函数调用来解决了
```

### 表设计
```html
CREATE TABLE operation_log (
`id` INT(10) NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键id',
`name` VARCHAR(128) NULL DEFAULT NULL COMMENT '操作业务名',
`table_name` VARCHAR(16) NULL DEFAULT NULL COMMENT '操作表名',
`table_id` VARCHAR(16) NULL DEFAULT NULL COMMENT '操作表id',
`type` VARCHAR(8) NULL DEFAULT NULL COMMENT '操作类型,(添加ADD,删除DELETE,修改UPDATE)' ,
`operator_id` VARCHAR(16) NULL DEFAULT NULL COMMENT '操作人id',
`operator_name` VARCHAR(16) NULL DEFAULT NULL COMMENT '操作人名',
`operation_time` TIMESTAMP NULL DEFAULT NULL COMMENT '操作时间'
)ENGINE INNODB CHARSET utf8 COMMENT '用户操作日志记录表';
	
CREATE TABLE operation_log_detail (
`id` INT(10) NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '主键id',
`operation_log_id` INT(10) NULL DEFAULT NULL COMMENT '操作日志id',
`clm_name` VARCHAR(16) NULL DEFAULT NULL COMMENT '字段名',
`clm_comment` VARCHAR(128) NULL DEFAULT NULL COMMENT '字段描述',
`old_string` VARCHAR(128) NULL DEFAULT NULL COMMENT '旧值',
`new_string` VARCHAR(128) NULL DEFAULT NULL COMMENT '新值'
)ENGINE INNODB CHARSET utf8 COMMENT '操作日志详情表';
```

### AOP实现
```java
0目标: 在业务代码函数上使用注解,通过注解实现执行时的环形切面,在切面前,切面后,做数据的变更记录操作
开始:
1创建注解
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
由于使用了一个枚举下面提供一个枚举,作用是分辨操作类型
package com.csp.operationlog.aspect.enums;
public enum OperationType {
	ADD,
	UPDATE,
	DELETE;

	public String getType() {
		if (this.equals(ADD)) {
			return "ADD";
		}
		if (this.equals(UPDATE)) {
			return "UPDATE";
		}
		if (this.equals(DELETE)) {
			return "DELETE";
		}
		return null;
	};
}
2使用注解,只是提前看看使用效果
    @OperationLog(name = "更新账户",type = OperationType.UPDATE,operatorIdRef = 0,operatorNameRef = 1,idRef = 2,table = "account")
    public void updateAccount(String operatorId,String operatorName,Integer accountId){
        Account account = new Account();
        account.setId(accountId);
        account.setAccount(1100);
        accountMapper.updateAccount(account);
    }
3下面开始实现切面
package com.csp.operationlog.aspect.aop;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.csp.operationlog.aspect.enums.OperationType;
import com.csp.operationlog.dto.ColumnComment;
import com.csp.operationlog.mapper.OperationLogDetailMapper;
import com.csp.operationlog.mapper.OperationLogMapper;
import com.csp.operationlog.model.OperationLog;
import com.csp.operationlog.model.OperationLogDetail;

@Aspect
@Component
public class OperationLogAop {
	@Autowired
	private OperationLogMapper operationLogMapper;
	@Autowired
	private OperationLogDetailMapper operationLogDetailMapper;
	@Autowired
	private TransactionTemplate txTemplate;

	@Around(value = "@annotation(operationlog)")
	public void logAround(final ProceedingJoinPoint p,final com.csp.operationlog.aspect.annotation.OperationLog operationlog) throws Throwable {
		OperationType type = operationlog.type();
		if (OperationType.UPDATE.equals(type)) {
			update(p, operationlog);
		}
		if (OperationType.ADD.equals(type)) {
			add(p, operationlog);
		}
		if (OperationType.DELETE.equals(type)) {
			delete(p, operationlog);
		}
	}
	
	public void delete(final ProceedingJoinPoint p,final com.csp.operationlog.aspect.annotation.OperationLog operationlog) {
		txTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				StringBuilder sql = new StringBuilder();
				OperationType type = operationlog.type();
				Object[] args = p.getArgs();
				String logName = operationlog.name();
				String logTable = operationlog.table();
				if (operationlog.idRef()==-1) {
					throw new RuntimeException();
				}
				String id = args[operationlog.idRef()].toString();
				String[] cloum = operationlog.cloum();
				String operatorId = args[operationlog.operatorIdRef()].toString();
				String operatorName = args[operationlog.operatorNameRef()].toString();

				Map<String, Object> columnCommentMap = new HashMap<String, Object>();
				List<ColumnComment> columnCommentList = operationLogMapper.selectColumnCommentByTable(logTable);

				for (ColumnComment cc : columnCommentList) {
					columnCommentMap.put(cc.getColumn(), cc.getComment());
				}
				if (cloum.length == 0) {
					Set<String> keySet = columnCommentMap.keySet();
					List<String> list = new ArrayList<String>();
					for (String o : keySet) {
						list.add(o.toString());
					}
					cloum = list.toArray(new String[list.size()]);
				}
				sql.append("SELECT ");
				for (int i = 0; i < cloum.length; i++) {
					if (i == 0) {
						sql.append("`" + cloum[i] + "` ");
					} else {
						sql.append(",`" + cloum[i] + "` ");
					}
				}
				sql.append(" FROM " + logTable + " WHERE id=" + id);
				Map<String, Object> oldMap = operationLogMapper.selectAnyTalbe(sql.toString());

				try {
					p.proceed();
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}

				if (oldMap!=null) {
					OperationLog op = new OperationLog();
					op.setName(logName);
					op.setTableName(logTable);
					op.setTableId(id);
					op.setType(type.getType());
					op.setOperatorId(operatorId);
					op.setOperatorName(operatorName);
					op.setOperationTime(new Timestamp(System.currentTimeMillis()));
					operationLogMapper.insertOperationLog(op);
					List<OperationLogDetail> opds = new ArrayList<OperationLogDetail>();
					for (String clm : cloum) {
						Object oldclm = oldMap.get(clm);
						OperationLogDetail opd = new OperationLogDetail();
						opd.setOldString(oldclm.toString());
						opd.setNewString("");
						opd.setClmName(clm);
						opd.setClmComment(columnCommentMap.get(clm).toString());
						opd.setOperationLogId(op.getId());
						opds.add(opd);
					}
					if (!opds.isEmpty()) {
						operationLogDetailMapper.insertOperationLogDetail(opds);
					}
				}
			}
		});
	}

	private void add(final ProceedingJoinPoint p,final com.csp.operationlog.aspect.annotation.OperationLog operationlog) {
		txTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				StringBuilder sql = new StringBuilder();
				OperationType type = operationlog.type();
				Object[] args = p.getArgs();
				String logName = operationlog.name();
				String logTable = operationlog.table();
				String[] cloum = operationlog.cloum();
				String operatorId = args[operationlog.operatorIdRef()].toString();
				String operatorName = args[operationlog.operatorNameRef()].toString();

				Map<String, Object> columnCommentMap = new HashMap<String, Object>();
				List<ColumnComment> columnCommentList = operationLogMapper.selectColumnCommentByTable(logTable);
				
				for (ColumnComment cc : columnCommentList) {
					columnCommentMap.put(cc.getColumn(), cc.getComment());
				}
				if (cloum.length == 0) {
					Set<String> keySet = columnCommentMap.keySet();
					List<String> list = new ArrayList<String>();
					for (String o : keySet) {
						list.add(o.toString());
					}
					cloum = list.toArray(new String[list.size()]);
				}
				sql.append("SELECT ");
				for (int i = 0; i < cloum.length; i++) {
					if (i == 0) {
						sql.append("`" + cloum[i] + "` ");
					} else {
						sql.append(",`" + cloum[i] + "` ");
					}
				}
				sql.append(" FROM " + logTable + " ORDER BY id DESC LIMIT 1");
				Map<String, Object> oldMap = operationLogMapper.selectAnyTalbe(sql.toString());
				try {
					p.proceed();
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
				Map<String, Object> newMap = operationLogMapper.selectAnyTalbe(sql.toString());
				if ((oldMap==null)||(!oldMap.get("id").toString().equals(newMap.get("id").toString()))) {
					
					OperationLog op = new OperationLog();
					op.setName(logName);
					op.setTableName(logTable);
					op.setTableId("");
					op.setType(type.getType());
					op.setOperatorId(operatorId);
					op.setOperatorName(operatorName);
					op.setOperationTime(new Timestamp(System.currentTimeMillis()));
					operationLogMapper.insertOperationLog(op);
					List<OperationLogDetail> opds = new ArrayList<OperationLogDetail>();
					for (String clm : cloum) {
						Object oldclm = "";
						Object newclm = newMap.get(clm);
						OperationLogDetail opd = new OperationLogDetail();
						opd.setOldString(oldclm.toString());
						opd.setNewString(newclm.toString());
						opd.setClmName(clm);
						opd.setClmComment(columnCommentMap.get(clm).toString());
						opd.setOperationLogId(op.getId());
						opds.add(opd);
					}
					if (!opds.isEmpty()) {
						operationLogDetailMapper.insertOperationLogDetail(opds);
					}
					
				}
			}
		});
	}

	public void update(final ProceedingJoinPoint p,final com.csp.operationlog.aspect.annotation.OperationLog operationlog) {
		txTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				StringBuilder sql = new StringBuilder();
				OperationType type = operationlog.type();
				Object[] args = p.getArgs();
				String logName = operationlog.name();
				String logTable = operationlog.table();
				if (operationlog.idRef()==-1) {
					throw new RuntimeException();
				}
				String id = args[operationlog.idRef()].toString();
				String[] cloum = operationlog.cloum();
				String operatorId = args[operationlog.operatorIdRef()].toString();
				String operatorName = args[operationlog.operatorNameRef()].toString();

				Map<String, Object> columnCommentMap = new HashMap<String, Object>();
				List<ColumnComment> columnCommentList = operationLogMapper.selectColumnCommentByTable(logTable);
				
				for (ColumnComment cc : columnCommentList) {
					columnCommentMap.put(cc.getColumn(), cc.getComment());
				}
				if (cloum.length == 0) {
					Set<String> keySet = columnCommentMap.keySet();
					List<String> list = new ArrayList<String>();
					for (String o : keySet) {
						list.add(o.toString());
					}
					cloum = list.toArray(new String[list.size()]);
				}
				sql.append("SELECT ");
				for (int i = 0; i < cloum.length; i++) {
					if (i == 0) {
						sql.append("`" + cloum[i] + "` ");
					} else {
						sql.append(",`" + cloum[i] + "` ");
					}
				}
				sql.append(" FROM " + logTable + " WHERE id=" + id);
				Map<String, Object> oldMap = operationLogMapper.selectAnyTalbe(sql.toString());

				try {
					p.proceed();
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}

				Map<String, Object> newMap = operationLogMapper.selectAnyTalbe(sql.toString());
				if (oldMap!=null&&newMap!=null) {
					OperationLog op = new OperationLog();
					op.setName(logName);
					op.setTableName(logTable);
					op.setTableId(id);
					op.setType(type.getType());
					op.setOperatorId(operatorId);
					op.setOperatorName(operatorName);
					op.setOperationTime(new Timestamp(System.currentTimeMillis()));
					operationLogMapper.insertOperationLog(op);
					List<OperationLogDetail> opds = new ArrayList<OperationLogDetail>();
					for (String clm : cloum) {
						Object oldclm = oldMap.get(clm);
						Object newclm = newMap.get(clm);
						OperationLogDetail opd = new OperationLogDetail();
						opd.setOldString(oldclm.toString());
						opd.setNewString(newclm.toString());
						opd.setClmName(clm);
						opd.setClmComment(columnCommentMap.get(clm).toString());
						opd.setOperationLogId(op.getId());
						opds.add(opd);
					}
					if (!opds.isEmpty()) {
						operationLogDetailMapper.insertOperationLogDetail(opds);
					}
				}
			}
		});
	}
}
4 可以看出上面实现中用到了表对应的实体类,以及操作数据库的持久层mapper,还有一个数据对象
我们提供一下,这里简单说明一下,我用的是mybatis,最后提供pom.xml
package com.csp.operationlog.model;
import java.sql.Timestamp;
/**
 * 操作日志主信息模型
 * @author taoken
 */
public class OperationLog {
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

package com.csp.operationlog.mapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.csp.operationlog.model.OperationLogDetail;
/**
 * 操作日志详情持久层
 * @author taoken
 */
@Mapper
public interface OperationLogDetailMapper {
	public static class OperationLogDetailMapperProvider{
		public String insertOperationLogDetailSQL(Map<String,List<OperationLogDetail>> map) {
			List<OperationLogDetail> ops = map.get("ops");
			StringBuilder sqlBuid = new StringBuilder("INSERT INTO operation_log_detail (operation_log_id,clm_name,clm_comment,old_string,new_string) VALUES ");
			for (int i = 0; i < ops.size(); i++) {
				OperationLogDetail o = ops.get(i);
				if (i==0) {
					sqlBuid.append(" ('"+o.getOperationLogId()+"','"+o.getClmName()+"','"+o.getClmComment()+"','"+o.getOldString()+"','"+o.getNewString()+"') ");
				}else {
					sqlBuid.append(" ,('"+o.getOperationLogId()+"','"+o.getClmName()+"','"+o.getClmComment()+"','"+o.getOldString()+"','"+o.getNewString()+"') ");
				}
			}
			return sqlBuid.toString();
		}
	}
	//批量添加操作详情
	@InsertProvider( type=OperationLogDetailMapperProvider.class, method="insertOperationLogDetailSQL" )
	public void insertOperationLogDetail(@Param("ops")List<OperationLogDetail> operationLogDetails);
}

package com.csp.operationlog.mapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import com.csp.operationlog.dto.ColumnComment;
import com.csp.operationlog.model.OperationLog;

/**
 * 操作日志持久层
 * @author taoken
 */
@Mapper
public interface OperationLogMapper {
	public static class OperationLogMapperProvider{
		public String selectAnyTalbeSQL(Map<String,String> map) {
			return map.get("sql");
		}
	}
	//添加操作日志
	@Insert("INSERT INTO operation_log (name,table_name,table_id,type,operator_id,operator_name,operation_time) VALUES (#{p.name},#{p.tableName},#{p.tableId},#{p.type},#{p.operatorId},#{p.operatorName},#{p.operationTime});")
	@Options(useGeneratedKeys=true,keyColumn="id",keyProperty="p.id")
	public void insertOperationLog(@Param("p")OperationLog operationLog);
	
	//查询任意sql
	@SelectProvider(type=OperationLogMapperProvider.class,method="selectAnyTalbeSQL")
	public Map<String,Object> selectAnyTalbe(@Param("sql")String sql);
	
	//查询任意表的字段与备注
	@Select("SELECT COLUMN_NAME `column`,column_comment `comment` FROM INFORMATION_SCHEMA.Columns WHERE table_name=#{table}")
	public List<ColumnComment> selectColumnCommentByTable(@Param("table")String tableName);
}

package com.csp.operationlog.dto;
public class ColumnComment {
	private String column;
	private String comment;
	public String getColumn() {
		return column;
	}
	public void setColumn(String column) {
		this.column = column;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
}

package com.csp.operationlog.util;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class HumpUtil {
	public static final char UNDERLINE = '_';
	/**
	 * (userId:user_id)
	 * @param param
	 * @return
	 */
	public static String camelToUnderline(String param) {
		if (param == null || "".equals(param.trim())) {
			return "";
		}
		int len = param.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = param.charAt(i);
			if (Character.isUpperCase(c)) {
				sb.append(UNDERLINE);
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * (user_id:userId)
	 * @param param
	 * @return
	 */
	public static String underlineToCamel(String param) {
		if (param == null || "".equals(param.trim())) {
			return "";
		}
		StringBuilder sb = new StringBuilder(param);
		Matcher mc = Pattern.compile(UNDERLINE + "").matcher(param);
		int i = 0;
		while (mc.find()) {
			int position = mc.end() - (i++);
			String.valueOf(Character.toUpperCase(sb.charAt(position)));
			sb.replace(position - 1, position + 1,
					sb.substring(position, position + 1).toUpperCase());
		}
		return sb.toString();
	}
}

package com.csp.operationlog.util;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
public class ToMapUtil {
	@SuppressWarnings({ "unchecked"})
	public static <T> Map<String, Object> toMap(T bean) {
		if (bean instanceof Map) {
			return (Map<String, Object>)bean;
		}
		BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
		Map<String, Object> map = new HashMap<String, Object>();
		PropertyDescriptor[] pds = beanWrapper.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			if (!"class".equals(pd.getName())) {
				map.put(pd.getName(),
						beanWrapper.getPropertyValue(pd.getName()));
			}
		}
		return map;
	}
}

pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>com.csp.service</groupId>
	<artifactId>service-operationlog</artifactId>
	<version>1.0</version>
	<packaging>jar</packaging>

	<name>service-operationlog</name>
	<description>service-operationlog project for Spring Boot</description>
	
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>1.5.9.RELEASE</version>
		<relativePath />
	</parent>

	<dependencies>
		<!-- aop -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-aop</artifactId>
		</dependency>
		<!-- mybatis -->
		<dependency>
			<groupId>org.mybatis.spring.boot</groupId>
			<artifactId>mybatis-spring-boot-starter</artifactId>
			<version>1.3.2</version>
		</dependency>
		<!-- mysql -->
		<dependency>
			<groupId>mysql</groupId>
			<artifactId>mysql-connector-java</artifactId>
		</dependency>
		<!--long3-->
		<dependency>
			<groupId>org.apache.commons</groupId>
			<artifactId>commons-lang3</artifactId>
			<version>3.3.2</version>
		</dependency>
	</dependencies>
</project>
```


###  开始测试
创建springboot的测试demo项目
```java
1启动相关配置与启动类,这里模拟我们的真实项目
pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>1.5.9.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.example</groupId>
	<artifactId>demo</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>demo</name>
	<description>Demo project for Spring Boot</description>

	<properties>
		<java.version>1.8</java.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>com.csp.service</groupId>
			<artifactId>service-operationlog</artifactId>
			<version>1.0</version>
		</dependency>
		<!-- mybatis -->
		<dependency>
			<groupId>org.mybatis.spring.boot</groupId>
			<artifactId>mybatis-spring-boot-starter</artifactId>
			<version>1.3.2</version>
		</dependency>
		<!-- mysql -->
		<dependency>
			<groupId>mysql</groupId>
			<artifactId>mysql-connector-java</artifactId>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>

package com.example.demo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
@SpringBootApplication
@ComponentScan(basePackages = {"com.csp.**","com.example.**"})//这里是项目对SpringBean注入的扫描,前面是对operationlog项目中bean的扫描,后面是demo项目的bean的扫描
@MapperScan({"com.csp.operationlog.mapper","com.example.demo.**.mapper"})//这里com.csp.**是扫描我的operationlog项目的mapper,而com.example.**扫描的是我的demo项目的mapper
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}

2 我们使用一个账户表,用来测试操作账户,看看是否能够实现日志记录
表创建:
CREATE TABLE `account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `account` int(10) DEFAULT NULL COMMENT '账户',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8

2 创建对应实体与mapper
package com.example.demo.domain;
import java.io.Serializable;
public class Account implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	
	private Integer id;
    private Integer account;

    public Account clone() {
        try {
            Account proto = (Account) super.clone();
            return proto;
        }catch (CloneNotSupportedException e){
            return null;
        }
    }
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public Integer getAccount() {
        return account;
    }
    public void setAccount(Integer account) {
        this.account = account;
    }
}

package com.example.demo.mapper;
import com.example.demo.domain.Account;
import org.apache.ibatis.annotations.*;
@Mapper
public interface AccountMapper {
    @Insert("INSERT INTO account (account) VALUES (#{a.account})")
    public void insertAccount(@Param("a") Account a);
    @Update("UPDATE account SET account=#{a.account} WHERE id=#{a.id}")
    public void updateAccount(@Param("a") Account a);
    @Select("DELETE FROM account WHERE id=#{id}")
    public void deleteAccountById(@Param("id") Integer id);
    @Select("SELECT id,account FROM account WHERE id=#{id}")
    public Account selectAccountById(@Param("id")Integer id);
}

3 后面是具体的业务代码
package com.example.demo.service;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import com.csp.operationlog.aspect.annotation.OperationLog;
import com.csp.operationlog.aspect.enums.OperationType;
import com.csp.operationlog.service.OperationLogService;
import com.example.demo.domain.Account;
import com.example.demo.mapper.AccountMapper;
@Service
public class AccountService {
    @Resource
    AccountMapper accountMapper;
    @Resource
    OperationLogService operationLogService;

    //采用注解方式,实现操作日志的记录,适用于大多数简单服务,不涉及代码中多表更改的业务
    @OperationLog(name = "添加账户",type = OperationType.ADD,operatorIdRef = 0,operatorNameRef = 1,table = "account")
    public void addAccount(String operatorId,String operatorName){
        Account account = new Account();
        account.setAccount(181);
        accountMapper.insertAccount(account);
    }

    @OperationLog(name = "更新账户",type = OperationType.UPDATE,operatorIdRef = 0,operatorNameRef = 1,idRef = 2,table = "account")
    public void updateAccount(String operatorId,String operatorName,Integer accountId){
        Account account = new Account();
        account.setId(accountId);
        account.setAccount(1100);
        accountMapper.updateAccount(account);
    }

    @OperationLog(name = "删除账户",type = OperationType.DELETE,operatorIdRef = 0,operatorNameRef = 1,idRef = 2,table = "account")
    public void deleteAccount(String operatorId,String operatorName,Integer accountId){
        accountMapper.deleteAccountById(accountId);
    }

    //使用服务调用的方式,实现操作日志的记录,为了在注解无法解决业务代码中对多个表操作时的应对方法
    public void addAcccount2(){
        Account account=new Account();
        account.setAccount(181);
        accountMapper.insertAccount(account);
        operationLogService.logForAdd("添加账户","account","1","liutao",account);
    }

    public void updateAccount2(){
        Account account = accountMapper.selectAccountById(1);
        if (account!=null){
            Account accountOld = account.clone();
            account.setId(1);
            account.setAccount(0);
            accountMapper.updateAccount(account);
            operationLogService.logForUpd("更新账户","account",account.getId().toString(),"1","liutao",accountOld,account);
        }
    }

    public void deleteAccount2(){
        Account account = accountMapper.selectAccountById(1);
        if (account!=null){
            accountMapper.deleteAccountById(1);
            operationLogService.logForDel("删除账户","account",account.getId().toString(),"1","liutao",account);
        }
    }
}
4 为了方便测试,我们写几个controller进行测试,有页面调用,模拟实际业务操作
package com.example.demo.controller;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.example.demo.service.AccountService;
@Controller
public class AccountControler {
    @Resource
    private AccountService accountService;
    //采用注解方式
    @RequestMapping("/t1")
    @ResponseBody
    public String addTest(){
        accountService.addAccount("1","liutao");
        return "ok";
    }
    @RequestMapping("/t2")
    @ResponseBody
    public String updateTest(){
        accountService.updateAccount("1","liutao",1);
        return "ok";
    }
    @RequestMapping("/t3")
    @ResponseBody
    public String deleteTest(){
        accountService.deleteAccount("1","liutao",1);
        return "ok";
    }

    //采用服务调用
    @RequestMapping("/s1")
    @ResponseBody
    public String addTest2(){
        accountService.addAcccount2();
        return "ok";
    }
    @RequestMapping("/s2")
    @ResponseBody
    public String updateTest2() {
        accountService.updateAccount2();
        return "ok";
    }
    @RequestMapping("/s3")
    @ResponseBody
    public String deleteTest2(){
        accountService.deleteAccount2();
        return "ok";
    }
}

配置文件application.yml
spring:
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/demo?characterEncoding=utf-8&useSSL=false
    username: root
    password: root
```




### 备注
有了上面的基本实现,和测试demo,应该可以基本实现日志的记录,
对于相关细节,我都放到备注中,
1  apo实现,需要再建立在mysql事务级别在可重复读级别上(一般默认就是哈!)
2 服务调用实现,可以异步处理啦,如果有为了效率可以再我备注的地方实现即可,自己选择mq实现就行了
3 只能保证基本实现了,质量不保证,主要是提供思路和实现逻辑,有了思路,自己可以写的


