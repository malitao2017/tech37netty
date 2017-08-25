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
package n2nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Administrator
 * @date 2014年2月16日
 * @version 1.0
 */
public class MultiplexerTimeServer implements Runnable {

	private Selector selector;

	private ServerSocketChannel servChannel;

	private volatile boolean stop;

	/**
	 * 初始化多路复用器、绑定监听端口
	 * 功能：
	 * 1.构造方法，进行资源初始化
	 * 
	 * @param port
	 */
	public MultiplexerTimeServer(int port) {
		try {
			//创建Reactor线程，创建多路复用器（并启动线程，shu无）
			selector = Selector.open();
			//监听客户端连接，所有客户端连接的父管道
			servChannel = ServerSocketChannel.open();
			//设置连接为非阻塞模式
			servChannel.configureBlocking(false);
			//绑定监听端口
			servChannel.socket().bind(new InetSocketAddress(port), 1024);
			//将ServerSocketChannel注册到Reactor线程的多路复用器Selector上
			servChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("The time server is start in port : " + port);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void stop() {
		this.stop = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	/**
	 * 功能：
	 * 1.循环遍历selector，其休眠时间为1s，无论是否有读写操作，都会每隔1s被唤醒一次
	 * 2.当有就绪状态的Channel时候，就对这个集合进行迭代，进行读写操作
	 */
	@Override
	public void run() {
		while (!stop) {
			try {
				//多路复用器在线程run方法的无限循环体内轮询准备就绪的Key
				selector.select(1000);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				SelectionKey key = null;
				while (it.hasNext()) {
					key = it.next();
					it.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							key.cancel();
							if (key.channel() != null)
								key.channel().close();
						}
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		// 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
		if (selector != null)
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private void handleInput(SelectionKey key) throws IOException {

		if (key.isValid()) {
			// 处理新接入的请求消息
			/**
			 * 功能：
			 * 1.通过accept接收客户端的请求并创建socketchannel实例
			 * 2.相当于完成tcp的三次握手，tcp的数据链路层正式建立
			 * 3.需要将新创建的socketchannel设置为异步非阻塞，同时设置tcp的参数：tcp接收和发送缓冲区的大小，此为简单案例
			 */
			if (key.isAcceptable()) {
				// Accept the new connection
				ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
				//多路复用器监听到有新的客户端接入，处理新的接入请求，完成TCP的三次握手，建立数据链路
				SocketChannel sc = ssc.accept();
				//设置客户端链路为非阻塞模式
				sc.configureBlocking(false);
				// Add the new connection to the selector
				//将新接入的客户端连接注册到Reactor线程的多路复用器撒花姑娘，监听读操作，读取客户端发送的网络消息
				sc.register(selector, SelectionKey.OP_READ);
			}
			/**
			 * 功能：
			 * 1.读取客户端的请求消息
			 * 2.上一步已设置，这里的read是异步字节数大小意义：>0,读到了字节；=0，没有读到字节属于正常场景；-1,链路已经关闭需要关闭SocketChannel，释放资源
			 * 3.flip操作的作用是：将缓存区当前的limit设置为position，设置为0，用于后续对缓存的读操作
			 * 4.get操作是将缓存区可读的字节数组复制到新建数组中
			 */
			if (key.isReadable()) {
				// Read the data
				SocketChannel sc = (SocketChannel) key.channel();
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				//异步读取客户端请求消息到缓存区
				int readBytes = sc.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					//这里比较简单，完整的会有安全逻辑：对ByteBuffer进行编解码。
					//while(readBuffer.hasRemaining()),说明有板报消息指针reset，继续读取后续的报文，将解码成功的消息封装成Task,投递到业务线程池中，进行业务逻辑编排
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("The time server receive order : " + body);
					String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)
							? new java.util.Date(System.currentTimeMillis()).toString() : "BAD ORDER";
					
					//开始写
					doWrite(sc, currentTime);
				} else if (readBytes < 0) {
					// 对端链路关闭
					key.cancel();
					sc.close();
				} else
					; // 读到0字节，忽略
			}
		}
	}

	/**
	 * 功能：
	 * 1.写出数据
	 * 2.此为简单案例，正常的情况下，socketchannel是异步非阻塞的，并不能保证把所有的数组一次性传完，即写半包，此刻
	 * 		需要：注册写操作；轮询selector将没有发送完的bytebuffer发送完毕；通过bytebuffer的hasmain方法判断是否发送完成。
	 * @param channel
	 * @param response
	 * @throws IOException
	 */
	private void doWrite(SocketChannel channel, String response) throws IOException {
		if (response != null && response.trim().length() > 0) {
			byte[] bytes = response.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			//调用SocketChannel的异步write接口，将消息异步发送到客户端
			channel.write(writeBuffer);
		}
	}
}
