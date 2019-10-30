/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.messaging.activemq.ha;


import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONFIGURATION;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LIVE_ONLY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_COLOCATED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_SLAVE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_COLOCATED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_SLAVE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SLAVE;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.ALLOW_FAILBACK;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.BACKUP_PORT_OFFSET;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.BACKUP_REQUEST_RETRIES;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.BACKUP_REQUEST_RETRY_INTERVAL;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.CHECK_FOR_LIVE_SERVER;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.CLUSTER_NAME;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.EXCLUDED_CONNECTORS;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.GROUP_NAME;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.MAX_BACKUPS;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.REQUEST_BACKUP;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.RESTART_BACKUP;
import static org.wildfly.extension.messaging.activemq.ha.ScaleDownAttributes.addScaleDownConfiguration;

import java.util.List;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.HAPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ScaleDownConfiguration;
import org.apache.activemq.artemis.core.config.ha.ColocatedPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.LiveOnlyPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.ReplicaPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.ReplicatedPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.SharedStoreMasterPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.SharedStoreSlavePolicyConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class HAPolicyConfigurationBuilder {

    private static final HAPolicyConfigurationBuilder INSTANCE = new HAPolicyConfigurationBuilder();

    private HAPolicyConfigurationBuilder() {
    }

    public static HAPolicyConfigurationBuilder getInstance() {
        return INSTANCE;
    }

    public void addHAPolicyConfiguration(OperationContext context, Configuration configuration, ModelNode model) throws OperationFailedException {

        if (!model.hasDefined(HA_POLICY)) {
            return;
        }
        Property prop = model.get(HA_POLICY).asProperty();
        ModelNode haPolicy = prop.getValue();

        final HAPolicyConfiguration haPolicyConfiguration;
        String type = prop.getName();
        switch (type) {
            case LIVE_ONLY: {
                haPolicyConfiguration = buildLiveOnlyConfiguration(context, haPolicy);
                break;
            }
            case REPLICATION_MASTER: {
                haPolicyConfiguration = buildReplicationMasterConfiguration(context, haPolicy);
                break;
            }
            case REPLICATION_SLAVE: {
                haPolicyConfiguration = buildReplicationSlaveConfiguration(context, haPolicy);
                break;
            }
            case REPLICATION_COLOCATED: {
                haPolicyConfiguration = buildReplicationColocatedConfiguration(context, haPolicy);
                break;
            }
            case SHARED_STORE_MASTER: {
                haPolicyConfiguration = buildSharedStoreMasterConfiguration(context, haPolicy);
                break;
            }
            case SHARED_STORE_SLAVE: {
                haPolicyConfiguration = buildSharedStoreSlaveConfiguration(context, haPolicy);
                break;
            }
            case SHARED_STORE_COLOCATED: {
                haPolicyConfiguration = buildSharedStoreColocatedConfiguration(context, haPolicy);
                break;
            }
            default: {
                throw new OperationFailedException("unknown ha policy type");
            }
        }
        configuration.setHAPolicyConfiguration(haPolicyConfiguration);
    }

    private HAPolicyConfiguration buildLiveOnlyConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ScaleDownConfiguration scaleDownConfiguration = ScaleDownAttributes.addScaleDownConfiguration(context, model);
        return new LiveOnlyPolicyConfiguration(scaleDownConfiguration);
    }

    private HAPolicyConfiguration buildReplicationMasterConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ReplicatedPolicyConfiguration haPolicyConfiguration = new ReplicatedPolicyConfiguration();
        haPolicyConfiguration.setCheckForLiveServer(CHECK_FOR_LIVE_SERVER.resolveModelAttribute(context, model).asBoolean())
                .setInitialReplicationSyncTimeout(INITIAL_REPLICATION_SYNC_TIMEOUT.resolveModelAttribute(context, model).asLong());
        ModelNode clusterName = CLUSTER_NAME.resolveModelAttribute(context, model);
        if (clusterName.isDefined()) {
            haPolicyConfiguration.setClusterName(clusterName.asString());
        }
        ModelNode groupName = GROUP_NAME.resolveModelAttribute(context, model);
        if (groupName.isDefined()) {
            haPolicyConfiguration.setGroupName(groupName.asString());
        }
        return haPolicyConfiguration;
    }

    private HAPolicyConfiguration buildReplicationSlaveConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ReplicaPolicyConfiguration haPolicyConfiguration = new ReplicaPolicyConfiguration()
                .setAllowFailBack(ALLOW_FAILBACK.resolveModelAttribute(context, model).asBoolean())
                .setInitialReplicationSyncTimeout(INITIAL_REPLICATION_SYNC_TIMEOUT.resolveModelAttribute(context, model).asLong())
                .setMaxSavedReplicatedJournalsSize(MAX_SAVED_REPLICATED_JOURNAL_SIZE.resolveModelAttribute(context, model).asInt())
                .setScaleDownConfiguration(addScaleDownConfiguration(context, model))
                .setRestartBackup(RESTART_BACKUP.resolveModelAttribute(context, model).asBoolean());
        ModelNode clusterName = CLUSTER_NAME.resolveModelAttribute(context, model);
        if (clusterName.isDefined()) {
            haPolicyConfiguration.setClusterName(clusterName.asString());
        }
        ModelNode groupName = GROUP_NAME.resolveModelAttribute(context, model);
        if (groupName.isDefined()) {
            haPolicyConfiguration.setGroupName(groupName.asString());
        }
        return haPolicyConfiguration;
    }

    private HAPolicyConfiguration buildReplicationColocatedConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ColocatedPolicyConfiguration haPolicyConfiguration = new ColocatedPolicyConfiguration()
                .setRequestBackup(REQUEST_BACKUP.resolveModelAttribute(context, model).asBoolean())
                .setBackupRequestRetries(BACKUP_REQUEST_RETRIES.resolveModelAttribute(context, model).asInt())
                .setBackupRequestRetryInterval(BACKUP_REQUEST_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong())
                .setMaxBackups(MAX_BACKUPS.resolveModelAttribute(context, model).asInt())
                .setBackupPortOffset(BACKUP_PORT_OFFSET.resolveModelAttribute(context, model).asInt());
        List<String> connectors = EXCLUDED_CONNECTORS.unwrap(context, model);
        if (!connectors.isEmpty()) {
            haPolicyConfiguration.setExcludedConnectors(connectors);
        }
        ModelNode masterConfigurationModel = model.get(CONFIGURATION, MASTER);
        HAPolicyConfiguration masterConfiguration = buildReplicationMasterConfiguration(context, masterConfigurationModel);
        haPolicyConfiguration.setLiveConfig(masterConfiguration);
        ModelNode slaveConfigurationModel = model.get(CONFIGURATION, SLAVE);
        HAPolicyConfiguration slaveConfiguration = buildReplicationSlaveConfiguration(context, slaveConfigurationModel);
        haPolicyConfiguration.setBackupConfig(slaveConfiguration);
        return haPolicyConfiguration;
    }

    private HAPolicyConfiguration buildSharedStoreMasterConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        return new SharedStoreMasterPolicyConfiguration()
                .setFailoverOnServerShutdown(FAILOVER_ON_SERVER_SHUTDOWN.resolveModelAttribute(context, model).asBoolean());
    }

    private HAPolicyConfiguration buildSharedStoreSlaveConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        return new SharedStoreSlavePolicyConfiguration()
                .setAllowFailBack(ALLOW_FAILBACK.resolveModelAttribute(context, model).asBoolean())
                .setFailoverOnServerShutdown(FAILOVER_ON_SERVER_SHUTDOWN.resolveModelAttribute(context, model).asBoolean())
                .setRestartBackup(RESTART_BACKUP.resolveModelAttribute(context, model).asBoolean())
                .setScaleDownConfiguration(ScaleDownAttributes.addScaleDownConfiguration(context, model));
    }

    private HAPolicyConfiguration buildSharedStoreColocatedConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ColocatedPolicyConfiguration haPolicyConfiguration = new ColocatedPolicyConfiguration()
                .setRequestBackup(REQUEST_BACKUP.resolveModelAttribute(context, model).asBoolean())
                .setBackupRequestRetries(BACKUP_REQUEST_RETRIES.resolveModelAttribute(context, model).asInt())
                .setBackupRequestRetryInterval(BACKUP_REQUEST_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong())
                .setMaxBackups(MAX_BACKUPS.resolveModelAttribute(context, model).asInt())
                .setBackupPortOffset(BACKUP_PORT_OFFSET.resolveModelAttribute(context, model).asInt());

        ModelNode masterConfigurationModel = model.get(CONFIGURATION, MASTER);
        HAPolicyConfiguration masterConfiguration = buildSharedStoreMasterConfiguration(context, masterConfigurationModel);
        haPolicyConfiguration.setLiveConfig(masterConfiguration);

        ModelNode slaveConfigurationModel = model.get(CONFIGURATION, SLAVE);
        HAPolicyConfiguration slaveConfiguration = buildSharedStoreSlaveConfiguration(context, slaveConfigurationModel);
        haPolicyConfiguration.setBackupConfig(slaveConfiguration);

        return haPolicyConfiguration;
    }
}
