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
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import com.alibaba.sprite.core.ErrorCode;
import com.alibaba.sprite.core.PacketTypes;
import com.alibaba.sprite.core.packet.AuthPacket;
import com.alibaba.sprite.core.packet.QuitPacket;
import com.alibaba.sprite.core.util.SecurityUtil;
import com.alibaba.sprite.server.ServerConnection;
import com.alibaba.sprite.server.ServerHandler;

/**
 * @author xianmao.hexm
 */
public final class AuthHandler implements ServerHandler {

    private static final Logger LOGGER = Logger.getLogger(AuthHandler.class);
    private static final byte[] AUTH_OK = new byte[] { 7, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0 };
    private static final String SECURITY_KEY = "&(~ER#*&^gy9JK";

    protected final ServerConnection source;

    public AuthHandler(ServerConnection source) {
        this.source = source;
    }

    @Override
    public void handle(byte[] data) {
        // check quit packet
        if (data.length == QuitPacket.QUIT.length && data[4] == PacketTypes.COM_QUIT) {
            source.close();
            return;
        }

        // check auth packet
        AuthPacket packet = new AuthPacket();
        packet.read(data);
        if (check(packet)) {
            success(packet);
        } else {
            failure(ErrorCode.ER_ACCESS_DENIED_ERROR, "Access denied for user '" + packet.user + "'");
        }
    }

    protected boolean check(AuthPacket packet) {
        // check password
        byte[] clientPass = packet.password;
        String passSource = SECURITY_KEY;

        // check null
        if (passSource == null || passSource.length() == 0) {
            if (clientPass == null || clientPass.length == 0) {
                return true;
            } else {
                return false;
            }
        }
        if (clientPass == null || clientPass.length == 0) {
            return false;
        }

        // encrypt
        byte[] serverPass = null;
        try {
            serverPass = SecurityUtil.scramble411(passSource.getBytes(), source.getAuthSeed());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn(source.toString(), e);
            return false;
        }
        if (serverPass != null && (serverPass.length == clientPass.length)) {
            int i = serverPass.length;
            while (i-- != 0) {
                if (serverPass[i] != clientPass[i]) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }

    protected void success(AuthPacket packet) {
        source.setUser(packet.user);
        source.setAuthenticated(true);
        source.setHandler(new CommandHandler(source));
        if (LOGGER.isInfoEnabled()) {
            StringBuilder s = new StringBuilder();
            s.append(source).append('\'').append(packet.user).append("' login success");
            byte[] extra = packet.extra;
            if (extra != null && extra.length > 0) {
                s.append(",extra:").append(new String(extra));
            }
            LOGGER.info(s.toString());
        }

        // 通知客户端认证成功
        ByteBuffer buffer = source.allocateBuffer();
        source.postWrite(source.writeToBuffer(AUTH_OK, buffer));
    }

    protected void failure(int errno, String info) {
        LOGGER.error(source.toString() + info);
        source.writeErrMessage((byte) 2, errno, info);
        source.postClose();
    }

}
