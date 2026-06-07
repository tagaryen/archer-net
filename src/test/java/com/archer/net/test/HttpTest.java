package com.archer.net.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archer.net.Bytes;
import com.archer.net.http.*;
import com.archer.net.http.client.NativeRequest;
import com.archer.net.http.client.NativeResponse;
import com.archer.net.http.multipart.FormData;
import com.archer.net.http.multipart.Multipart;
import com.archer.net.http.multipart.MultipartParser;
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
	public static void httpServer() {
		HttpServer server = new HttpServer();
		HttpUpgradeHandler handler = new HttpUpgradeHandler() {

			@Override
			public void handle(HttpRequest req, HttpResponse res) {
				System.out.println("body = "+new String(req.getContent(), StandardCharsets.UTF_8));
//				HttpStreamWriter writer = res.streamWriter();
//				writer.write("nihaowa".getBytes(StandardCharsets.UTF_8));
//				writer.end();
				res.sendContent("{\"nihao\":\"aaaa\"}".getBytes());
			}

			@Override
			public void handleException(Throwable t) {
				t.printStackTrace();
			}
		};
		handler.addWebSocketListenner(new WebSocketListenner("/wstest") {

			public void onConnected(WebSocketChannel wsChannel) {
				System.out.println("on connected ws");
				wsChannel.send(new Bytes("nihao client".getBytes()));
			}
			public void onMessage(WebSocketChannel wsChannel, Bytes in) {
				System.out.println("on message ws: " + new String(in.readAll()));
			}
			public void onError(WebSocketChannel wsChannel, Throwable t) {
				
			}
			public void onClose(WebSocketChannel wsChannel) {
				
			}
		});
		try {
			server.listen("127.0.0.1", 9617, handler);
		} catch (HttpServerException e) {
			e.printStackTrace();
		}
	}
	
	public static void uploadFile() {
		List<Multipart> parts = new ArrayList<>();
		parts.add(new Multipart("Node-Id", "alice"));
		try {
			parts.add(new Multipart("file", "data1029.csv", Files.readAllBytes(Paths.get("D:/da.csv"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		String boundary = MultipartParser.generateBoundary();
		String body = MultipartParser.generateMultipartBody(parts, boundary);

		NativeRequest.Options opts = new NativeRequest.Options();
		HashMap<String, String> headers = new HashMap<>();
		headers.put("User-Token","16fa3b5d0cf1422589909b282328ea0a");
		headers.put("Content-Type", MultipartParser.MULTIPART_HEADER + boundary);
		opts.headers(headers);
		
		NativeResponse res = NativeRequest.post("http://10.32.122.172:32614/api/v1alpha1/data/upload", body.getBytes(StandardCharsets.UTF_8), opts);
		System.out.println(new String(res.getBody(), StandardCharsets.UTF_8));
	}
	
	
	public static void listTest() {

		NativeRequest.Options opts = new NativeRequest.Options();
		HashMap<String, String> headers = new HashMap<>();
		headers.put("User-Token","16fa3b5d0cf1422589909b282328ea0a");
		headers.put("Content-Type", "application/json");
		opts.headers(headers);
		
		NativeResponse res = NativeRequest.post("http://10.32.122.172:32614/api/v1alpha1/inst/node/list", new byte[0], opts);
		System.out.println(new String(res.getBody(), StandardCharsets.UTF_8));
	}
	
	public static void streamUploadFile() {
		FormData form = new FormData();
		form.put("你", "好");
		try {
			form.put("file", new File("D:/install.sh"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		NativeRequest.streamRequest("POST", "http://127.0.0.1:8080/asdasd", form, null, 
			res -> {
				System.out.println(res.getContentType());
			}, 
			data -> {
				System.out.println("*******");
				System.out.println(new String(data.readAll()));
			}
		);
	}
	
	
    public static void uploadTest()
    {
    	FormData form = new FormData();
    	try {
			form.put("file", "ReadMe.ico", Files.readAllBytes(Paths.get("D:\\projects\\cppProject\\archer_multiples\\icon.ico")));
		} catch (IOException e) {
			e.printStackTrace();
		}
    	String authKey = "e11!^cvvcs$a1@ad";
    	String uri = "/archer/file-api/file-upload";
    	String t = System.currentTimeMillis() + "";
    	byte[] sig = SM4Util.encrypt((uri+t).getBytes(), authKey.getBytes());
    	//WHCWsxX9qXMtVZDCTEE8NGsCEW4lTJMlPFL5xqwL YKoOka532GwO4/gBcy1Kxua
    	//WHCWsxX9qXMtVZDCTEE8NGsCEW4lTJMlPFL5xqwL+YKoOka532GwO4/gBcy1Kxua
    	try {
			System.out.println("sig="+URLEncoder.encode(Base64.getEncoder().encodeToString(sig), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String signature = "";
    	try {
			signature = URLEncoder.encode(Base64.getEncoder().encodeToString(sig), "UTF-8");
		} catch (UnsupportedEncodingException ignore) {}
    	String url = "http://10.32.122.172:9617" + uri + "?t="+t+"&signature="+signature+"&filename=ReadMe.ico";
    	System.out.println(url);
    	
    	NativeResponse res = NativeRequest.request("POST", url, form, null);
    	System.out.println(new String(res.getBody()));
    	
    }
	
	public static void main(String args[]) {
		

//    	String authKey = "e11!^cvvcs$a1@ad";
//    	String uri = "/archer/file-api/file-view";
//    	String nonce = "1234567890123456";
//    	byte[] sig = SM4Util.encrypt((uri+nonce).getBytes(), authKey.getBytes());
//    	//WHCWsxX9qXMtVZDCTEE8NGsCEW4lTJMlPFL5xqwL YKoOka532GwO4/gBcy1Kxua
//    	//WHCWsxX9qXMtVZDCTEE8NGsCEW4lTJMlPFL5xqwL+YKoOka532GwO4/gBcy1Kxua
//    	try {
//			System.out.println("sig="+URLEncoder.encode(Base64.getEncoder().encodeToString(sig), "UTF-8"));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    	String signature = "";
//    	try {
//			signature = URLEncoder.encode(Base64.getEncoder().encodeToString(sig), "UTF-8");
//		} catch (UnsupportedEncodingException ignore) {}
//    	String url = "http://127.0.0.1:9617" + uri + "?nonce="+nonce+"&signature="+signature+"&filename=ReadMe.md";
//    	System.out.println(url);
		
//		uploadTest();
		
//		NativeRequest.getAsync("https://www.aliyun.com", (res) -> {
//			try {
//				Files.write(Paths.get("E:/ali.html"), res.getBody());
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}, null);
//		NativeRequest.getAsync("https://www.zhihu.com", (res) -> {
//			System.out.println(new String(res.getBody()));
//		}, null);
//		sslTest();
		
		httpServer();
//		uploadFile();
//		listTest();
//		streamUploadFile();
	}
}
