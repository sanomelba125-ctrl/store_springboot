package com.example.store.utils;

import java.io.Serializable;

public class Result<T> implements Serializable {
	private String statusCode = ResultType.SUCCESS.getCode();
	private String message = ResultType.SUCCESS.getName();
	private T data = null;

	// --- Getter / Setter ---

	public String getStatusCode() {
		return statusCode;
	}

	public Result<T> setStatusCode(String statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public Result<T> setMessage(String message) {
		this.message = message;
		return this;
	}

	public T getData() {
		return data;
	}

	public Result<T> setData(T data) {
		this.data = data;
		return this;
	}


	public Result<T> addError(String message) {
		this.message = message;
		this.statusCode = ResultType.INTERNAL_SERVER_ERROR.getCode();
		if (this.message == null || "".equals(this.message)) {
			this.message = ResultType.INTERNAL_SERVER_ERROR.getName();
		}
		return this;
	}

	public Result<T> success() {
		return this.success("");
	}

	public Result<T> success(String message) {
		this.message = message;
		this.statusCode = ResultType.SUCCESS.getCode();
		if (this.message == null || "".equals(this.message)) {
			this.message = ResultType.SUCCESS.getName();
		}
		return this;
	}

	public Result<T> fail() {
		return this.fail("");
	}

	public Result<T> fail(String message) {
		this.message = message;
		this.statusCode = ResultType.FAIL.getCode();
		if (this.message == null || "".equals(this.message)) {
			this.message = ResultType.FAIL.getName();
		}
		return this;
	}

	public Result<T> unauthorized(String message) {
		this.message = message;
		this.statusCode = ResultType.UNAUTHORIZED.getCode();
		if (this.message == null || "".equals(this.message)) {
			this.message = ResultType.UNAUTHORIZED.getName();
		}
		return this;
	}

	public Result<T> notFound(String message) {
		this.message = message;
		this.statusCode = ResultType.NOT_FOUND.getCode();
		if (this.message == null || "".equals(this.message)) {
			this.message = ResultType.NOT_FOUND.getName();
		}
		return this;
	}

	public Result<T> againLogin(String message) {
		this.message = message;
		this.statusCode = ResultType.AGAIN_LOGIN.getCode();
		if (this.message == null || "".equals(this.message)) {
			this.message = ResultType.AGAIN_LOGIN.getName();
		}
		return this;
	}
}