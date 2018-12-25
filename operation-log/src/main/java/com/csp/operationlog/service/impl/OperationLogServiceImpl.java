package com.csp.operationlog.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.csp.operationlog.aspect.enums.OperationType;
import com.csp.operationlog.dto.ColumnComment;
import com.csp.operationlog.mapper.OperationLogDetailMapper;
import com.csp.operationlog.mapper.OperationLogMapper;
import com.csp.operationlog.model.OperationLog;
import com.csp.operationlog.model.OperationLogDetail;
import com.csp.operationlog.service.OperationLogService;
import com.csp.operationlog.util.HumpUtil;
import com.csp.operationlog.util.ToMapUtil;

/**
 * 操作日志记录,相关服务实现
 * @author taoken
 */
@Service
public class OperationLogServiceImpl implements OperationLogService{
	@Resource
	private OperationLogMapper operationLogMapper;
	@Resource
	private OperationLogDetailMapper operationLogDetailMapper;

	@Override
	public void logForAdd(String professionalName, String logTableName,String opratorId, String operatorName, Object addedBean) {
		//基本处理
		if (addedBean==null) {
			return;
		}
		if (StringUtils.isBlank(logTableName)||StringUtils.isBlank(professionalName)||StringUtils.isBlank(opratorId)) {
			return;
		}
		Map<String, Object> map = ToMapUtil.toMap(addedBean);
		Set<String> beanKeySet = map.keySet();
		if (beanKeySet.size()==0) {
			return;
		}
		
		// 修改成异步操作,由此行以上结束处发送到队列!由以下开始处做消费!即可实现异步处理
		
		//表信息查询
		Map<String, Object> columnCommentMap = new HashMap<String, Object>();
		List<ColumnComment> columnCommentList = operationLogMapper.selectColumnCommentByTable(logTableName);
		for (ColumnComment cc : columnCommentList) {
			columnCommentMap.put(cc.getColumn(), cc.getComment());
		}
		
		//操作日志信息插入
		OperationLog op = new OperationLog();
		op.setName(professionalName);
		op.setTableName(logTableName);
		op.setTableId("");
		op.setType(OperationType.ADD.getType());
		op.setOperatorId(opratorId);
		op.setOperatorName(operatorName);
		op.setOperationTime(new Timestamp(System.currentTimeMillis()));
		operationLogMapper.insertOperationLog(op);
		
		List<OperationLogDetail> opds = new ArrayList<OperationLogDetail>();
		
		for (String key : beanKeySet) {
			String value = map.get(key)!=null?map.get(key).toString():"";
			key= HumpUtil.camelToUnderline(key);
			OperationLogDetail opd = new OperationLogDetail();
			opd.setClmName(key);//字段名
			opd.setNewString(value);//值
			opd.setClmComment(columnCommentMap.get(key).toString());//字段备注 
			opd.setOldString("");//原字段的值,添加操作无原值
			opd.setOperationLogId(op.getId());
			opds.add(opd);
		}
			
		if (!opds.isEmpty()) {
			operationLogDetailMapper.insertOperationLogDetail(opds);
		}
	}

	@Override
	public void logForDel(String professionalName, String logTableName,String primaryKeyId, String opratorId, String operatorName,Object beforDeledBean) {
		//基本处理
		if (beforDeledBean==null) {
			return;
		}
		if (StringUtils.isBlank(primaryKeyId)||StringUtils.isBlank(logTableName)||StringUtils.isBlank(professionalName)||StringUtils.isBlank(opratorId)) {
			return;
		}
		Map<String, Object> map = ToMapUtil.toMap(beforDeledBean);
		Set<String> beanKeySet = map.keySet();
		if (beanKeySet.size()==0) {
			return;
		}
		
		// 修改成异步操作,由此行以上结束处发送到队列!由以下开始处做消费!即可实现异步处理
		
		//表信息查询
		Map<String, Object> columnCommentMap = new HashMap<String, Object>();
		List<ColumnComment> columnCommentList = operationLogMapper.selectColumnCommentByTable(logTableName);
		for (ColumnComment cc : columnCommentList) {
			columnCommentMap.put(cc.getColumn(), cc.getComment());
		}
		
		//操作日志信息插入
		OperationLog op = new OperationLog();
		op.setName(professionalName);
		op.setTableName(logTableName);
		op.setTableId(primaryKeyId);
		op.setType(OperationType.DELETE.getType());
		op.setOperatorId(opratorId);
		op.setOperatorName(operatorName);
		op.setOperationTime(new Timestamp(System.currentTimeMillis()));
		operationLogMapper.insertOperationLog(op);
		
		List<OperationLogDetail> opds = new ArrayList<OperationLogDetail>();
		for (String key : beanKeySet) {
			String value = map.get(key)!=null?map.get(key).toString():"";
			key= HumpUtil.camelToUnderline(key);
			OperationLogDetail opd = new OperationLogDetail();
			opd.setClmName(key);//字段名
			opd.setOldString(value);//值
			opd.setClmComment(columnCommentMap.get(key).toString());//字段备注 
			opd.setNewString("");//新字段的值,删除操作无新值
			opd.setOperationLogId(op.getId());
			opds.add(opd);
		}
		if (!opds.isEmpty()) {
			operationLogDetailMapper.insertOperationLogDetail(opds);
		}
	}

	@Override
	public void logForUpd(String professionalName, String logTableName,String primaryKeyId, String opratorId, String operatorName,Object beforUpedBean, Object afterUpedBean) {
		//基本处理
		if (beforUpedBean==null||afterUpedBean==null) {
			return;
		}
		if (StringUtils.isBlank(primaryKeyId)||StringUtils.isBlank(logTableName)||StringUtils.isBlank(professionalName)||StringUtils.isBlank(opratorId)) {
			return;
		}
		Map<String, Object> map1 = ToMapUtil.toMap(beforUpedBean);
		Map<String, Object> map2 = ToMapUtil.toMap(afterUpedBean);
		Set<String> beanKeySet1 = map1.keySet();
		Set<String> beanKeySet2 = map2.keySet();
		if (beanKeySet1.size()==0||beanKeySet2.size()==0) {
			return;
		}
		
		// 修改成异步操作,由此行以上结束处发送到队列!由以下开始处做消费!即可实现异步处理
		
		//表信息查询
		Map<String, Object> columnCommentMap = new HashMap<String, Object>();
		List<ColumnComment> columnCommentList = operationLogMapper.selectColumnCommentByTable(logTableName);
		for (ColumnComment cc : columnCommentList) {
			columnCommentMap.put(cc.getColumn(), cc.getComment());
		}
		
		//操作日志信息插入
		OperationLog op = new OperationLog();
		op.setName(professionalName);
		op.setTableName(logTableName);
		op.setTableId(primaryKeyId);
		op.setType(OperationType.UPDATE.getType());
		op.setOperatorId(opratorId);
		op.setOperatorName(operatorName);
		op.setOperationTime(new Timestamp(System.currentTimeMillis()));
		operationLogMapper.insertOperationLog(op);
		
		List<OperationLogDetail> opds = new ArrayList<OperationLogDetail>();
		for (String key : beanKeySet1) {
			String valueOld = map1.get(key)!=null?map1.get(key).toString():"";
			String valueNew = map2.get(key)!=null?map2.get(key).toString():"";
			key= HumpUtil.camelToUnderline(key);
			OperationLogDetail opd = new OperationLogDetail();
			opd.setClmName(key);//字段名
			opd.setOldString(valueOld);//老值
			opd.setNewString(valueNew);//新值
			opd.setClmComment(columnCommentMap.get(key).toString());//字段备注 
			opd.setOperationLogId(op.getId());
			opds.add(opd);
		}
		if (!opds.isEmpty()) {
			operationLogDetailMapper.insertOperationLogDetail(opds);
		}
	}

}









