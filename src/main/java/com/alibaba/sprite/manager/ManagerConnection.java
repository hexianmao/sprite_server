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
package com.alibaba.sprite.manager;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.alibaba.sprite.SpriteServer;
import com.alibaba.sprite.core.BufferPool;
import com.alibaba.sprite.core.BufferQueue;
import com.alibaba.sprite.core.Capabilities;
import com.alibaba.sprite.core.ErrorCode;
import com.alibaba.sprite.core.Versions;
import com.alibaba.sprite.core.net.Connection;
import com.alibaba.sprite.core.net.Handler;
import com.alibaba.sprite.core.net.Processor;
import com.alibaba.sprite.core.packet.ErrorPacket;
import com.alibaba.sprite.core.packet.HandshakePacket;
import com.alibaba.sprite.core.util.CharsetUtil;
import com.alibaba.sprite.core.util.RandomUtil;
import com.alibaba.sprite.core.util.TimeUtil;

/**
 * @author xianmao.hexm
 */
public final class ManagerConnection implements Connection {

    private static final Logger LOGGER = Logger.getLogger(ManagerConnection.class);
    private static final int OP_NOT_READ = ~SelectionKey.OP_READ;
    private static final int OP_NOT_WRITE = ~SelectionKey.OP_WRITE;
    private static final long AUTH_TIMEOUT = 10 * 1000L;

    private final SocketChannel channel;
    private final ReentrantLock keyLock;
    private final ReentrantLock writeLock;
    private final AtomicBoolean isClosed;
    private long id;
    private String host;
    private int port;
    private int localPort;
    private int packetHeaderSize;
    private int maxPacketSize;
    private BufferQueue writeQueue;
    private long idleTimeout;
    private String charset;
    private int charsetIndex;
    private ByteBuffer readBuffer;
    private int readBufferOffset;
    private Processor processor;
    private boolean isRegistered;
    private SelectionKey selectionKey;
    private byte[] authSeed;
    private boolean isAuthenticated;
    private long lastReadTime;
    private long lastWriteTime;
    private long netInBytes;
    private long netOutBytes;
    private Handler handler;

    public ManagerConnection(SocketChannel channel) {
        this.channel = channel;
        this.keyLock = new ReentrantLock();
        this.writeLock = new ReentrantLock();
        this.isClosed = new AtomicBoolean(false);
        this.lastReadTime = TimeUtil.currentTimeMillis();
        this.lastWriteTime = this.lastReadTime;
    }

    public long getNetInBytes() {
        return netInBytes;
    }

    public long getNetOutBytes() {
        return netOutBytes;
    }

    public byte[] getAuthSeed() {
        return authSeed;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public String getCharset() {
        return charset;
    }

    public boolean setCharset(String charset) {
        int ci = CharsetUtil.getIndex(charset);
        if (ci > 0) {
            this.charset = charset;
            this.charsetIndex = ci;
            return true;
        } else {
            return false;
        }
    }

    public int getPacketHeaderSize() {
        return packetHeaderSize;
    }

    public void setPacketHeaderSize(int packetHeaderSize) {
        this.packetHeaderSize = packetHeaderSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public void setWriteQueue(BufferQueue writeQueue) {
        this.writeQueue = writeQueue;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public void setAuthenticated(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    /**
     * 分配缓存
     */
    public ByteBuffer allocate() {
        return processor.getBuffers().allocate();
    }

    /**
     * 回收缓存
     */
    public void recycle(ByteBuffer buffer) {
        processor.getBuffers().recycle(buffer);
    }

    public boolean isIdleTimeout() {
        if (isAuthenticated) {
            return TimeUtil.currentTimeMillis() > Math.max(lastWriteTime, lastReadTime) + idleTimeout;
        } else {
            return TimeUtil.currentTimeMillis() > Math.max(lastWriteTime, lastReadTime) + AUTH_TIMEOUT;
        }
    }

    /**
     * 连接被接受后，选择对应处理器和一些初始化工作。
     */
    public void accept(Processor processor) {
        this.processor = processor;
        this.readBuffer = processor.getBuffers().allocate();
        processor.postRegister(this);
    }

    /**
     * 连接第一次被处理器处理时，需要调用此方法。
     */
    public void register(Selector selector) throws IOException {
        try {
            selectionKey = channel.register(selector, SelectionKey.OP_READ, this);
            isRegistered = true;
        } finally {
            if (isClosed.get()) {
                clearSelectionKey();
            }
        }
        if (!isClosed.get()) {
            // 生成认证数据
            byte[] rand1 = RandomUtil.randomBytes(8);
            byte[] rand2 = RandomUtil.randomBytes(12);

            // 保存认证数据
            byte[] seed = new byte[rand1.length + rand2.length];
            System.arraycopy(rand1, 0, seed, 0, rand1.length);
            System.arraycopy(rand2, 0, seed, rand1.length, rand2.length);
            this.authSeed = seed;

            // 发送握手数据包
            HandshakePacket hs = new HandshakePacket();
            hs.packetId = 0;
            hs.protocolVersion = Versions.PROTOCOL_VERSION;
            hs.serverVersion = Versions.SERVER_VERSION;
            hs.threadId = id;
            hs.seed = rand1;
            hs.serverCapabilities = getServerCapabilities();
            hs.serverCharsetIndex = (byte) (charsetIndex & 0xff);
            hs.serverStatus = 2;
            hs.restOfScrambleBuff = rand2;
            hs.write(this);
        }
    }

    /**
     * 读取数据
     */
    public void read() throws IOException {
        if (isClosed.get()) {
            return;
        }
        // TODO 需要考虑关闭连接时readBuffer还在被引用的问题
        ByteBuffer buffer = this.readBuffer;
        int got = channel.read(buffer);
        lastReadTime = TimeUtil.currentTimeMillis();
        if (got < 0) {
            throw new EOFException();
        }
        netInBytes += got;
        processor.addNetInBytes(got);

        // 处理数据
        int offset = readBufferOffset, length = 0, position = buffer.position();
        for (;;) {
            length = getPacketLength(buffer, offset);
            if (length == -1) {// 未达到可计算数据包长度的数据
                if (!buffer.hasRemaining()) {
                    checkReadBuffer(buffer, offset, position);
                }
                break;
            }
            if (position >= offset + length) {
                // 提取一个数据包的数据进行处理
                buffer.position(offset);
                byte[] data = new byte[length];
                buffer.get(data, 0, length);
                handle(data);

                // 设置偏移量
                offset += length;
                if (position == offset) {// 数据正好全部处理完毕
                    if (readBufferOffset != 0) {
                        readBufferOffset = 0;
                    }
                    buffer.clear();
                    break;
                } else {// 还有剩余数据未处理
                    readBufferOffset = offset;
                    buffer.position(position);
                    continue;
                }
            } else {// 未到达一个数据包的数据
                if (!buffer.hasRemaining()) {
                    checkReadBuffer(buffer, offset, position);
                }
                break;
            }
        }
    }

    /**
     * 基于处理器队列的方式写数据
     */
    public void writeByQueue() throws IOException {
        if (isClosed.get()) {
            return;
        }
        final ReentrantLock lock = this.writeLock;
        lock.lock();
        try {
            // 满足以下两个条件时，切换到基于事件的写操作。
            // 1.当前key对写事件不该兴趣。
            // 2.write0()返回false。
            if ((selectionKey.interestOps() & SelectionKey.OP_WRITE) == 0 && !write0()) {
                enableWrite();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 基于监听事件的方式写数据
     */
    public void writeByEvent() throws IOException {
        if (isClosed.get()) {
            return;
        }
        final ReentrantLock lock = this.writeLock;
        lock.lock();
        try {
            // 满足以下两个条件时，切换到基于队列的写操作。
            // 1.write0()返回true。
            // 2.发送队列的buffer为空。
            if (write0() && writeQueue.size() == 0) {
                disableWrite();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 处理数据
     */
    protected void handle(final byte[] data) {
        SpriteServer.getInstance().getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    handler.handle(data);
                } catch (Throwable t) {
                    error(ErrorCode.ERR_HANDLE_DATA, t);
                }
            }
        });
    }

    /**
     * 发生错误
     */
    public void error(int errCode, Throwable t) {
        // 根据异常类型和信息，选择日志输出级别。
        if (t instanceof EOFException) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(toString(), t);
            }
        } else if (isConnectionReset(t)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(toString(), t);
            }
        } else {
            LOGGER.warn(toString(), t);
        }

        // 异常返回码处理
        switch (errCode) {
        case ErrorCode.ERR_HANDLE_DATA:
            String msg = t.getMessage();
            writeErrMessage((byte) 1, ErrorCode.ER_YES, msg == null ? t.getClass().getSimpleName() : msg);
            break;
        default:
            close();
        }
    }

    /**
     * 关闭连接
     */
    public boolean close() {
        if (isClosed.get()) {
            return false;
        } else {
            if (closeSocket()) {
                if (isClosed.compareAndSet(false, true)) {
                    cleanup();
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * 写出一块缓存数据
     */
    public void postWrite(ByteBuffer buffer) {
        if (isClosed.get()) {
            processor.getBuffers().recycle(buffer);
            return;
        }
        if (isRegistered) {
            try {
                writeQueue.put(buffer);
            } catch (InterruptedException e) {
                error(ErrorCode.ERR_PUT_WRITE_QUEUE, e);
                return;
            }
            processor.postWrite(this);
        } else {
            processor.getBuffers().recycle(buffer);
            close();
        }
    }

    /**
     * 评估当前Buffer的容量，不够则写出当前缓存块并申请新的缓存块。
     */
    public ByteBuffer evaluateBuffer(ByteBuffer buffer, int capacity) {
        if (capacity > buffer.remaining()) {
            postWrite(buffer);
            return processor.getBuffers().allocate();
        } else {
            return buffer;
        }
    }

    /**
     * 把数据写到给定的缓存中，如果满了则提交当前缓存并申请新的缓存。
     */
    public ByteBuffer writeToBuffer(byte[] src, ByteBuffer buffer) {
        int offset = 0;
        int length = src.length;
        int remaining = buffer.remaining();
        while (length > 0) {
            if (remaining >= length) {
                buffer.put(src, offset, length);
                break;
            } else {
                buffer.put(src, offset, remaining);
                postWrite(buffer);
                buffer = processor.getBuffers().allocate();
                offset += remaining;
                length -= remaining;
                remaining = buffer.remaining();
                continue;
            }
        }
        return buffer;
    }

    public void writeErrMessage(byte packetId, int errno, String msg) {
        ErrorPacket packet = new ErrorPacket();
        packet.packetId = packetId;
        packet.errno = errno;
        packet.message = encodeString(msg, charset);
        packet.write(this);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("[thread=")
                                  .append(Thread.currentThread().getName())
                                  .append(",host=")
                                  .append(host)
                                  .append(",port=")
                                  .append(port)
                                  .append(",localPort=")
                                  .append(localPort)
                                  .append(']')
                                  .toString();
    }

    /**
     * 打开读事件
     */
    protected void enableRead() {
        final Lock lock = this.keyLock;
        lock.lock();
        try {
            SelectionKey key = this.selectionKey;
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
        } finally {
            lock.unlock();
        }
        selectionKey.selector().wakeup();
    }

    /**
     * 关闭读事件
     */
    protected void disableRead() {
        final Lock lock = this.keyLock;
        lock.lock();
        try {
            SelectionKey key = this.selectionKey;
            key.interestOps(key.interestOps() & OP_NOT_READ);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 打开写事件
     */
    protected void enableWrite() {
        final Lock lock = this.keyLock;
        lock.lock();
        try {
            SelectionKey key = this.selectionKey;
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } finally {
            lock.unlock();
        }
        selectionKey.selector().wakeup();
    }

    /**
     * 关闭写事件
     */
    protected void disableWrite() {
        final Lock lock = this.keyLock;
        lock.lock();
        try {
            SelectionKey key = this.selectionKey;
            key.interestOps(key.interestOps() & OP_NOT_WRITE);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取数据包长度
     */
    private int getPacketLength(ByteBuffer buffer, int offset) {
        if (buffer.position() < offset + packetHeaderSize) {
            return -1;
        } else {
            int length = buffer.get(offset) & 0xff;
            length |= (buffer.get(++offset) & 0xff) << 8;
            length |= (buffer.get(++offset) & 0xff) << 16;
            return length + packetHeaderSize;
        }
    }

    private void clearSelectionKey() {
        final Lock lock = this.keyLock;
        lock.lock();
        try {
            SelectionKey key = this.selectionKey;
            if (key != null && key.isValid()) {
                key.attach(null);
                key.cancel();
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean write0() throws IOException {
        // 检查是否有遗留数据未写出
        ByteBuffer buffer = writeQueue.attachment();
        if (buffer != null) {
            int written = channel.write(buffer);
            if (written > 0) {
                netOutBytes += written;
                processor.addNetOutBytes(written);
            }
            lastWriteTime = TimeUtil.currentTimeMillis();
            if (buffer.hasRemaining()) {
                return false;
            } else {
                writeQueue.attach(null);
                processor.getBuffers().recycle(buffer);
            }
        }
        // 写出发送队列中的数据块
        if ((buffer = writeQueue.poll()) != null) {
            // 如果是一块未使用过的buffer，则执行关闭连接。
            if (buffer.position() == 0) {
                processor.getBuffers().recycle(buffer);
                close();
                return true;
            }
            buffer.flip();
            int written = channel.write(buffer);
            if (written > 0) {
                netOutBytes += written;
                processor.addNetOutBytes(written);
            }
            lastWriteTime = TimeUtil.currentTimeMillis();
            if (buffer.hasRemaining()) {
                writeQueue.attach(buffer);
                return false;
            } else {
                processor.getBuffers().recycle(buffer);
            }
        }
        return true;
    }

    /**
     * 检查ReadBuffer容量，不够则扩展当前缓存，直到最大值。
     */
    private ByteBuffer checkReadBuffer(ByteBuffer buffer, int offset, int position) {
        // 当偏移量为0时需要扩容，否则移动数据至偏移量为0的位置。
        if (offset == 0) {
            if (buffer.capacity() >= maxPacketSize) {
                throw new IllegalArgumentException("Packet size over the limit.");
            }
            int size = buffer.capacity() << 1;
            size = (size > maxPacketSize) ? maxPacketSize : size;
            ByteBuffer newBuffer = ByteBuffer.allocate(size);
            buffer.position(offset);
            newBuffer.put(buffer);
            readBuffer = newBuffer;
            // 回收扩容前的缓存块
            processor.getBuffers().recycle(buffer);
            return newBuffer;
        } else {
            buffer.position(offset);
            buffer.compact();
            readBufferOffset = 0;
            return buffer;
        }
    }

    private boolean closeSocket() {
        clearSelectionKey();
        SocketChannel channel = this.channel;
        if (channel != null) {
            boolean isSocketClosed = true;
            Socket socket = channel.socket();
            if (socket != null) {
                try {
                    socket.close();
                } catch (Throwable e) {
                }
                isSocketClosed = socket.isClosed();
            }
            try {
                channel.close();
            } catch (Throwable e) {
            }
            return isSocketClosed && (!channel.isOpen());
        } else {
            return true;
        }
    }

    /**
     * 清理遗留资源
     */
    private void cleanup() {
        BufferPool pool = processor.getBuffers();
        ByteBuffer buffer = null;

        // 回收接收缓存
        buffer = this.readBuffer;
        if (buffer != null) {
            this.readBuffer = null;
            pool.recycle(buffer);
        }

        // 回收发送缓存
        while ((buffer = writeQueue.poll()) != null) {
            pool.recycle(buffer);
        }
    }

    private static int getServerCapabilities() {
        int flag = 0;
        flag |= Capabilities.CLIENT_LONG_PASSWORD;
        flag |= Capabilities.CLIENT_FOUND_ROWS;
        flag |= Capabilities.CLIENT_LONG_FLAG;
        flag |= Capabilities.CLIENT_CONNECT_WITH_DB;
        // flag |= Capabilities.CLIENT_NO_SCHEMA;
        // flag |= Capabilities.CLIENT_COMPRESS;
        flag |= Capabilities.CLIENT_ODBC;
        // flag |= Capabilities.CLIENT_LOCAL_FILES;
        flag |= Capabilities.CLIENT_IGNORE_SPACE;
        flag |= Capabilities.CLIENT_PROTOCOL_41;
        flag |= Capabilities.CLIENT_INTERACTIVE;
        // flag |= Capabilities.CLIENT_SSL;
        flag |= Capabilities.CLIENT_IGNORE_SIGPIPE;
        flag |= Capabilities.CLIENT_TRANSACTIONS;
        // flag |= ServerDefs.CLIENT_RESERVED;
        flag |= Capabilities.CLIENT_SECURE_CONNECTION;
        return flag;
    }

    private static boolean isConnectionReset(Throwable t) {
        if (t instanceof IOException) {
            String msg = t.getMessage();
            return (msg != null && msg.contains("Connection reset by peer"));
        }
        return false;
    }

    private static byte[] encodeString(String src, String charset) {
        if (src == null) {
            return null;
        }
        if (charset == null) {
            return src.getBytes();
        }
        try {
            return src.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            return src.getBytes();
        }
    }

}
