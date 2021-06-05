/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.hornetq.api.core.client.HornetQClient;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.messaging.activemq.ha.HAAttributes;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;
import org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition;

public class AttributeDefaultsTest extends AbstractSubsystemTest {

    public AttributeDefaultsTest() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Test
    public void testAttributeValues() {
        Assert.assertNotEquals(ServerDefinition.GLOBAL_MAX_DISK_USAGE.getName(), ServerDefinition.GLOBAL_MAX_DISK_USAGE.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultMaxDiskUsage());
        Assert.assertNotEquals(ServerDefinition.GLOBAL_MAX_MEMORY_SIZE.getName(), ServerDefinition.GLOBAL_MAX_MEMORY_SIZE.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultMaxGlobalSize());
        Assert.assertNotEquals(ServerDefinition.JOURNAL_POOL_FILES.getName(), ServerDefinition.JOURNAL_POOL_FILES.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultJournalPoolFiles());

        Assert.assertEquals(BridgeDefinition.INITIAL_CONNECT_ATTEMPTS.getName(), BridgeDefinition.INITIAL_CONNECT_ATTEMPTS.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultBridgeInitialConnectAttempts());
        Assert.assertEquals(BridgeDefinition.RECONNECT_ATTEMPTS.getName(), BridgeDefinition.RECONNECT_ATTEMPTS.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultBridgeReconnectAttempts());
        Assert.assertEquals(BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE.getName(), BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultBridgeConnectSameNode());
        Assert.assertEquals(BridgeDefinition.USE_DUPLICATE_DETECTION.getName(), BridgeDefinition.USE_DUPLICATE_DETECTION.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultBridgeDuplicateDetection());

        Assert.assertEquals(ClusterConnectionDefinition.CHECK_PERIOD.getName(), ClusterConnectionDefinition.CHECK_PERIOD.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultClusterFailureCheckPeriod());
        Assert.assertEquals(ClusterConnectionDefinition.CONNECTION_TTL.getName(), ClusterConnectionDefinition.CONNECTION_TTL.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultClusterConnectionTtl());
        Assert.assertEquals(ClusterConnectionDefinition.INITIAL_CONNECT_ATTEMPTS.getName(), ClusterConnectionDefinition.INITIAL_CONNECT_ATTEMPTS.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultClusterInitialConnectAttempts());
        Assert.assertEquals(ClusterConnectionDefinition.MAX_HOPS.getName(), ClusterConnectionDefinition.MAX_HOPS.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultClusterMaxHops());
        Assert.assertEquals(ClusterConnectionDefinition.MAX_RETRY_INTERVAL.getName(), ClusterConnectionDefinition.MAX_RETRY_INTERVAL.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultClusterMaxRetryInterval());
        Assert.assertEquals(ClusterConnectionDefinition.MESSAGE_LOAD_BALANCING_TYPE.getName(), ClusterConnectionDefinition.MESSAGE_LOAD_BALANCING_TYPE.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultClusterMessageLoadBalancingType());
        Assert.assertEquals(ClusterConnectionDefinition.NOTIFICATION_ATTEMPTS.getName(), ClusterConnectionDefinition.NOTIFICATION_ATTEMPTS.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultClusterNotificationAttempts());
        Assert.assertEquals(ClusterConnectionDefinition.NOTIFICATION_INTERVAL.getName(), ClusterConnectionDefinition.NOTIFICATION_INTERVAL.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultClusterNotificationInterval());
        Assert.assertEquals(ClusterConnectionDefinition.PRODUCER_WINDOW_SIZE.getName(), ClusterConnectionDefinition.PRODUCER_WINDOW_SIZE.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultBridgeProducerWindowSize());
        Assert.assertEquals(ClusterConnectionDefinition.RECONNECT_ATTEMPTS.getName(), ClusterConnectionDefinition.RECONNECT_ATTEMPTS.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultClusterReconnectAttempts());
        Assert.assertEquals(ClusterConnectionDefinition.RETRY_INTERVAL.getName(), ClusterConnectionDefinition.RETRY_INTERVAL.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultClusterRetryInterval());
        Assert.assertEquals(ClusterConnectionDefinition.RETRY_INTERVAL_MULTIPLIER.getName(), ClusterConnectionDefinition.RETRY_INTERVAL_MULTIPLIER.getDefaultValue().asDouble(), ActiveMQDefaultConfiguration.getDefaultClusterRetryIntervalMultiplier(), 0);

        Assert.assertEquals(CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE.getName(), CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultBridgeConfirmationWindowSize());
        Assert.assertEquals(CommonAttributes.CALL_TIMEOUT.getName(), CommonAttributes.CALL_TIMEOUT.getDefaultValue().asLong(), ActiveMQClient.DEFAULT_CALL_TIMEOUT);
        Assert.assertEquals(CommonAttributes.CHECK_PERIOD.getName(), CommonAttributes.CHECK_PERIOD.getDefaultValue().asLong(), ActiveMQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD);
        Assert.assertEquals(CommonAttributes.CONNECTION_TTL.getName(), CommonAttributes.CONNECTION_TTL.getDefaultValue().asLong(), ActiveMQClient.DEFAULT_CONNECTION_TTL);
        Assert.assertEquals(CommonAttributes.HA.getName(), CommonAttributes.HA.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_HA);
        Assert.assertEquals(CommonAttributes.MAX_RETRY_INTERVAL.getName(), CommonAttributes.MAX_RETRY_INTERVAL.getDefaultValue().asLong(), ActiveMQClient.DEFAULT_MAX_RETRY_INTERVAL);
        Assert.assertEquals(CommonAttributes.MIN_LARGE_MESSAGE_SIZE.getName(), CommonAttributes.MIN_LARGE_MESSAGE_SIZE.getDefaultValue().asLong(), ActiveMQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE);
        Assert.assertEquals(CommonAttributes.RETRY_INTERVAL.getName(), CommonAttributes.RETRY_INTERVAL.getDefaultValue().asLong(), ActiveMQClient.DEFAULT_RETRY_INTERVAL);
        Assert.assertEquals(CommonAttributes.RETRY_INTERVAL_MULTIPLIER.getName(), CommonAttributes.RETRY_INTERVAL_MULTIPLIER.getDefaultValue().asDouble(), ActiveMQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER, 0);

        Assert.assertEquals(ConnectionFactoryAttributes.Common.AUTO_GROUP.getName(), ConnectionFactoryAttributes.Common.AUTO_GROUP.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_AUTO_GROUP);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.BLOCK_ON_ACKNOWLEDGE.getName(), ConnectionFactoryAttributes.Common.BLOCK_ON_ACKNOWLEDGE.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.BLOCK_ON_DURABLE_SEND.getName(), ConnectionFactoryAttributes.Common.BLOCK_ON_DURABLE_SEND.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_BLOCK_ON_DURABLE_SEND);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.BLOCK_ON_NON_DURABLE_SEND.getName(), ConnectionFactoryAttributes.Common.BLOCK_ON_NON_DURABLE_SEND.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.CACHE_LARGE_MESSAGE_CLIENT.getName(), ConnectionFactoryAttributes.Common.CACHE_LARGE_MESSAGE_CLIENT.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES.getName(), ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_COMPRESS_LARGE_MESSAGES);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE.getName(), ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE.getDefaultValue().asInt(), ActiveMQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.CONNECTION_LOAD_BALANCING_CLASS_NAME.getName(), ConnectionFactoryAttributes.Common.CONNECTION_LOAD_BALANCING_CLASS_NAME.getDefaultValue().asString(), ActiveMQClient.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.CONSUMER_MAX_RATE.getName(), ConnectionFactoryAttributes.Common.CONSUMER_MAX_RATE.getDefaultValue().asInt(), ActiveMQClient.DEFAULT_CONSUMER_MAX_RATE);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.CONSUMER_WINDOW_SIZE.getName(), ConnectionFactoryAttributes.Common.CONSUMER_WINDOW_SIZE.getDefaultValue().asInt(), ActiveMQClient.DEFAULT_CONSUMER_WINDOW_SIZE);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.DUPS_OK_BATCH_SIZE.getName(), ConnectionFactoryAttributes.Common.DUPS_OK_BATCH_SIZE.getDefaultValue().asInt(), ActiveMQClient.DEFAULT_ACK_BATCH_SIZE);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.FAILOVER_ON_INITIAL_CONNECTION.getName(), ConnectionFactoryAttributes.Common.FAILOVER_ON_INITIAL_CONNECTION.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.PRE_ACKNOWLEDGE.getName(), ConnectionFactoryAttributes.Common.PRE_ACKNOWLEDGE.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_PRE_ACKNOWLEDGE);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.PRODUCER_MAX_RATE.getName(), ConnectionFactoryAttributes.Common.PRODUCER_MAX_RATE.getDefaultValue().asInt(), ActiveMQClient.DEFAULT_PRODUCER_MAX_RATE);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.PRODUCER_WINDOW_SIZE.getName(), ConnectionFactoryAttributes.Common.PRODUCER_WINDOW_SIZE.getDefaultValue().asInt(), ActiveMQClient.DEFAULT_PRODUCER_WINDOW_SIZE);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.RECONNECT_ATTEMPTS.getName(), ConnectionFactoryAttributes.Common.RECONNECT_ATTEMPTS.getDefaultValue().asInt(), ActiveMQClient.DEFAULT_RECONNECT_ATTEMPTS);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.SCHEDULED_THREAD_POOL_MAX_SIZE.getName(), ConnectionFactoryAttributes.Common.SCHEDULED_THREAD_POOL_MAX_SIZE.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultScheduledThreadPoolMaxSize());
        Assert.assertEquals(ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS.getName(), ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_USE_GLOBAL_POOLS);
        Assert.assertEquals(ConnectionFactoryAttributes.Common.USE_TOPOLOGY.getName(), ConnectionFactoryAttributes.Common.USE_TOPOLOGY.getDefaultValue().asBoolean(), ActiveMQClient.DEFAULT_USE_TOPOLOGY_FOR_LOADBALANCING);

        Assert.assertEquals(DivertDefinition.EXCLUSIVE.getName(), DivertDefinition.EXCLUSIVE.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultDivertExclusive());

        Assert.assertEquals(GroupingHandlerDefinition.GROUP_TIMEOUT.getName(), GroupingHandlerDefinition.GROUP_TIMEOUT.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultGroupingHandlerGroupTimeout());
        Assert.assertEquals(GroupingHandlerDefinition.REAPER_PERIOD.getName(), GroupingHandlerDefinition.REAPER_PERIOD.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultGroupingHandlerReaperPeriod());
        Assert.assertEquals(GroupingHandlerDefinition.TIMEOUT.getName(), GroupingHandlerDefinition.TIMEOUT.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultGroupingHandlerTimeout());

        Assert.assertEquals(HAAttributes.ALLOW_FAILBACK.getName(), HAAttributes.ALLOW_FAILBACK.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultAllowAutoFailback());
        Assert.assertEquals(HAAttributes.BACKUP_PORT_OFFSET.getName(), HAAttributes.BACKUP_PORT_OFFSET.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultHapolicyBackupPortOffset());
        Assert.assertEquals(HAAttributes.BACKUP_REQUEST_RETRIES.getName(), HAAttributes.BACKUP_REQUEST_RETRIES.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultHapolicyBackupRequestRetries());
        Assert.assertEquals(HAAttributes.BACKUP_REQUEST_RETRY_INTERVAL.getName(), HAAttributes.BACKUP_REQUEST_RETRY_INTERVAL.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultHapolicyBackupRequestRetryInterval());
        Assert.assertEquals(HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN.getName(), HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultFailoverOnServerShutdown());
        Assert.assertEquals(HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT.getName(), HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultInitialReplicationSyncTimeout());
        Assert.assertEquals(HAAttributes.MAX_BACKUPS.getName(), HAAttributes.MAX_BACKUPS.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultHapolicyMaxBackups());
        Assert.assertEquals(HAAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE.getName(), HAAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultMaxSavedReplicatedJournalsSize());
        Assert.assertEquals(HAAttributes.REQUEST_BACKUP.getName(), HAAttributes.REQUEST_BACKUP.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultHapolicyRequestBackup());
        Assert.assertEquals(HAAttributes.RESTART_BACKUP.getName(), HAAttributes.RESTART_BACKUP.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultRestartBackup());

        Assert.assertEquals(JGroupsBroadcastGroupDefinition.BROADCAST_PERIOD.getName(), JGroupsBroadcastGroupDefinition.BROADCAST_PERIOD.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultBroadcastPeriod());

        Assert.assertEquals(JGroupsDiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.getName(), JGroupsDiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.getDefaultValue().asLong(), ActiveMQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT);
        Assert.assertEquals(JGroupsDiscoveryGroupDefinition.REFRESH_TIMEOUT.getName(), JGroupsDiscoveryGroupDefinition.REFRESH_TIMEOUT.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultBroadcastRefreshTimeout());

        Assert.assertEquals(LegacyConnectionFactoryDefinition.AUTO_GROUP.getName(), LegacyConnectionFactoryDefinition.AUTO_GROUP.getDefaultValue().asBoolean(), HornetQClient.DEFAULT_AUTO_GROUP);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.BLOCK_ON_ACKNOWLEDGE.getName(), LegacyConnectionFactoryDefinition.BLOCK_ON_ACKNOWLEDGE.getDefaultValue().asBoolean(), HornetQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.BLOCK_ON_DURABLE_SEND.getName(), LegacyConnectionFactoryDefinition.BLOCK_ON_DURABLE_SEND.getDefaultValue().asBoolean(), HornetQClient.DEFAULT_BLOCK_ON_DURABLE_SEND);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.BLOCK_ON_NON_DURABLE_SEND.getName(), LegacyConnectionFactoryDefinition.BLOCK_ON_NON_DURABLE_SEND.getDefaultValue().asBoolean(), HornetQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.CACHE_LARGE_MESSAGE_CLIENT.getName(), LegacyConnectionFactoryDefinition.CACHE_LARGE_MESSAGE_CLIENT.getDefaultValue().asBoolean(), HornetQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.CLIENT_FAILURE_CHECK_PERIOD.getName(), LegacyConnectionFactoryDefinition.CLIENT_FAILURE_CHECK_PERIOD.getDefaultValue().asLong(), HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.COMPRESS_LARGE_MESSAGES.getName(), LegacyConnectionFactoryDefinition.COMPRESS_LARGE_MESSAGES.getDefaultValue().asBoolean(), HornetQClient.DEFAULT_COMPRESS_LARGE_MESSAGES);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.CONFIRMATION_WINDOW_SIZE.getName(), LegacyConnectionFactoryDefinition.CONFIRMATION_WINDOW_SIZE.getDefaultValue().asInt(), HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.CONNECTION_LOAD_BALANCING_CLASS_NAME.getName(), LegacyConnectionFactoryDefinition.CONNECTION_LOAD_BALANCING_CLASS_NAME.getDefaultValue().asString(), HornetQClient.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.CONNECTION_TTL.getName(), LegacyConnectionFactoryDefinition.CONNECTION_TTL.getDefaultValue().asLong(), HornetQClient.DEFAULT_CONNECTION_TTL);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.CONSUMER_MAX_RATE.getName(), LegacyConnectionFactoryDefinition.CONSUMER_MAX_RATE.getDefaultValue().asInt(), HornetQClient.DEFAULT_CONSUMER_MAX_RATE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.CONSUMER_WINDOW_SIZE.getName(), LegacyConnectionFactoryDefinition.CONSUMER_WINDOW_SIZE.getDefaultValue().asInt(), HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.DUPS_OK_BATCH_SIZE.getName(), LegacyConnectionFactoryDefinition.DUPS_OK_BATCH_SIZE.getDefaultValue().asInt(), HornetQClient.DEFAULT_ACK_BATCH_SIZE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.FAILOVER_ON_INITIAL_CONNECTION.getName(), LegacyConnectionFactoryDefinition.FAILOVER_ON_INITIAL_CONNECTION.getDefaultValue().asBoolean(), HornetQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.INITIAL_CONNECT_ATTEMPTS.getName(), LegacyConnectionFactoryDefinition.INITIAL_CONNECT_ATTEMPTS.getDefaultValue().asInt(), HornetQClient.INITIAL_CONNECT_ATTEMPTS);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.INITIAL_MESSAGE_PACKET_SIZE.getName(), LegacyConnectionFactoryDefinition.INITIAL_MESSAGE_PACKET_SIZE.getDefaultValue().asInt(), HornetQClient.DEFAULT_INITIAL_MESSAGE_PACKET_SIZE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.MAX_RETRY_INTERVAL.getName(), LegacyConnectionFactoryDefinition.MAX_RETRY_INTERVAL.getDefaultValue().asLong(), HornetQClient.DEFAULT_MAX_RETRY_INTERVAL);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.MIN_LARGE_MESSAGE_SIZE.getName(), LegacyConnectionFactoryDefinition.MIN_LARGE_MESSAGE_SIZE.getDefaultValue().asInt(), HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.PRE_ACKNOWLEDGE.getName(), LegacyConnectionFactoryDefinition.PRE_ACKNOWLEDGE.getDefaultValue().asBoolean(), HornetQClient.DEFAULT_PRE_ACKNOWLEDGE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.PRODUCER_MAX_RATE.getName(), LegacyConnectionFactoryDefinition.PRODUCER_MAX_RATE.getDefaultValue().asInt(), HornetQClient.DEFAULT_PRODUCER_MAX_RATE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.PRODUCER_WINDOW_SIZE.getName(), LegacyConnectionFactoryDefinition.PRODUCER_WINDOW_SIZE.getDefaultValue().asInt(), HornetQClient.DEFAULT_PRODUCER_WINDOW_SIZE);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.RECONNECT_ATTEMPTS.getName(), LegacyConnectionFactoryDefinition.RECONNECT_ATTEMPTS.getDefaultValue().asInt(), HornetQClient.DEFAULT_RECONNECT_ATTEMPTS);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.RETRY_INTERVAL.getName(), LegacyConnectionFactoryDefinition.RETRY_INTERVAL.getDefaultValue().asLong(), HornetQClient.DEFAULT_RETRY_INTERVAL);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.RETRY_INTERVAL_MULTIPLIER.getName(), LegacyConnectionFactoryDefinition.RETRY_INTERVAL_MULTIPLIER.getDefaultValue().asDouble(), HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER, 0);
        Assert.assertEquals(LegacyConnectionFactoryDefinition.USE_GLOBAL_POOLS.getName(), LegacyConnectionFactoryDefinition.USE_GLOBAL_POOLS.getDefaultValue().asBoolean(), HornetQClient.DEFAULT_USE_GLOBAL_POOLS);

        Assert.assertEquals(ServerDefinition.ASYNC_CONNECTION_EXECUTION_ENABLED.getName(), ServerDefinition.ASYNC_CONNECTION_EXECUTION_ENABLED.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultAsyncConnectionExecutionEnabled());
        Assert.assertEquals(ServerDefinition.CLUSTER_PASSWORD.getName(), ServerDefinition.CLUSTER_PASSWORD.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultClusterPassword());
        Assert.assertEquals(ServerDefinition.CLUSTER_USER.getName(), ServerDefinition.CLUSTER_USER.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultClusterUser());
        Assert.assertEquals(ServerDefinition.CONNECTION_TTL_OVERRIDE.getName(), ServerDefinition.CONNECTION_TTL_OVERRIDE.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultConnectionTtlOverride());
        Assert.assertEquals(ServerDefinition.CREATE_BINDINGS_DIR.getName(), ServerDefinition.CREATE_BINDINGS_DIR.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultCreateBindingsDir());
        Assert.assertEquals(ServerDefinition.CREATE_JOURNAL_DIR.getName(), ServerDefinition.CREATE_JOURNAL_DIR.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultCreateJournalDir());
        Assert.assertEquals(ServerDefinition.DISK_SCAN_PERIOD.getName(), ServerDefinition.DISK_SCAN_PERIOD.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultDiskScanPeriod());
        Assert.assertEquals(ServerDefinition.ID_CACHE_SIZE.getName(), ServerDefinition.ID_CACHE_SIZE.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultIdCacheSize());
        Assert.assertEquals(ServerDefinition.JMX_DOMAIN.getName(), ServerDefinition.JMX_DOMAIN.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultJmxDomain());
        Assert.assertEquals(ServerDefinition.JOURNAL_BINDINGS_TABLE.getName(), ServerDefinition.JOURNAL_BINDINGS_TABLE.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultBindingsTableName());
        Assert.assertEquals(ServerDefinition.JOURNAL_COMPACT_MIN_FILES.getName(), ServerDefinition.JOURNAL_COMPACT_MIN_FILES.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultJournalCompactMinFiles());
        Assert.assertEquals(ServerDefinition.JOURNAL_COMPACT_PERCENTAGE.getName(), ServerDefinition.JOURNAL_COMPACT_PERCENTAGE.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultJournalCompactPercentage());
        Assert.assertEquals(ServerDefinition.JOURNAL_FILE_OPEN_TIMEOUT.getName(), ServerDefinition.JOURNAL_FILE_OPEN_TIMEOUT.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultJournalFileOpenTimeout());
        Assert.assertEquals(ServerDefinition.JOURNAL_FILE_SIZE.getName(), ServerDefinition.JOURNAL_FILE_SIZE.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultJournalFileSize());
        Assert.assertEquals(ServerDefinition.JOURNAL_JDBC_LOCK_EXPIRATION.getName(), ServerDefinition.JOURNAL_JDBC_LOCK_EXPIRATION.getDefaultValue().asInt() * 1000, ActiveMQDefaultConfiguration.getDefaultJdbcLockExpirationMillis());
        Assert.assertEquals(ServerDefinition.JOURNAL_JDBC_LOCK_RENEW_PERIOD.getName(), ServerDefinition.JOURNAL_JDBC_LOCK_RENEW_PERIOD.getDefaultValue().asInt() * 1000, ActiveMQDefaultConfiguration.getDefaultJdbcLockRenewPeriodMillis());
        Assert.assertEquals(ServerDefinition.JOURNAL_JDBC_NETWORK_TIMEOUT.getName(), ServerDefinition.JOURNAL_JDBC_NETWORK_TIMEOUT.getDefaultValue().asInt() * 1000, ActiveMQDefaultConfiguration.getDefaultJdbcNetworkTimeout());
        Assert.assertEquals(ServerDefinition.JOURNAL_LARGE_MESSAGES_TABLE.getName(), ServerDefinition.JOURNAL_LARGE_MESSAGES_TABLE.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultLargeMessagesTableName());
        Assert.assertEquals(ServerDefinition.JOURNAL_MESSAGES_TABLE.getName(), ServerDefinition.JOURNAL_MESSAGES_TABLE.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultMessageTableName());
        Assert.assertEquals(ServerDefinition.JOURNAL_MIN_FILES.getName(), ServerDefinition.JOURNAL_MIN_FILES.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultJournalMinFiles());
        Assert.assertEquals(ServerDefinition.JOURNAL_NODE_MANAGER_STORE_TABLE.getName(), ServerDefinition.JOURNAL_NODE_MANAGER_STORE_TABLE.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultNodeManagerStoreTableName());
        Assert.assertEquals(ServerDefinition.JOURNAL_PAGE_STORE_TABLE.getName(), ServerDefinition.JOURNAL_PAGE_STORE_TABLE.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultPageStoreTableName());
        Assert.assertEquals(ServerDefinition.JOURNAL_SYNC_NON_TRANSACTIONAL.getName(), ServerDefinition.JOURNAL_SYNC_NON_TRANSACTIONAL.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultJournalSyncNonTransactional());
        Assert.assertEquals(ServerDefinition.JOURNAL_SYNC_TRANSACTIONAL.getName(), ServerDefinition.JOURNAL_SYNC_TRANSACTIONAL.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultJournalSyncTransactional());
        Assert.assertEquals(ServerDefinition.LOG_JOURNAL_WRITE_RATE.getName(), ServerDefinition.LOG_JOURNAL_WRITE_RATE.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultJournalLogWriteRate());
        Assert.assertEquals(ServerDefinition.MANAGEMENT_ADDRESS.getName(), ServerDefinition.MANAGEMENT_ADDRESS.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultManagementAddress().toString());
        Assert.assertEquals(ServerDefinition.MANAGEMENT_NOTIFICATION_ADDRESS.getName(), ServerDefinition.MANAGEMENT_NOTIFICATION_ADDRESS.getDefaultValue().asString(), ActiveMQDefaultConfiguration.getDefaultManagementNotificationAddress().toString());
        Assert.assertEquals(ServerDefinition.MEMORY_MEASURE_INTERVAL.getName(), ServerDefinition.MEMORY_MEASURE_INTERVAL.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultMemoryMeasureInterval());
        Assert.assertEquals(ServerDefinition.MEMORY_WARNING_THRESHOLD.getName(), ServerDefinition.MEMORY_WARNING_THRESHOLD.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultMemoryWarningThreshold());
        Assert.assertEquals(ServerDefinition.MESSAGE_COUNTER_MAX_DAY_HISTORY.getName(), ServerDefinition.MESSAGE_COUNTER_MAX_DAY_HISTORY.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultMessageCounterMaxDayHistory());
        Assert.assertEquals(ServerDefinition.MESSAGE_COUNTER_SAMPLE_PERIOD.getName(), ServerDefinition.MESSAGE_COUNTER_SAMPLE_PERIOD.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultMessageCounterSamplePeriod());
        Assert.assertEquals(ServerDefinition.MESSAGE_EXPIRY_SCAN_PERIOD.getName(), ServerDefinition.MESSAGE_EXPIRY_SCAN_PERIOD.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultMessageExpiryScanPeriod());
        Assert.assertEquals(ServerDefinition.MESSAGE_EXPIRY_THREAD_PRIORITY.getName(), ServerDefinition.MESSAGE_EXPIRY_THREAD_PRIORITY.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultMessageExpiryThreadPriority());
        Assert.assertEquals(ServerDefinition.PAGE_MAX_CONCURRENT_IO.getName(), ServerDefinition.PAGE_MAX_CONCURRENT_IO.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultMaxConcurrentPageIo());
        Assert.assertEquals(ServerDefinition.PERSISTENCE_ENABLED.getName(), ServerDefinition.PERSISTENCE_ENABLED.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultPersistenceEnabled());
        Assert.assertEquals(ServerDefinition.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY.getName(), ServerDefinition.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultPersistDeliveryCountBeforeDelivery());
        Assert.assertEquals(ServerDefinition.PERSIST_ID_CACHE.getName(), ServerDefinition.PERSIST_ID_CACHE.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultPersistIdCache());
        Assert.assertEquals(ServerDefinition.SECURITY_ENABLED.getName(), ServerDefinition.SECURITY_ENABLED.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultSecurityEnabled());
        Assert.assertEquals(ServerDefinition.SECURITY_INVALIDATION_INTERVAL.getName(), ServerDefinition.SECURITY_INVALIDATION_INTERVAL.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultSecurityInvalidationInterval());
        Assert.assertEquals(ServerDefinition.SERVER_DUMP_INTERVAL.getName(), ServerDefinition.SERVER_DUMP_INTERVAL.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultServerDumpInterval());
        Assert.assertEquals(ServerDefinition.THREAD_POOL_MAX_SIZE.getName(), ServerDefinition.THREAD_POOL_MAX_SIZE.getDefaultValue().asInt(), ActiveMQDefaultConfiguration.getDefaultThreadPoolMaxSize());
        Assert.assertEquals(ServerDefinition.TRANSACTION_TIMEOUT.getName(), ServerDefinition.TRANSACTION_TIMEOUT.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultTransactionTimeout());
        Assert.assertEquals(ServerDefinition.TRANSACTION_TIMEOUT_SCAN_PERIOD.getName(), ServerDefinition.TRANSACTION_TIMEOUT_SCAN_PERIOD.getDefaultValue().asLong(), ActiveMQDefaultConfiguration.getDefaultTransactionTimeoutScanPeriod());
        Assert.assertEquals(ServerDefinition.WILD_CARD_ROUTING_ENABLED.getName(), ServerDefinition.WILD_CARD_ROUTING_ENABLED.getDefaultValue().asBoolean(), ActiveMQDefaultConfiguration.isDefaultWildcardRoutingEnabled());
    }
}
