package com.archer.net.http;

public class HttpException extends RuntimeException {

	private static final long serialVersionUID = 128378174983472L;

	private int code;
	

	public HttpException(HttpStatus status) {
		this(status.getCode(), status.getMsg());
	}
	
	public HttpException(HttpStatus status, Exception e) {
		this(status.getCode(), e);
	}
	
	public HttpException(HttpStatus status, Throwable t) {
		this(status.getCode(), t);
	}
	
	public HttpException(int code, String msg) {
		super(msg);
		this.code = code;
	}
	
	public HttpException(int code, Exception e) {
		super(e);
		this.code = code;
	}

	public HttpException(int code, Throwable t) {
		super(t);
		this.code = code;
	}
	public int getCode() {
		return code;
	}
}
