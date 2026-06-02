package com.archer.net.http;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;

public class WebSocketChannel {

	private Bytes input;
	private ChannelContext ctx;
	public WebSocketChannel(ChannelContext ctx) {
		this.ctx = ctx;
		this.input = new Bytes();
	}

	public void send(Bytes data) {
		ctx.toLastOnWrite(wrapWebSocketMessage(data));
	}
	
	public void close() {
		ctx.close();
	}
	
	public String remoteHost() {
		return ctx.channel().remoteHost();
	}
	
	public int remotePort() {
		return ctx.channel().remotePort();
	}
	
	protected Bytes resetAndGet() {
		Bytes input = this.input;
		this.input = new Bytes();
		return input;
	}
	
	protected Bytes wrapWebSocketMessage(Bytes out) {
		Bytes output = new Bytes();
		output.writeInt8(129);
		int length = out.available();
		if(length >= 65536) {
			output.writeInt8(127);
			output.writeInt32(length);
		} else if(length >= 126) {
			output.writeInt8(126);
			output.writeInt16(length);
		} else {
			output.writeInt8(length);
		}
		output.readFromBytes(out);
		return output;
	}

	protected boolean parseWebSocketMessage(Bytes in) throws HttpServerException {
		Bytes buf = this.input;
		int b = in.readInt8();
		int fin = b >> 7, opcode = b & 0xf;
	    b = in.readInt8();
	    int mask = b >> 7;
		int payloadLen = b & 0x7F;
		
		if(opcode == 0x8) {
			return false;
		}
		
		if(payloadLen == 126) {
			payloadLen = in.readInt16();
		} else if(payloadLen == 127) {
			payloadLen = in.readInt32();
		}
		byte[] content;
		byte[] maskingKey;
		if(mask == 1) {
			maskingKey = in.read(4);
			content = in.readAll();
			if(content.length != payloadLen) {
				throw new HttpServerException("can not parse websocket input data");
			}
			for(int i = 0; i < payloadLen; i++) {
				content[i] = (byte) (content[i] ^ maskingKey[i%4]);
			}
		} else {
			content = in.readAll();
			if(content.length != payloadLen) {
				throw new HttpServerException("can not parse websocket input data");
			}
		}
		buf.write(content);
		return fin == 1;
	}
}
