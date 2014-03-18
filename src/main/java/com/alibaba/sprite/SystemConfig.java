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
package com.alibaba.sprite;

/**
 * @author xianmao.hexm
 */
public class SystemConfig {

    private static final int MANAGER_DEFAULT_PORT = 9999;
    private static final int SERVER_DEFAULT_PORT = 8888;
    private static final int SERVER_DEFAULT_EXECUTOR = 4;
    private static final int PROCESSOR_DEFAULT_COUNT = 4;
    private static final int PROCESSOR_DEFAULT_EXECUTOR = 4;
    private static final int PROCESSOR_DEFAULT_BUFFER_SIZE = 1024 * 1024 * 16;
    private static final int PROCESSOR_DEFAULT_CHUNK_SIZE = 4096;

    private int serverPort;
    private int managerPort;
    private int serverExecutor;
    private int processorCount;
    private int processorExecutor;
    private int processorBufferSize;
    private int processorChunkSize;

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getManagerPort() {
        return managerPort;
    }

    public void setManagerPort(int managerPort) {
        this.managerPort = managerPort;
    }

    public int getServerExecutor() {
        return serverExecutor;
    }

    public void setServerExecutor(int serverExecutor) {
        this.serverExecutor = serverExecutor;
    }

    public int getProcessorCount() {
        return processorCount;
    }

    public void setProcessorCount(int processorCount) {
        this.processorCount = processorCount;
    }

    public int getProcessorExecutor() {
        return processorExecutor;
    }

    public void setProcessorExecutor(int processorExecutor) {
        this.processorExecutor = processorExecutor;
    }

    public int getProcessorBufferSize() {
        return processorBufferSize;
    }

    public void setProcessorBufferSize(int processorBufferSize) {
        this.processorBufferSize = processorBufferSize;
    }

    public int getProcessorChunkSize() {
        return processorChunkSize;
    }

    public void setProcessorChunkSize(int processorChunkSize) {
        this.processorChunkSize = processorChunkSize;
    }

    public final static SystemConfig getDefault() {
        SystemConfig config = new SystemConfig();
        config.setManagerPort(MANAGER_DEFAULT_PORT);
        config.setServerPort(SERVER_DEFAULT_PORT);
        config.setServerExecutor(SERVER_DEFAULT_EXECUTOR);
        config.setProcessorCount(PROCESSOR_DEFAULT_COUNT);
        config.setProcessorExecutor(PROCESSOR_DEFAULT_EXECUTOR);
        config.setProcessorBufferSize(PROCESSOR_DEFAULT_BUFFER_SIZE);
        config.setProcessorChunkSize(PROCESSOR_DEFAULT_CHUNK_SIZE);
        return config;
    }

}
