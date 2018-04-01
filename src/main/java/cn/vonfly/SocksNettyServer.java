package cn.vonfly;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class SocksNettyServer {
    public void start() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(new NioEventLoopGroup(1), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,30000)
                .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                //inBound handlers
                ch.pipeline()
//                        .addLast(new HttpRequestDecoder())
                        .addLast(new ConnectDstHandler());

//                //outBound handler
//                ch.pipeline().addFirst(new HttpResponseEncoder());

            }
        });
        try {
            System.out.println("启动 socksNettyServer ,监听端口：2081");
            serverBootstrap.bind(2081).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
