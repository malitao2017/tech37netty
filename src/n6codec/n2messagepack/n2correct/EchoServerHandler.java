package n6codec.n2messagepack.n2correct;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import n6codec.n2messagepack.UserInfo;

public class EchoServerHandler extends ChannelHandlerAdapter{

	int counter = 0;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("This is " + ++counter + " times receive client : [" + msg + "]");
		UserInfo info = (UserInfo)msg;
		ctx.writeAndFlush(info);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
