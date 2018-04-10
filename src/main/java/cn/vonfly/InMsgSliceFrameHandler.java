package cn.vonfly;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.vonfly.InMsgSliceFrameHandler.FrameState.FRAME_CONTENT;
import static cn.vonfly.InMsgSliceFrameHandler.FrameState.FRAME_FINISH;
import static cn.vonfly.InMsgSliceFrameHandler.FrameState.FRAME_START;

/**
 * 非共享的
 * 入站信息切片
 */
public class InMsgSliceFrameHandler extends ReplayingDecoder<InMsgSliceFrameHandler.FrameState> {
    private int frameLength = -1;
    private ByteBuf frameContent;
    private ByteBuf frameLenByteBuf = Unpooled.buffer(2);
    private List<ByteBuf> frameContentList = new ArrayList<>();

    public InMsgSliceFrameHandler() {
        super(FRAME_START);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int len = in.writerIndex() - in.readerIndex();
        if (len == 0) {
            return;
        }
        switch (state()) {
            case FRAME_START: {
                //3 cases
                //1、First req and only 1byte
                //2、First req and len > 1
                //3、N-st req and need 1byte
                if (len < 2 || frameLenByteBuf.writerIndex() == 1) {
                    frameLenByteBuf.writeByte(in.readByte());
                } else {
                    frameLength = in.readUnsignedShortLE();
                }
                if (frameLenByteBuf.writerIndex() == 2) {
                    frameLength = frameLenByteBuf.readUnsignedShortLE();
                    frameLenByteBuf.clear();
                }
                if (frameLength > 0) {
                    frameContent = ctx.alloc().buffer(frameLength);
                    //状态变更
                    state(FRAME_CONTENT);
                }
                break;
            }
            case FRAME_CONTENT: {
                int readableBytes = in.writerIndex() - in.readerIndex();
                if (readableBytes > 0) {
                    int leftWriteLength = frameLength - frameContent.writerIndex();
                    if (leftWriteLength > 0) {
                        frameContent.writeBytes(in, leftWriteLength > readableBytes ? readableBytes : leftWriteLength);
                    }
                    if (frameLength == frameContent.writerIndex()) {
                        state(FRAME_FINISH);
                    }
                }
                //in.writerIndex() - in.readerIndex()使decode方法不会再被调用,不能使用in.isReadable()，此处会调用ReplayingDecoderByteBuf.isReadable()返回并非预期值
                if (in.writerIndex() - in.readerIndex() > 0) {
                    break;
                }
            }
            case FRAME_FINISH: {
                if (frameLength == frameContent.writerIndex()) {
                    frameContentList.add(frameContent);
                    state(FRAME_START);
                    frameLength = -1;
                }
                if (in.writerIndex() - in.readerIndex() <= 0) {
                    out.addAll(frameContentList);
                    frameContentList.clear();
                }
                break;
            }
            default: {
                throw new Error();
            }
        }
    }

    enum FrameState {
        FRAME_START,
        FRAME_CONTENT,
        FRAME_FINISH
    }
}
