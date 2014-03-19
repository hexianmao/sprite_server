package com.alibaba.sprite;

import java.util.Timer;
import java.util.TimerTask;

import com.alibaba.sprite.core.util.TimeUtil;

/**
 * @author hexianmao
 */
public class SystemTimer {

    private static final long TIME_UPDATE_PERIOD = 20L;

    private final Timer timer;

    public SystemTimer() {
        this.timer = new Timer("SystemTimer", true);
    }

    public void startup(SystemConfig config) {
        timer.schedule(updateTime(), 0L, TIME_UPDATE_PERIOD);
        //timer.schedule(processorCheck(sprite.getProcessors()), 0L, sc.getProcessorCheckPeriod());
        // timer.schedule(clusterHeartbeat(config), 0L, sc.getClusterHeartbeatPeriod());
    }

    public void scheldule(TimerTask task, long delay, long period) {
        timer.schedule(task, delay, period);
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

    //    // 处理器定时检查任务
    //    private TimerTask processorCheck(final NIOProcessor[] processors) {
    //        return new TimerTask() {
    //            @Override
    //            public void run() {
    //                Sprite.getInstance().getTaskExecutor().execute(new Runnable() {
    //                    @Override
    //                    public void run() {
    //                        for (NIOProcessor p : processors) {
    //                            p.check();
    //                        }
    //                    }
    //                });
    //            }
    //        };
    //    }

    //    // 集群节点定时心跳任务
    //    private TimerTask clusterHeartbeat(final Config config) {
    //        return new TimerTask() {
    //            @Override
    //            public void run() {
    //                Sprite.getInstance().getTaskExecutor().execute(new Runnable() {
    //                    @Override
    //                    public void run() {
    //                        Map<String, Node> nodes = config.getCluster().getNodes();
    //                        for (Node node : nodes.values()) {
    //                            node.doHeartbeat();
    //                        }
    //                    }
    //                });
    //            }
    //        };
    //    }

}
