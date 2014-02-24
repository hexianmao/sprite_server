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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.alibaba.sprite.Node;
import com.alibaba.sprite.Sprite;
import com.alibaba.sprite.config.Fields;
import com.alibaba.sprite.heartbeat.Heartbeat;
import com.alibaba.sprite.manager.ManagerConnection;
import com.alibaba.sprite.net.packet.EOFPacket;
import com.alibaba.sprite.net.packet.FieldPacket;
import com.alibaba.sprite.net.packet.ResultSetHeaderPacket;
import com.alibaba.sprite.net.packet.RowDataPacket;
import com.alibaba.sprite.net.util.PacketUtil;
import com.alibaba.sprite.util.IntegerUtil;
import com.alibaba.sprite.util.LongUtil;

/**
 * @author xianmao.hexm
 */
public class ShowHeartbeat {

    private static final int FIELD_COUNT = 10;
    private static final ResultSetHeaderPacket header = PacketUtil.getHeader(FIELD_COUNT);
    private static final FieldPacket[] fields = new FieldPacket[FIELD_COUNT];
    private static final EOFPacket eof = new EOFPacket();
    static {
        int i = 0;
        byte packetId = 0;
        header.packetId = ++packetId;

        fields[i] = PacketUtil.getField("NAME", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("HOST", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("PORT", Fields.FIELD_TYPE_LONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("RS_CODE", Fields.FIELD_TYPE_LONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("RETRY", Fields.FIELD_TYPE_LONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("STATUS", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("TIMEOUT", Fields.FIELD_TYPE_LONGLONG);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("EXECUTE_TIME", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("LAST_ACTIVE_TIME", Fields.FIELD_TYPE_DATETIME);
        fields[i++].packetId = ++packetId;

        fields[i] = PacketUtil.getField("STOP", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;

        eof.packetId = ++packetId;
    }

    public static void response(ManagerConnection c) {
        ByteBuffer buffer = c.allocate();

        // write header
        buffer = header.write(buffer, c);

        // write fields
        for (FieldPacket field : fields) {
            buffer = field.write(buffer, c);
        }

        // write eof
        buffer = eof.write(buffer, c);

        // write rows
        byte packetId = eof.packetId;
        for (RowDataPacket row : getRows()) {
            row.packetId = ++packetId;
            buffer = row.write(buffer, c);
        }

        // write last eof
        EOFPacket lastEof = new EOFPacket();
        lastEof.packetId = ++packetId;
        buffer = lastEof.write(buffer, c);

        // post write
        c.write(buffer);
    }

    private static List<RowDataPacket> getRows() {
        List<RowDataPacket> list = new LinkedList<RowDataPacket>();
        Map<String, Node> nodes = Sprite.getInstance().getConfig().getCluster().getNodes();
        List<String> nodeKeys = new ArrayList<String>(nodes.size());
        nodeKeys.addAll(nodes.keySet());
        Collections.sort(nodeKeys);
        for (String key : nodeKeys) {
            Node node = nodes.get(key);
            if (node != null) {
                Heartbeat hb = node.getHeartbeat();
                RowDataPacket row = new RowDataPacket(FIELD_COUNT);
                row.add(node.getName().getBytes());
                row.add(node.getConfig().getHost().getBytes());
                row.add(IntegerUtil.toBytes(node.getConfig().getPort()));
                row.add(IntegerUtil.toBytes(hb.getStatus()));
                row.add(IntegerUtil.toBytes(hb.getErrorCount()));
                row.add(hb.isChecking() ? "checking".getBytes() : "idle".getBytes());
                row.add(LongUtil.toBytes(hb.getTimeout()));
                row.add(hb.getRecorder().get().getBytes());
                String at = hb.lastActiveTime();
                row.add(at == null ? null : at.getBytes());
                row.add(hb.isStop() ? "true".getBytes() : "false".getBytes());
                list.add(row);
            }
        }
        return list;
    }

}
