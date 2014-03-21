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
package com.alibaba.sprite.server.packet;

import com.alibaba.sprite.core.AbstractPacket;
import com.alibaba.sprite.server.ServerConnection;

/**
 * @author xianmao.hexm
 */
public class AnwserPacket extends AbstractPacket implements SendPacket {

    public byte rsCode;

    @Override
    public void write(ServerConnection c) {

    }

    @Override
    public int packetSize() {
        // TODO Auto-generated method stub
        return 0;
    }

}
