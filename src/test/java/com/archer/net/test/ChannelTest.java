package com.archer.net.test;

import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.HandlerList;
import com.archer.net.ServerChannel;
import com.archer.net.handler.AbstractWrappedHandler;

public class ChannelTest {
	
	public static class MessageA {
		public String name;
	}
	
	public static void main(String args[]) {
		ServerChannel server = new ServerChannel();
		server.handlerList(new HandlerList().add(new AbstractWrappedHandler<MessageA>(){
			
			@Override
			public void onConnect(ChannelContext ctx) {}

			@Override
			public void onDisconnect(ChannelContext ctx) {}

			@Override
			public void onError(ChannelContext ctx, Throwable t) {}

			@Override
			public void onMessage(ChannelContext ctx, MessageA input) {
				System.out.println("server receive: " + input.name);
			}

			@Override
			public MessageA decodeInput(Bytes in) {
				int len = in.readInt32();
				MessageA a = new MessageA();
				a.name = new String(in.read(len));
				return a;
			}}));
		server.listen("127.0.0.1", 9617);
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		AbstractWrappedHandler<MessageA> handler = new AbstractWrappedHandler<MessageA>(){
			@Override
			public void onConnect(ChannelContext ctx) {
				Bytes out = new Bytes();
				byte[] name = "xuyihahaha".getBytes();
				out.writeInt32(name.length);
				out.write(name);
				ctx.write(out);
			}

			@Override
			public void onDisconnect(ChannelContext ctx) {
				System.out.println("client disconnect");
			}

			@Override
			public void onError(ChannelContext ctx, Throwable t) {}

			@Override
			public void onMessage(ChannelContext ctx, MessageA input) {}

			@Override
			public MessageA decodeInput(Bytes in) {return null;}
		};
		
		Channel ch1 = new Channel();
		ch1.handlerList(new HandlerList().add(handler));
		

		Channel ch2 = new Channel();
		ch2.handlerList(new HandlerList().add(handler));
		
		
		ch1.connect("127.0.0.1", 9617);
		ch2.connect("127.0.0.1", 9617);
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		server.close();
		
	}
}
