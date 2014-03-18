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

import org.apache.log4j.Logger;

import com.alibaba.sprite.manager.handler.ReloadHandler;
import com.alibaba.sprite.manager.handler.RollbackHandler;
import com.alibaba.sprite.manager.handler.SelectHandler;
import com.alibaba.sprite.manager.handler.ShowHandler;
import com.alibaba.sprite.manager.parser.ManagerParser;
import com.alibaba.sprite.packet.OkPacket;
import com.alibaba.sprite.util.ErrorCode;

/**
 * @author xianmao.hexm
 */
public class ManagerQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(ManagerQueryHandler.class);

    public static void handle(String query, ManagerConnection c) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(new StringBuilder().append(c).append(query).toString());
        }
        int rs = ManagerParser.parse(query);
        switch (rs & 0xff) {
        case ManagerParser.SELECT:
            SelectHandler.handle(query, c, rs >>> 8);
            break;
        case ManagerParser.SET:
            c.postWrite(c.writeToBuffer(OkPacket.OK, c.allocate()));
            break;
        case ManagerParser.SHOW:
            ShowHandler.handle(query, c, rs >>> 8);
            break;
        case ManagerParser.RELOAD:
            ReloadHandler.handle(query, c, rs >>> 8);
            break;
        case ManagerParser.ROLLBACK:
            RollbackHandler.handle(query, c, rs >>> 8);
            break;
        default:
            c.writeErrMessage((byte) 1, ErrorCode.ER_YES, "Unsupported statement");
        }
    }

}
