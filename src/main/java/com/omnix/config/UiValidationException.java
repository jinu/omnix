package com.omnix.config;

public class UiValidationException extends RuntimeException {
	private static final long serialVersionUID = -3220082175451038474L;
	private Object[] args;

	public UiValidationException() {
		super();
	}

	public UiValidationException(String s) {
		super(s);
	}

	public UiValidationException(String s, Throwable throwable) {
		super(s, throwable);
	}

	public UiValidationException(Throwable throwable) {
		super(throwable);
	}

	public UiValidationException(String s, Object[] array) {
		super(s);
		this.args = array;
	}

	public Object[] getArgs() {
		return args;
	}

}
