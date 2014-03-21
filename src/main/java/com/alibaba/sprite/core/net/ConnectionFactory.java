package com.alibaba.sprite.core.net;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author xianmao.hexm
 */
public interface ConnectionFactory {

    Connection make(SocketChannel channel) throws IOException;

}
