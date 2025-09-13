package com.archer.net.http;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;
import com.archer.net.util.HexUtil;

import java.nio.charset.StandardCharsets;

public class HttpStreamWriter {

    private ChannelContext ctx;

    public HttpStreamWriter(ChannelContext ctx) {
        this.ctx = ctx;
    }

    public void write(byte[] data) {
        Bytes chunk = new Bytes(data.length + 16);
        String hex = HexUtil.intToHex(data.length);
        chunk.write(("\r\n" + hex + "\r\n").getBytes());
        chunk.write(data);
        ctx.toLastOnWrite(chunk);
    }

    public void end() {
        Bytes end = new Bytes(("\r\n0\r\n\r\n").getBytes());
        ctx.toLastOnWrite(end);
    }
}
