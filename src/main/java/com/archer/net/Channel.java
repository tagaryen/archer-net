package com.archer.net;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.archer.net.ssl.SslContext;

public class Channel {
	
	private static final int BUF_SIZE = 4 * 1024;
	
	static {
		Library.loadDetectLibrary();
	}

	protected static native long init(Channel channel);

	protected static native void setChannel(long channelfd, Channel channel);

	protected static native void write(long channelfd, ByteBuffer write, int length);

	protected static native int read(long channelfd, ByteBuffer read, int length);

	protected static native int readableSize(long channelfd);

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
	protected void onRead() {
		if(handlerList != null) {
			handlerList.onRead(this);
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
			handlerList.onError(this, new ChannelException(new String(msg).trim()));
		}
	}
	
	/**
	 * @since 1.5.0 this method will never be called since 1.5.0
	 * */
	@Deprecated()
	protected void onCertCallback(byte[] crt) {
	}
	
	protected void throwError(byte[] msg) {
		throw new ChannelException(new String(msg).trim());
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
	private Object attachment;
	
	private ChannelContext ctx = null;
	private ByteBuffer readBuf = ByteBuffer.allocateDirect(BUF_SIZE);
	private ByteBuffer writeBuf = ByteBuffer.allocateDirect(BUF_SIZE);
	
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
	
	protected ChannelContext ctx() {
		if(ctx == null) {
			if(handlerList == null || handlerList.handlerCount() <= 0) {
				return null;
			}
			int index = 0;
			ChannelContext head = new ChannelContext(handlerList.at(index++), this);
			ChannelContext cur, last = head;
			for(; index < handlerList.handlerCount(); index++) {
				cur = new ChannelContext(handlerList.at(index), this);
				cur.last(last);
				last = cur;
			}
			ctx = head;
		};
		return ctx;
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
	
	public void write(byte[] output) {
		write(output, 0, output.length);
	}
	
	public void write(byte[] output, int off, int len) {
		if(output.length < off + len) {
			len = output.length - off;
		}
		int writeCnt = 0;
		while((writeCnt + BUF_SIZE) < len) {
			writeBuf.clear();
			writeBuf.put(output, off, BUF_SIZE);
			write(channelfd, writeBuf, BUF_SIZE);
			off += BUF_SIZE;
			writeCnt += BUF_SIZE;
		}
		writeBuf.clear();
		writeBuf.put(output, off, (len - writeCnt));
		write(channelfd, writeBuf, len - writeCnt);
	}

	public byte[] read(int len) {
		byte[] input = null;
		int size = readableSize(channelfd);
		if(size < len) {
			input = new byte[size];
		} else {
			input = new byte[len];
		}
		read(input, 0, input.length);
		return input;
	}
	
	public int read(byte[] input) {
		return read(input, 0, input.length);
	}
	
	public int read(byte[] input, int off, int len) {
		if(input.length < off + len) {
			len = input.length - off;
		}
		boolean readEnd = false;
		int readCnt = 0, reads = 0;
		while((readCnt + BUF_SIZE) < len) {
			readBuf.clear();
			reads = read(channelfd, readBuf, BUF_SIZE);
			if(reads == 0) {
				readEnd = true;
				break;
			}
			readBuf.get(input, off, reads);
			
			readCnt += reads;
		}
		if(!readEnd) {
			readBuf.clear();
			reads = read(channelfd, readBuf, len - readCnt);
			readBuf.get(input, off, reads);
			readCnt += reads;
		}
		return readCnt;
	}
	
	public void writeInt8(byte n) {
		writeBuf.clear();
		writeBuf.put(n);
		write(channelfd, writeBuf, 1);
	}
	
	public void writeInt16(short n) {
		writeBuf.clear();
		writeBuf.putShort(n);
		write(channelfd, writeBuf, 2);
	}
	
	public void writeInt32(int n) {
		writeBuf.clear();
		writeBuf.putInt(n);
		write(channelfd, writeBuf, 4);
	}
	
	public void writeInt64(long n) {
		writeBuf.clear();
		writeBuf.putLong(n);
		write(channelfd, writeBuf, 8);
	}
	
	public byte readInt8() {
    	readBuf.clear();
		read(channelfd, readBuf, 1);
    	return readBuf.get();
	}
	
	public short readInt16() {
		if(readableSize(channelfd) < 2) {
			return -1;
		}
    	readBuf.clear();
		read(channelfd, readBuf, 2);
    	return readBuf.getShort();
	}
	
	public int readInt32() {
		if(readableSize(channelfd) < 4) {
			return -1;
		}
    	readBuf.clear();
		read(channelfd, readBuf, 4);
    	return readBuf.getInt();
	}
	
	public long readInt64() {
		if(readableSize(channelfd) < 8) {
			return -1;
		}
    	readBuf.clear();
		read(channelfd, readBuf, 8);
		return readBuf.getLong();
	}
	
	public int readableSize() {
		return readableSize(channelfd);
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

	public Object getAttachment() {
		return attachment;
	}

	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}
}
