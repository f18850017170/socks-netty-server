package cn.vonfly;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.Charset;

/**
 * 信息流：remote->client
 * 执行操作：信息进行加密
 */
public class OutMsg2Encrypt extends MessageToByteEncoder<ByteBuf> {
    private final AESEncrypt aesEncrypt = new AESEncrypt();
    private final byte[] key = "6P(g*(%gYDrBggFk".getBytes(Charset.forName("UTF-8"));

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        int len = msg.readableBytes();
        byte[] array = new byte[len];
        if (msg.hasArray()) {
            array = msg.array();
        } else {//非数组支撑，是直接缓冲区
            msg.readBytes(array);
        }
        byte[] encrypt = aesEncrypt.encrypt(key, array);
        int length = encrypt.length;
        out.writeShortLE((short)length);
        out.writeBytes(encrypt);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        ctx.flush();
    }
}
