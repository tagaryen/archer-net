package com.archer.net.http;

import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.Debugger;
import com.archer.net.handler.Handler;


public abstract class HttpWrappedHandler implements Handler {

    public HttpWrappedHandler() {}
    
	@Override
	public void onAccept(ChannelContext ctx) {
		//nothing
	}
    
	@Override
	public void onConnect(ChannelContext ctx) {
		Channel ch = ctx.channel();
		ch.setAttachment(new HttpContext(ch.remoteHost(), ch.remotePort(), ctx));
	}

	@Override
	public void onRead(ChannelContext ctx, Bytes in) {
		if(in.available() <= 0) {
			return ;
		}

		HttpContext context = (HttpContext) ctx.channel().getAttachment();
		if(context == null) {
			handleException(null, null, new NullPointerException("fetch http context error"));
			return ;
		}
		HttpRequest req = context.request;
		HttpResponse res = context.response;
		byte[] msg = in.readAll();

		try {
			if(req.isEmpty()) {
				req.parse(msg);
				res.setVersion(req.getHttpVersion());
				String connection = req.getHeader("connection");
				if(null != connection) {
					res.setHeader("connection", connection);
				}
			} else {
				req.putContent(msg);
			}
		} catch(Exception e) {
			HttpStatus status;
			if(e instanceof HttpException) {
				status = HttpStatus.valueOf(((HttpException)e).getCode());
			} else {
				status = HttpStatus.BAD_REQUEST;
			}
			res.setVersion(req.getHttpVersion());
			res.setStatus(status);
			res.sendContent(status.getMsg().getBytes());
			return ;
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
			if(HttpRequest.HTTP_10.equals(req.getHttpVersion())) {
				ctx.close();
			}
			reset(req, res);
		}

		try {
		} catch(Exception e) {
			onError(ctx, e);
		}
	}
	
	@Override
	public void onWrite(ChannelContext ctx, Bytes out) {
		ctx.toLastOnWrite(out);
	}

	public void reset(HttpRequest req, HttpResponse res) {
		req.clear();
		res.clear();
	}
	
	@Override
	public void onDisconnect(ChannelContext ctx) {
    	ctx.channel().setAttachment(null);
	}

	@Override
	public void onError(ChannelContext ctx, Throwable t) {
		HttpContext context = (HttpContext) ctx.channel().getAttachment();
		if(context == null) {
			handleException(null, null, new NullPointerException("fetch http context error"));
			return ;
		}
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
	
//	private HttpContext getHttpContext(ChannelContext ctx) {
//		HttpContext context = contextCache.getOrDefault(ctx, null);
//		if(context == null) {
//			Channel ch = ctx.channel();
//			context = new HttpContext(ch.remoteHost(), ch.remotePort(), ctx);
//		}
//		return context;
//	}
	
	public abstract void handle(HttpRequest req, HttpResponse res) throws Exception;
	
	public abstract void handleException(HttpRequest req, HttpResponse res, Throwable t);
	
	
	private static class HttpContext {
		
		HttpRequest request;
		HttpResponse response;
		
		public HttpContext(String host, int port, ChannelContext ctx) {
			this.request = new HttpRequest(host, port);
			this.response = new HttpResponse(ctx);
		}
	}
}
