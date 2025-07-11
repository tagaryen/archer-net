package com.archer.net;

abstract class ChannelFuture extends Thread {
	
	public ChannelFuture(String name) {
		super(name);
	}
	
	@Override
	public void run() {
		apply();
	}
	
	public abstract void apply();

}
