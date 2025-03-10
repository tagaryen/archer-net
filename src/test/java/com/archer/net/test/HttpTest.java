package com.archer.net.test;

import com.archer.net.http.client.NativeRequest;

public class HttpTest {
	
	
	public static void baiduTest() {
		NativeRequest.getAsync("https://www.baidu.com/", (res) -> {
			System.out.println("baidu res = " + res.getStatus());
		}, null);
		NativeRequest.getAsync("https://www.google.com/", (res) -> {
			System.out.println("cn.bing res = " + res.getStatus());
		}, null);
		NativeRequest.getAsync("https://www.bing.com/", (res) -> {
			System.out.println("www.bing res = " + res.getStatus());
		}, null);
	}
	
	public static void knownTest() {
		NativeRequest.getAsync("https://45.24.65.45/", (res) -> {
			System.out.println(new String(res.getBody()));
		}, null);
	}
	
	public static void main(String args[]) {
		baiduTest();
	}
}
