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
package com.alibaba.sprite.config;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.sprite.Cluster;
import com.alibaba.sprite.Node;
import com.alibaba.sprite.config.model.DataSourceConfig;
import com.alibaba.sprite.config.model.QuarantineConfig;
import com.alibaba.sprite.config.model.SchemaConfig;
import com.alibaba.sprite.config.model.SystemConfig;
import com.alibaba.sprite.config.model.UserConfig;
import com.alibaba.sprite.server.session.MySQLDataNode;
import com.alibaba.sprite.util.TimeUtil;

/**
 * @author xianmao.hexm
 */
public class Config {
    private static final int RELOAD = 1;
    private static final int ROLLBACK = 2;

    private volatile SystemConfig system;
    private volatile Cluster cluster;
    private volatile Cluster _cluster;
    private volatile QuarantineConfig quarantine;
    private volatile QuarantineConfig _quarantine;
    private volatile Map<String, UserConfig> users;
    private volatile Map<String, UserConfig> _users;
    private volatile Map<String, SchemaConfig> schemas;
    private volatile Map<String, SchemaConfig> _schemas;
    private volatile Map<String, MySQLDataNode> dataNodes;
    private volatile Map<String, MySQLDataNode> _dataNodes;
    private volatile Map<String, DataSourceConfig> dataSources;
    private volatile Map<String, DataSourceConfig> _dataSources;
    private long reloadTime;
    private long rollbackTime;
    private int status;
    private final ReentrantLock lock;

    public Config() {
        ConfigInitializer confInit = new ConfigInitializer();
        this.system = confInit.getSystem();
        this.users = confInit.getUsers();
        this.schemas = confInit.getSchemas();
        this.dataSources = confInit.getDataSources();
        this.dataNodes = confInit.getDataNodes();
        this.quarantine = confInit.getQuarantine();
        this.cluster = confInit.getCluster();

        this.reloadTime = TimeUtil.currentTimeMillis();
        this.rollbackTime = -1L;
        this.status = RELOAD;
        this.lock = new ReentrantLock();
    }

    public SystemConfig getSystem() {
        return system;
    }

    public Map<String, UserConfig> getUsers() {
        return users;
    }

    public Map<String, UserConfig> getBackupUsers() {
        return _users;
    }

    public Map<String, SchemaConfig> getSchemas() {
        return schemas;
    }

    public Map<String, SchemaConfig> getBackupSchemas() {
        return _schemas;
    }

    public Map<String, MySQLDataNode> getDataNodes() {
        return dataNodes;
    }

    public Map<String, MySQLDataNode> getBackupDataNodes() {
        return _dataNodes;
    }

    public Map<String, DataSourceConfig> getDataSources() {
        return dataSources;
    }

    public Map<String, DataSourceConfig> getBackupDataSources() {
        return _dataSources;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Cluster getBackupCluster() {
        return _cluster;
    }

    public QuarantineConfig getQuarantine() {
        return quarantine;
    }

    public QuarantineConfig getBackupQuarantine() {
        return _quarantine;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public long getReloadTime() {
        return reloadTime;
    }

    public long getRollbackTime() {
        return rollbackTime;
    }

    public void reload(Map<String, UserConfig> users, Map<String, SchemaConfig> schemas,
                       Map<String, MySQLDataNode> dataNodes, Map<String, DataSourceConfig> dataSources,
                       Cluster cluster, QuarantineConfig quarantine) {
        apply(users, schemas, dataNodes, dataSources, cluster, quarantine);
        this.reloadTime = TimeUtil.currentTimeMillis();
        this.status = RELOAD;
    }

    public boolean canRollback() {
        if (_users == null || _schemas == null || _dataNodes == null || _dataSources == null || _cluster == null
                || _quarantine == null || status == ROLLBACK) {
            return false;
        } else {
            return true;
        }
    }

    public void rollback(Map<String, UserConfig> users, Map<String, SchemaConfig> schemas,
                         Map<String, MySQLDataNode> dataNodes, Map<String, DataSourceConfig> dataSources,
                         Cluster cluster, QuarantineConfig quarantine) {
        apply(users, schemas, dataNodes, dataSources, cluster, quarantine);
        this.rollbackTime = TimeUtil.currentTimeMillis();
        this.status = ROLLBACK;
    }

    private void apply(Map<String, UserConfig> users, Map<String, SchemaConfig> schemas,
                       Map<String, MySQLDataNode> dataNodes, Map<String, DataSourceConfig> dataSources,
                       Cluster cluster, QuarantineConfig quarantine) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // stop heartbeat
            Cluster oldCluster = this.cluster;
            if (oldCluster != null) {
                Map<String, Node> nodes = oldCluster.getNodes();
                for (Node n : nodes.values()) {
                    if (n != null) {
                        n.stopHeartbeat();
                    }
                }
            }
            this._users = this.users;
            this._schemas = this.schemas;
            this._dataNodes = this.dataNodes;
            this._dataSources = this.dataSources;
            this._cluster = this.cluster;
            this._quarantine = this.quarantine;

            // start heartbeat
            if (cluster != null) {
                Map<String, Node> nodes = cluster.getNodes();
                for (Node n : nodes.values()) {
                    if (n != null) {
                        n.startHeartbeat();
                    }
                }
            }
            this.users = users;
            this.schemas = schemas;
            this.dataNodes = dataNodes;
            this.dataSources = dataSources;
            this.cluster = cluster;
            this.quarantine = quarantine;
        } finally {
            lock.unlock();
        }
    }

}
