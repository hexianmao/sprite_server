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
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.alibaba.sprite.core.ErrorCode;
import com.alibaba.sprite.core.PacketTypes;
import com.alibaba.sprite.core.packet.OkPacket;
import com.alibaba.sprite.server.ServerConnection;
import com.alibaba.sprite.server.ServerHandler;
import com.alibaba.sprite.server.packet.CallPacket;

/**
 * @author xianmao.hexm
 */
public final class CommandHandler implements ServerHandler {

    private static final Logger LOGGER = Logger.getLogger(CommandHandler.class);

    protected final ServerConnection source;

    public CommandHandler(ServerConnection source) {
        this.source = source;
    }

    @Override
    public void handle(byte[] data) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(new StringBuilder().append(source).append(Arrays.toString(data)).toString());
        }
        switch (data[4]) {
        case PacketTypes.COM_INIT_DB:
        case PacketTypes.COM_QUERY: {
            ByteBuffer buffer = source.allocateBuffer();
            buffer = source.writeToBuffer(OkPacket.OK, buffer);
            source.postWrite(buffer);
            break;
        }
        case PacketTypes.COM_PING: {
            break;
        }
        case PacketTypes.COM_ECHO: {
            ByteBuffer buffer = source.allocateBuffer();
            buffer = source.writeToBuffer(data, buffer);
            source.postWrite(buffer);
            break;
        }
        case PacketTypes.COM_CALL: {
            CallPacket packet = new CallPacket();
            packet.read(data);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("<" + source.getUser() + "> calling <" + packet.value + ">");
            }
            break;
        }
        case PacketTypes.COM_QUIT:
        case PacketTypes.COM_PROCESS_KILL: {
            source.close();
            break;
        }
        default:
            source.writeErrMessage((byte) 1, ErrorCode.ER_UNKNOWN_COM_ERROR, "unknown command");
        }
    }

}
