package com.alibaba.sprite;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;

import com.alibaba.sprite.core.Log4jInitializer;
import com.alibaba.sprite.core.NameableExecutor;
import com.alibaba.sprite.core.net.Acceptor;
import com.alibaba.sprite.core.net.Processor;
import com.alibaba.sprite.core.util.ExecutorUtil;
import com.alibaba.sprite.core.util.TimeUtil;
import com.alibaba.sprite.manager.ManagerConnectionFactory;
import com.alibaba.sprite.server.ServerConnection;
import com.alibaba.sprite.server.ServerConnectionFactory;

/**
 * @author xianmao.hexm
 */
public class SpriteServer {

    private static final String NAME = SpriteServer.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(SpriteServer.class);
    private static final long LOG_WATCH_DELAY = 60000L;
    private static final String DATA_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final SpriteServer INSTANCE = new SpriteServer();

    private SystemConfig config;
    private SystemTimer serverTimer;
    private Acceptor managerAcceptor;
    private Acceptor serverAcceptor;
    private Processor[] processors;
    private NameableExecutor executor;
    private long startupTime;
    private ConcurrentMap<String, ServerConnection> users;

    public static final SpriteServer getInstance() {
        return INSTANCE;
    }

    public NameableExecutor getExecutor() {
        return executor;
    }

    public Processor[] getProcessors() {
        return processors;
    }

    public ConcurrentMap<String, ServerConnection> getUsers() {
        return users;
    }

    public long getStartupTime() {
        return startupTime;
    }

    private SpriteServer() {
        this.config = SystemConfig.getDefault();
        this.serverTimer = new SystemTimer();
        this.executor = ExecutorUtil.create("Server-Executor", config.getServerExecutor());
        this.users = new ConcurrentHashMap<String, ServerConnection>();
        this.startupTime = TimeUtil.currentTimeMillis();
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
        LOGGER.info("===================================================");
        LOGGER.info("System[" + NAME + "] is ready to startup ...");

        // startup timer
        LOGGER.info("Startup SystemTimer ...");
        serverTimer.startup(config);

        // startup processors
        LOGGER.info("Startup Processors ...");
        processors = new Processor[config.getProcessorCount()];
        for (int i = 0; i < processors.length; i++) {
            processors[i] = new Processor(
                    "Processor" + i,
                    config.getProcessorBufferSize(),
                    config.getProcessorChunkSize(),
                    config.getProcessorExecutor());
            processors[i].startup();
        }

        // startup manager
        managerAcceptor = new Acceptor("ManagerAcceptor", config.getManagerPort());
        managerAcceptor.setFactory(new ManagerConnectionFactory());
        managerAcceptor.setProcessors(processors);
        managerAcceptor.start();
        LOGGER.info("Startup " + managerAcceptor.getName() + " and Listening on " + managerAcceptor.getPort() + " ...");

        // startup server
        serverAcceptor = new Acceptor("ServerAcceptor", config.getServerPort());
        serverAcceptor.setFactory(new ServerConnectionFactory());
        serverAcceptor.setProcessors(processors);
        serverAcceptor.start();
        LOGGER.info("Startup " + serverAcceptor.getName() + " and Listening on " + serverAcceptor.getPort() + " ...");

        // startup completed
        LOGGER.info("System[" + NAME + "] startup completed ...");
        LOGGER.info("===================================================");
    }

    public static void main(String[] args) {
        try {
            SpriteServer.getInstance().startup();
        } catch (Throwable e) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATA_FORMAT);
            LogLog.error(sdf.format(new Date()) + " startup error", e);
            System.exit(-1);
        }
    }

}
