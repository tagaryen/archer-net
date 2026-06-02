package com.archer.net.http;

import java.util.Base64;

import com.archer.net.Bytes;
import com.archer.net.util.Sha1Util;

public abstract class WebSocketListenner {
	
	private static final String MAGIC_NUMBER = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	private static final String Sec_WebSocket_Key = "Sec-WebSocket-Key";
	private static final String Sec_WebSocket_Accept = "Sec-WebSocket-Accept";
	private static final String Sec_WebSocket_Version = "Sec-WebSocket-Version";
	private static final String Upgrade = "Upgrade";
	private static final String Upgrade_Val = "websocket";
	private static final String Connection = "Connection";
	private static final String Connection_Val = "Upgrade";

	private String uri;
	
	public WebSocketListenner(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return uri;
	}
	
	protected boolean setWsResponse(HttpRequest request, HttpResponse response) {
		if(!"GET".equals(request.getMethod())) {
			response.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
			return false;
		}
		if(request.getContentLength() > 0) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			return false;
		}
		if(!Upgrade_Val.equals(request.getHeader(Upgrade))) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			return false;
		}
		if(!Connection_Val.equals(request.getHeader(Connection))) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			return false;
		}
		String version = request.getHeader(Sec_WebSocket_Version);
		if(null == version) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			return false;
		}
		
		String key = request.getHeader(Sec_WebSocket_Key);
		String accept = key + MAGIC_NUMBER;
		accept = new String(Base64.getEncoder().encode(Sha1Util.hash(accept.getBytes())));
		
		response.setStatus(HttpStatus.SWITCHING_PROTOCOLS);
		response.setHeader(Upgrade, Upgrade_Val);
		response.setHeader(Connection, Connection_Val);
		response.setHeader(Sec_WebSocket_Accept, accept);
		response.setHeader(Sec_WebSocket_Version, version);
		
		return true;
	}
	
	protected boolean websocketUriMatch(String uri) {
		String pattern = uri;
	    int uri_len = uri.length(), pattern_len = pattern.length(); 
	    if(pattern.indexOf('*') < 0) {
	        return pattern.equals(uri);
	    }
	    if(uri_len < pattern_len) {
	        return false;
	    }
	    int h = 0, star = 0;
	    for(int i = 0; i < pattern_len; ++i, ++h) {
	        if(uri_len - h < pattern_len - i) {
	            return false;
	        }
	        if(pattern.charAt(i) == '*') {
	            star = 1;
	            if(i >= pattern_len - 1) {
	                return true;
	            }
	            ++i;
	            while(uri.charAt(h) != pattern.charAt(i)) {
	                ++h;
	                if(h >= uri_len) {
	                    return false;
	                }
	            }
	        } else if(uri.charAt(h) != pattern.charAt(i)) {
	            if(star == 1 && h < uri_len - 1) {
	                ++h;
	                while(uri.charAt(h) != pattern.charAt(i)) {
	                    ++h;
	                    if(h >= uri_len) {
	                        return false;
	                    }
	                }
	            } else {
	                return false;
	            }
	        } 
	    }
	    return true;
	}


	public abstract void onConnected(WebSocketChannel wsChannel);
	public abstract void onMessage(WebSocketChannel wsChannel, Bytes input);
	public abstract void onError(WebSocketChannel wsChannel, Throwable t);
	public abstract void onClose(WebSocketChannel wsChannel);
}
