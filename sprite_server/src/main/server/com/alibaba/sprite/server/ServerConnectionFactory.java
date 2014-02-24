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
package com.alibaba.sprite.server;

import java.nio.channels.SocketChannel;

import com.alibaba.sprite.Sprite;
import com.alibaba.sprite.config.model.SystemConfig;
import com.alibaba.sprite.net.FrontendConnection;
import com.alibaba.sprite.net.FrontendConnectionFactory;
import com.alibaba.sprite.server.handler.ServerPrepareHandler;
import com.alibaba.sprite.server.session.BlockingSession;

/**
 * @author xianmao.hexm
 */
public class ServerConnectionFactory extends FrontendConnectionFactory {

    @Override
    protected FrontendConnection getConnection(SocketChannel channel) {
        SystemConfig sys = Sprite.getInstance().getConfig().getSystem();
        ServerConnection c = new ServerConnection(channel);
        c.setPrivileges(new ServerPrivileges());
        c.setQueryHandler(new ServerQueryHandler(c));
        c.setPrepareHandler(new ServerPrepareHandler(c));
        c.setTxIsolation(sys.getTxIsolation());
        c.setSession(new BlockingSession(c));
        return c;
    }

}
