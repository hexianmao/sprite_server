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
import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.alibaba.sprite.SpriteServer;
import com.alibaba.sprite.core.Fields;
import com.alibaba.sprite.core.net.Processor;
import com.alibaba.sprite.core.util.PacketUtil;
import com.alibaba.sprite.manager.ManagerConnection;
import com.alibaba.sprite.manager.packet.RsEOFPacket;
import com.alibaba.sprite.manager.packet.RsFieldPacket;
import com.alibaba.sprite.manager.packet.RsHeaderPacket;
import com.alibaba.sprite.manager.packet.RsRowDataPacket;

/**
 * @author xianmao.hexm 2010-9-30 下午01:47:38
 */
public final class ShowRouter {

    private static final int FIELD_COUNT = 5;
    private static final RsHeaderPacket header = PacketUtil.getHeader(FIELD_COUNT);
    private static final RsFieldPacket[] fields = new RsFieldPacket[FIELD_COUNT];
    private static final RsEOFPacket eof = new RsEOFPacket();
    static {
        int i = 0;
        byte packetId = 0;
        header.packetId = ++packetId;

        fields[i] = PacketUtil.getField("PROCESSOR_NAME", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("ROUTE_COUNT", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("TIME_COUNT", Fields.FIELD_TYPE_FLOAT);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("MAX_ROUTE_TIME", Fields.FIELD_TYPE_FLOAT);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("MAX_ROUTE_SQL_ID", Fields.FIELD_TYPE_LONGLONG);
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

    private static final NumberFormat nf = DecimalFormat.getInstance();
    static {
        nf.setMaximumFractionDigits(3);
    }

    private static RsRowDataPacket getRow(Processor processor, String charset) {
        RsRowDataPacket row = new RsRowDataPacket(FIELD_COUNT);
        row.add(processor.getName().getBytes());
        row.add(null);
        row.add(null);
        row.add(null);
        row.add(null);
        return row;
    }

}
