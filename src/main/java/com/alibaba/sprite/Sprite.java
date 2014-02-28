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
package com.alibaba.sprite;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;

import com.alibaba.sprite.config.Config;
import com.alibaba.sprite.config.model.SystemConfig;
import com.alibaba.sprite.manager.ManagerConnectionFactory;
import com.alibaba.sprite.net.NIOAcceptor;
import com.alibaba.sprite.net.NIOConnector;
import com.alibaba.sprite.net.NIOProcessor;
import com.alibaba.sprite.server.ServerConnectionFactory;
import com.alibaba.sprite.statistic.SQLRecorder;
import com.alibaba.sprite.util.ExecutorUtil;
import com.alibaba.sprite.util.NameableExecutor;
import com.alibaba.sprite.util.TimeUtil;

/**
 * @author xianmao.hexm 2011-4-19 下午02:58:59
 */
public final class Sprite {
    private static final String NAME = Sprite.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(Sprite.class);
    private static final long LOG_WATCH_DELAY = 60000L;
    private static final String DATA_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final Sprite INSTANCE = new Sprite();

    private final Config config;
    private final SystemTimer timer;
    private final NameableExecutor taskExecutor;
    private final NameableExecutor managerExecutor;
    private final NameableExecutor serverExecutor;
    private final SQLRecorder sqlRecorder;
    private final AtomicBoolean isOnline;
    private final long startupTime;
    private NIOProcessor[] processors;
    private NIOConnector connector;
    private NIOAcceptor manager;
    private NIOAcceptor server;

    public static final Sprite getInstance() {
        return INSTANCE;
    }

    private Sprite() {
        this.config = new Config();
        this.timer = new SystemTimer();
        SystemConfig sc = config.getSystem();
        this.taskExecutor = ExecutorUtil.create("Task-Executor", sc.getTaskExecutor());
        this.managerExecutor = ExecutorUtil.create("Mamanger-Executor", sc.getManagerExecutor());
        this.serverExecutor = ExecutorUtil.create("Server-Executor", sc.getServerExecutor());
        this.sqlRecorder = new SQLRecorder(sc.getSqlRecordCount());
        this.isOnline = new AtomicBoolean(true);
        this.startupTime = TimeUtil.currentTimeMillis();
    }

    public Config getConfig() {
        return config;
    }

    public NIOProcessor[] getProcessors() {
        return processors;
    }

    public NIOConnector getConnector() {
        return connector;
    }

    public NameableExecutor getManagerExecutor() {
        return managerExecutor;
    }

    public NameableExecutor getServerExecutor() {
        return serverExecutor;
    }

    public NameableExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public SQLRecorder getSqlRecorder() {
        return sqlRecorder;
    }

    public long getStartupTime() {
        return startupTime;
    }

    public boolean isOnline() {
        return isOnline.get();
    }

    public void offline() {
        isOnline.set(false);
    }

    public void online() {
        isOnline.set(true);
    }

    private void startup() throws IOException {
        // before start
        String home = System.getProperty("sprite.home");
        if (home == null) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT);
            LogLog.warn(sdf.format(new Date()) + " [sprite.home] is not set.");
        } else {
            Log4jInitializer.configureAndWatch(home + "/conf/log4j.xml", LOG_WATCH_DELAY);
        }

        // startup system
        SystemConfig system = config.getSystem();
        LOGGER.info("================================================");
        LOGGER.info("System[" + NAME + "] is ready to startup ...");

        // startup processors
        LOGGER.info("Startup Processors ...");
        processors = new NIOProcessor[system.getProcessors()];
        for (int i = 0; i < processors.length; i++) {
            processors[i] = new NIOProcessor("Processor" + i);
            processors[i].startup();
        }

        // startup connector
        LOGGER.info("Startup Connector ...");
        connector = new NIOConnector("Connector");
        connector.setProcessors(processors);
        connector.start();

        // startup manager
        ManagerConnectionFactory mf = new ManagerConnectionFactory();
        mf.setCharset(system.getCharset());
        mf.setIdleTimeout(system.getIdleTimeout());
        manager = new NIOAcceptor("Manager", system.getManagerPort(), mf);
        manager.setProcessors(processors);
        manager.start();
        LOGGER.info("Startup " + manager.getName() + " and Listening on " + manager.getPort() + " ...");

        // startup server
        ServerConnectionFactory sf = new ServerConnectionFactory();
        sf.setIdleTimeout(system.getIdleTimeout());
        server = new NIOAcceptor("Server", system.getServerPort(), sf);
        server.setProcessors(processors);
        server.start();
        LOGGER.info("Startup " + server.getName() + " and Listening on " + server.getPort() + " ...");

        // startup systemTimer
        LOGGER.info("Startup SystemTimer ...");
        timer.startup(this);

        // startup completed
        LOGGER.info("System[" + NAME + "] startup completed ...");
        LOGGER.info("================================================");
    }

    // main
    public static void main(String[] args) {
        try {
            Sprite.getInstance().startup();
        } catch (Throwable e) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT);
            LogLog.error(sdf.format(new Date()) + " startup error", e);
            System.exit(-1);
        }
    }

}
