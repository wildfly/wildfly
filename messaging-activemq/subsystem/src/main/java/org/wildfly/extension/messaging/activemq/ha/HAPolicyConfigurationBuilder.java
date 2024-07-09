/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.ha;


import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONFIGURATION;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LIVE_ONLY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PRIMARY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_COLOCATED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_PRIMARY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_SECONDARY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_SLAVE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SECONDARY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_COLOCATED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_PRIMARY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_SECONDARY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_SLAVE;
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
import org.apache.activemq.artemis.core.config.ha.PrimaryOnlyPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.ReplicaPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.ReplicatedPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.SharedStorePrimaryPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.SharedStoreBackupPolicyConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

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
            case REPLICATION_MASTER:
            case REPLICATION_PRIMARY: {
                haPolicyConfiguration = buildReplicationPrimaryConfiguration(context, haPolicy);
                break;
            }
            case REPLICATION_SLAVE:
            case REPLICATION_SECONDARY: {
                haPolicyConfiguration = buildReplicationSecondaryConfiguration(context, haPolicy);
                break;
            }
            case REPLICATION_COLOCATED: {
                haPolicyConfiguration = buildReplicationColocatedConfiguration(context, haPolicy);
                break;
            }
            case SHARED_STORE_MASTER:
            case SHARED_STORE_PRIMARY: {
                haPolicyConfiguration = buildSharedStorePrimaryConfiguration(context, haPolicy);
                break;
            }
            case SHARED_STORE_SLAVE:
            case SHARED_STORE_SECONDARY: {
                haPolicyConfiguration = buildSharedStoreSecondaryConfiguration(context, haPolicy);
                break;
            }
            case SHARED_STORE_COLOCATED: {
                haPolicyConfiguration = buildSharedStoreColocatedConfiguration(context, haPolicy);
                break;
            }
            default: {
                throw MessagingLogger.ROOT_LOGGER.unknownHAPolicyType();
            }
        }
        configuration.setHAPolicyConfiguration(haPolicyConfiguration);
    }

    private HAPolicyConfiguration buildLiveOnlyConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ScaleDownConfiguration scaleDownConfiguration = ScaleDownAttributes.addScaleDownConfiguration(context, model);
        return new PrimaryOnlyPolicyConfiguration(scaleDownConfiguration);
    }

    private HAPolicyConfiguration buildReplicationPrimaryConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ReplicatedPolicyConfiguration haPolicyConfiguration = new ReplicatedPolicyConfiguration();
        haPolicyConfiguration.setCheckForActiveServer(CHECK_FOR_LIVE_SERVER.resolveModelAttribute(context, model).asBoolean())
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

    private HAPolicyConfiguration buildReplicationSecondaryConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
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
        ModelNode masterConfigurationModel = model.get(CONFIGURATION, PRIMARY);
        HAPolicyConfiguration masterConfiguration = buildReplicationPrimaryConfiguration(context, masterConfigurationModel);
        haPolicyConfiguration.setPrimaryConfig(masterConfiguration);
        ModelNode slaveConfigurationModel = model.get(CONFIGURATION, SECONDARY);
        HAPolicyConfiguration slaveConfiguration = buildReplicationSecondaryConfiguration(context, slaveConfigurationModel);
        haPolicyConfiguration.setBackupConfig(slaveConfiguration);
        return haPolicyConfiguration;
    }

    private HAPolicyConfiguration buildSharedStorePrimaryConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        return new SharedStorePrimaryPolicyConfiguration()
                .setFailoverOnServerShutdown(FAILOVER_ON_SERVER_SHUTDOWN.resolveModelAttribute(context, model).asBoolean());
    }

    private HAPolicyConfiguration buildSharedStoreSecondaryConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        return new SharedStoreBackupPolicyConfiguration()
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

        ModelNode masterConfigurationModel = model.get(CONFIGURATION, PRIMARY);
        HAPolicyConfiguration masterConfiguration = buildSharedStorePrimaryConfiguration(context, masterConfigurationModel);
        haPolicyConfiguration.setPrimaryConfig(masterConfiguration);

        ModelNode slaveConfigurationModel = model.get(CONFIGURATION, SECONDARY);
        HAPolicyConfiguration slaveConfiguration = buildSharedStoreSecondaryConfiguration(context, slaveConfigurationModel);
        haPolicyConfiguration.setBackupConfig(slaveConfiguration);

        return haPolicyConfiguration;
    }
}
