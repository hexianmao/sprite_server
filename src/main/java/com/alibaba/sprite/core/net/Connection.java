/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.sprite.core.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

/**
 * @author xianmao.hexm
 */
public interface Connection {

    int getPacketHeaderSize();

    void accept(Processor processor);

    void register(Selector selector) throws IOException;

    void read() throws IOException;

    void writeByQueue() throws IOException;

    void writeByEvent() throws IOException;

    void error(int errCode, Throwable t);

    ByteBuffer allocate();

    void postWrite(ByteBuffer buffer);

    ByteBuffer evaluateBuffer(ByteBuffer buffer, int capacity);

    ByteBuffer writeToBuffer(byte[] src, ByteBuffer buffer);

}
