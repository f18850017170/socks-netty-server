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
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        channel.write(msg.retain());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        channel.flush();
    }
}
