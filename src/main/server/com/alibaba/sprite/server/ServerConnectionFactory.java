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
package com.alibaba.sprite.server;

import java.nio.channels.SocketChannel;

import com.alibaba.sprite.Sprite;
import com.alibaba.sprite.config.model.SystemConfig;
import com.alibaba.sprite.net.FrontendConnection;
import com.alibaba.sprite.net.FrontendConnectionFactory;
import com.alibaba.sprite.net.util.BufferQueue;
import com.alibaba.sprite.server.handler.ServerPrepareHandler;
import com.alibaba.sprite.server.session.BlockingSession;

/**
 * @author xianmao.hexm
 */
public class ServerConnectionFactory extends FrontendConnectionFactory {

    protected String charset = "utf8";
    protected int packetHeaderSize = 4;
    protected int maxPacketSize = 16 * 1024 * 1024;
    protected int writeQueueCapcity = 16;
    protected long idleTimeout = 8 * 3600 * 1000L;

    @Override
    protected FrontendConnection getConnection(SocketChannel channel) {
        SystemConfig sys = Sprite.getInstance().getConfig().getSystem();
        ServerConnection c = new ServerConnection(channel);
        c.setPacketHeaderSize(packetHeaderSize);
        c.setMaxPacketSize(maxPacketSize);
        c.setWriteQueue(new BufferQueue(writeQueueCapcity));
        c.setIdleTimeout(idleTimeout);
        c.setCharset(charset);
        c.setPrivileges(new ServerPrivileges());
        c.setQueryHandler(new ServerQueryHandler(c));
        c.setPrepareHandler(new ServerPrepareHandler(c));
        c.setTxIsolation(sys.getTxIsolation());
        c.setSession(new BlockingSession(c));
        return c;
    }

    public int getPacketHeaderSize() {
        return packetHeaderSize;
    }

    public void setPacketHeaderSize(int packetHeaderSize) {
        this.packetHeaderSize = packetHeaderSize;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public int getWriteQueueCapcity() {
        return writeQueueCapcity;
    }

    public void setWriteQueueCapcity(int writeQueueCapcity) {
        this.writeQueueCapcity = writeQueueCapcity;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

}
