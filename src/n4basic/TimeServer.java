/*
 * Copyright 2013-2018 Lilinfeng.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package n4basic;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class TimeServer {

	public void bind(int port) throws Exception {
		// 配置服务端的NIO线程组
		/**
		 * 实际它就是Reator线程组
		 * 一个是用于服务端接收客户端的连接
		 * 一个是用于进行Socketchannel的网络读写
		 */
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			//服务端的辅助启动类，降低服务端的开发难度
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)//把两个线程组传入到sbs中
				//对应jdk nio中的ServerSocketChannel
				.channel(NioServerSocketChannel.class)
				//配置nssc的tcp参数，将backlog设置为1024
				.option(ChannelOption.SO_BACKLOG, 1024)
				//绑定i/o事件的处理类，类似nio的Reactor模式中的Handler类，主要处理网络i/o事件，如记录日志、对消息进行编码等
				.childHandler(new ChildChannelHandler());
			// 绑定端口，同步等待成功
			ChannelFuture f = b.bind(port).sync();

			// 等待服务端监听端口关闭
			f.channel().closeFuture().sync();
		} finally {
			// 优雅退出，释放线程池资源
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
		@Override
		protected void initChannel(SocketChannel arg0) throws Exception {
			arg0.pipeline().addLast(new TimeServerHandler());
		}

	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				// 采用默认值
			}
		}
		new TimeServer().bind(port);
	}
}
