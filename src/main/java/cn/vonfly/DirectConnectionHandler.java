package cn.vonfly;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 信息流 client->proxy
 * 执行操作proxy->dst
 */
public class DirectConnectionHandler extends SimpleChannelInboundHandler<ByteBuf> {
    //连接到dst的channel
    private Channel out2DstSocketChannel;

    public DirectConnectionHandler(Channel out2DstSocketChannel) {
        this.out2DstSocketChannel = out2DstSocketChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        System.out.println(Thread.currentThread().getName() + "接受来自ss local channel的信息，即将写入dst.address.channel,len=" + msg.readableBytes());
        out2DstSocketChannel.write(msg.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        out2DstSocketChannel.flush();

    }
}
