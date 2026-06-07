package com.archer.net.http;

import java.util.ArrayList;
import java.util.List;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;


public abstract class HttpUpgradeHandler extends HttpAbstractHandler {

	private List<WebSocketListenner> wslistenners = new ArrayList<>();
	
    public HttpUpgradeHandler() {}
    
    public void addWebSocketListenner(WebSocketListenner wslistenner) {
    	this.wslistenners.add(wslistenner);
    }
    
	@Override
	public void onConnect(ChannelContext ctx) {
		ctx.addChannelAttachment(new HttpContext(ctx));
	}

	@Override
	public void onRead(ChannelContext ctx) {
		HttpContext context = (HttpContext) ctx.getChannelAttachment();
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
			if(context.iswebsocket) {
				try {
					if(context.wschannel.parseWebSocketMessage(new Bytes(context.buf, 0, len))) {
						context.wslistenner.onMessage(context.wschannel, context.wschannel.resetAndGet());
					} else {
						ctx.close();
						context.wslistenner.onClose(context.wschannel);
					}
				} catch (Exception e) {
					context.wslistenner.onError(context.wschannel, e);
				}
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
				e.printStackTrace();
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
				if(!context.iswebsocket) {
					checkWebSocket(context);
				}
				if(context.iswebsocket) {
					boolean ok = context.wslistenner.setWsResponse(req, res);
					res.sendContent(null);
					if(!ok) {
						ctx.channel().close();
						return ;
					}
					context.wslistenner.onConnected(context.wschannel);
				} else {
					try {
						handle(req, res);
					} catch(Throwable e) {
						handleException(e);
					}
					req.clear();
					res.clear();
//					ctx.close();
				}
			}
		}
	}
	
	@Override
	public void onDisconnect(ChannelContext ctx) {
		HttpContext context = (HttpContext) ctx.getChannelAttachment();
		if(context == null) {
			return ;
		}
		if(context.iswebsocket) {
			context.wslistenner.onClose(context.wschannel);
		}
	}

	@Override
	public void onError(ChannelContext ctx, Throwable t) {
		try {
			handleException(t);
		} catch(Exception tx) {
			tx.printStackTrace();
		}
	}
	
	private void checkWebSocket(HttpContext context) {
		for(WebSocketListenner wslistenner: this.wslistenners) {
			if(wslistenner.websocketUriMatch(context.request.getUri())) {
				context.setWs(wslistenner);
				return ;
			}
		}
	}
	
	
	private static class HttpContext {
		HttpRequest request;
		HttpResponse response;
		ChannelContext ctx;
		byte[] buf = new byte[4096];
		boolean iswebsocket;
		WebSocketListenner wslistenner;
		WebSocketChannel wschannel;
		
		public HttpContext(ChannelContext ctx) {
			this.ctx = ctx;
			this.request = new HttpRequest(ctx.channel().remoteHost(), ctx.channel().remotePort());
			this.response = new HttpResponse(ctx);
			this.iswebsocket = false;
		}
		
		public void setWs(WebSocketListenner wslistenner) {
			this.iswebsocket = true;
			this.wslistenner = wslistenner;
			this.wschannel = new WebSocketChannel(this.ctx);
		}
	}
}
