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
package com.alibaba.sprite.manager.response;

import java.nio.ByteBuffer;

import com.alibaba.sprite.SpriteServer;
import com.alibaba.sprite.core.Fields;
import com.alibaba.sprite.core.net.Processor;
import com.alibaba.sprite.core.util.IntegerUtil;
import com.alibaba.sprite.core.util.LongUtil;
import com.alibaba.sprite.core.util.PacketUtil;
import com.alibaba.sprite.manager.ManagerConnection;
import com.alibaba.sprite.manager.packet.RsEOFPacket;
import com.alibaba.sprite.manager.packet.RsFieldPacket;
import com.alibaba.sprite.manager.packet.RsHeaderPacket;
import com.alibaba.sprite.manager.packet.RsRowDataPacket;

/**
 * 查看处理器状态
 * 
 * @author xianmao.hexm 2010-1-25 下午01:11:00
 * @author wenfeng.cenwf 2011-4-25
 */
public final class ShowProcessor {

    private static final int FIELD_COUNT = 8;
    private static final RsHeaderPacket header = PacketUtil.getHeader(FIELD_COUNT);
    private static final RsFieldPacket[] fields = new RsFieldPacket[FIELD_COUNT];
    private static final RsEOFPacket eof = new RsEOFPacket();
    static {
        int i = 0;
        byte packetId = 0;
        header.packetId = ++packetId;

        fields[i] = PacketUtil.getField("NAME", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("NET_IN", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("NET_OUT", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("REACT_COUNT", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("R_QUEUE", Fields.FIELD_TYPE_LONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("W_QUEUE", Fields.FIELD_TYPE_LONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("FREE_BUFFER", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("TOTAL_BUFFER", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        eof.packetId = ++packetId;
    }

    public static void execute(ManagerConnection c) {
        ByteBuffer buffer = c.allocateBuffer();

        // write header
        buffer = header.write(buffer, c);

        // write fields
        for (RsFieldPacket field : fields) {
            buffer = field.write(buffer, c);
        }

        // write eof
        buffer = eof.write(buffer, c);

        // write rows
        byte packetId = eof.packetId;
        for (Processor p : SpriteServer.getInstance().getProcessors()) {
            RsRowDataPacket row = getRow(p, c.getCharset());
            row.packetId = ++packetId;
            buffer = row.write(buffer, c);
        }

        // write last eof
        RsEOFPacket lastEof = new RsEOFPacket();
        lastEof.packetId = ++packetId;
        buffer = lastEof.write(buffer, c);

        // write buffer
        c.postWrite(buffer);
    }

    private static RsRowDataPacket getRow(Processor processor, String charset) {
        RsRowDataPacket row = new RsRowDataPacket(FIELD_COUNT);
        row.add(processor.getName().getBytes());
        row.add(LongUtil.toBytes(processor.getNetInBytes()));
        row.add(LongUtil.toBytes(processor.getNetOutBytes()));
        row.add(LongUtil.toBytes(processor.getReactCount()));
        row.add(IntegerUtil.toBytes(processor.getRegisterQueueSize()));
        row.add(IntegerUtil.toBytes(processor.getWriteQueueSize()));
        row.add(IntegerUtil.toBytes(processor.getBuffers().size()));
        row.add(IntegerUtil.toBytes(processor.getBuffers().capacity()));
        return row;
    }

}
