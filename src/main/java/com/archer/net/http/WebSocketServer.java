package com.archer.net.http;

import java.util.ArrayList;
import java.util.List;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;
import com.archer.net.HandlerList;
import com.archer.net.ServerChannel;
import com.archer.net.handler.Handler;
import com.archer.net.ssl.SslContext;

public class WebSocketServer {
	
	private SslContext sslCtx;
	private ServerChannel server;

	private List<WebSocketListenner> wslistenners = new ArrayList<>();
	
	public WebSocketServer() {
		this(null);
	}
	
	public WebSocketServer(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}

    public void addWebSocketListenner(WebSocketListenner wslistenner) {
    	this.wslistenners.add(wslistenner);
    }
    
	public void listen(String host, int port) throws HttpServerException {
		HandlerList handlerList = new HandlerList();
		handlerList.add(new WebSocketHandler());
		if(sslCtx != null) {
			if(sslCtx.crt() == null || sslCtx.key() == null) {
				throw new HttpServerException("certificate and privateKey is required");
			}
			server = new ServerChannel(sslCtx);
		} else {
			server = new ServerChannel();
		}
		server.handlerList(handlerList);
		server.listen(host, port);
	}
	
	public void destroy() {
		server.close();
	}
	
	
	private class WebSocketHandler implements Handler {
		
	    public WebSocketHandler() {}
	    
		@Override
		public void onAccept(ChannelContext ctx) {
			//nothing
		}
	    
		@Override
		public void onConnect(ChannelContext ctx) {
			ctx.addChannelAttachment(new WsContext(ctx));
		}

		@Override
		public void onRead(ChannelContext ctx, Bytes in) {
			
			WsContext wsctx = (WsContext) ctx.getChannelAttachment();
			if(wsctx == null) {
				return ;
			}
			HttpRequest req = wsctx.request;
			HttpResponse res = wsctx.response;
			
			try {
				if(wsctx.wschannel != null) {
					if(wsctx.wschannel.parseWebSocketMessage(in)) {
						wsctx.wslistenner.onMessage(wsctx.wschannel, wsctx.wschannel.resetAndGet());
					} else {
						ctx.close();
						wsctx.wslistenner.onClose(wsctx.wschannel);
					}
				} else {
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
						boolean ok = false;
						for(WebSocketListenner listenner: wslistenners) {
							if(listenner.websocketUriMatch(req.getUri())) {
								wsctx.setWs(listenner);
								ok = true;
								break;
							}
						}
						if(!ok) {
							res.setStatus(HttpStatus.NOT_FOUND);
							res.sendContent("Unknown websocket uri".getBytes());
							ctx.close();
							return ;
						}
						ok = wsctx.wslistenner.setWsResponse(req, res);
						res.sendContent(null);
						if(!ok) {
							res.setStatus(HttpStatus.NOT_FOUND);
							res.sendContent("Unknown websocket uri".getBytes());
							ctx.close();
							return ;
						}
						wsctx.wslistenner.onConnected(wsctx.wschannel);
					}
				}
			} catch(Exception e) {
				wsctx.wslistenner.onError(wsctx.wschannel, e);
			}
		}
		
		@Override
		public void onWrite(ChannelContext ctx, Bytes out) {}
		
		@Override
		public void onDisconnect(ChannelContext ctx) {
			WsContext wsctx = (WsContext) ctx.getChannelAttachment();
			if(wsctx == null) {
				return ;
			}
			wsctx.wslistenner.onClose(wsctx.wschannel);
		}

		@Override
		public void onError(ChannelContext ctx, Throwable t) {
			WsContext wsctx = (WsContext) ctx.getChannelAttachment();
			if(wsctx == null) {
				return ;
			}
			wsctx.wslistenner.onError(wsctx.wschannel, t);
		}
		
		@Override
		public void onSslCertificate(ChannelContext ctx, byte[] cert) {
			//we do nothing here
		}
	}
	

	private static class WsContext {
		HttpRequest request;
		HttpResponse response;
		ChannelContext ctx;
		WebSocketListenner wslistenner;
		WebSocketChannel wschannel;
		
		public WsContext(ChannelContext ctx) {
			this.ctx = ctx;
			this.request = new HttpRequest(ctx.channel().remoteHost(), ctx.channel().remotePort());
			this.response = new HttpResponse(ctx);
		}
		
		public void setWs(WebSocketListenner wslistenner) {
			this.wslistenner = wslistenner;
			this.wschannel = new WebSocketChannel(this.ctx);
		}
	}
}
