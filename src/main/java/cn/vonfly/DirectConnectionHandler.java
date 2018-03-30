package cn.vonfly;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetAddress;

public class DirectConnectionHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        byte addressType = byteBuf.readByte();
        SocksAddressType socksAddressType = SocksAddressType.valueOf(addressType);
        int port;
        String host = null;
        switch (socksAddressType) {
            case IPv4:
                host = SocksCommonUtils.intToIp(byteBuf.readInt());
                break;
            case DOMAIN:
                byte len = byteBuf.readByte();
                host = byteBuf.toString(byteBuf.readerIndex(), len, CharsetUtil.US_ASCII);
                byteBuf.skipBytes(len);
                break;
            case IPv6:
                byte[] bytes = new byte[16];
                byteBuf.readBytes(bytes);
                host = SocksCommonUtils.ipv6toStr(bytes);
                break;
        }
        port = byteBuf.readUnsignedShort();
        final Channel channel = ctx.channel();
        ReferenceCountUtil.retain(byteBuf);
        Bootstrap bootstrap = new Bootstrap();
        final ByteBuf defaultHttpRequest = byteBuf.readBytes(byteBuf.readableBytes());
        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
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
        final InetAddress byName = InetAddress.getByName(host);
        bootstrap.connect(host, port).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    future.channel().writeAndFlush(defaultHttpRequest);
                } else {
                    future.cause().printStackTrace();
                    System.out.println("连接远程服务器失败" + byName);
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
