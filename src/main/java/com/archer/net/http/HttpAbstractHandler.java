package com.archer.net.http;

import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.Debugger;
import com.archer.net.handler.Handler;


public abstract class HttpAbstractHandler implements Handler {

    public HttpAbstractHandler() {}
    
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
	public void onRead(ChannelContext ctx) {
		HttpContext context = (HttpContext) ctx.channel().getAttachment();
		if(context == null) {
			ctx.close();
			return ;
		}
		HttpRequest req = context.request;
		HttpResponse res = context.response;
		while(true) {
			int len = ctx.read(context.buf);
			if(len == 0) {
				return ;
			}
			try {
				if(!req.headerParsed()) {
					req.parse(context.buf, len);
					res.setVersion(req.getHttpVersion());
					String connection = req.getHeader("connection");
					if(null != connection) {
						res.setHeader("connection", connection);
					}
				} else {
					req.putContent(context.buf, len);
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
				} catch(Throwable e) {
					handleException(e);
				}
				if(Debugger.enableDebug()) {
					System.out.println("http response, content-length is" + res.getContentLength());
				}
				ctx.close();
				return ;
			}
		}
	}
	
	@Override
	public void onWrite(ChannelContext ctx, byte[] out) {
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
		try {
			handleException(t);
		} catch(Throwable tx) {
			tx.printStackTrace();
		}
	}

	/**
	 * @since 1.5.0 this method will never be called since 1.5.0
	 * */
	@Override
	@Deprecated
	public void onSslCertificate(ChannelContext ctx, byte[] cert) {
		//we do nothing here
	}
	
	public abstract void handle(HttpRequest req, HttpResponse res);
	
	public abstract void handleException(Throwable t);
	
	
	private static class HttpContext {
		
		HttpRequest request;
		HttpResponse response;
		byte[] buf = new byte[4096];
		
		public HttpContext(String host, int port, ChannelContext ctx) {
			this.request = new HttpRequest(host, port);
			this.response = new HttpResponse(ctx);
		}
	}
}
