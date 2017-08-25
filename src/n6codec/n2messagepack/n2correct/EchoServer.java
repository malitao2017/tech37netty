package n6codec.n2messagepack.n2correct;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import n6codec.n2messagepack.MsgPackDecoder;
import n6codec.n2messagepack.MsgpackEncoder;

public class EchoServer {
	
	public void run(int port){
		//注册服务
		EventLoopGroup accptorGroup = new NioEventLoopGroup();
		EventLoopGroup IOGroup = new NioEventLoopGroup();
		try{
			ServerBootstrap b = new ServerBootstrap();
			b.group(accptorGroup, IOGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 100)
				.handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
//						// LengthFieldBasedFrameDecoder用于处理半包消息
						// 这样后面的MsgpackDecoder接收的永远是整包消息
//						ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2));
//						ch.pipeline().addLast("msgpack decoder", new MsgPackDecoder());
//						// 在ByteBuf之前增加2个字节的消息长度字段
//						ch.pipeline().addLast("frameEncoder", new LengthFieldPrepender(2));
//						ch.pipeline().addLast("msgpack encoder", new MsgpackEncoder());
						
						//经过试验，书中的messagePack的解码器不能使用，只能参考n6codec.n1serializable.netty中的解码器案例
						ch.pipeline().addLast(new ObjectDecoder(1024,ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
						ch.pipeline().addLast(new ObjectEncoder());
						
						ch.pipeline().addLast(new EchoServerHandler());
					}

				});
			// 绑定端口，同步等待成功
			ChannelFuture f = b.bind(port).sync();
			// 等待服务端监听端口关闭
			f.channel().closeFuture().sync();
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			// 优雅退出，释放线程池资源
			accptorGroup.shutdownGracefully();
			IOGroup.shutdownGracefully();
		}
	}
	public static void main(String[] args) {
		System.out.println("服务端接收序列化对象的案例：");
		new EchoServer().run(8080);
	}

}
