package com.archer.net.http;

public class HttpException extends RuntimeException {

	private static final long serialVersionUID = 128378174983472L;

	private int code;
	
	private String msg;
	
	public HttpException(HttpStatus status) {
		this(status.getCode(), status.getMsg());
		this.msg = status.getMsg();
	}
	
	public HttpException(HttpStatus status, Exception e) {
		this(status.getCode(), e);
		this.msg = status.getMsg();
	}
	
	public HttpException(HttpStatus status, Throwable t) {
		this(status.getCode(), t);
		this.msg = status.getMsg();
	}
	
	public HttpException(HttpStatus status, String msg) {
		super(msg);
		this.msg = msg;
		this.code = status.getCode();
	}
	
	public HttpException(int code, String msg) {
		super(msg);
		this.msg = msg;
		this.code = code;
	}
	
	public HttpException(int code, Exception e) {
		super(e);
		this.msg = "";
		this.code = code;
	}

	public HttpException(int code, Throwable t) {
		super(t);
		this.msg = "";
		this.code = code;
	}
	public int getCode() {
		return code;
	}
	public String getMsg() {
		return msg;
	}
}
