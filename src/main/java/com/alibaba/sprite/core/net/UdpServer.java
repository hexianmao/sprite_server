package com.alibaba.sprite.core.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

import com.alibaba.sprite.core.BufferPool;

/**
 * @author xianmao.hexm
 */
public class UdpServer {

    private final String name;
    private final BufferPool buffers;
    private final DatagramChannel channel;
    private final Selector selector;

    public UdpServer(String name, int buffer, int chunk) throws IOException {
        this.name = name;
        this.buffers = new BufferPool(buffer, chunk);
        this.channel = DatagramChannel.open();
        this.selector = Selector.open();
    }

    public String getName() {
        return name;
    }

    public BufferPool getBuffers() {
        return buffers;
    }

    public DatagramChannel getChannel() {
        return channel;
    }

    public void startup(int port) throws IOException {
        final DatagramChannel channel = this.channel;
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(port));
        channel.register(selector, SelectionKey.OP_READ);

        final Selector selector = this.selector;
        for (;;) {
            selector.select(1000L);
            Set<SelectionKey> keys = selector.selectedKeys();
            try {
                for (SelectionKey key : keys) {
                    if (key.isValid() && key.isReadable()) {
                        ByteBuffer buffer = buffers.allocate();
                        SocketAddress source = channel.receive(buffer);
                        handle(buffer, source);
                    } else {
                        key.cancel();
                    }
                }
            } finally {
                keys.clear();
            }
        }
    }

    private void handle(ByteBuffer buffer, SocketAddress source) {

    }

}
