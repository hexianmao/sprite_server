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
package com.alibaba.sprite.server.handler;

import com.alibaba.sprite.net.handler.NIOHandler;
import com.alibaba.sprite.server.ServerConnection;
import com.alibaba.sprite.server.packet.AudioPacket;

/**
 * 前端命令处理器
 * 
 * @author xianmao.hexm
 */
public class ServerCommandHandler implements NIOHandler {

    protected final ServerConnection source;

    public ServerCommandHandler(ServerConnection source) {
        this.source = source;
    }

    @Override
    public void handle(byte[] data) {
        switch (data[3]) {
        case AudioPacket.COM_UID: {
            String uid = new String(data, 4, data.length - source.getPacketHeaderSize());
            source.setUid(uid);
            AudioPacket packet = new AudioPacket();
            packet.type = AudioPacket.COM_OK;
            packet.data = new byte[0];
            packet.write(source);
            break;
        }
        case AudioPacket.COM_CALL:
            String uid = new String(data, 4, data.length - source.getPacketHeaderSize());
            ServerConnection con = ServerConnection.getByUid(uid);
            if (con != null && !con.isClosed()) {
                source.getTarget().put(uid, con);
                AudioPacket packet = new AudioPacket();
                packet.type = AudioPacket.COM_OK;
                packet.data = new byte[0];
                packet.write(source);
            } else {
                AudioPacket packet = new AudioPacket();
                packet.type = AudioPacket.COM_ERR;
                packet.data = new byte[0];
                packet.write(source);
            }
            break;
        case AudioPacket.COM_STREAM:
            for (ServerConnection c : source.getTarget().values()) {
                if (c != null) {
                    if (c.isClosed()) {
                        source.getTarget().remove(c.getUid());
                    } else {
                        c.write(data);
                    }
                }
            }
            break;
        default:
            source.close();
        }
    }

}
