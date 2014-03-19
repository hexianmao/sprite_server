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

import com.alibaba.sprite.MainServer;
import com.alibaba.sprite.core.Fields;
import com.alibaba.sprite.core.net.Processor;
import com.alibaba.sprite.core.packet.RsEOFPacket;
import com.alibaba.sprite.core.packet.RsFieldPacket;
import com.alibaba.sprite.core.packet.RsHeaderPacket;
import com.alibaba.sprite.core.packet.RsRowDataPacket;
import com.alibaba.sprite.core.util.PacketUtil;
import com.alibaba.sprite.manager.ManagerConnection;

/**
 * 统计各类数据包的执行次数
 * 
 * @author xianmao.hexm 2010-9-29 下午03:06:42
 * @author wenfeng.cenwf 2011-4-25
 */
public final class ShowCommand {

    private static final int FIELD_COUNT = 10;
    private static final RsHeaderPacket header = PacketUtil.getHeader(FIELD_COUNT);
    private static final RsFieldPacket[] fields = new RsFieldPacket[FIELD_COUNT];
    private static final RsEOFPacket eof = new RsEOFPacket();
    static {
        int i = 0;
        byte packetId = 0;
        header.packetId = ++packetId;

        fields[i] = PacketUtil.getField("PROCESSOR", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("INIT_DB", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("QUERY", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("STMT_PREPARE", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("STMT_EXECUTE", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("STMT_CLOSE", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("PING", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("KILL", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("QUIT", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("OTHER", Fields.FIELD_TYPE_LONGLONG);
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
        for (Processor p : MainServer.getInstance().getProcessors()) {
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
        //        CommandCount cc = processor.getCommands();
        RsRowDataPacket row = new RsRowDataPacket(FIELD_COUNT);
        //        row.add(processor.getName().getBytes());
        //        row.add(LongUtil.toBytes(cc.initDBCount()));
        //        row.add(LongUtil.toBytes(cc.queryCount()));
        //        row.add(LongUtil.toBytes(cc.stmtPrepareCount()));
        //        row.add(LongUtil.toBytes(cc.stmtExecuteCount()));
        //        row.add(LongUtil.toBytes(cc.stmtCloseCount()));
        //        row.add(LongUtil.toBytes(cc.pingCount()));
        //        row.add(LongUtil.toBytes(cc.killCount()));
        //        row.add(LongUtil.toBytes(cc.quitCount()));
        //        row.add(LongUtil.toBytes(cc.otherCount()));
        return row;
    }

}
