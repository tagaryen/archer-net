package com.archer.net.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.HandlerList;
import com.archer.net.handler.AbstractWrappedHandler;
import com.archer.net.ssl.ProtocolVersion;
import com.archer.net.ssl.SslContext;
import com.archer.net.test.ChannelTest.MessageA;

public class GMSSLTest {
	
	public static class MessageA {
		public String a;
	}
	
	public static void main(String args[]) {

		SslContext sslctx = new SslContext(true, ProtocolVersion.TLS1_3_VERSION, ProtocolVersion.TLS1_1_VERSION);
		try {
			String basePath = "D:\\projects\\javaProject\\maven\\archer-net\\crt\\gm_cert\\";
			sslctx.trustCertificateAuth(Files.readAllBytes(Paths.get(basePath + "ca.crt")));
			sslctx.useCertificate(Files.readAllBytes(Paths.get(basePath + "server.crt")), Files.readAllBytes(Paths.get(basePath + "server.key")));
			sslctx.useEncryptCertificate(Files.readAllBytes(Paths.get(basePath + "server_en.crt")), Files.readAllBytes(Paths.get(basePath + "server_en.key")));
		} catch (IOException e) {
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
		
		Channel channel = new Channel(sslctx);
		channel.handlerList(new HandlerList().add(handler));
		channel.connect("127.0.0.1", 9607);
		
	}
}
