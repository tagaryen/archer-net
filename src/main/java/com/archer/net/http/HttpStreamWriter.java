package com.archer.net.http;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;
import com.archer.net.util.HexUtil;

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
        ctx.toLastOnWrite(chunk.readAll());
    }

    public void end() {
        ctx.toLastOnWrite(("\r\n0\r\n\r\n").getBytes());
    }
}
