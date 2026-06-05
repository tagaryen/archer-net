package com.archer.net;

import com.archer.net.handler.Handler;
import com.archer.net.handler.HandlerException;

public final class HandlerList {
    private static final int DEFAULT_SIZE = 16;

	private Handler[] handlers = new Handler[DEFAULT_SIZE];
	private int pos = 0;
	
	private ThreadPool pool = null;
	
	public HandlerList() {}
	
	public HandlerList(Handler ...handlers) {
		add(handlers);
	}
	
	protected HandlerList threadPool(ThreadPool pool) {
		this.pool = pool;
		return this;
	}
	
	public HandlerList addFirst(Handler handler) {
		if(handler == null) {
			return this;
		}
		return insert(0, handler);
	}
	
	public HandlerList add(Handler ...handlers) {
		if(handlers == null || handlers.length <= 0) {
			return this;
		}
		checkCap(handlers.length);
		System.arraycopy(handlers, 0, this.handlers, pos, handlers.length);
		pos += handlers.length;
		return this;
	}
	
	public HandlerList insert(int index, Handler handler) {
		if(handler == null) {
			return this;
		}
		checkCap(1);
		System.arraycopy(handlers, index, handlers, index + 1, pos - index);
		handlers[index] = handler;
		pos++;
		return this;
	}
	
	public HandlerList remove(int index) {
		if(0 <= index && index < pos) {
			System.arraycopy(handlers, index + 1, handlers, index, pos - index - 1);
			pos--;
		}
		return this;
	}
	
	public Handler at(int index) {
		if(index < 0 || index >= pos) {
			throw new HandlerException("index " +index+ " out of range 0 - " + pos);
		}
		return handlers[index];
	}
	
	public int handlerCount() {
		return pos;
	}
	
	private void checkCap(int inputSize) {
		if(inputSize + pos > handlers.length) {
			int size = handlers.length << 1;
			while(size < handlers.length + pos) {
				size <<= 1;
			}
			Handler[] tmp = new Handler[size];
			System.arraycopy(this.handlers, 0, tmp, 0, pos);
			handlers = tmp;
		}
	}
	
	
	protected void onAccept(Channel channel) {
		ChannelContext ctx = channel.ctx();
		try {
			ctx.onAccept();
		} catch(Exception e) {
			ctx.onError(e);
		}
	}
	
	protected void onConnect(Channel channel) {
		ChannelContext ctx = channel.ctx();
		try {
			ctx.onConnect();
		} catch(Exception e) {
			ctx.onError(e);
		}
	}
	
	protected void onRead(Channel channel) {
		ChannelContext ctx = channel.ctx();
		if(pool != null) {
			pool.submit(ctx);
		} else {
			try {
				ctx.onRead();
			} catch(Exception e) {
				ctx.onError(e);
			}	
		}
	}
	
	protected void onDisconnect(Channel channel) {
		ChannelContext ctx = channel.ctx();
		try {
			ctx.onDisconnect();
		} catch(Exception e) {
			ctx.onError(e);
		}	
	}

	protected void onError(Channel channel, Exception e) {
		ChannelContext ctx = channel.ctx();
		try {
			ctx.onError(e);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @since 1.5.0 this method will never be called since 1.5.0
	 * */
	@Deprecated()
	protected void onCertCallback(Channel channel, byte[] cert) {
	}

}