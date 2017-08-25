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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * @author Administrator
 * @date 2014年2月16日
 * @version 1.0
 */
public class AsyncTimeClientHandler implements CompletionHandler<Void, AsyncTimeClientHandler>, Runnable {

	private AsynchronousSocketChannel client;
	private String host;
	private int port;
	private CountDownLatch latch;

	public AsyncTimeClientHandler(String host, int port) {
		this.host = host;
		this.port = port;
		try {
			client = AsynchronousSocketChannel.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		//防止异步操作没有执行完就关闭了线程
		latch = new CountDownLatch(1);
		//第一个this的地方用于回调通知时作为入参被喘气，调用者可以自定义
		//第二个this的地方，异步操作回调通知接口，由调用者实现
		client.connect(new InetSocketAddress(host, port), this, this);
		try {
			latch.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 异步连接成功之后！ 的回调
	 */
	@Override
	public void completed(Void result, AsyncTimeClientHandler attachment) {
		byte[] req = "QUERY TIME ORDER".getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		client.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
			/**
			 * 写操作完之后的回调
			 */
			@Override
			public void completed(Integer result, ByteBuffer buffer) {
				/**
				 * 发送缓存区尚有未发送的字节，将继续异步发送，如果已经发送完成，则执行异步读取操作
				 */
				if (buffer.hasRemaining()) {
					client.write(buffer, buffer, this);
				} else {
					ByteBuffer readBuffer = ByteBuffer.allocate(1024);
					/**
					 * 客户端异步读取服务器端应答消息的处理逻辑
					 * 注：
					 * 读取是异步的，当读取完成，通过实现内部匿名类completionhandler接口，被JDK回调时候，构造应答消息，从bytebuffer中读数打印
					 */
					client.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {
						@Override
						public void completed(Integer result, ByteBuffer buffer) {
							buffer.flip();
							byte[] bytes = new byte[buffer.remaining()];
							buffer.get(bytes);
							String body;
							try {
								body = new String(bytes, "UTF-8");
								System.out.println("Now is : " + body);
								/**
								 * 读完之后，使用CountDownLatch结束线程
								 */
								latch.countDown();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void failed(Throwable exc, ByteBuffer attachment) {
							try {
								client.close();
								latch.countDown();
							} catch (IOException e) {
								// ingnore on close
							}
						}
					});
				}
			}

			@Override
			public void failed(Throwable exc, ByteBuffer attachment) {
				try {
					client.close();
					latch.countDown();
				} catch (IOException e) {
					// ingnore on close
				}
			}
		});
	}

	@Override
	public void failed(Throwable exc, AsyncTimeClientHandler attachment) {
		exc.printStackTrace();
		try {
			client.close();
			latch.countDown();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
