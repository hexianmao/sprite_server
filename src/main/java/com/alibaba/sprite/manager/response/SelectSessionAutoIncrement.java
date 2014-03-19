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

import com.alibaba.sprite.core.Fields;
import com.alibaba.sprite.core.packet.RsEOFPacket;
import com.alibaba.sprite.core.packet.RsFieldPacket;
import com.alibaba.sprite.core.packet.RsHeaderPacket;
import com.alibaba.sprite.core.packet.RsRowDataPacket;
import com.alibaba.sprite.core.util.LongUtil;
import com.alibaba.sprite.core.util.PacketUtil;
import com.alibaba.sprite.manager.ManagerConnection;

/**
 * @author xianmao.hexm 2011-5-9 下午06:06:12
 */
public final class SelectSessionAutoIncrement {

    private static final int FIELD_COUNT = 1;
    private static final RsHeaderPacket header = PacketUtil.getHeader(FIELD_COUNT);
    private static final RsFieldPacket[] fields = new RsFieldPacket[FIELD_COUNT];
    private static final RsEOFPacket eof = new RsEOFPacket();
    static {
        int i = 0;
        byte packetId = 0;
        header.packetId = ++packetId;

        fields[i] = PacketUtil.getField("SESSION.AUTOINCREMENT", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        eof.packetId = ++packetId;
    }

    public static void execute(ManagerConnection c) {
        ByteBuffer buffer = c.allocate();

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
        RsRowDataPacket row = new RsRowDataPacket(FIELD_COUNT);
        row.packetId = ++packetId;
        row.add(LongUtil.toBytes(1));
        buffer = row.write(buffer, c);

        // write last eof
        RsEOFPacket lastEof = new RsEOFPacket();
        lastEof.packetId = ++packetId;
        buffer = lastEof.write(buffer, c);

        // post write
        c.postWrite(buffer);
    }

}
