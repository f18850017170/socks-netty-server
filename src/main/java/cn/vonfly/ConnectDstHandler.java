package cn.vonfly;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.net.InetAddress;

/**
 * 连接到目标地址dst
 */
public class ConnectDstHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final byte SHAKE_HAND_SUCC = 0x07;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        byte addressType = byteBuf.readByte();
        SocksAddressType socksAddressType = SocksAddressType.valueOf(addressType);
        if (SocksAddressType.UNKNOWN == socksAddressType) {
            int len = byteBuf.readableBytes();
            System.out.println("未知的协议类型,len=" + len + ",capacity=" + byteBuf.capacity());
            byte[] array = new byte[len];
            if (byteBuf.hasArray()) {
                array = byteBuf.array();
            } else {//非数组支撑，是直接缓冲区
                //getxx操作不会移动 readIndex
                byteBuf.getBytes(byteBuf.readerIndex(), array);
            }
            System.out.println("未知的协议类型，请求信息:" + new String(array));
            ctx.writeAndFlush(HttpResponseStatus.NOT_FOUND);
            return;
        }

        final String host;
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
            default:
                host = null;
        }

        final int port = byteBuf.readUnsignedShort();
        System.out.println(Thread.currentThread().getName() + "接受请求,dest.host=" + host + ",dest.port=" + port + ",请求数据len=" + byteBuf.readableBytes() + ",dst.address.info.len=" + byteBuf.readerIndex());
        final Channel client2ProxyServerChannel = ctx.channel();//客户单连接到代理服务器的channel
        ReferenceCountUtil.retain(byteBuf);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        //outBound httpMessage->bytes
                        //inBound bytes ProxyRelayingChannel->DirectConnectionHandler
                        ch.pipeline().addLast(new ProxyRelayingChannelHandler(client2ProxyServerChannel));
                    }
                });
        final InetAddress byName = InetAddress.getByName(host);


        final Promise<Channel> promise = ctx.executor().newPromise();
        bootstrap.connect(host, port).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                //连接到dst的future
                if (future.isSuccess()) {
                    //连接成功通过promise传递dstChannel
                    promise.setSuccess(future.channel());
                    System.out.println(Thread.currentThread().getName() + "数据写入dst.address.channel,len=");
                } else {
                    future.cause().printStackTrace();
                    System.out.println("连接远程服务器失败" + byName + ",host=" + host);
                    client2ProxyServerChannel.close();
                }
            }
        });
        promise.addListener(new GenericFutureListener<Future<Channel>>() {

            @Override
            public void operationComplete(Future<Channel> future) throws Exception {
                if (future.isSuccess()) {
                    Channel dstChannel = future.getNow();
                    //TODO 此时移除，不能保证已有其他请求已传输到该handler并且被处理过，导致未知协议的出现
                    //TODO 传输DST地址内容，应该在连接建立时处理完成
                    client2ProxyServerChannel.pipeline().remove(ConnectDstHandler.class);
                    client2ProxyServerChannel.pipeline().addFirst(new DirectConnectionHandler(dstChannel));
                    client2ProxyServerChannel.writeAndFlush(Unpooled.copiedBuffer(new byte[]{SHAKE_HAND_SUCC})).addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            if (future.isSuccess()) {
                                System.out.println("确认握手协议=连接建立完毕===准备开始数据传输");
                            } else {

                                System.out.println("确认握手协议失败连==关闭连接");
                                future.cause().printStackTrace();
                                client2ProxyServerChannel.close();
                                dstChannel.close();
                            }
                        }
                    });
                } else {
                    //TODO暂时直接关闭来处理
                    client2ProxyServerChannel.close();
                }
            }
        });

    }
}
