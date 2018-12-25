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
