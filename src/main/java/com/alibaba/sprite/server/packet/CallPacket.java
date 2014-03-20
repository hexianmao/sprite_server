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

import com.alibaba.sprite.core.Message;
import com.alibaba.sprite.core.Packet;

/**
 * @author xianmao.hexm
 */
public class CallPacket extends Packet {

    public String value;

    public void read(byte[] data) {
        Message mm = new Message(data);
        packetLength = mm.readUB3();
        packetId = mm.read();
        mm.read();//move command flag
        value = mm.readStringWithNull();
    }

    @Override
    public int calcPacketSize() {
        return 1 + ((value == null) ? 1 : value.length() + 1);
    }

    @Override
    protected String getPacketInfo() {
        return "Call Packet";
    }

}
