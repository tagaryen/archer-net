# archer-net
network framework written native c, support latest openssl(gmssl) 1.3 
support encrypted key and encrypted certificate  
maven:  
``` maven  
    <dependency>  
        <groupId>io.github.tagaryen</groupId>  
        <artifactId>archer-net</artifactId>  
        <version>1.5.4</version>  
    </dependency>  
```  
## Benchmark (32GB RAM 8 cores)  

### spring 3.x  (enable virtual threads) JDK21
``` java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class Controller {
    @GetMapping("/test")
    public String test() {
        return "nihaow";
    }
}
```
#### 1: memmory
memory cost: 1022MB (200000 requests)  
#### 2: 10000 Concurrency
| Item | Value |
| --- | --- |
| Concurrency Level | 10000 |
| Time taken for tests | 18.750 seconds |
| Complete requests | 100000 |
| Failed requests | 0 |
| Requests per second | 5333.40 [#/sec] (mean) |
| Time per request | 1874.978 [ms] (mean) |
 
#### 3: 20000 Concurrency
| Item | Value |
| --- | --- |
| Concurrency Level | 20000 |
| Time taken for tests | 386.288 seconds |
| Complete requests | 200000 |
| Failed requests |9105 |
| Requests per second | 517.75 [#/sec] (mean) |
| Time per request | 38628.758 [ms] (mean) | 


### archer-net HttpServer (based on libevent, directBuffer) JDK21  
``` java  
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;
import com.archer.net.http.HttpServer;
import com.archer.net.http.HttpServerException;

import com.archer.net.http.HttpAbstractHandler;

public class App {
    
    public static void main(String args[]) {
        HttpServer server = new HttpServer();
        server.setThreadNum(3);
        server.listen("0.0.0.0", 9610, new HttpAbstractHandler() {
            @Override
            public void handle(HttpRequest req, HttpResponse res) {
                res.sendContent("nihaow".getBytes());
           }

           @Override
           public void handleException(Throwable t) {
               t.printStackTrace();
           }
       });
    }
}
```
#### 1: memory
memory cost: 1894MB  (200000 requests) 
#### 2: 10000 Concurrency
| Item | Value |
| --- | --- |
| Concurrency Level | 10000 |
| Time taken for tests | 7.545 seconds |
| Complete requests | 100000 |
| Failed requests | 0 |
| Requests per second | 13254.49 [#/sec] (mean) |
| Time per request | 754.462 [ms] (mean) |
#### 3: 20000 Concurrency
| Item | Value |
| --- | --- |
| Concurrency Level | 20000 |
| Time taken for tests | 38.493 seconds |
| Complete requests | 200000 |
| Failed requests | 0 |
| Requests per second | 5195.73 [#/sec] (mean) |
| Time per request | 3849.312 [ms] (mean) |


### archer-net  ProHttpServer (based on native libevent) JDK21
``` java
import com.archer.net.http.pro.HttpMessageListenner;
import com.archer.net.http.pro.HttpRequest;
import com.archer.net.http.pro.HttpResponse;
import com.archer.net.http.pro.HttpServer;


public class App {

    public static void main(String args[]) {
        HttpServer server = new HttpServer();
        server.setThreadNum(3);
        server.addMessageListenner(new HttpMessageListenner() {
            @Override
            public void handle(HttpRequest req, HttpResponse res) {
                res.sendBody("nihaow".getBytes());
            }
            @Override
            public void handleException(Throwable t) {
                t.printStackTrace();
            }
        });
        server.listen("0.0.0.0", 9610);
    }
}
```
#### 1: memory  
memory cost: 255MB (200000 requests)  
#### 2: 10000 Concurrency
| Item | Value |
| --- | --- |
| Concurrency Level | 10000 |
| Time taken for tests | 5.857 seconds |
| Complete requests | 100000 |
| Failed requests | 0 |
| Requests per second | 17072.92 [#/sec] (mean) |
| Time per request | 585.723 [ms] (mean) | 
#### 3: 20000 Concurrency
| Item | Value |
| --- | --- |
| Concurrency Level | 20000 |
| Time taken for tests | 18.679 seconds |
| Complete requests | 200000 |
| Failed requests | 0 |
| Requests per second | 10707.05 [#/sec] (mean) |
| Time per request | 1867.929 [ms] (mean) |  



### netty 4.1.108.Final
``` java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

public class HttpServer {
    private final int port;
    public HttpServer(int port) {
        this.port = port;
    }
    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(3);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new HttpServerCodec(),
                                    new HttpObjectAggregator(65536),
                                    new HttpServerHandler()
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer("nihaow", CharsetUtil.UTF_8)
            );
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (!keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, "close");
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
                ctx.writeAndFlush(response);
            }
        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
    public static void main(String[] args) throws Exception {
        int port = 9610;
        new HttpServer(port).start();
    }
}
```
****** jdk8  compare to jdk21    
| Item | Value |
| --- | --- |
| Concurrency Level | 10000 |
| Time taken for tests | 17.371 seconds |
| Complete requests | 100000 |
| Failed requests | 0 |
| Requests per second | 5756.59 [#/sec] (mean) |
| Time per request | 1737.139 [ms] (mean) | 
***** jdk8  

#### 1: memory  
memory cost: 671MB (200000 requests) 
#### 2: 10000 Concurrency
| Item | Value |
| --- | --- |
| Concurrency Level | 10000 |
| Time taken for tests | 8.980 seconds |
| Complete requests | 100000 |
| Failed requests | 0 |
| Requests per second | 11136.34 [#/sec] (mean) |  
| Time per request | 897.961 [ms] (mean) |  
#### 3: 20000 Concurrency
| Item | Value |
| --- | --- |
| Concurrency Level | 20000 |
| Time taken for tests | 44.096 seconds |
| Complete requests | 200000 |
| Failed requests | 0 |
| Requests per second | 4535.60 [#/sec] (mean) |  
| Time per request | 4409.560 [ms] (mean) |   

## gmssl examples 
``` java 
    String root = getCurrentWorkDir();

    String ca = root + "crt/ca.crt";
    String key = root + "crt/server.key";
    String crt = root + "crt/server.crt";
    String enkey = root + "crt/server_en.key";
    String encrt = root + "crt/server_en.crt";
    String clikey = root + "crt/cli.key";
    String clicrt = root + "crt/cli.crt";
    String enclikey = root + "crt/cli_en.key";
    String enclicrt = root + "crt/cli_en.crt";

    //event loop
    HandlerList handlerlist = new HandlerList();
    handlerlist.add(
        new FrameReadHandler(0, 2, 2),
        new AbstractWrappedHandler<Msg>() {
        @Override
        public void onMessage(ChannelContext ctx, Msg input) {
            System.out.println("Handler: receive: " + input.data);
        }
        @Override
        public Msg decode(Bytes in) {
            Msg msg = new Msg();
            msg.decode(in);
            return msg;
        }
            
        @Override
        public void onConnect(ChannelContext ctx) {
            Channel channel = ctx.channel();
            String host = channel.remoteHost();
            int port = channel.remotePort();
            System.out.println("Handler: " + host+":"+port+" connected.");
            ctx.write(new Msg());
        }
        @Override
        public void onDisconnect(ChannelContext ctx) {
            Channel channel = ctx.channel();
            String host = channel.remoteHost();
            int port = channel.remotePort();
            System.out.println("Handler: " + host+":"+port+" disconnected.");
        }
        @Override
        public void onError(ChannelContext ctx, Throwable t) {
            t.printStackTrace();
        }
        @Override
        public void onAccept(ChannelContext ctx) {
            Channel channel = ctx.channel();
            String host = channel.remoteHost();
            int port = channel.remotePort();
            System.out.println(host+":"+port+" accepted.");
        }
    });
    try {
        //ca crt
        byte[] caBytes= Files.readAllBytes(Paths.get(ca));

        //server crt
        byte[] crtBytes= Files.readAllBytes(Paths.get(crt));
        byte[] keyBytes= Files.readAllBytes(Paths.get(key));
        byte[] enCrtBytes= Files.readAllBytes(Paths.get(encrt));
        byte[] enKeyBytes= Files.readAllBytes(Paths.get(enkey));
        //client crt
        byte[] clicrtBytes= Files.readAllBytes(Paths.get(clicrt));
        byte[] clikeyBytes= Files.readAllBytes(Paths.get(clikey));
        byte[] enClicrtBytes= Files.readAllBytes(Paths.get(enclicrt));
        byte[] enClikeyBytes= Files.readAllBytes(Paths.get(enclikey));

        //start a gmssl server
        System.out.println("start server.");
        SslContext opt = new SslContext(false, true).trustCertificateAuth(caBytes).useCertificate(crtBytes, keyBytes).useEncryptCertificate(enClicrtBytes, enClikeyBytes);
        ServerChannel server = new ServerChannel(opt);
        server.handlerList(handlerlist);
        server.listen("127.0.0.1", 8081);

        // wait
        Thread.sleep(1000);

        //start a gmssl client
        SslContext cliopt = new SslContext(true, false).trustCertificateAuth(caBytes).useCertificate(clicrtBytes, clikeyBytes).useEncryptCertificate(enClicrtBytes, enClikeyBytes);
        Channel cli = new Channel(cliopt);
        cli.handlerList(handlerlist);
        cli.connect("127.0.0.1", 8081);

        //wait
        Thread.sleep(2000);

        //close client
        cli.close();
        
    } catch (Exception e) {
        e.printStackTrace();
    }
```
## gmssl https server examples 
``` java
    String root = getCurrentWorkDir();

    String ca = root + "crt/ca.crt";
    String key = root + "crt/server.key";
    String crt = root + "crt/server.crt";
    String enkey = root + "crt/server_en.key";
    String encrt = root + "crt/server_en.crt";
    
    byte[] crtBytes= Files.readAllBytes(Paths.get(crt));
    byte[] keyBytes= Files.readAllBytes(Paths.get(key));
    byte[] enCrtBytes= Files.readAllBytes(Paths.get(encrt));
    byte[] enKeyBytes= Files.readAllBytes(Paths.get(enkey));

    System.out.println("start server.");
    
    HandlerList handlerlist = new HandlerList();
    handlerlist.add(new HttpWrappedHandler() {
        @Override
        public void handle(HttpRequest req, HttpResponse res) throws Exception {
            String uri = req.getUri();
            res.setContentType(ContentType.APPLICATION_JSON);
            if(uri.equals("/nihao")) {
            res.setStatus(HttpStatus.OK);
            res.sendContent("{\"nihao\":\"ni\"}".getBytes());
        } else {
            res.setStatus(HttpStatus.NOT_FOUND);
            res.sendContent("{\"nihao\":\"ni\"}".getBytes());
        }
    }

    @Override
    public void handleException(HttpRequest req, HttpResponse res, Throwable t) {
        t.printStackTrace();
        String body = "{" +
            "\"server\": \"Java/"+System.getProperty("java.version")+"\"," +
            "\"time\": \"" + LocalDateTime.now().toString() + "\"," +
            "\"status\": \"" + HttpStatus.SERVICE_UNAVAILABLE.getStatus() + "\"" +
            "}";

        res.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
        res.setContentType(ContentType.APPLICATION_JSON);
        res.sendContent(body.getBytes());
        }
    });

    SslContext opt = new SslContext().trustCertificateAuth(caBytes).useCertificate(crtBytes, keyBytes);
    ServerChannel server = new ServerChannel(opt);
    server.handlerList(handlerlist);
    server.listen(8080);
```
or 
``` java
    String root = getCurrentWorkDir();

    String ca = root + "crt/ca.crt";
    String key = root + "crt/server.key";
    String crt = root + "crt/server.crt";
    String enkey = root + "crt/server_en.key";
    String encrt = root + "crt/server_en.crt";
    try {
        byte[] caBytes= Files.readAllBytes(Paths.get(ca));
        byte[] crtBytes= Files.readAllBytes(Paths.get(crt));
        byte[] keyBytes= Files.readAllBytes(Paths.get(key));
        byte[] enCrtBytes= Files.readAllBytes(Paths.get(encrt));
        byte[] enKeyBytes= Files.readAllBytes(Paths.get(enkey));
        
        System.out.println("start server.");
        SslContext opt = new SslContext(false).trustCertificateAuth(caBytes).useCertificate(crtBytes, keyBytes).useEncryptCertificate(enCrtBytes, enKeyBytes);
        HttpServer server = new HttpServer(opt);
        server.listen("127.0.0.1", 8081, new HttpWrappedHandler() {

        @Override
        public void handle(HttpRequest req, HttpResponse res) throws Exception {
            //do something here
        }

        @Override
        public void handleException(HttpRequest req, HttpResponse res, Throwable t) {
            t.printStackTrace();
        }
        });
    } catch(Exception e) {
        e.printStackTrace();
    }    
```

## http(s) client examples 
``` java
    NativeResponse baidu = NativeRequest.get("https://www.baidu.com");
    System.out.println(baidu.getStatus());
    System.out.println(new String(baidu.getBody()));

    //request localhost (above server) 
    String root = getCurrentWorkDir();
    String ca = root + "crt/ca.crt";
    NativeResponse localhost = NativeRequest.get("https://127.0.0.1:8080/hihao", new NativeRequest.Options().caPath(ca));
    System.out.println(localhost.getStatus());
    System.out.println(new String(localhost.getBody()));
``` 
