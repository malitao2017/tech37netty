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
package n3aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author lilinfeng
 * @date 2014年2月16日
 * @version 1.0
 */
public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler> {

	@Override
	public void completed(AsynchronousSocketChannel result, AsyncTimeServerHandler attachment) {
		//调用此方法的类是个单线程，这里写这个内容能实现一直循环的接收新的客户端
		//理解为运行到此已经完成，继续下一次的接受，再往下的逻辑是用key一类的唯一标识去读取已有的数据
		attachment.asynchronousServerSocketChannel.accept(attachment, this);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		//实现socketchannel的异步调用，往下的逻辑拿着调用对象的句柄，进行使用和回调应用
//		result.read(buffer, buffer, new ReadCompletionHandler(result));
		ReadCompletionHandler rch = new ReadCompletionHandler(result);
		/**
		 * 参数说明：
		 * 1.接收缓存区，用于从异步channel中读取数据包
		 * 2.异步channel携带的附件，通知回调时候作为入参使用
		 * 3.接收通知回调的业务handler
		 */
		result.read(buffer, buffer, rch);
		
	}

	@Override
	public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
		exc.printStackTrace();
		attachment.latch.countDown();
	}

}
