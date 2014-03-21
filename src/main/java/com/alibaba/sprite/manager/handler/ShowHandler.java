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
package com.alibaba.sprite.manager.handler;

import com.alibaba.sprite.manager.ErrorCode;
import com.alibaba.sprite.manager.ManagerConnection;
import com.alibaba.sprite.manager.parser.ManagerParseShow;

/**
 * @author xianmao.hexm
 */
public final class ShowHandler {

    public static void handle(String query, ManagerConnection c, int offset) {
        int rs = ManagerParseShow.parse(query, offset);
        switch (rs & 0xff) {
        case ManagerParseShow.COMMAND:
            ShowCommand.execute(c);
            break;
        case ManagerParseShow.COLLATION:
            ShowCollation.execute(c);
            break;
        case ManagerParseShow.CONNECTION:
            ShowConnection.execute(c);
            break;
        case ManagerParseShow.HELP:
            ShowHelp.execute(c);
            break;
        case ManagerParseShow.PARSER:
            ShowParser.execute(c);
            break;
        case ManagerParseShow.PROCESSOR:
            ShowProcessor.execute(c);
            break;
        case ManagerParseShow.ROUTER:
            ShowRouter.execute(c);
            break;
        case ManagerParseShow.SERVER:
            ShowServer.execute(c);
            break;
        case ManagerParseShow.THREADPOOL:
            ShowThreadPool.execute(c);
            break;
        case ManagerParseShow.TIME_CURRENT:
            ShowTime.execute(c, ManagerParseShow.TIME_CURRENT);
            break;
        case ManagerParseShow.TIME_STARTUP:
            ShowTime.execute(c, ManagerParseShow.TIME_STARTUP);
            break;
        case ManagerParseShow.VARIABLES:
            ShowVariables.execute(c);
            break;
        case ManagerParseShow.VERSION:
            ShowVersion.execute(c);
            break;
        case ManagerParseShow.BACKEND:
        case ManagerParseShow.CONNECTION_SQL:
        case ManagerParseShow.HEARTBEAT:
        case ManagerParseShow.SQL:
        case ManagerParseShow.SQL_DETAIL:
        case ManagerParseShow.SQL_EXECUTE:
        case ManagerParseShow.SQL_SLOW:
            c.writeErrMessage((byte) 1, ErrorCode.ER_YES, "Unsupported statement");
            break;
        default:
            c.writeErrMessage((byte) 1, ErrorCode.ER_YES, "Unsupported statement");
        }
    }

}
