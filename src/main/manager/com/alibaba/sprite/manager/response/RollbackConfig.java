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
package com.alibaba.sprite.manager.response;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.alibaba.sprite.Cluster;
import com.alibaba.sprite.Sprite;
import com.alibaba.sprite.config.Config;
import com.alibaba.sprite.config.ErrorCode;
import com.alibaba.sprite.config.model.DataSourceConfig;
import com.alibaba.sprite.config.model.QuarantineConfig;
import com.alibaba.sprite.config.model.SchemaConfig;
import com.alibaba.sprite.config.model.UserConfig;
import com.alibaba.sprite.manager.ManagerConnection;
import com.alibaba.sprite.net.packet.OkPacket;

/**
 * @author xianmao.hexm
 */
public final class RollbackConfig {
    private static final Logger LOGGER = Logger.getLogger(RollbackConfig.class);

    public static void execute(ManagerConnection c) {
        final ReentrantLock lock = Sprite.getInstance().getConfig().getLock();
        lock.lock();
        try {
            if (rollback()) {
                StringBuilder s = new StringBuilder();
                s.append(c).append("Rollback config success by manager");
                LOGGER.warn(s.toString());
                OkPacket ok = new OkPacket();
                ok.packetId = 1;
                ok.affectedRows = 1;
                ok.serverStatus = 2;
                ok.message = "Rollback config success".getBytes();
                ok.write(c);
            } else {
                c.writeErrMessage(ErrorCode.ER_YES, "Rollback config failure");
            }
        } finally {
            lock.unlock();
        }
    }

    private static boolean rollback() {
        Config conf = Sprite.getInstance().getConfig();
        Map<String, UserConfig> users = conf.getBackupUsers();
        Map<String, SchemaConfig> schemas = conf.getBackupSchemas();
        Map<String, DataSourceConfig> dataSources = conf.getBackupDataSources();
        Cluster cluster = conf.getBackupCluster();
        QuarantineConfig quarantine = conf.getBackupQuarantine();

        // 检查可回滚状态
        if (!conf.canRollback()) {
            return false;
        }

        // 应用回滚
        conf.rollback(users, schemas, dataSources, cluster, quarantine);

        return true;
    }

}
