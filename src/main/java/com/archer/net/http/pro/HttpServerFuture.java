package com.archer.net.http.pro;

abstract class HttpServerFuture extends Thread {

	public HttpServerFuture(String name) {
		super(name);
	}
	
	@Override
	public void run() {
		apply();
	}
	
	public abstract void apply();
}
