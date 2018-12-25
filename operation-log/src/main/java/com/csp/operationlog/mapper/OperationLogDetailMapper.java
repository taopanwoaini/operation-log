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
