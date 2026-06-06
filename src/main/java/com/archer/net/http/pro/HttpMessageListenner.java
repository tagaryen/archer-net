package com.archer.net.http.pro;

public interface HttpMessageListenner {

	void handle(HttpRequest req, HttpResponse res);
	
	void handleException(Throwable t);
	
}
