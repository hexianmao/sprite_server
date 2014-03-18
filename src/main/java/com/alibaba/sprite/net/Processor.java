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
package com.alibaba.sprite.net;

import java.io.IOException;

import com.alibaba.sprite.util.BufferPool;
import com.alibaba.sprite.util.ExecutorUtil;
import com.alibaba.sprite.util.NameableExecutor;

/**
 * @author xianmao.hexm
 */
public final class Processor {

    private final String name;
    private final Reactor reactor;
    private final BufferPool buffers;
    private final NameableExecutor executor;
    private long netInBytes;
    private long netOutBytes;

    public Processor(String name, int buffer, int chunk, int executor) throws IOException {
        this.name = name;
        this.reactor = new Reactor(name);
        this.buffers = new BufferPool(buffer, chunk);
        this.executor = (executor > 0) ? ExecutorUtil.create(name + "-Executor", executor) : null;
    }

    public String getName() {
        return name;
    }

    public BufferPool getBuffers() {
        return buffers;
    }

    public NameableExecutor getExecutor() {
        return executor;
    }

    public int getRegisterQueueSize() {
        return reactor.getRegisterQueue().size();
    }

    public int getWriteQueueSize() {
        return reactor.getWriteQueue().size();
    }

    public void startup() {
        reactor.startup();
    }

    public void postRegister(Connection c) {
        reactor.postRegister(c);
    }

    public void postWrite(Connection c) {
        reactor.postWrite(c);
    }

    public long getNetInBytes() {
        return netInBytes;
    }

    public void addNetInBytes(long bytes) {
        netInBytes += bytes;
    }

    public long getNetOutBytes() {
        return netOutBytes;
    }

    public void addNetOutBytes(long bytes) {
        netOutBytes += bytes;
    }

    public long getReactCount() {
        return reactor.getReactCount();
    }

}
