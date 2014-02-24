package com.alibaba.sprite;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.alibaba.sprite.config.Config;
import com.alibaba.sprite.config.model.SystemConfig;
import com.alibaba.sprite.net.NIOProcessor;
import com.alibaba.sprite.server.session.MySQLDataNode;
import com.alibaba.sprite.util.TimeUtil;

/**
 * 系统定时器
 * 
 * @author hexianmao
 */
public class SystemTimer {
    private static final long TIME_UPDATE_PERIOD = 20L;

    private final Timer timer;

    public SystemTimer() {
        this.timer = new Timer("SystemTimer", true);
    }

    public void startup(Sprite sprite) {
        Config config = sprite.getConfig();
        SystemConfig sc = config.getSystem();
        timer.schedule(updateTime(), 0L, TIME_UPDATE_PERIOD);
        timer.schedule(processorCheck(sprite.getProcessors()), 0L, sc.getProcessorCheckPeriod());
        timer.schedule(dataNodeIdleCheck(config), 0L, sc.getDataNodeIdleCheckPeriod());
        timer.schedule(clusterHeartbeat(config), 0L, sc.getClusterHeartbeatPeriod());
    }

    // 系统时间定时更新任务
    private TimerTask updateTime() {
        return new TimerTask() {
            @Override
            public void run() {
                TimeUtil.update();
            }
        };
    }

    // 处理器定时检查任务
    private TimerTask processorCheck(final NIOProcessor[] processors) {
        return new TimerTask() {
            @Override
            public void run() {
                Sprite.getInstance().getTaskExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        for (NIOProcessor p : processors) {
                            p.check();
                        }
                    }
                });
            }
        };
    }

    // 数据节点定时连接空闲超时检查任务
    private TimerTask dataNodeIdleCheck(final Config config) {
        return new TimerTask() {
            @Override
            public void run() {
                Sprite.getInstance().getTaskExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, MySQLDataNode> nodes = config.getDataNodes();
                        for (MySQLDataNode node : nodes.values()) {
                            node.idleCheck();
                        }
                        Map<String, MySQLDataNode> _nodes = config.getBackupDataNodes();
                        if (_nodes != null) {
                            for (MySQLDataNode node : _nodes.values()) {
                                node.idleCheck();
                            }
                        }
                    }
                });
            }
        };
    }

    // 集群节点定时心跳任务
    private TimerTask clusterHeartbeat(final Config config) {
        return new TimerTask() {
            @Override
            public void run() {
                Sprite.getInstance().getTaskExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, Node> nodes = config.getCluster().getNodes();
                        for (Node node : nodes.values()) {
                            node.doHeartbeat();
                        }
                    }
                });
            }
        };
    }

}
