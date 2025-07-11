package com.archer.net.test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.archer.net.http.client.NativeRequest;
import com.archer.net.http.client.NativeResponse;
import com.archer.net.ssl.SslContext;

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
	
	public static void sslTest() {
		SslContext ctx = new SslContext(true, false);
		NativeRequest.Options opts = new NativeRequest.Options();
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Token","OKCjKBw6I8ONFj2ZCrjNQJQKrsjQRc3egsVdPfMbLbUsPK7dmbh5B/OXZdyKOLPIUQwen7So1WIPooGwnRVeSYI/WUhVmkq2lWC4BXhx5UJKhnD2Uf22K//AUqYrbvGScgI08kjgnAq0fIvGnNqhL/JpSvjn2YhfB82SHIaYBJStpUmtVFkj+JHPOzJGJvaFlha/oel5p/wgww0rOxUOgq4EZOiIirh+YdsVCVOSSQUNHiCMLY2ZYH5eZEsbPyMtCCQnHc6R7vVs9lY46pcFFDBcd/VTLsWpt0WVmYxY3Iv1GDda7fdFNwEqxIP7vq1tdR8HWJR7BfGOHaByuccjBA==");
		headers.put("Workspace", "2025061300010400001239");
		headers.put("Content-Type", "application/json");
		opts.headers(headers).sslcontext(ctx);
		String body = "{\"content\":{\"workspaceId\":\"2025061300010400001239\"},\"method\":\"gaia.openapi.mine.workspace.getOne\"}";
		
		NativeResponse res = NativeRequest.post("https://gaiac-104.base.trustbe.cn/gaia/v1/janus/invoke/v1", body.getBytes(StandardCharsets.UTF_8), opts);
		System.out.println(new String(res.getBody(), StandardCharsets.UTF_8));
	}
	
	public static void main(String args[]) {
//		NativeRequest.getAsync("http://10.32.122.172:9610/nihao/hhh", (res) -> {
//			System.out.println("9610 *****");
//		}, null);
		NativeRequest.getAsync("https://www.zhihu.com", (res) -> {
			System.out.println(new String(res.getBody()));
		}, null);
//		sslTest();
	}
}
