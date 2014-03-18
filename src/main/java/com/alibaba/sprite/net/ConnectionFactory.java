package com.alibaba.sprite.net;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author xianmao.hexm
 */
public interface ConnectionFactory {

    Connection make(SocketChannel channel) throws IOException;

    static final IdGenerator ID_GENERATOR = new IdGenerator();

    static final class IdGenerator {

        private static final long MAX_VALUE = 0xffffffffL;

        private long acceptId = 0L;
        private final Object lock = new Object();

        public long newId() {
            synchronized (lock) {
                if (acceptId >= MAX_VALUE) {
                    acceptId = 0L;
                }
                return ++acceptId;
            }
        }
    }

}
