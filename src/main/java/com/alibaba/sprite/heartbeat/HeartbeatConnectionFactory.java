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
package com.alibaba.sprite.heartbeat;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.alibaba.sprite.Sprite;
import com.alibaba.sprite.config.model.NodeConfig;
import com.alibaba.sprite.config.model.SystemConfig;
import com.alibaba.sprite.net.BackendConnectionFactory;

/**
 * @author xianmao.hexm
 */
public class HeartbeatConnectionFactory extends BackendConnectionFactory {

    public HeartbeatConnectionFactory() {
        this.idleTimeout = 120 * 1000L;
    }

    public HeartbeatConnection make(Heartbeat heartbeat) throws IOException {
        SocketChannel channel = openSocketChannel();
        NodeConfig cnc = heartbeat.getNode().getConfig();
        SystemConfig sys = Sprite.getInstance().getConfig().getSystem();
        HeartbeatConnection detector = new HeartbeatConnection(channel);
        detector.setHost(cnc.getHost());
        detector.setPort(cnc.getPort());
        detector.setUser(sys.getClusterHeartbeatUser());
        detector.setPassword(sys.getClusterHeartbeatPass());
        detector.setHeartbeatTimeout(sys.getClusterHeartbeatTimeout());
        detector.setHeartbeat(heartbeat);
        postConnect(detector, Sprite.getInstance().getConnector());
        return detector;
    }

}
