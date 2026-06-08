package com.archer.net;

import com.archer.net.http.pro.HttpMessageListenner;
import com.archer.net.http.pro.HttpRequest;
import com.archer.net.http.pro.HttpResponse;
import com.archer.net.http.pro.HttpServer;

public class App {
	
	public static void main(String args[]) {
		HttpServer http = new HttpServer();
		http.setThreadNum(3);
		http.addMessageListenner(new HttpMessageListenner() {

			@Override
			public void handle(HttpRequest req, HttpResponse res) {
				res.sendBody("nihaow".getBytes());
			}

			@Override
			public void handleException(Throwable t) {}
			
		});
		
		http.listen("0.0.0.0", 9610);
	}

}
