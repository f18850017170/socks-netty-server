package cn.vonfly;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.Queue;

public class InMsgSliceFrameHandlerTest {
    @Test
    public void sliceFrameTest(){
        EmbeddedChannel channel = new EmbeddedChannel(new InMsgSliceFrameHandler());
        ByteBuf buffer = Unpooled.buffer(1);
        buffer.writeByte(0);
        channel.writeInbound(buffer.duplicate());

        buffer = Unpooled.buffer(1);
        buffer.writeByte(1);
        channel.writeInbound(buffer.duplicate());

        buffer = Unpooled.buffer(3);
        buffer.writeBytes( new byte[]{1,1,1});
        channel.writeInbound(buffer);
        Queue<Object> objects = channel.inboundMessages();
        System.out.println(objects);
    }
}
