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
package com.alibaba.sprite.manager.packet;

import java.nio.ByteBuffer;

import com.alibaba.sprite.core.AbstractPacket;
import com.alibaba.sprite.core.Message;
import com.alibaba.sprite.core.net.Connection;
import com.alibaba.sprite.core.util.BufferUtil;

/**
 * From server to client after command, if no error and result set -- that is,
 * if the command was a query which returned a result set. The Result Set Header
 * Packet is the first of several, possibly many, packets that the server sends
 * for result sets. The order of packets for a result set is:
 * 
 * <pre>
 * (Result Set Header Packet)   the number of columns
 * (Field Packets)                     column descriptors
 * (EOF Packet)                       marker: end of Field Packets
 * (Row Data Packets)             row contents
 * (EOF Packet)                       marker: end of Data Packets
 * 
 * Bytes                        Name
 * -----                        ----
 * 1-9   (Length-Coded-Binary)  field_count
 * 1-9   (Length-Coded-Binary)  extra
 * 
 * @see http://forge.mysql.com/wiki/MySQL_Internals_ClientServer_Protocol#Result_Set_Header_Packet
 * </pre>
 * 
 * @author xianmao.hexm 2010-7-22 下午05:59:55
 */
public class RsHeaderPacket extends AbstractPacket {

    public int fieldCount;
    public long extra;

    public void read(byte[] data) {
        Message mm = new Message(data);
        this.packetLength = mm.readUB3();
        this.packetId = mm.read();
        this.fieldCount = (int) mm.readLength();
        if (mm.hasRemaining()) {
            this.extra = mm.readLength();
        }
    }

    public ByteBuffer write(ByteBuffer buffer, Connection c) {
        int size = packetSize();
        buffer = c.evaluateBuffer(buffer, c.getPacketHeaderSize() + size);
        BufferUtil.writeUB3(buffer, size);
        buffer.put(packetId);
        BufferUtil.writeLength(buffer, fieldCount);
        if (extra > 0) {
            BufferUtil.writeLength(buffer, extra);
        }
        return buffer;
    }

    @Override
    public int packetSize() {
        int size = BufferUtil.getLength(fieldCount);
        if (extra > 0) {
            size += BufferUtil.getLength(extra);
        }
        return size;
    }

}
