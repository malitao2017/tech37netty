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
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Administrator
 * @date 2014年2月16日
 * @version 1.0
 */
public class TimeClientHandle implements Runnable {

	private String host;
	private int port;

	private Selector selector;
	private SocketChannel socketChannel;

	private volatile boolean stop;

	public TimeClientHandle(String host, int port) {
		this.host = host == null ? "127.0.0.1" : host;
		this.port = port;
		try {
			// 创建Reactor线程，创建多路复用器并启动线程
			selector = Selector.open();
			// 打开socketchannel，绑定地址
			socketChannel = SocketChannel.open();
			// 设置socketchannel为非阻塞的，设置客户端连接的tcp参数
			socketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!stop) {
			try {
				// 多路复用器在线程run中无限循环轮询准备就绪额的Key
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
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
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
			// 判断是否连接成功
			SocketChannel sc = (SocketChannel) key.channel();
			// 接受connect事件进行处理
			if (key.isConnectable()) {
				// 判断连接结果，若是连接成功，注册读事件到多路复用器
				if (sc.finishConnect()) {
					sc.register(selector, SelectionKey.OP_READ);
					
					//开始写
					doWrite(sc);
				} else
					System.exit(1);// 连接失败，进程退出
			}
			if (key.isReadable()) {
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				// 异步读客户端请求消息到缓存区bytebuffer
				int readBytes = sc.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("Now is : " + body);
					this.stop = true;
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
	 * 功能： 1.异步连接服务端
	 * 
	 * @throws IOException
	 */
	private void doConnect() throws IOException {
		// 如果直接连接成功，则注册到多路复用器上，发送请求消息，读应答
		if (socketChannel.connect(new InetSocketAddress(host, port))) {
			socketChannel.register(selector, SelectionKey.OP_READ);
			doWrite(socketChannel);
		} else// 没有成功，说明客户端已经发送sync包，服务端没有返回ack包，数据链路还没有建立
				// 用于初始化：向Reactor线程的多路复用器注册OP_CONNECT状态位，监听服务器端的tcp的ACK应答
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
	}

	private void doWrite(SocketChannel sc) throws IOException {
		byte[] req = "QUERY TIME ORDER".getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		// 以bytebuffer的形式调用socketchannel的异步write接口将消息发送到客户端
		sc.write(writeBuffer);
		if (!writeBuffer.hasRemaining())
			System.out.println("Send order 2 server succeed.");
	}

}
