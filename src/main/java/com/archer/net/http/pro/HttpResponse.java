package com.archer.net.http.pro;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.archer.net.http.ContentType;
import com.archer.net.http.HttpException;
import com.archer.net.http.HttpStatus;

public class HttpResponse {

	private long fd;
	private boolean sended = false;
	private boolean chunkStarted = false;
	
	private int statusCode = 0;
	
	protected HttpResponse(long fd) {
		this.fd = fd;
	}
	
	public void setStatus(HttpStatus code) {
		statusCode = code.getCode();
		setStatus(fd, code.getCode());
	}

	protected void setStatusCode(int status) {
		statusCode = status;
		setStatus(fd, status);
	}
	
	public void setHeader(String key, String value) {
		setHeader(fd, key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
	}
	
	public void setContentType(ContentType type) {
		setHeader(fd, "Content-Type".getBytes(StandardCharsets.UTF_8), type.getName().getBytes(StandardCharsets.UTF_8));
	}
	
	public void sendBody(byte[] body) {
		if(sended) {
			return ;
		}
		sended = true;
		if(statusCode == 0) {
			setStatus(HttpStatus.OK);
		}
		ByteBuffer buf = ByteBuffer.allocateDirect(body.length);
		buf.put(body);
		sendBody(fd, buf, body.length);
	}
	
	public void sendChunkStart() {
		if(sended) {
			return ;
		}
		chunkStarted = true;
		if(statusCode == 0) {
			setStatus(HttpStatus.OK);
		}
		sendStart(fd);
	}
	
	public void sendChunk(byte[] chunk) {
		sendChunk(chunk, 0, chunk.length);
	}
	
	public void sendChunk(byte[] chunk, int off, int len) {
		if(sended || !chunkStarted) {
			return ;
		}
		if(len + off > chunk.length) {
			len = chunk.length - off;
		}
		if(len <= 0) {
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "chunk len overflow");
		}
		ByteBuffer buf = ByteBuffer.allocateDirect(len);
		buf.put(chunk, off, len);
		sendChunk(fd, buf, len);
	}

	public void sendChunkEnd() {
		if(sended || !chunkStarted) {
			return ;
		}
		sended = true;
		sendEnd(fd);
	}
	
	public void sendInternalServerError() {
		setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		sendBody(getHtmlBody(HttpStatus.INTERNAL_SERVER_ERROR.getMsg()).getBytes());
	}
	
	public void sendException(HttpException e) {
		setStatusCode(e.getCode());
		sendBody(getHtmlBody(e.getMsg()).getBytes());
	}
	
	private String getHtmlBody(String content) {
		return "<html><head><title>ARCHER-SERVER</title></head><body><h3 style=\"text-align: center; width: 100%\">"+content+"</h3><body><html>";
	}
	
	protected native void setStatus(long fd, int status);
	
	protected native void setHeader(long fd, byte[] key, byte[] val);
	
	protected native void sendBody(long fd, ByteBuffer write, int length);
	
	protected native void sendStart(long fd);

	protected native void sendChunk(long fd, ByteBuffer write, int length);

	protected native void sendEnd(long fd);
}
