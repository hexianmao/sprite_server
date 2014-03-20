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
import java.util.LinkedList;
import java.util.List;

import com.alibaba.sprite.SpriteServer;
import com.alibaba.sprite.core.Fields;
import com.alibaba.sprite.core.NameableExecutor;
import com.alibaba.sprite.core.net.Processor;
import com.alibaba.sprite.core.util.IntegerUtil;
import com.alibaba.sprite.core.util.LongUtil;
import com.alibaba.sprite.core.util.PacketUtil;
import com.alibaba.sprite.core.util.StringUtil;
import com.alibaba.sprite.manager.ManagerConnection;
import com.alibaba.sprite.manager.packet.RsEOFPacket;
import com.alibaba.sprite.manager.packet.RsFieldPacket;
import com.alibaba.sprite.manager.packet.RsHeaderPacket;
import com.alibaba.sprite.manager.packet.RsRowDataPacket;

/**
 * 查看线程池状态
 * 
 * @author xianmao.hexm
 */
public final class ShowThreadPool {

    private static final int FIELD_COUNT = 6;
    private static final RsHeaderPacket header = PacketUtil.getHeader(FIELD_COUNT);
    private static final RsFieldPacket[] fields = new RsFieldPacket[FIELD_COUNT];
    private static final RsEOFPacket eof = new RsEOFPacket();
    static {
        int i = 0;
        byte packetId = 0;
        header.packetId = ++packetId;

        fields[i] = PacketUtil.getField("NAME", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("POOL_SIZE", Fields.FIELD_TYPE_LONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("ACTIVE_COUNT", Fields.FIELD_TYPE_LONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("TASK_QUEUE_SIZE", Fields.FIELD_TYPE_LONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("COMPLETED_TASK", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("TOTAL_TASK", Fields.FIELD_TYPE_LONGLONG);
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
        List<NameableExecutor> executors = getExecutors();
        for (NameableExecutor exec : executors) {
            if (exec != null) {
                RsRowDataPacket row = getRow(exec, c.getCharset());
                row.packetId = ++packetId;
                buffer = row.write(buffer, c);
            }
        }

        // write last eof
        RsEOFPacket lastEof = new RsEOFPacket();
        lastEof.packetId = ++packetId;
        buffer = lastEof.write(buffer, c);

        // write buffer
        c.postWrite(buffer);
    }

    private static RsRowDataPacket getRow(NameableExecutor exec, String charset) {
        RsRowDataPacket row = new RsRowDataPacket(FIELD_COUNT);
        row.add(StringUtil.encode(exec.getName(), charset));
        row.add(IntegerUtil.toBytes(exec.getPoolSize()));
        row.add(IntegerUtil.toBytes(exec.getActiveCount()));
        row.add(IntegerUtil.toBytes(exec.getQueue().size()));
        row.add(LongUtil.toBytes(exec.getCompletedTaskCount()));
        row.add(LongUtil.toBytes(exec.getTaskCount()));
        return row;
    }

    private static List<NameableExecutor> getExecutors() {
        SpriteServer server = SpriteServer.getInstance();
        List<NameableExecutor> list = new LinkedList<NameableExecutor>();
        list.add(server.getServerExecutor());
        list.add(server.getTaskExecutor());
        for (Processor p : server.getProcessors()) {
            list.add(p.getExecutor());
        }
        return list;
    }

}
