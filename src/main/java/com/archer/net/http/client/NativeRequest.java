package com.archer.net.http.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.HandlerList;
import com.archer.net.handler.Handler;
import com.archer.net.http.HttpException;
import com.archer.net.http.HttpStatus;
import com.archer.net.http.multipart.FormData;
import com.archer.net.http.multipart.MultipartParser;
import com.archer.net.ssl.SslContext;
import com.archer.net.util.HexUtil;

public class NativeRequest {


    private static final int TIMEOUT = 3000;
    private static final int BASE_HEADER_LEN = 128;
    private static final int DEFAULT_HEADER_SIZE = 12;
    private static final char COLON = ':';
	private static final char SPACE = ' ';
	private static final String ENTER = "\r\n";
	
	private static final String HTTP_PROTOCOL = "HTTP/1.1";
	private static final String HEADER_CONTENT_LENGTH = "content-length";
	private static final String HEADER_CONTENT_ENCODE = "content-encoding";
	private static final String HEADER_HOST = "host";
	private static final String DEFAULT_CONTENT_ENCODE = "utf-8";
	private static final String[] HEADER_KEY = {"user-agent", "connection", "content-type", "accept"};
	private static final String[] HEADER_VAL = 
		{"Archer-Net/Java", "close", "application/x-www-form-urlencoded",
		 "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"};

    public static NativeResponse get(String httpUrl) {
        return get(httpUrl, null);
    }

    public static NativeResponse post(String httpUrl, byte[] body) {
        return post(httpUrl, body, null);
    }

    public static NativeResponse put(String httpUrl, byte[] body) {
        return put(httpUrl, body, null);
    }

    public static NativeResponse delete(String httpUrl, byte[] body) {
        return delete(httpUrl, body, null);
    }

    public static NativeResponse get(String httpUrl, Options opt) {
        return request("GET", httpUrl, (byte[])null, opt);
    }

    public static NativeResponse post(String httpUrl, byte[] body, Options opt) {
        return request("POST", httpUrl, body, opt);
    }

    public static NativeResponse put(String httpUrl, byte[] body, Options opt) {
        return request("PUT", httpUrl, body, opt);
    }

    public static NativeResponse delete(String httpUrl, byte[] body, Options opt) {
        return request("DELETE", httpUrl, body, opt);
    }
    

    public static void getAsync(String httpUrl, 
    		Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
        getAsync(httpUrl, null, callback, exceptionCallback);
    }

    public static void postAsync(String httpUrl, byte[] body, 
    		Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
        postAsync(httpUrl, body, null, callback, exceptionCallback);
    }

    public static void putAsync(String httpUrl, byte[] body, 
    		Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
        putAsync(httpUrl, body, null, callback, exceptionCallback);
    }

    public static void deleteAsync(String httpUrl, byte[] body, 
    		Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
        deleteAsync(httpUrl, body, null, callback, exceptionCallback);
    }

    public static void getAsync(String httpUrl, Options opt, 
    		Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
        requestAsync("GET", httpUrl, (byte[])null, opt, callback, exceptionCallback);
    }

    public static void postAsync(String httpUrl, byte[] body, Options opt, 
    		Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
    	requestAsync("POST", httpUrl, body, opt, callback, exceptionCallback);
    }

    public static void putAsync(String httpUrl, byte[] body, Options opt, 
    		Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
        requestAsync("PUT", httpUrl, body, opt, callback, exceptionCallback);
    }

    public static void deleteAsync(String httpUrl, byte[] body, Options opt, 
    		Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
        requestAsync("DELETE", httpUrl, body, opt, callback, exceptionCallback);
    }
	
	public static NativeResponse request(String method, String httpUrl, byte[] body, Options opt) {
		if(opt == null) {
			opt = new Options();
		}
		if(body == null) {
			body = new byte[0];
		}
		HttpUrl url = HttpUrl.parse(httpUrl);
		Channel ch = prepareChannel(method, httpUrl, opt, url);
		HttpRequestHandler handler = null;
		HttpException t = null;
		try {
			HandlerList handlers = new HandlerList();
			handler = 
					new HttpRequestHandler(opt.getTimeout(), new Bytes(getRequestAsBytes(method, url, opt, body))); 
			handlers.add(handler);
			ch.handlerList(handlers);
			ch.connect(url.getHost(), url.getPort());
			handler.await();
			if(!handler.res.finished() && handler.err != null) {
				t = handler.err;
			}
		} catch(RuntimeException e) {
			if(e instanceof HttpException) {
				t = (HttpException) e;
			} else {
				t = new HttpException(HttpStatus.BAD_REQUEST, e);
			}
		}
		ch.close();
		if(t != null) {
			throw t;
		}
		if(handler == null) {
			throw new HttpException(HttpStatus.REQUEST_TIMEOUT); 
		}
		return handler.res;
	}
	
	public static void requestAsync(String method, String httpUrl, byte[] body, Options opt, 
			Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
		if(opt == null) {
			opt = new Options();
		}
		if(body == null) {
			body = new byte[0];
		}
		HttpUrl url = HttpUrl.parse(httpUrl);
		Channel ch = prepareChannel(method, httpUrl, opt, url);

		HandlerList handlers = new HandlerList();
		HttpAsyncRequestHandler handler = 
				new HttpAsyncRequestHandler(new Bytes(getRequestAsBytes(method, url, opt, body)), callback, exceptionCallback); 
		handlers.add(handler);
		ch.handlerList(handlers);
		ch.connect(url.getHost(), url.getPort());
	}
	
	public static NativeResponse request(String method, String httpUrl, FormData body, Options opt) {
		if(opt == null) {
			opt = new Options();
		}
		if(body == null) {
			throw new NullPointerException("body can not be null");
		}
		if(opt.headers == null) {
			opt.headers = new HashMap<>();
		}
		opt.headers.put("content-type", MultipartParser.MULTIPART_HEADER + body.getBoundary());
		opt.headers.put("transfer-encoding", "chunked");
//		try {
//			opt.headers.put("content-length", String.valueOf(body.calculateFormDataLength()));
//		} catch (IOException e) {
//			throw new HttpException(HttpStatus.BAD_REQUEST, e);
//		}
		HttpUrl url = HttpUrl.parse(httpUrl);
		Channel ch = prepareChannel(method, httpUrl, opt, url);
		HttpRequestHandler handler = null;
		HttpException t = null;
		try {
			HandlerList handlers = new HandlerList();
			handler = 
					new HttpRequestHandler(opt.getTimeout(), new Bytes(getRequestAsBytes(method, url, opt, null))); 
			handler.form = body;
			handlers.add(handler);
			ch.handlerList(handlers);
			ch.connect(url.getHost(), url.getPort());
			handler.await();
			if(!handler.res.finished() && handler.err != null) {
				t = handler.err;
			}
		} catch(RuntimeException e) {
			if(e instanceof HttpException) {
				t = (HttpException) e;
			} else {
				t = new HttpException(HttpStatus.BAD_REQUEST, e);
			}
		}
		ch.close();
		if(t != null) {
			throw t;
		}
		if(handler == null) {
			throw new HttpException(HttpStatus.REQUEST_TIMEOUT); 
		}
		return handler.res;
	}
	
	public static void requestAsync(String method, String httpUrl, FormData body, Options opt, 
			Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
		if(opt == null) {
			opt = new Options();
		}
		if(body == null) {
			throw new NullPointerException("body can not be null");
		}
		if(opt.headers == null) {
			opt.headers = new HashMap<>();
		}
		opt.headers.put("content-type", MultipartParser.MULTIPART_HEADER + body.getBoundary());
		opt.headers.put("transfer-encoding", "chunked");
		HttpUrl url = HttpUrl.parse(httpUrl);
		Channel ch = prepareChannel(method, httpUrl, opt, url);

		HandlerList handlers = new HandlerList();
		HttpAsyncRequestHandler handler = 
				new HttpAsyncRequestHandler(new Bytes(getRequestAsBytes(method, url, opt, null)), callback, exceptionCallback); 
		handler.form = body;
		handlers.add(handler);
		ch.handlerList(handlers);
		ch.connect(url.getHost(), url.getPort());
	}

	public static void streamRequest(String method, String httpUrl, byte[] body, Options opt, 
			Consumer<NativeResponse> onresponse, Consumer<Bytes> onchunk) {
		if(opt == null) {
			opt = new Options();
		}
		if(body == null) {
			body = new byte[0];
		}
		HttpUrl url = HttpUrl.parse(httpUrl);
		Channel ch = prepareChannel(method, httpUrl, opt, url);

		HandlerList handlers = new HandlerList();
		HttpAsyncRequestHandler handler = 
				new HttpAsyncRequestHandler(new Bytes(getRequestAsBytes(method, url, opt, body)), onresponse, null);
		handler.onchunk = onchunk;
		handlers.add(handler);
		ch.handlerList(handlers);
		ch.connect(url.getHost(), url.getPort());
	}
	
	public static void streamRequest(String method, String httpUrl, FormData body, Options opt, 
			Consumer<NativeResponse> onresponse, Consumer<Bytes> onchunk) {

		if(opt == null) {
			opt = new Options();
		}
		if(body == null) {
			throw new NullPointerException("body can not be null");
		}
		if(opt.headers == null) {
			opt.headers = new HashMap<>();
		}
		opt.headers.put("content-type", MultipartParser.MULTIPART_HEADER + body.getBoundary());
		opt.headers.put("transfer-encoding", "chunked");
		HttpUrl url = HttpUrl.parse(httpUrl);
		Channel ch = prepareChannel(method, httpUrl, opt, url);

		HandlerList handlers = new HandlerList();
		HttpAsyncRequestHandler handler = 
				new HttpAsyncRequestHandler(new Bytes(getRequestAsBytes(method, url, opt, null)), onresponse, null); 
		handler.form = body;
		handler.onchunk = onchunk;
		handlers.add(handler);
		ch.handlerList(handlers);
		ch.connect(url.getHost(), url.getPort());
	}
	
	private static Channel prepareChannel(String method, String httpUrl, Options opt, HttpUrl url) {
		if(method == null || httpUrl == null) {
			throw new NullPointerException();
		}
		
		SslContext ctx = null;
		if(url.isHttps()) {
			ctx = opt.getSslContext();
			if(ctx == null) {
				ctx = new SslContext(true);
			}
		}
		return new Channel(ctx);
	}
	 
	private static byte[] getRequestAsBytes(String method, HttpUrl url, Options opt, byte[] body) {
		Map<String, String> headers = opt.getHeaders();
		Map<String, String> newHeaders = new HashMap<>(DEFAULT_HEADER_SIZE);
		if(headers != null && headers.size() > 0) {
			for(Map.Entry<String, String> header: headers.entrySet()) {
				newHeaders.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
			}
		}
		StringBuilder sb = new StringBuilder(BASE_HEADER_LEN * (newHeaders.size() + 3));
		sb.append(method).append(SPACE).append(url.getUri()).append(SPACE).append(HTTP_PROTOCOL).append(ENTER);
		sb.append(HEADER_HOST).append(COLON).append(SPACE)
				.append(url.getHost()).append(COLON).append(url.getPort()).append(ENTER);
		for(int i = 0 ; i < HEADER_KEY.length; i++) {
			sb.append(HEADER_KEY[i]).append(COLON).append(SPACE);
			if(newHeaders.containsKey(HEADER_KEY[i])) {
				sb.append(newHeaders.get(HEADER_KEY[i])).append(ENTER);
				newHeaders.remove(HEADER_KEY[i]);
			} else {
				sb.append(HEADER_VAL[i]).append(ENTER);
			}
		}
		for(Map.Entry<String, String> header: newHeaders.entrySet()) {
			sb.append(header.getKey()).append(COLON).append(SPACE).append(header.getValue()).append(ENTER);
		}
		if(body != null) {
			sb.append(HEADER_CONTENT_LENGTH).append(COLON).append(SPACE).append(body.length).append(ENTER);
			sb.append(HEADER_CONTENT_ENCODE).append(COLON).append(SPACE).append(opt.getEncoding()).append(ENTER);
		}
		sb.append(ENTER);
		byte[] headerBytes = sb.toString().getBytes();
		byte[] requestBytes;
		if(body != null) {
			requestBytes = new byte[headerBytes.length + body.length];
			System.arraycopy(headerBytes, 0, requestBytes, 0, headerBytes.length);
			System.arraycopy(body, 0, requestBytes, headerBytes.length, body.length);
		} else {
			requestBytes = headerBytes;
		}
		return requestBytes;
	}

	
	public static class Options {
    	
		private SslContext sslcontext;
		
    	
    	private Map<String, String> headers = null;
    	
    	private int timeout = TIMEOUT;

		private String encoding = DEFAULT_CONTENT_ENCODE;
    	
    	public Options() {}
    	
		public SslContext getSslContext() {
			return sslcontext;
		}

		public Options sslcontext(SslContext sslcontext) {
			this.sslcontext = sslcontext;
			return this;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}

		public Options headers(Map<String, String> headers) {
			this.headers = headers;
			return this;
		}

		public int getTimeout() {
			return timeout;
		}

		public Options timeout(int timeout) {
			this.timeout = timeout;
			return this;
		}

		public String getEncoding() {
			return encoding;
		}

		public Options encoding(String encoding) {
			this.encoding = encoding;
			return this;
		}
	}
	
	final static class HttpUrl {

		private String url;

		private String protocol;

		private String host;

		private int port;

		private String uri;

		private HttpUrl(String url, String protocol, String host, int port, String uri) {
			this.url = url;
			this.protocol = protocol;
			this.host = host;
			this.port = port;
			this.uri = uri;
		}

		public String getUrl() {
			return url;
		}
		
		public String getProtocol() {
			return protocol;
		}
		
		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		public String getUri() {
			return uri;
		}
		
		public boolean isHttps() {
			return PROTOCOL_HTTPS.equals(protocol);
		}
		
		private static final char COLON = ':';
		private static final char SLASH = '/';

	    private static final char[] HTTP = { 'h', 't', 't', 'p' };
	    private static final char[] HTTPS = { 'h', 't', 't', 'p', 's' };
	    
	    private static final char[] PROTOCOL_SEP = { ':', '/', '/' };
	    
	    private static final String PROTOCOL_HTTP = "http";
	    private static final String PROTOCOL_HTTPS = "https";
	    
		
		public static HttpUrl parse(String httpUrl) {
			if(httpUrl == null || httpUrl.length() < HTTP.length + PROTOCOL_SEP.length + 2) {
				throw new IllegalArgumentException("invalid http url " + httpUrl);
			}
			String protocol = null, host = null, uri = null;
			int port = 0;
			char[] urlChars = httpUrl.toCharArray();
			int i = 0, t = 0;
			for(; i < HTTP.length; i++) {
				if(urlChars[i] != HTTP[i]) {
					throw new IllegalArgumentException("invalid http url " + httpUrl);
				}
			}
			if(urlChars[i] == HTTPS[i]) {
				protocol = PROTOCOL_HTTPS;
				i++;
			} else {
				protocol = PROTOCOL_HTTP;
			}
			t = i;
			for(; i < t + PROTOCOL_SEP.length; i++ ) {
				if(urlChars[i] != PROTOCOL_SEP[i - t]) {
					throw new IllegalArgumentException("invalid http url " + httpUrl);
				}
			}
			try {
				t = i;
				for(; i < urlChars.length; i++) {
					if(urlChars[i] == COLON) {
						char[] hostChars = Arrays.copyOfRange(urlChars, t, i);
						host = new String(hostChars);
						i++;
						break;
					}
					if(urlChars[i] == SLASH) {
						char[] hostChars = Arrays.copyOfRange(urlChars, t, i);
						host = new String(hostChars);
						port = PROTOCOL_HTTPS.equals(protocol) ? 443 :80;
						break;
					}
					if(i >= urlChars.length - 1) {
						char[] hostChars = Arrays.copyOfRange(urlChars, t, urlChars.length);
						host = new String(hostChars);
						port = PROTOCOL_HTTPS.equals(protocol) ? 443 :80;
					}
				}
			} catch(Exception e) {
				throw new IllegalArgumentException("invalid http url " + httpUrl);
			}
			if(i < urlChars.length && port == 0) {
				try {
					t = i;
					for(; i < urlChars.length; i++) {
						if(urlChars[i] == SLASH) {
							char[] portChars = Arrays.copyOfRange(urlChars, t, i);
							port = Integer.parseInt(new String(portChars));
							break ;
						}
						if(i >= urlChars.length - 1) {
							char[] portChars = Arrays.copyOfRange(urlChars, t, urlChars.length);
							port = Integer.parseInt(new String(portChars));
						}
					}
				} catch(Exception e) {
					throw new IllegalArgumentException("invalid http url " + httpUrl);
				}
			}
			uri = new String(Arrays.copyOfRange(urlChars, i, urlChars.length));
            if (uri.length() == 0) {
            	uri = "/";
            } else if (uri.charAt(0) == '?') {
            	uri = "/" + uri;
            }
			if(host == null || port == 0) {
				throw new IllegalArgumentException("invalid http url " + httpUrl);
			}
			return new HttpUrl(httpUrl, protocol, host, port, uri);
		}
	}
	

	private static class HttpAsyncRequestHandler implements Handler {

		NativeResponse res = new NativeResponse();
		Bytes requestData;
		FormData form;
		Consumer<NativeResponse> callback;
		Consumer<Throwable> exceptionCallback;
		Consumer<Bytes> onchunk = null;
		
		
		HttpAsyncRequestHandler(Bytes requestData, Consumer<NativeResponse> callback, Consumer<Throwable> exceptionCallback) {
			this.requestData = requestData;
			this.callback = callback;
			this.exceptionCallback = exceptionCallback;
		}
		
		@Override
		public void onRead(ChannelContext ctx, Bytes input) {
			if(res.headerParsed()) {
				res.parseContent(input.readAll());
			} else {
				res.parseHead(input.readAll());
				if(this.onchunk != null && this.callback != null) {
					this.callback.accept(res);
				}
			}
			if(this.onchunk != null && res.getBody() != null && res.getBody().length > 0) {
				this.onchunk.accept(new Bytes(res.getBody()));
				res.clearBody();
				if(res.finished()) {
					ctx.close();
				}
			} else if(res.finished()) {
				ctx.close();
				
				if(this.callback != null) {
					try {
						this.callback.accept(res);
					} catch(Exception e) {
						if(this.exceptionCallback != null) {
							this.exceptionCallback.accept(e);
						} else {
							e.printStackTrace();
						}
					}
				}
			}
		}
		@Override
		public void onWrite(ChannelContext ctx, Bytes output) {}
		@Override
		public void onError(ChannelContext ctx, Throwable t) {
			if(this.exceptionCallback != null) {
				this.exceptionCallback.accept(t);
			} else {
				t.printStackTrace();
			}
		}
		@Override
		public void onConnect(ChannelContext ctx) {
			ctx.toLastOnWrite(requestData);
			if(this.form != null) {
				Bytes out = null;
				try {
					Bytes chunk = new Bytes(FormData.CACHE_SIZE + 16);
					while((out = this.form.read()) != null) {
						chunk.clear();
						chunk.write((HexUtil.intToHex(out.available()) + "\r\n").getBytes());
						chunk.readFromBytes(out);
						chunk.write("\r\n".getBytes());
						ctx.toLastOnWrite(chunk);
					}
					ctx.toLastOnWrite(new Bytes("0\r\n\r\n".getBytes()));
				} catch (IOException e) {
					onError(ctx, e);
					ctx.close();
					return ;
				}
			}
		}
		@Override
		public void onAccept(ChannelContext ctx) {}
		@Override
		public void onDisconnect(ChannelContext ctx) {}
		@Override
		public void onSslCertificate(ChannelContext ctx, byte[] cert) {}
		
	}
	

	private static class HttpRequestHandler implements Handler {

		NativeResponse res = new NativeResponse();
		Object lock = new Object();
		Bytes requestData;
		HttpException err;
		long timeout;
		FormData form = null;
		
		HttpRequestHandler(long timeout, Bytes requestData) {
			this.timeout = timeout;
			this.requestData = requestData;
		}
		
		public void await() {
			if(err != null) {
				return ;
			}
			long start = System.currentTimeMillis();
			synchronized(lock) {
				try {
					lock.wait(timeout);
				} catch (InterruptedException e) {}
			}
			long end = System.currentTimeMillis();
			if(end - start >= timeout) {
				err = new HttpException(HttpStatus.BAD_REQUEST.getCode(), "connect timeout");
			}
		}
		
		public void notifyLock() {
			synchronized(lock) {
				lock.notifyAll();
			}
		}
		
		@Override
		public void onRead(ChannelContext ctx, Bytes input) {
			if(res.headerParsed()) {
				res.parseContent(input.readAll());
			} else {
				res.parseHead(input.readAll());
			}
			if(res.finished()) {
				notifyLock();
			}
		}
		@Override
		public void onWrite(ChannelContext ctx, Bytes output) {
			ctx.toLastOnWrite(output);
		}
		@Override
		public void onError(ChannelContext ctx, Throwable t) {
			err = new HttpException(HttpStatus.SERVICE_UNAVAILABLE, t);
			notifyLock();
		}
		@Override
		public void onConnect(ChannelContext ctx) {
			ctx.write(requestData);
			if(this.form != null) {
				Bytes out = null;
				try {
					Bytes chunk = new Bytes(FormData.CACHE_SIZE + 16);
					while((out = this.form.read()) != null) {
						chunk.clear();
						chunk.write((HexUtil.intToHex(out.available()) + "\r\n").getBytes());
						chunk.readFromBytes(out);
						chunk.write("\r\n".getBytes());
						ctx.toLastOnWrite(chunk);
					}
					ctx.toLastOnWrite(new Bytes("0\r\n\r\n".getBytes()));
				} catch (IOException e) {
					onError(ctx, e);
					ctx.close();
					return ;
				}
			}
		}
		@Override
		public void onAccept(ChannelContext ctx) {}
		@Override
		public void onDisconnect(ChannelContext ctx) {
			notifyLock();
		}
		@Override
		public void onSslCertificate(ChannelContext ctx, byte[] cert) {}
		
	}
	
}

