package com.archer.net;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.archer.net.ssl.SslContext;

public class Channel {
	
	static {
		Library.loadDetectLibrary();
	}

	protected static native long init(Channel channel);

	protected static native void setChannel(long channelfd, Channel channel);

	protected static native void write(long channelfd, byte[] data);

	protected static native boolean connect(long channelfd, byte[] host, int port);

	protected static native void close(long channelfd);
	
	protected static native void startEventloop();
	
	protected static native void stopEventloop();
	
	protected void onConnect() {
		active.compareAndSet(false, true);
		if(handlerList != null) {
			handlerList.onConnect(this);
		}
	}
	protected void onRead(byte[] data) {
		if(handlerList != null) {
			handlerList.onRead(this, data);
		}
	}
	protected void onDisconnect() {
		errBeforeLoop = true;
		if(active.compareAndSet(true, false) && clientSide) {
			if(ifEnd()) {
				stopEventloop();
			}
		}
		if(handlerList != null) {
			handlerList.onDisconnect(this);
		}
	}
	protected void onError(byte[] msg) {
		errBeforeLoop = true;
		if(active.compareAndSet(true, false) && clientSide) {
			if(ifEnd()) {
				stopEventloop();
			}
		}
		if(handlerList != null) {
			handlerList.onError(this, msg);
		}
	}
	protected void onCertCallback(byte[] crt) {
		if(handlerList != null) {
			handlerList.onCertCallback(this, crt);
		}
	}
	
	protected void throwError(byte[] msg) {
		throw new ChannelException(new String(msg));
	}
	
	
	/*******above are native methods********/
	
	private static final long TIMEOUT = 3500;
	private static AtomicInteger channelCount = new AtomicInteger(0);
	
	private static boolean ifStart() {
		int old;
		boolean ok = false;
		while(true) {
			old = channelCount.get();
			if(channelCount.compareAndSet(old, old+1)) {
				if(old <= 0) {
					ok = true;
				}
				break;
			}
		}
		return ok;
	}
	
	private static boolean ifEnd() {
		int old;
		boolean ok = false;
		while(true) {
			old = channelCount.get();
			if(channelCount.compareAndSet(old, old-1)) {
				if(old <= 1) {
					ok = true;
				}
				break;
			}
		}
		return ok;
	}
	
	private long channelfd;
	private AtomicBoolean active = new AtomicBoolean(false);
	private volatile boolean errBeforeLoop = false;
	
	private boolean clientSide;
	
	private String host;
	private int port;
	private SslContext sslCtx;
	private HandlerList handlerList;
	private ChannelFuture future;
		public Channel() {
		this(null);
	}
		public Channel(SslContext sslCtx) {
		this.sslCtx = sslCtx;
		this.clientSide = true;
	}
	
	/**
	 * for server connected channel
	 * */
	protected Channel(long channelfd, byte[] host, int port) {
		this.channelfd = channelfd;
		this.port = port;
		this.active.set(true);
		this.clientSide = false;
		this.host = new String(host);
		setChannel(channelfd, this);
	}
	
	private synchronized void initChannelfd() {
		if(Debugger.enableDebug()) {
			System.out.println("initializing channel");
		}
		this.channelfd = init(this);
	}

	public void handlerList(HandlerList handlerList) {
		this.handlerList = handlerList;
	}
	
	public HandlerList handlerList() {
		return handlerList;
	}
	
	public synchronized void connect(String host, int port) {
		if(active.get()) {
			return ;
		}
		this.host = host;
		this.port = port;
		
		if(Debugger.enableDebug()) {
			System.out.println("starting connect to " + host + ":" + port);
		}
		initChannelfd();
		if(sslCtx != null) {
			if(!sslCtx.isClientMode()) {
				throw new ChannelException("can not use a server-side sslcontext within client channel");
			}
			sslCtx.setSsl(channelfd);
		}
		connect(channelfd, host.getBytes(), port);
		active.set(true);
		if(ifStart()) {
			this.future = new ChannelFuture(host+port) {
				public void apply() {
					if(!errBeforeLoop) {
						startEventloop();
					}
				}
			};
			this.future.start();
		}
	}
	
	public void write(byte[] data) {
		write(channelfd, data);
	}
	
	public synchronized void close() {
		if(active.compareAndSet(true, false)) {
			close(channelfd);
			if(clientSide) {
				if(ifEnd()) {
					stopEventloop();
				}
			}
		}
	}
	
	public String remoteHost() {
		return host;
	}
	
	public int remotePort() {
		return port;
	}
	
	public boolean isActive() {
		return active.get();
	}
	
	public boolean isClientSide() {
		return clientSide;
	}
	
	protected long getChannelfd() {
		return channelfd;
	}
}
