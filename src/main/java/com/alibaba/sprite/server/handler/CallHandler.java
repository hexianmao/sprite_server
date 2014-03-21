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

import org.apache.log4j.Logger;

import com.alibaba.sprite.SpriteServer;
import com.alibaba.sprite.server.ErrorCode;
import com.alibaba.sprite.server.ServerConnection;
import com.alibaba.sprite.server.packet.CallPacket;

/**
 * @author xianmao.hexm
 */
public class CallHandler {

    private static final Logger LOGGER = Logger.getLogger(CallHandler.class);

    public static void handle(CallPacket packet, ServerConnection source) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("<" + source.getUser() + "> calling <" + packet.user + ">");
        }
        String cid = SpriteServer.getInstance().getUsers().get(packet.user);
        if (cid == null) {
            source.writeErrMessage((byte) 1, ErrorCode.ER_USER_NOT_EXIST, "user not exist");
        } else {
            ServerConnection target = SpriteServer.getInstance().getConnections().get(cid);
            if (target == null) {
                source.writeErrMessage((byte) 1, ErrorCode.ER_USER_NOT_ONLINE, "user not online");
            } else {
                packet.user = source.getUser();
                packet.write(target);
            }
        }
    }

}
