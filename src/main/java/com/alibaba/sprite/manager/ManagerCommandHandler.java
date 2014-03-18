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
package com.alibaba.sprite.manager;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.alibaba.sprite.net.Handler;
import com.alibaba.sprite.packet.OkPacket;
import com.alibaba.sprite.util.ErrorCode;
import com.alibaba.sprite.util.Message;
import com.alibaba.sprite.util.Packets;

/**
 * @author xianmao.hexm
 */
public final class ManagerCommandHandler implements Handler {

    protected final ManagerConnection source;

    public ManagerCommandHandler(ManagerConnection source) {
        this.source = source;
    }

    @Override
    public void handle(byte[] data) {
        switch (data[4]) {
        case Packets.COM_INIT_DB:
        case Packets.COM_PING: {
            ByteBuffer buffer = source.allocate();
            buffer = source.writeToBuffer(OkPacket.OK, buffer);
            source.postWrite(buffer);
            break;
        }
        case Packets.COM_QUERY: {
            Message mm = new Message(data);
            mm.position(5);
            String query = null;
            try {
                query = mm.readString(source.getCharset());
            } catch (UnsupportedEncodingException e) {
                source.writeErrMessage(
                        (byte) 1,
                        ErrorCode.ER_UNKNOWN_CHARACTER_SET,
                        "Unknown charset '" + source.getCharset() + "'");
                return;
            }
            if (query == null || query.length() == 0) {
                source.writeErrMessage((byte) 1, ErrorCode.ER_NOT_ALLOWED_COMMAND, "Empty SQL");
                return;
            }
            ManagerQueryHandler.handle(query, source);
            break;
        }
        case Packets.COM_QUIT:
        case Packets.COM_PROCESS_KILL:
            source.close();
            break;
        default:
            source.writeErrMessage((byte) 1, ErrorCode.ER_UNKNOWN_COM_ERROR, "unknown command error");
        }
    }

}
