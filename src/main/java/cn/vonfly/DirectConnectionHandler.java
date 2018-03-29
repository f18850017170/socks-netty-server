package cn.vonfly;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;

public class DirectConnectionHandler extends SimpleChannelInboundHandler<HttpMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
        DefaultHttpRequest defaultHttpRequest = (DefaultHttpRequest) msg;
        HttpMethod method = defaultHttpRequest.method();
        String uri = defaultHttpRequest.uri();
        String host = defaultHttpRequest.headers().get(HttpHeaderNames.HOST);

    }
}
