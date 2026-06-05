package com.archer.net.handler;

import com.archer.net.ChannelContext;

public interface Handler {
	
	void onAccept(ChannelContext ctx);
	
	void onConnect(ChannelContext ctx);
	
	void onDisconnect(ChannelContext ctx);
	
	void onRead(ChannelContext ctx);

	void onWrite(ChannelContext ctx, byte[] input);
	
	
	void onError(ChannelContext ctx, Throwable t);
	
	/**
	 * @since 1.5.0 this method will never be called since 1.5.0
	 * */
	@Deprecated()
	void onSslCertificate(ChannelContext ctx, byte[] cert);
}
