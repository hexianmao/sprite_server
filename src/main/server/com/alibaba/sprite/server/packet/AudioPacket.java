package com.alibaba.sprite.server.packet;

import java.nio.ByteBuffer;

import com.alibaba.sprite.net.util.BufferUtil;
import com.alibaba.sprite.server.ServerConnection;

public class AudioPacket {
    public static final byte COM_INIT = 0;
    public static final byte COM_UID = 1;
    public static final byte COM_OK = 2;
    public static final byte COM_ERR = 3;
    public static final byte COM_CALL = 4;
    public static final byte COM_STREAM = 5;

    public int length;
    public byte type;
    public byte[] data;

    public ByteBuffer write(ByteBuffer buffer, ServerConnection c) {
        buffer = c.checkWriteBuffer(buffer, c.getPacketHeaderSize());
        BufferUtil.writeUB3(buffer, calcPacketSize());
        buffer.put(type);
        buffer = c.writeToBuffer(data, buffer);
        return buffer;
    }

    public void write(ServerConnection c) {
        ByteBuffer buffer = c.allocate();
        buffer = write(buffer, c);
        c.write(buffer);
    }

    public int calcPacketSize() {
        return data == null ? 0 : data.length;
    }

}
