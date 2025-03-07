package com.archer.net.http;

import java.util.concurrent.ConcurrentHashMap;

import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.Debugger;
import com.archer.net.handler.Handler;


public abstract class HttpWrappedHandler implements Handler {
	
	private static ConcurrentHashMap<ChannelContext, HttpContext> contextCache = new ConcurrentHashMap<>();
    
    public HttpWrappedHandler() {}
    
	@Override
	public void onAccept(ChannelContext ctx) {
		//nothing
	}
    
	@Override
	public void onConnect(ChannelContext ctx) {
		HttpContext context = getHttpContext(ctx, true);
		contextCache.put(ctx, context);
	}

	@Override
	public void onRead(ChannelContext ctx, Bytes in) {
		if(in.available() <= 0) {
			return ;
		}

		HttpContext context = getHttpContext(ctx, true);
		HttpRequest req = context.request;
		HttpResponse res = context.response;
		byte[] msg = in.readAll();
		req.lock();
		try {
			if(req.isEmpty()) {
				try {
					req.parse(msg);
				} catch(HttpException e) {
					res.setStatus(HttpStatus.valueOf(e.getCode()));
					onWrite(ctx, new Bytes(res.toBytes()));
				}
			} else {
				req.putContent(msg);
			}
			if(req.isFinished()) {
				if(Debugger.enableDebug()) {
					System.out.println("http request finished, content-length is " + req.getContentLength());
				}
				try {
					handle(req, res);
				} catch(Exception e) {
					handleException(req, res, e);
				}
				if(Debugger.enableDebug()) {
					System.out.println("http response, content-length is" + res.getContentLength());
				}
				onWrite(ctx, new Bytes(res.toBytes()));
			}
		} catch(Exception e) {
			onError(ctx, e);
		} finally {
			req.unlock();
		}
	}
	
	@Override
	public void onWrite(ChannelContext ctx, Bytes out) {
		HttpContext context = getHttpContext(ctx, false);
		if(context == null) {
			onError(ctx, new HttpException(HttpStatus.INTERNAL_SERVER_ERROR));
		}
		HttpRequest req = context.request;
		HttpResponse res = context.response;
		
		req.clear();
		res.clear();
		ctx.toLastOnWrite(out);
	}
	
	@Override
	public void onDisconnect(ChannelContext ctx) {
		contextCache.remove(ctx);
	}

	@Override
	public void onError(ChannelContext ctx, Throwable t) {
		HttpContext context = getHttpContext(ctx, true);
		HttpRequest req = context.request;
		HttpResponse res = context.response;
		try {
			handleException(req, res, t);
		} catch(Exception ignore) {}
	}
	
	@Override
	public void onSslCertificate(ChannelContext ctx, byte[] cert) {
		//we do nothing here
	}
	
	private HttpContext getHttpContext(ChannelContext ctx, boolean create) {
		HttpContext context = contextCache.getOrDefault(ctx, null);
		if(create && context == null) {
			Channel ch = ctx.channel();
			context = new HttpContext(ch.remoteHost(), ch.remotePort());
		}
		return context;
	}
	
	public abstract void handle(HttpRequest req, HttpResponse res) throws Exception;
	
	public abstract void handleException(HttpRequest req, HttpResponse res, Throwable t);
	
	
	private static class HttpContext {
		
		HttpRequest request;
		HttpResponse response;
		
		public HttpContext(String host, int port) {
			this.request = new HttpRequest(host, port);
			this.response = new HttpResponse(host, port);
		}
	}
}
