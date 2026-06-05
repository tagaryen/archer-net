package com.archer.net.handler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.archer.net.ChannelContext;

public class BaseFrameHandler implements Handler {

	private static ConcurrentHashMap<ChannelContext, BlockedMessage> frameCache = new ConcurrentHashMap<>();
	
	public BaseFrameHandler() {}

	@Override
	public void onAccept(ChannelContext ctx) {
		ctx.toNextOnAccept();
	}
	
	@Override
	public void onConnect(ChannelContext ctx) {
		toFrameMessage(ctx);
		ctx.toNextOnConnect();
	}

	@Override
	public void onRead(ChannelContext ctx) {
		BlockedMessage frame = toFrameMessage(ctx);
		frame.readLen(ctx);
		if(ctx.readableSize() >= frame.size) {
			ctx.toNextOnRead();
		}
	}
	
	@Override
	public void onWrite(ChannelContext ctx, byte[] out) {
		int i = out.length;
		byte[] newOut = new byte[4 + i];
		newOut[0] = (byte) (i >> 24);
		newOut[1] = (byte) (i >> 16);
		newOut[2] = (byte) (i >> 8);
		newOut[3] = (byte) i;
		
		System.arraycopy(out, 0, newOut, 4, i);
		ctx.toLastOnWrite(newOut);
	}

	@Override
	public void onDisconnect(ChannelContext ctx) {
		frameCache.remove(ctx);
		ctx.toNextOnDisconnect();
	}

	@Override
	public void onError(ChannelContext ctx, Throwable t) {
		ctx.toNextOnError(t);
	}

	/**
	 * @since 1.5.0 this method will never be called since 1.5.0
	 * */
	@Override
	@Deprecated
	public void onSslCertificate(ChannelContext ctx, byte[] cert) {
		ctx.toNextOnCertificate(cert);
	}

	
	private BlockedMessage toFrameMessage(ChannelContext ctx) {
		BlockedMessage msg = frameCache.getOrDefault(ctx, null);
		if(msg == null) {
			msg = new BlockedMessage();
			frameCache.put(ctx, msg);
		}
		return msg;
	}
	
	private class BlockedMessage {
		
        ReentrantLock frameLock = new ReentrantLock(true);
        int size = 0;
		
		public BlockedMessage() {}
		
		public void readLen(ChannelContext ctx) {
			frameLock.lock();
			try {
				if(size == 0) {
					int len = ctx.readInt32();
					if(len < 0) {
						return ;
					}
					size = len;
				}
			} finally {
				frameLock.unlock();
			}
		}
	}
}
