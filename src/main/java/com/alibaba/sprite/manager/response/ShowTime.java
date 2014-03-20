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
import com.alibaba.sprite.core.util.LongUtil;
import com.alibaba.sprite.core.util.PacketUtil;
import com.alibaba.sprite.manager.ManagerConnection;
import com.alibaba.sprite.manager.packet.RsEOFPacket;
import com.alibaba.sprite.manager.packet.RsFieldPacket;
import com.alibaba.sprite.manager.packet.RsHeaderPacket;
import com.alibaba.sprite.manager.packet.RsRowDataPacket;
import com.alibaba.sprite.manager.parser.ManagerParseShow;

/**
 * @author xianmao.hexm 2010-9-27 下午02:53:11
 */
public final class ShowTime {

    private static final int FIELD_COUNT = 1;
    private static final RsHeaderPacket header = PacketUtil.getHeader(FIELD_COUNT);
    private static final RsFieldPacket[] fields = new RsFieldPacket[FIELD_COUNT];
    private static final RsEOFPacket eof = new RsEOFPacket();
    static {
        int i = 0;
        byte packetId = 0;
        header.packetId = ++packetId;

        fields[i] = PacketUtil.getField("TIMESTAMP", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        eof.packetId = ++packetId;
    }

    public static void execute(ManagerConnection c, int type) {
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
        RsRowDataPacket row = getRow(type);
        row.packetId = ++packetId;
        buffer = row.write(buffer, c);

        // write last eof
        RsEOFPacket lastEof = new RsEOFPacket();
        lastEof.packetId = ++packetId;
        buffer = lastEof.write(buffer, c);

        // post write
        c.postWrite(buffer);
    }

    private static RsRowDataPacket getRow(int type) {
        RsRowDataPacket row = new RsRowDataPacket(FIELD_COUNT);
        switch (type) {
        case ManagerParseShow.TIME_CURRENT:
            row.add(LongUtil.toBytes(System.currentTimeMillis()));
            break;
        case ManagerParseShow.TIME_STARTUP:
            row.add(LongUtil.toBytes(SpriteServer.getInstance().getStartupTime()));
            break;
        default:
            row.add(LongUtil.toBytes(0L));
        }
        return row;
    }

}
