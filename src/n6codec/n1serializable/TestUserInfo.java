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
package n6codec.n1serializable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author Administrator
 * @date 2014年2月23日
 * @version 1.0
 */
public class TestUserInfo {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("jdk序列化机制编码后的二进制数组大小比二进制编码的5倍");
		UserInfo info = new UserInfo();
		info.buildUserID(100).buildUserName("Welcome to Netty");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(bos);
		os.writeObject(info);
		os.flush();
		os.close();
		byte[] b = bos.toByteArray();
		System.out.println(b);
		//[-84, -19, 0, 5, 115, 114, 0, 31, 110, 54, 99, 111, 100, 101, 99, 46, 110, 49, 115, 101, 114, 105, 97, 108, 105, 122, 97, 98, 108, 101, 46, 85, 115, 101, 114, 73, 110, 102, 111, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0, 2, 73, 0, 6, 117, 115, 101, 114, 73, 68, 76, 0, 8, 117, 115, 101, 114, 78, 97, 109, 101, 116, 0, 18, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 120, 112, 0, 0, 0, 100, 116, 0, 16, 87, 101, 108, 99, 111, 109, 101, 32, 116, 111, 32, 78, 101, 116, 116, 121]
		System.out.println("The jdk serializable length is : " + b.length);
		bos.close();
		System.out.println("-------------------------------------");
		byte[] b1 =info.codeC();
		System.out.println(b1);
		//[0, 0, 0, 16, 87, 101, 108, 99, 111, 109, 101, 32, 116, 111, 32, 78, 101, 116, 116, 121, 0, 0, 0, 100]
		System.out.println("The byte array serializable length is : " + b1.length);

	}

}
