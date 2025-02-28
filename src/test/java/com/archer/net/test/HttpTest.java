package com.archer.net.test;

import com.archer.net.http.client.NativeRequest;

public class HttpTest {
	
	
	public static void baiduTest() {
		NativeRequest.getAsync("https://www.baidu.com/", (res) -> {
			System.out.println(new String(res.getBody()));
		});
	}
	
	public static void knownTest() {
		NativeRequest.getAsync("https://45.24.65.45/", (res) -> {
			System.out.println(new String(res.getBody()));
		});
	}
	
	public static void main(String args[]) {
		knownTest();
	}
}
