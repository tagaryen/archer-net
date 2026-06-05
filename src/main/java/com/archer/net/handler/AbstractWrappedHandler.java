package com.archer.net.handler;

import com.archer.net.ChannelContext;

public abstract class AbstractWrappedHandler<I> implements Handler {
	
	@Override
	public void onAccept(ChannelContext ctx) {}
	
	@Override
	public void onRead(ChannelContext ctx) {
		onMessage(ctx, decodeInput(ctx));
	}

	@Override
	public void onWrite(ChannelContext ctx, byte[] out) {
		ctx.toLastOnWrite(out);
	}

	/**
	 * @since 1.5.0 this method will never be called since 1.5.0
	 * */
	@Override
	@Deprecated
	public void onSslCertificate(ChannelContext ctx, byte[] cert) {
		// we do nothing here
	}
	
	public abstract void onMessage(ChannelContext ctx, I input);
	
	public abstract I decodeInput(ChannelContext ctx);
	
}
