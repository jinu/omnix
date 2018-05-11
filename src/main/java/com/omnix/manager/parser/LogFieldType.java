package com.omnix.manager.parser;

public enum LogFieldType {
	TEXT, STRING, INT, LONG, DOUBLE, FLOAT, IP, COUNTRY, DATETIME, NONE;

	public String getMessage() {
		switch (this) {
		case TEXT:
			return "CONFIG_COLUMN_TYPE_TEXT";

		case STRING:
			return "CONFIG_COLUMN_TYPE_STRING";

		case INT:
			return "CONFIG_COLUMN_TYPE_INT";

		case LONG:
			return "CONFIG_COLUMN_TYPE_LONG";

		case DOUBLE:
			return "CONFIG_COLUMN_TYPE_DOUBLE";

		case FLOAT:
			return "CONFIG_COLUMN_TYPE_FLOAT";

		case IP:
			return "CONFIG_COLUMN_TYPE_IP";

		case COUNTRY:
			return "CONFIG_COLUMN_TYPE_COUNTRY";

		case DATETIME:
			return "CONFIG_COLUMN_TYPE_DATETIME";

		case NONE:
		default:
			return "CONFIG_COLUMN_TYPE_NONE";
		}
	}

	public boolean isKeyField() {
		if (this == TEXT || this == STRING || this == IP || this == COUNTRY) {
			return true;
		}
		return false;
	}

	public boolean isStatField() {
		if (this == INT || this == LONG || this == DOUBLE || this == FLOAT) {
			return true;
		}
		return false;
	}

	public boolean isInteger() {
		if (this == INT) {
			return true;
		}
		return false;
	}
}
