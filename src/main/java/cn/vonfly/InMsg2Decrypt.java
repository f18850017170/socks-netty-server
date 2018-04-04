package cn.vonfly;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 消息解密处理
 * 信息流 client->proxy
 */
public class InMsg2Decrypt extends ByteToMessageDecoder {
    private final AESEncrypt aesEncrypt = new AESEncrypt();
    private final byte[] key = "6P(g*(%gYDrBggFk".getBytes(Charset.forName("UTF-8"));

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int len = in.readableBytes();
        byte[] array = new byte[len];
        if (in.hasArray()) {
            array = in.array();
        } else {//非数组支撑，是直接缓冲区
            in.readBytes(array);
        }
        byte[] encrypt = aesEncrypt.decrypt(key, array);
        out.add(ctx.alloc().buffer(encrypt.length).writeBytes(encrypt));
    }
}
