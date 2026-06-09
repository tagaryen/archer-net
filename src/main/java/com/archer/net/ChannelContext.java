package com.archer.net;

import com.archer.net.handler.Handler;

public class ChannelContext {
	
	private Handler handler;
	
	private Channel channel;
	
	private ChannelContext last;

	private ChannelContext next;
	
	public ChannelContext(Handler handler, Channel channel) {
		this.handler = handler;
		this.channel = channel;
	}
	
	public void write(byte[] output) {
		channel.write(output);
	}

	public void write(byte[] output, int off, int length) {
		channel.write(output, off, length);
	}
	
	public byte[] read(int len) {
		return channel.read(len);
	}
	
	public int read(byte[] input) {
		return channel.read(input);
	}
	
	public int read(byte[] input, int off, int length) {
		return channel.read(input, off, length);
	}
	
	public byte readInt8() {
		return channel.readInt8();
	}
	
	public short readInt16() {
		return channel.readInt16();
	}
	
	public int readInt32() {
		return channel.readInt32();
	}
	
	public long readInt64() {
		return channel.readInt32();
	}
	
	public void writeInt8(byte n) {
		channel.writeInt8(n);
	}
	
	public void writeInt16(short n) {
		channel.writeInt16(n);
	}
	
	public void writeInt32(int n) {
		channel.writeInt32(n);
	}
	
	public void writeInt64(long n) {
		channel.writeInt64(n);
	}
	
	public int readableSize() {
		return channel.readableSize();
	}
	
	public Channel channel() {
		return channel;
	}
	
	public void toNextOnAccept() {
		if(next != null) {
			next.onAccept();
		}
	}
	
	public void toNextOnConnect() {
		if(next != null) {
			next.onConnect();
		}
	}
	
	public void toNextOnDisconnect() {
		if(next != null) {
			next.onDisconnect();
		}
	}

	/**
	 * @since 1.5.0 do not call this method since 1.5.0
	 * */
	@Deprecated()
	public void toNextOnRead(byte[] input) {
	}
	
	
	public void toNextOnRead() {
		if(next != null) {
			next.onRead();
		}
	}
	
	public void toLastOnWrite(byte[] output) {
		if(last != null) {
			last.onWrite(output);
		} else {
			if(channel.isActive()) {
				channel.write(output);
			} else {
				if(Debugger.enableDebug()) {
					System.err.println("channel is not active, writting is not supported");
				}
			}
		}
	}
	
	public void toNextOnError(Throwable t) {
		if(next != null) {
			next.onError(t);
		}
	}
	
	public void toNextOnCertificate(byte[] cert) {
		if(next != null) {
			next.onSslCertificate(cert);
		}
	}
	
	public void close() {
		channel.close();
	}
	
	public void addChannelAttachment(Object obj) {
		this.channel.setAttachment(obj);
	}

	public Object getChannelAttachment() {
		return this.channel.getAttachment();
	}
	
	protected void onAccept() {
		handler.onAccept(this);
	}
	
	protected void onConnect() {
		handler.onConnect(this);
	}
	
	protected void onDisconnect() {
		handler.onDisconnect(this);
	}
	
	protected void onRead() {
		handler.onRead(this);
	}

	protected void onWrite(byte[] output) {
		handler.onWrite(this, output);
	}
	
	protected void onError(Throwable t) {
		handler.onError(this, t);
	}

	/**
	 * @since 1.5.0 this method will never be called since 1.5.0
	 * */
	@Deprecated()
	protected void onSslCertificate(byte[] cert) {
		handler.onSslCertificate(this, cert);
	}
	
	protected ChannelContext next() {
		return next;
	}
	
	protected ChannelContext last() {
		return last;
	}
	
	
	protected void last(ChannelContext last) {
		this.last = last;
		if(last != null) {
			last.next = this;
		}
	}
	
}