package cn.vonfly;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.BootstrapConfig;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetAddress;
import java.net.URI;

public class DirectConnectionHandler extends SimpleChannelInboundHandler<HttpMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
        DefaultHttpRequest defaultHttpRequest = (DefaultHttpRequest) msg;
        HttpMethod method = defaultHttpRequest.method();
        String uri = defaultHttpRequest.uri();
        String host = defaultHttpRequest.headers().get(HttpHeaderNames.HOST);
        final Channel channel = ctx.channel();
        ReferenceCountUtil.retain(defaultHttpRequest);
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        //outBound httpMessage->bytes
                        ch.pipeline().addFirst(new HttpRequestEncoder());
                        //inBound bytes ProxyRelayingChannel->DirectConnectionHandler
                        ch.pipeline().addLast(new ProxyRelayingChannelHandler(channel));
                    }
                });
        InetAddress byName = InetAddress.getByName(host);
        bootstrap.connect(byName, 80).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    future.channel().writeAndFlush(defaultHttpRequest);
                } else {
                    future.cause().printStackTrace();
                    System.out.println("连接远程服务器失败"+byName);
                    channel.close();
                    eventLoopGroup.shutdownGracefully();
                }
            }
        });

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
