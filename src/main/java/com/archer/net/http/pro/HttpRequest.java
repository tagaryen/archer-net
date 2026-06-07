package com.archer.net.http.pro;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

	private long fd;
	
	private String uri;
	private String method;
	
	private boolean queriesParsed = false;
	private Map<String, String> headers = new HashMap<>();
	private Map<String, String> queries = new HashMap<>();
	private String contentType = "";
	
	protected HttpRequest(long fd, byte[] uri, byte[] method) {
		this.fd = fd;
		this.uri = new String(uri);
		this.method = new String(method);
	}
	
	public String getUri() {
		return uri;
	}

	public String getMethod() {
		return method;
	}

	public String getContentType() {
		if("".equals(contentType)) {
			byte[] typebs = getContentType(fd);
			if(typebs == null) {
				contentType = null;
			} else {
				contentType = new String(typebs, StandardCharsets.UTF_8);
			}
		}
		return contentType;
	}
	
	public String getHeader(String key) {
		String lower = key.toLowerCase();
		String val = headers.getOrDefault(lower, null);
		if(val == null) {
			byte[] valbs = getHeader(fd, lower.getBytes(StandardCharsets.UTF_8));
			if(valbs != null) {
				val = new String(valbs, StandardCharsets.UTF_8);
				headers.put(lower, val);
			}
		}
		return val;
	}
	
	public String getQuery(String key) {
		String val = queries.getOrDefault(key.toLowerCase(), null);
		if(val == null) {
			byte[] valbs = getQuery(fd, key.getBytes(StandardCharsets.UTF_8));
			if(valbs != null) {
				val = new String(valbs, StandardCharsets.UTF_8);
				queries.put(key, val);
			}
		}
		return val;
	}
	
	public Map<String, String> getQueries() {
		if(queriesParsed) {
			return queries;
		}
		int size = getQuerySize(fd);
		for(int i = 0; i < size; i++) {
			byte[] linebs = getQueryLine(fd, i);
			setQuery(linebs);
		}
		queriesParsed = true;
		return queries;
	}
	
	public byte[] getBody() {
		ByteBuffer buf = ByteBuffer.allocateDirect(2048);
		byte[] bodybuf = new byte[2048];
		int reads = 0, cnt = 0;
		while(true) {
			buf.clear();
			reads = readBody(fd, buf, 2048);
			if(reads == 0) {
				break;
			}
			if(cnt + reads > bodybuf.length) {
				byte[] tmp = new byte[(cnt + reads) * 2];
				System.arraycopy(bodybuf, 0, tmp, 0, cnt);
				bodybuf = tmp;
			}
			buf.get(bodybuf, cnt, reads);
			cnt += reads;
		}
		return Arrays.copyOf(bodybuf, cnt);
	}
	
	public int readBody(byte[] buffer) {
		return readBody(buffer, 0, buffer.length);
	}
	
	public int readBody(byte[] buffer, int off, int len) {
		if(off + len > buffer.length) {
			len = buffer.length - off;
		}
		if(len <= 0) {
			return 0;
		}
		ByteBuffer buf = ByteBuffer.allocateDirect(len);
		int reads = readBody(fd, buf, len);
		buf.get(buffer, off, reads);
		return reads;
	}
	
	private void setQuery(byte[] linebs) {
		int i = 0;
		for(; i < linebs.length; i++) {
			if(linebs[i] == ':') {
				break;
			}
		}
		String key = new String(Arrays.copyOfRange(linebs, 0, i), StandardCharsets.UTF_8);
		String val = new String(Arrays.copyOfRange(linebs, i+1, linebs.length), StandardCharsets.UTF_8);
		queries.put(key, val);
	}
	
	
	protected native byte[] getHeader(long fd, byte[] key);
	
	protected native byte[] getQuery(long fd, byte[] key);
	
	protected native int getQuerySize(long fd);
	
	protected native byte[] getQueryLine(long fd, int idx);

	protected native byte[] getContentType(long fd);
	
	protected native int readBody(long fd, ByteBuffer read, int length);
	
	
}
