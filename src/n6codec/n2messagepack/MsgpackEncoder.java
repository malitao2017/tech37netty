package n6codec.n2messagepack;

import org.msgpack.MessagePack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MsgpackEncoder extends MessageToByteEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext arg0, Object arg1, ByteBuf arg2) throws Exception {
		MessagePack msgpack = new MessagePack();
		// 进行序列化
		byte[] raw = msgpack.write(arg1);
		arg2.writeBytes(raw);
	}

}