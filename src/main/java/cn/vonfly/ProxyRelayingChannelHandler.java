package cn.vonfly;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ProxyRelayingChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private Channel channel;

    public ProxyRelayingChannelHandler(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        channel.writeAndFlush(HttpResponseStatus.OK.codeAsText().array());
        ctx.fireChannelActive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        System.out.println(Thread.currentThread().getName() +"接受来自dst.address的信息，即将写入ss server channel,len=" + msg.readableBytes());
        channel.write(msg.retain());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        channel.flush();
    }
}
