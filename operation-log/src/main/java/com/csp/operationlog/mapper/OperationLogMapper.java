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
