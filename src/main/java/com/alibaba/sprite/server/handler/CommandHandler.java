/*
 * Copyright 1999-2014 Alibaba Group.
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

import java.nio.ByteBuffer;

import com.alibaba.sprite.core.ErrorCode;
import com.alibaba.sprite.core.PacketTypes;
import com.alibaba.sprite.core.net.Handler;
import com.alibaba.sprite.core.packet.OkPacket;
import com.alibaba.sprite.server.ServerConnection;

/**
 * @author xianmao.hexm
 */
public final class CommandHandler implements Handler {

    protected final ServerConnection source;

    public CommandHandler(ServerConnection source) {
        this.source = source;
    }

    @Override
    public void handle(byte[] data) {
        switch (data[4]) {
        case PacketTypes.COM_INIT_DB:
        case PacketTypes.COM_PING:
        case PacketTypes.COM_QUERY:
            ByteBuffer buffer = source.allocate();
            buffer = source.writeToBuffer(OkPacket.OK, buffer);
            source.postWrite(buffer);
            break;
        case PacketTypes.COM_QUIT:
        case PacketTypes.COM_PROCESS_KILL:
            source.close();
            break;
        default:
            source.writeErrMessage((byte) 1, ErrorCode.ER_UNKNOWN_COM_ERROR, "UNKNOWN_COM_ERROR");
        }
    }

}
