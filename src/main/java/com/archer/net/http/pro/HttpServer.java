package com.archer.net.http.pro;

import com.archer.net.Library;
import com.archer.net.http.HttpException;
import com.archer.net.http.HttpServerException;
import com.archer.net.ssl.SslContext;

public class HttpServer {
	
	static {
		Library.loadDetectLibrary();
	}

	private long fd;
	
	private SslContext sslctx;
	
	private int threadNum;
	private boolean running = false;
	private HttpServerFuture future;
	private HttpMessageListenner listenner;
	
	public HttpServer() {}

	public HttpServer(SslContext sslctx) {
		this.sslctx = sslctx;
	}
	
	public void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}
	
	public void addMessageListenner(HttpMessageListenner listenner) {
		this.listenner = listenner;
	}
	
	public synchronized void listen(String host, int port) {
		if(running) {
			return ;
		}
		running = true;
		
		if(sslctx == null) {
			fd = init(0);
		} else {
			fd = init(sslctx.getSsl());
		}

		this.future = new HttpServerFuture(host+port) {
			public void apply() {
				listen(fd, host.getBytes(), port, threadNum);
				sslctx.close();
				running = false;
			}
		};
		this.future.start();
	}
	
	public synchronized void destroy() {
		if(!running) {
			return ;
		}
		running = false;
		close(fd);
	}
	
	
	protected void handle(long reqfd, long resfd, byte[] uri, byte[] method) {
		if(listenner == null) {
			return ;
		}
		HttpRequest req = new HttpRequest(reqfd, uri, method);
		HttpResponse res = new HttpResponse(resfd);
		try {
			listenner.handle(req, res);
		} catch(Throwable t) {
			try {
				listenner.handleException(t);
			} catch(Throwable tx) {
				tx.printStackTrace();
			}
			if(t instanceof HttpException) {
				res.sendException(((HttpException)t));
			} else {
				res.sendInternalServerError();
			}
		}
	}
	
	protected void error(byte[] errors) {
		throw new HttpServerException(new String(errors));
	}
	
	
	
	protected native long init(long ssl);
	
	protected native void listen(long fd, byte[] host, int port, int threads);
	
	protected native void close(long fd);
}
