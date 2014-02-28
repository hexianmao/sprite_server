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

import com.alibaba.sprite.config.ErrorCode;
import com.alibaba.sprite.manager.ManagerConnection;
import com.alibaba.sprite.manager.parser.ManagerParseShow;
import com.alibaba.sprite.manager.response.ShowBackend;
import com.alibaba.sprite.manager.response.ShowCollation;
import com.alibaba.sprite.manager.response.ShowCommand;
import com.alibaba.sprite.manager.response.ShowConnection;
import com.alibaba.sprite.manager.response.ShowConnectionSQL;
import com.alibaba.sprite.manager.response.ShowHeartbeat;
import com.alibaba.sprite.manager.response.ShowHelp;
import com.alibaba.sprite.manager.response.ShowParser;
import com.alibaba.sprite.manager.response.ShowProcessor;
import com.alibaba.sprite.manager.response.ShowRouter;
import com.alibaba.sprite.manager.response.ShowSQL;
import com.alibaba.sprite.manager.response.ShowSQLDetail;
import com.alibaba.sprite.manager.response.ShowSQLExecute;
import com.alibaba.sprite.manager.response.ShowSQLSlow;
import com.alibaba.sprite.manager.response.ShowServer;
import com.alibaba.sprite.manager.response.ShowThreadPool;
import com.alibaba.sprite.manager.response.ShowTime;
import com.alibaba.sprite.manager.response.ShowVariables;
import com.alibaba.sprite.manager.response.ShowVersion;
import com.alibaba.sprite.parser.util.ParseUtil;

/**
 * @author xianmao.hexm
 */
public final class ShowHandler {

    public static void handle(String stmt, ManagerConnection c, int offset) {
        int rs = ManagerParseShow.parse(stmt, offset);
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
        case ManagerParseShow.BACKEND:
            ShowBackend.execute(c);
            break;
        case ManagerParseShow.CONNECTION_SQL:
            ShowConnectionSQL.execute(c);
            break;
        case ManagerParseShow.HELP:
            ShowHelp.execute(c);
            break;
        case ManagerParseShow.HEARTBEAT:
            ShowHeartbeat.response(c);
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
        case ManagerParseShow.SQL:
            ShowSQL.execute(c, ParseUtil.getSQLId(stmt));
            break;
        case ManagerParseShow.SQL_DETAIL:
            ShowSQLDetail.execute(c, ParseUtil.getSQLId(stmt));
            break;
        case ManagerParseShow.SQL_EXECUTE:
            ShowSQLExecute.execute(c);
            break;
        case ManagerParseShow.SQL_SLOW:
            ShowSQLSlow.execute(c);
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
        default:
            c.writeErrMessage(ErrorCode.ER_YES, "Unsupported statement");
        }
    }
}
