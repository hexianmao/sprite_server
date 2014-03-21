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
package com.alibaba.sprite.server;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.alibaba.sprite.manager.ErrorCode;
import com.alibaba.sprite.server.handler.CallHandler;
import com.alibaba.sprite.server.handler.EchoHandler;
import com.alibaba.sprite.server.packet.CallPacket;
import com.alibaba.sprite.server.packet.Packets;

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
        case Packets.COM_PING:
            break;
        case Packets.COM_ECHO:
            EchoHandler.handle(data, source);
            break;
        case Packets.COM_CALL: {
            CallPacket packet = new CallPacket();
            packet.read(data);
            CallHandler.handle(packet, source);
            break;
        }
        case Packets.COM_QUIT:
            source.close();
            break;
        default:
            source.writeErrMessage((byte) 1, ErrorCode.ER_UNKNOWN_COM_ERROR, "command error");
        }
    }

}
