package com.archer.net;

import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;
import com.archer.net.http.HttpServer;
import com.archer.net.http.HttpServerException;
import com.archer.net.http.HttpAbstractHandler;

public class App {

	public static void main(String args[]) {
		HttpServer server = new HttpServer();
		server.setThreadNum(12);
		try {
			server.listen("0.0.0.0", 9610, new HttpAbstractHandler() {
				@Override
				public void handle(HttpRequest req, HttpResponse res) {
					res.sendContent("nihaow".getBytes());
				}

				@Override
				public void handleException(HttpRequest req, HttpResponse res, Throwable t) {
					t.printStackTrace();
				}});
		} catch (HttpServerException e) {
			e.printStackTrace();
		}
	}
}
