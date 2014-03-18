package com.alibaba.sprite.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * @author xianmao.hexm
 */
public final class Acceptor extends Thread {

    private static final Logger LOGGER = Logger.getLogger(Acceptor.class);

    private final int port;
    private final ServerSocketChannel channel;
    private final Selector selector;
    private ConnectionFactory factory;
    private Processor[] processors;
    private int nextProcessor;

    public Acceptor(String name, int port) throws IOException {
        super.setName(name);
        this.port = port;
        this.selector = Selector.open();
        this.channel = ServerSocketChannel.open();
        this.channel.socket().bind(new InetSocketAddress(port));
        this.channel.configureBlocking(false);
        this.channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public int getPort() {
        return port;
    }

    public void setFactory(ConnectionFactory factory) {
        this.factory = factory;
    }

    public void setProcessors(Processor[] processors) {
        this.processors = processors;
    }

    @Override
    public void run() {
        final Selector selector = this.selector;
        for (;;) {
            try {
                selector.select(1000L);
                Set<SelectionKey> keys = selector.selectedKeys();
                try {
                    for (SelectionKey key : keys) {
                        if (key.isValid() && key.isAcceptable()) {
                            accept();
                        } else {
                            key.cancel();
                        }
                    }
                } finally {
                    keys.clear();
                }
            } catch (Throwable e) {
                LOGGER.warn(getName(), e);
            }
        }
    }

    private void accept() {
        SocketChannel channel = null;
        try {
            channel = this.channel.accept();
            channel.configureBlocking(false);
            Connection c = factory.make(channel);
            c.accept(nextProcessor());
        } catch (Throwable e) {
            closeChannel(channel);
            LOGGER.warn(getName(), e);
        }
    }

    private Processor nextProcessor() {
        if (++nextProcessor == processors.length) {
            nextProcessor = 0;
        }
        return processors[nextProcessor];
    }

    private static void closeChannel(SocketChannel channel) {
        if (channel == null) {
            return;
        }
        Socket socket = channel.socket();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
        try {
            channel.close();
        } catch (IOException e) {
        }
    }

}
