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
				map.put(pd.getName(),beanWrapper.getPropertyValue(pd.getName()));
			}
		}
		return map;
	}
}