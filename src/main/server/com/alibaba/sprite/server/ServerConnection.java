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

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.alibaba.sprite.net.FrontendConnection;
import com.alibaba.sprite.server.handler.ServerCommandHandler;
import com.alibaba.sprite.server.packet.AudioPacket;

/**
 * @author xianmao.hexm 2011-4-21 上午11:22:57
 */
public class ServerConnection extends FrontendConnection {
    private static final ConcurrentMap<Long, ServerConnection> cons = new ConcurrentHashMap<Long, ServerConnection>();
    private static final ConcurrentMap<String, Long> uids = new ConcurrentHashMap<String, Long>();
    private static final Logger LOGGER = Logger.getLogger(ServerConnection.class);

    public static final ServerConnection getByUid(String uid) {
        Long id = uids.get(uid);
        if (id != null) {
            return cons.get(id);
        }
        return null;
    }

    private String uid;
    private final ConcurrentMap<String, ServerConnection> target;

    public ServerConnection(SocketChannel channel) {
        super(channel);
        this.handler = new ServerCommandHandler(this);
        this.target = new ConcurrentHashMap<String, ServerConnection>();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        if (this.uid == null) {
            this.uid = uid;
            uids.put(uid, id);
            LOGGER.warn("con uid:" + uid);
        }
    }

    public ConcurrentMap<String, ServerConnection> getTarget() {
        return target;
    }

    @Override
    public void register(Selector selector) throws IOException {
        super.register(selector);
        if (!isClosed.get()) {
            cons.put(id, this);
            AudioPacket packet = new AudioPacket();
            packet.type = AudioPacket.COM_INIT;
            packet.data = new byte[0];
            packet.write(this);
        }
    }

    @Override
    public void error(int errCode, Throwable t) {
        close();
    }

    @Override
    public boolean close() {
        boolean isClose = super.close();
        if (isClose) {
            cons.remove(id);
            uids.remove(uid);
        }
        return isClose;
    }

}
