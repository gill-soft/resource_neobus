package com.gillsoft.client;

public class Response<T> {

	private String status;
	private T data;
	private String message;

	public String getStatus() {
		return status;
	}

	public T getData() {
		return data;
	}

	public String getMessage() {
		return message;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setData(T data) {
		this.data = data;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
