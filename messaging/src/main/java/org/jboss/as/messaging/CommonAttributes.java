/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.config.impl.FileConfiguration;
import org.hornetq.core.server.group.impl.GroupingHandlerConfiguration;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.domain.http.server.Constants;
import org.jboss.as.messaging.jms.ConnectionFactoryTypeValidator;
import org.jboss.as.messaging.jms.JndiEntriesAttribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {

    SimpleAttributeDefinition ALLOW_DIRECT_CONNECTIONS_ONLY = new SimpleAttributeDefinition("allow-direct-connections-only",
            new ModelNode().set(false), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition ADDRESS_FULL_MESSAGE_POLICY = new SimpleAttributeDefinition("address-full-policy",
            new ModelNode().set(AddressSettings.DEFAULT_ADDRESS_FULL_MESSAGE_POLICY.toString()), ModelType.STRING, true);

    SimpleAttributeDefinition ALLOW_FAILBACK = new SimpleAttributeDefinition("allow-failback",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ALLOW_AUTO_FAILBACK), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition ASYNC_CONNECTION_EXECUTION_ENABLED = new SimpleAttributeDefinition("async-connection-execution-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ASYNC_CONNECTION_EXECUTION_ENABLED), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition AUTO_GROUP = new SimpleAttributeDefinition("auto-group",
            new ModelNode().set(HornetQClient.DEFAULT_AUTO_GROUP), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition BACKUP = new SimpleAttributeDefinition("backup",
            new ModelNode().set(ConfigurationImpl.DEFAULT_BACKUP), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition BLOCK_ON_ACK = new SimpleAttributeDefinition("block-on-acknowledge",
            new ModelNode().set(HornetQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition BLOCK_ON_DURABLE_SEND = new SimpleAttributeDefinition("block-on-durable-send",
            new ModelNode().set(HornetQClient.DEFAULT_BLOCK_ON_DURABLE_SEND), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition BLOCK_ON_NON_DURABLE_SEND = new SimpleAttributeDefinition("block-on-non-durable-send",
            new ModelNode().set(HornetQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition BRIDGE_FORWARDING_ADDRESS = new SimpleAttributeDefinition("forwarding-address", ModelType.STRING, true);

    SimpleAttributeDefinition BRIDGE_RECONNECT_ATTEMPTS = new SimpleAttributeDefinition("reconnect-attempts",
            new ModelNode().set(ConfigurationImpl.DEFAULT_BRIDGE_RECONNECT_ATTEMPTS), ModelType.INT, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition BRIDGE_USE_DUPLICATE_DETECTION = new SimpleAttributeDefinition("use-duplicate-detection",
            new ModelNode().set(ConfigurationImpl.DEFAULT_BRIDGE_DUPLICATE_DETECTION), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition BROADCAST_PERIOD = new SimpleAttributeDefinition("broadcast-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_BROADCAST_PERIOD), ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition CACHE_LARGE_MESSAGE_CLIENT = new SimpleAttributeDefinition("cache-large-message-client",
            new ModelNode().set(HornetQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition CALL_TIMEOUT = new SimpleAttributeDefinition("call-timeout",
            new ModelNode().set(HornetQClient.DEFAULT_CALL_TIMEOUT), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition CLIENT_FAILURE_CHECK_PERIOD = new SimpleAttributeDefinition("client-failure-check-period",
            new ModelNode().set(HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD), ModelType.INT,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition CLIENT_ID = new SimpleAttributeDefinition("client-id",  ModelType.STRING, true);

    SimpleAttributeDefinition CLUSTERED = new SimpleAttributeDefinition("clustered",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTERED), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CLUSTER_CONNECTION_ADDRESS = new SimpleAttributeDefinition("cluster-connection-address", "address",
            null, ModelType.STRING, false, false, MeasurementUnit.NONE);

    SimpleAttributeDefinition CLUSTER_CONNECTION_RETRY_INTERVAL = new SimpleAttributeDefinition("retry-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_RETRY_INTERVAL), ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition CLUSTER_CONNECTION_USE_DUPLICATE_DETECTION = new SimpleAttributeDefinition("use-duplicate-detection",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_DUPLICATE_DETECTION), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition CLUSTER_PASSWORD = new SimpleAttributeDefinition("cluster-password",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_PASSWORD),  ModelType.STRING, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CLUSTER_USER = new SimpleAttributeDefinition("cluster-user",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_USER), ModelType.STRING, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition COMPRESS_LARGE_MESSAGES = new SimpleAttributeDefinition("compress-large-messages",
            new ModelNode().set(HornetQClient.DEFAULT_COMPRESS_LARGE_MESSAGES), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition CONFIRMATION_WINDOW_SIZE = new SimpleAttributeDefinition("confirmation-window-size",
            new ModelNode().set(HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition BRIDGE_CONFIRMATION_WINDOW_SIZE = new SimpleAttributeDefinition("confirmation-window-size",
            new ModelNode().set(FileConfiguration.DEFAULT_CONFIRMATION_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    JndiEntriesAttribute CONNECTION_ENTRIES = JndiEntriesAttribute.CONNECTION_FACTORY;

    SimpleAttributeDefinition CONNECTION_FACTORY_RECONNECT_ATTEMPTS = new SimpleAttributeDefinition("reconnect-attempts",
            new ModelNode().set(HornetQClient.DEFAULT_RECONNECT_ATTEMPTS), ModelType.INT, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinition("scheduled-thread-pool-max-size",
            new ModelNode().set(HornetQClient.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition CONNECTION_THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinition("thread-pool-max-size",
            new ModelNode().set(HornetQClient.DEFAULT_THREAD_POOL_MAX_SIZE), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition CONNECTION_TTL = new SimpleAttributeDefinition("connection-ttl",
            new ModelNode().set(HornetQClient.DEFAULT_CONNECTION_TTL), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition CONNECTION_TTL_OVERRIDE = new SimpleAttributeDefinition("connection-ttl-override",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CONNECTION_TTL_OVERRIDE), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CONNECTOR_REF = new SimpleAttributeDefinition("connector-ref", ModelType.STRING, false);

    SimpleAttributeDefinition CONSUMER_MAX_RATE = new SimpleAttributeDefinition("consumer-max-rate",
            new ModelNode().set(HornetQClient.DEFAULT_CONSUMER_MAX_RATE), ModelType.INT,  true, MeasurementUnit.PER_SECOND);

    SimpleAttributeDefinition CONSUMER_WINDOW_SIZE = new SimpleAttributeDefinition("consumer-window-size",
            new ModelNode().set(HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition CREATE_BINDINGS_DIR = new SimpleAttributeDefinition("create-bindings-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_BINDINGS_DIR), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CREATE_JOURNAL_DIR = new SimpleAttributeDefinition("create-journal-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_JOURNAL_DIR), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition DEAD_LETTER_ADDRESS = new SimpleAttributeDefinition("dead-letter-address", ModelType.STRING, true);

    SimpleAttributeDefinition DISCOVERY_GROUP_NAME = new SimpleAttributeDefinition("discovery-group-name", ModelType.STRING, true);

    SimpleAttributeDefinition DISCOVERY_INITIAL_WAIT_TIMEOUT = new SimpleAttributeDefinition("discovery-initial-wait-timeout", ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition DIVERT_ADDRESS = new SimpleAttributeDefinition("divert-address", "address", null, ModelType.STRING, false, false, MeasurementUnit.NONE);

    SimpleAttributeDefinition DIVERT_FORWARDING_ADDRESS = new SimpleAttributeDefinition("forwarding-address", ModelType.STRING, false);

    SimpleAttributeDefinition DUPS_OK_BATCH_SIZE = new SimpleAttributeDefinition("dups-ok-batch-size",
            new ModelNode().set(HornetQClient.DEFAULT_ACK_BATCH_SIZE), ModelType.INT, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition DURABLE = new SimpleAttributeDefinition("durable", new ModelNode().set(true), ModelType.BOOLEAN,  true);

    JndiEntriesAttribute ENTRIES = JndiEntriesAttribute.DESTINATION;

    SimpleAttributeDefinition EXCLUSIVE = new SimpleAttributeDefinition("exclusive",
            new ModelNode().set(ConfigurationImpl.DEFAULT_DIVERT_EXCLUSIVE), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition FACTORY_CLASS = new SimpleAttributeDefinition("factory-class", ModelType.STRING, false);

    SimpleAttributeDefinition EXPIRY_ADDRESS = new SimpleAttributeDefinition("expiry-address", ModelType.STRING, true);

    SimpleAttributeDefinition FAILBACK_DELAY = new SimpleAttributeDefinition("failback-delay",
            new ModelNode().set(ConfigurationImpl.DEFAULT_FAILBACK_DELAY), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition FAILOVER_ON_INITIAL_CONNECTION =  new SimpleAttributeDefinition("failover-on-initial-connection",
            new ModelNode().set(HornetQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = new SimpleAttributeDefinition("failover-on-server-shutdown", ModelType.BOOLEAN, true);

    SimpleAttributeDefinition FAILOVER_ON_SHUTDOWN = new SimpleAttributeDefinition("failover-on-shutdown",
            new ModelNode().set(false /*TODO should be ConfigurationImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN but field is private*/), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition FILTER = new SimpleAttributeDefinition("filter", ModelType.STRING, true);

    SimpleAttributeDefinition FORWARD_WHEN_NO_CONSUMERS = new SimpleAttributeDefinition("forward-when-no-consumers",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_FORWARD_WHEN_NO_CONSUMERS), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition GROUPING_HANDLER_ADDRESS = new SimpleAttributeDefinition("grouping-handler-address", "address",
            null, ModelType.STRING, false, false, MeasurementUnit.NONE);

    SimpleAttributeDefinition GROUP_ADDRESS = new SimpleAttributeDefinition("group-address", null, ModelType.STRING, false,
            new String[] {"socket-binding"});

    SimpleAttributeDefinition GROUP_ID = new SimpleAttributeDefinition("group-id", ModelType.STRING, true);

    SimpleAttributeDefinition GROUP_PORT = new SimpleAttributeDefinition("group-port", null, ModelType.INT, false,
            new String[] {"socket-binding"});

    SimpleAttributeDefinition HA = new SimpleAttributeDefinition("ha", new ModelNode().set(HornetQClient.DEFAULT_HA),  ModelType.BOOLEAN, true);

    SimpleAttributeDefinition ID_CACHE_SIZE = new SimpleAttributeDefinition("id-cache-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ID_CACHE_SIZE), ModelType.INT,  true,
            MeasurementUnit.NONE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition INITIAL_WAIT_TIMEOUT = new SimpleAttributeDefinition("initial-wait-timeout",
            new ModelNode().set(HornetQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition JMX_DOMAIN = new SimpleAttributeDefinition("jmx-domain",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JMX_DOMAIN), ModelType.STRING, true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JMX_MANAGEMENT_ENABLED = new SimpleAttributeDefinition("jmx-management-enabled",
            new ModelNode().set(false), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JNDI_PARAMS = new SimpleAttributeDefinition("jndi-params", ModelType.STRING, true);

    SimpleAttributeDefinition JOURNAL_BUFFER_SIZE = new SimpleAttributeDefinition("journal-buffer-size", ModelType.LONG,
            true, MeasurementUnit.BYTES, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_BUFFER_TIMEOUT = new SimpleAttributeDefinition("journal-buffer-timeout", ModelType.LONG,
            true, MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_COMPACT_MIN_FILES = new SimpleAttributeDefinition("journal-compact-min-files",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_COMPACT_MIN_FILES), ModelType.INT,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_COMPACT_PERCENTAGE = new SimpleAttributeDefinition("journal-compact-percentage",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_COMPACT_PERCENTAGE), ModelType.INT,  true,
            MeasurementUnit.PERCENTAGE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_FILE_SIZE = new SimpleAttributeDefinition("journal-file-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_FILE_SIZE), ModelType.LONG,  true,
            MeasurementUnit.BYTES, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_MAX_IO = new SimpleAttributeDefinition("journal-max-io", ModelType.INT,  true,
            MeasurementUnit.NONE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_MIN_FILES = new SimpleAttributeDefinition("journal-min-files",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_MIN_FILES), ModelType.INT,  true,
            MeasurementUnit.NONE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_SYNC_NON_TRANSACTIONAL = new SimpleAttributeDefinition("journal-sync-non-transactional",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_NON_TRANSACTIONAL), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_SYNC_TRANSACTIONAL = new SimpleAttributeDefinition("journal-sync-transactional",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_TRANSACTIONAL), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_TYPE = new SimpleAttributeDefinition("journal-type", "journal-type",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_TYPE.toString()), ModelType.STRING,  true, false,
            MeasurementUnit.NONE, JournalTypeValidator.INSTANCE, null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    LiveConnectorRefAttribute LIVE_CONNECTOR_REF = LiveConnectorRefAttribute.INSTANCE;

    SimpleAttributeDefinition LOAD_BALANCING_CLASS_NAME = new SimpleAttributeDefinition("connection-load-balancing-policy-class-name",
            new ModelNode().set(HornetQClient.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME), ModelType.STRING, true);

    SimpleAttributeDefinition LOCAL_BIND_ADDRESS = new SimpleAttributeDefinition("local-bind-address", null, ModelType.STRING, true,
            new String[] {"socket-binding"});

    SimpleAttributeDefinition LOCAL_BIND_PORT = new SimpleAttributeDefinition("local-bind-port", new ModelNode().set(-1), ModelType.INT, true,
            new String[] {"socket-binding"});

    SimpleAttributeDefinition LOG_JOURNAL_WRITE_RATE = new SimpleAttributeDefinition("log-journal-write-rate",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_LOG_WRITE_RATE), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition LVQ = new SimpleAttributeDefinition("last-value-queue",
            new ModelNode().set(AddressSettings.DEFAULT_LAST_VALUE_QUEUE), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition MANAGEMENT_ADDRESS = new SimpleAttributeDefinition("management-address",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_ADDRESS.toString()), ModelType.STRING, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MANAGEMENT_NOTIFICATION_ADDRESS = new SimpleAttributeDefinition("management-notification-address",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_NOTIFICATION_ADDRESS.toString()), ModelType.STRING, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MAX_HOPS = new SimpleAttributeDefinition("max-hops",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_MAX_HOPS), ModelType.INT,  true);

    SimpleAttributeDefinition MAX_DELIVERY_ATTEMPTS = new SimpleAttributeDefinition("max-delivery-attempts",
            new ModelNode().set(AddressSettings.DEFAULT_MAX_DELIVERY_ATTEMPTS), ModelType.INT, true);

    SimpleAttributeDefinition MAX_RETRY_INTERVAL = new SimpleAttributeDefinition("max-retry-interval",
            new ModelNode().set(HornetQClient.DEFAULT_MAX_RETRY_INTERVAL), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition MAX_SIZE_BYTES_NODE_NAME = new SimpleAttributeDefinition("max-size-bytes",
            new ModelNode().set(AddressSettings.DEFAULT_MAX_SIZE_BYTES), ModelType.INT, true);

    SimpleAttributeDefinition MEMORY_MEASURE_INTERVAL = new SimpleAttributeDefinition("memory-measure-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MEMORY_MEASURE_INTERVAL), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MEMORY_WARNING_THRESHOLD = new SimpleAttributeDefinition("memory-warning-threshold",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MEMORY_WARNING_THRESHOLD), ModelType.INT,  true,
            MeasurementUnit.PERCENTAGE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MESSAGE_COUNTER_ENABLED = new SimpleAttributeDefinition("message-counter-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_ENABLED), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition MESSAGE_COUNTER_HISTORY_DAY_LIMIT = new SimpleAttributeDefinition("message-counter-history-day-limit",
            new ModelNode().set(AddressSettings.DEFAULT_MESSAGE_COUNTER_HISTORY_DAY_LIMIT), ModelType.INT, true);

    SimpleAttributeDefinition MESSAGE_COUNTER_MAX_DAY_HISTORY = new SimpleAttributeDefinition("message-counter-max-day-history",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_MAX_DAY_HISTORY), ModelType.INT,  true, MeasurementUnit.DAYS);

    SimpleAttributeDefinition MESSAGE_COUNTER_SAMPLE_PERIOD = new SimpleAttributeDefinition("message-counter-sample-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_SAMPLE_PERIOD), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition MESSAGE_EXPIRY_SCAN_PERIOD = new SimpleAttributeDefinition("message-expiry-scan-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_EXPIRY_SCAN_PERIOD), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MESSAGE_EXPIRY_THREAD_PRIORITY = new SimpleAttributeDefinition("message-expiry-thread-priority",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_EXPIRY_THREAD_PRIORITY), ModelType.INT,  true,
            MeasurementUnit.NONE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MIN_LARGE_MESSAGE_SIZE = new SimpleAttributeDefinition("min-large-message-size",
            new ModelNode().set(HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinition("password",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_PASSWORD),  ModelType.STRING, true);

    SimpleAttributeDefinition PAGE_MAX_CACHE_SIZE = new SimpleAttributeDefinition("page-max-cache-size",
            new ModelNode(AddressSettings.DEFAULT_PAGE_MAX_CACHE), ModelType.INT, true);

    SimpleAttributeDefinition PAGE_SIZE_BYTES_NODE_NAME = new SimpleAttributeDefinition("page-size-bytes",
            new ModelNode(AddressSettings.DEFAULT_PAGE_SIZE), ModelType.LONG, true);

    SimpleAttributeDefinition PATH = new SimpleAttributeDefinition("path", ModelType.STRING, false);

    SimpleAttributeDefinition PERF_BLAST_PAGES = new SimpleAttributeDefinition("perf-blast-pages",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_PERF_BLAST_PAGES), ModelType.INT,  true,
            MeasurementUnit.NONE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY = new SimpleAttributeDefinition("persist-delivery-count-before-delivery",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition PERSISTENCE_ENABLED = new SimpleAttributeDefinition("persistence-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSISTENCE_ENABLED), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition PERSIST_ID_CACHE = new SimpleAttributeDefinition("persist-id-cache",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSIST_ID_CACHE), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition PCF_USER = new SimpleAttributeDefinition("user", "user", null, ModelType.STRING, true, true, null);

    SimpleAttributeDefinition PCF_PASSWORD = new SimpleAttributeDefinition("password", "password", null, ModelType.STRING, true, true, null);

    SimpleAttributeDefinition PRE_ACK = new SimpleAttributeDefinition("pre-acknowledge",
            new ModelNode().set(HornetQClient.DEFAULT_PRE_ACKNOWLEDGE), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition PRODUCER_MAX_RATE = new SimpleAttributeDefinition("producer-max-rate",
            new ModelNode().set(HornetQClient.DEFAULT_PRODUCER_MAX_RATE), ModelType.INT,  true, MeasurementUnit.PER_SECOND);

    SimpleAttributeDefinition PRODUCER_WINDOW_SIZE = new SimpleAttributeDefinition("producer-window-size",
            new ModelNode().set(HornetQClient.DEFAULT_PRODUCER_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition QUEUE_ADDRESS = new SimpleAttributeDefinition("queue-address", "address", null, ModelType.STRING, false, false, MeasurementUnit.NONE);

    SimpleAttributeDefinition QUEUE_NAME = new SimpleAttributeDefinition("queue-name", ModelType.STRING, false);

    SimpleAttributeDefinition REFRESH_TIMEOUT = new SimpleAttributeDefinition("refresh-timeout",
            new ModelNode().set(HornetQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition REDELIVERY_DELAY = new SimpleAttributeDefinition("redelivery-delay",
            new ModelNode().set(AddressSettings.DEFAULT_REDELIVER_DELAY), ModelType.INT, true);

    SimpleAttributeDefinition REDISTRIBUTION_DELAY = new SimpleAttributeDefinition("redistribution-delay",
            new ModelNode().set(AddressSettings.DEFAULT_REDISTRIBUTION_DELAY), ModelType.LONG, true);

    SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinition("relative-to", ModelType.STRING, true);

    RemotingInterceptorsAttribute REMOTING_INTERCEPTORS = RemotingInterceptorsAttribute.INSTANCE;

    SimpleAttributeDefinition RETRY_INTERVAL = new SimpleAttributeDefinition("retry-interval",
            new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL), ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition RETRY_INTERVAL_MULTIPLIER = new SimpleAttributeDefinition("retry-interval-multiplier",
            new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER), ModelType.BIG_DECIMAL, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition ROUTING_NAME = new SimpleAttributeDefinition("routing-name", ModelType.STRING, true);

    SimpleAttributeDefinition RUN_SYNC_SPEED_TEST = new SimpleAttributeDefinition("run-sync-speed-test",
            new ModelNode().set(ConfigurationImpl.DEFAULT_RUN_SYNC_SPEED_TEST), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinition("scheduled-thread-pool-max-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE), ModelType.INT,  true, MeasurementUnit.NONE,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinition("security-domain",
            new ModelNode().set("other"), ModelType.STRING,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SECURITY_ENABLED = new SimpleAttributeDefinition("security-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_ENABLED), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SECURITY_INVALIDATION_INTERVAL = new SimpleAttributeDefinition("security-invalidation-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_INVALIDATION_INTERVAL), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SELECTOR = new SimpleAttributeDefinition("selector", ModelType.STRING, true);

    SimpleAttributeDefinition SEND_TO_DLA_ON_NO_ROUTE = new SimpleAttributeDefinition("send-to-dla-on-no-route",
            new ModelNode().set(AddressSettings.DEFAULT_SEND_TO_DLA_ON_NO_ROUTE), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition SERVER_ID = new SimpleAttributeDefinition("server-id", ModelType.INT, false);

    SimpleAttributeDefinition SERVER_DUMP_INTERVAL = new SimpleAttributeDefinition("server-dump-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SERVER_DUMP_INTERVAL), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SETUP_ATTEMPTS = new SimpleAttributeDefinition("setup-attempts", ModelType.INT, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition SETUP_INTERVAL = new SimpleAttributeDefinition("setup-interval", ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition SHARED_STORE = new SimpleAttributeDefinition("shared-store",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SHARED_STORE), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinition("socket-binding", ModelType.STRING, false);

    SimpleAttributeDefinition SOCKET_BINDING_OPTIONAL = new SimpleAttributeDefinition("socket-binding", ModelType.STRING, true);

    SimpleAttributeDefinition SOCKET_BINDING_ALTERNATIVE = new SimpleAttributeDefinition("socket-binding", null, ModelType.STRING, false,
            new String[] {"group-address", "group-port"});

    SimpleAttributeDefinition THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinition("thread-pool-max-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_THREAD_POOL_MAX_SIZE), ModelType.INT,  true, MeasurementUnit.NONE,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition TIMEOUT =  new SimpleAttributeDefinition("timeout",
            new ModelNode().set(GroupingHandlerConfiguration.DEFAULT_TIMEOUT), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition TRANSACTION_ATTRIBUTE = new SimpleAttributeDefinition("transaction",
            new ModelNode().set("transaction"), ModelType.STRING,  true);

    SimpleAttributeDefinition TRANSACTION_BATCH_SIZE = new SimpleAttributeDefinition("transaction-batch-size",
            new ModelNode().set(HornetQClient.DEFAULT_ACK_BATCH_SIZE), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition TRANSACTION_TIMEOUT = new SimpleAttributeDefinition("transaction-timeout",
            new ModelNode().set(ConfigurationImpl.DEFAULT_TRANSACTION_TIMEOUT), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition TRANSACTION_TIMEOUT_SCAN_PERIOD = new SimpleAttributeDefinition("transaction-timeout-scan-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_TRANSACTION_TIMEOUT_SCAN_PERIOD), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition TRANSFORMER_CLASS_NAME = new SimpleAttributeDefinition("transformer-class-name", ModelType.STRING, true);

    SimpleAttributeDefinition TYPE = new SimpleAttributeDefinition("type", "type",
            null, ModelType.STRING,  true, false, MeasurementUnit.NONE, GroupingHandlerTypeValidator.INSTANCE);

    SimpleAttributeDefinition CONNECTION_FACTORY_TYPE = new SimpleAttributeDefinition("factory-type", "factory-type",
            null, ModelType.STRING,  true, false, MeasurementUnit.NONE, ConnectionFactoryTypeValidator.INSTANCE);

    SimpleAttributeDefinition USER = new SimpleAttributeDefinition("user",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_USER), ModelType.STRING, true);

    SimpleAttributeDefinition USE_GLOBAL_POOLS = new SimpleAttributeDefinition("use-global-pools",
            new ModelNode().set(HornetQClient.DEFAULT_USE_GLOBAL_POOLS), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition USE_JNDI = new SimpleAttributeDefinition("use-jndi", ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition USE_LOCAL_TX = new SimpleAttributeDefinition("use-local-tx", ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition VALUE = new SimpleAttributeDefinition("value", ModelType.STRING, false);

    SimpleAttributeDefinition WILD_CARD_ROUTING_ENABLED = new SimpleAttributeDefinition("wild-card-routing-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_WILDCARD_ROUTING_ENABLED), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    String ACCEPTOR ="acceptor";
    String ACCEPTORS ="acceptors";
    String ADDRESS_SETTING ="address-setting";
    String ADDRESS_SETTINGS ="address-settings";
    String BACKUP_CONNECTOR_NAME ="backup-connector-name";
    String BINDING_NAMES ="binding-names";
    String BINDINGS_DIRECTORY ="bindings-directory";
    String BRIDGE = "bridge";
    String BRIDGES = "bridges";
    String BROADCAST_GROUP = "broadcast-group";
    String BROADCAST_GROUPS = "broadcast-groups";
    String CLASS_NAME = "class-name";
    String CLUSTER_CONNECTION = "cluster-connection";
    String CLUSTER_CONNECTIONS = "cluster-connections";
    String CONNECTION_FACTORY ="connection-factory";
    String CONNECTOR ="connector";
    String CONNECTORS ="connectors";
    String CONNECTOR_NAME ="connector-name";
    String CONNECTOR_REF_STRING ="connector-ref";
    String CONNECTOR_SERVICE = "connector-service";
    String CONNECTOR_SERVICES = "connector-services";
    String CONSUMER_COUNT ="consumer-count";
    String CORE_ADDRESS ="core-address";
    String CORE_QUEUE ="core-queue";
    String CORE_QUEUES ="core-queues";
    String DELIVERING_COUNT ="delivering-count";
    String DISCOVERY_GROUP = "discovery-group";
    String DISCOVERY_GROUPS = "discovery-groups";
    String DISCOVERY_GROUP_REF ="discovery-group-ref";
    String DIVERT = "divert";
    String DIVERTS = "diverts";
    String DURABLE_MESSAGE_COUNT = "durable-message-count";
    String DURABLE_SUBSCRIPTION_COUNT = "durable-subscription-count";
    String ENTRIES_STRING = "entries";
    String ENTRY ="entry";
    String FACTORY_TYPE = "factory-type";
    String FILE_DEPLOYMENT_ENABLED ="file-deployment-enabled";
    String GENERIC_FACTORY = "GENERIC";
    String GROUPING_HANDLER ="grouping-handler";
    String ID ="id";
    String INITIAL_MESSAGE_PACKET_SIZE = "initial-message-packet-size";
    String IN_VM_ACCEPTOR ="in-vm-acceptor";
    String IN_VM_CONNECTOR ="in-vm-connector";
    String JMS_CONNECTION_FACTORIES ="jms-connection-factories";
    String JMS_DESTINATIONS = "jms-destinations";
    String JMS_QUEUE ="jms-queue";
    String JMS_TOPIC ="jms-topic";
    String JNDI_BINDING = "jndi-binding";
    String JOURNAL_DIRECTORY ="journal-directory";
    String KEY ="key";
    String INBOUND_CONFIG = "inbound-config";
    String LARGE_MESSAGES_DIRECTORY ="large-messages-directory";
    String LAST_VALUE_QUEUE = "last-value=queue";
    String LIVE_CONNECTOR_REF_STRING ="live-connector-ref";
    String LOCAL = "local";
    String LOCAL_TX = "LocalTransaction";
    String MANAGE_XML_NAME ="manage";
    String MATCH ="match";
    String MESSAGES_ADDED = "messages-added";
    String MESSAGE_COUNT = "message-count";
    String MODE = "mode";
    String NAME ="name";
    String NODE_ID = "node-id";
    String NETTY_ACCEPTOR ="netty-acceptor";
    String NETTY_CONNECTOR ="netty-connector";
    String NONE = "none";
    String NON_DURABLE_MESSAGE_COUNT = "non-durable-message-count";
    String NON_DURABLE_SUBSCRIPTION_COUNT = "non-durable-subscription-count";
    String NO_TX = "NoTransaction";
    String NUMBER_OF_BYTES_PER_PAGE = "number-of-bytes-per-page";
    String NUMBER_OF_PAGES = "number-of-pages";

    String PAGING_DIRECTORY ="paging-directory";
    String PARAM ="param";
    String PARAMS ="param";
    String PAUSED ="paused";
    String PERMISSION_ELEMENT_NAME ="permission";
    String POOLED_CONNECTION_FACTORY = "pooled-connection-factory";
    String QUEUE ="queue";
    String QUEUE_NAMES ="queue-names";
    String  REMOTING_INTERCEPTORS_STRING ="remoting-interceptors";
    String REMOTE_ACCEPTOR = "remote-acceptor";
    String REMOTE_CONNECTOR = "remote-connector";
    String REMOTING_INTERCEPTOR ="remoting-interceptor";
    String RESOURCE_ADAPTER = "resource-adapter";
    String ROLE = "role";
    String ROLES_ATTR_NAME ="roles";
    String SCHEDULED_COUNT = "scheduled-count";
    String SECURITY_SETTING ="security-setting";
    String SECURITY_SETTINGS ="security-settings";
    String TOPIC_FACTORY = "TOPIC";
    String HORNETQ_SERVER = "hornetq-server";
    String QUEUE_FACTORY = "QUEUE";
    String STARTED = "started";
    String STATIC_CONNECTORS = "static-connectors";
    String STRING ="string";
    String SUBSCRIPTION_COUNT = "subscription-count";
    String SUBSYSTEM ="subsystem";
    String TEMPORARY ="temporary";
    String TOPIC_ADDRESS ="topic-address";
    String TRANSACTION = "transaction";
    String TYPE_ATTR_NAME ="type";
    String VERSION = "version";
    String XA = "xa";
    String XA_TX = "XATransaction";
    String XA_GENERIC_FACTORY = "XA_GENERIC";
    String XA_QUEUE_FACTORY = "XA_QUEUE";
    String XA_TOPIC_FACTORY = "XA_TOPIC";

    AttributeDefinition[] SIMPLE_ROOT_RESOURCE_ATTRIBUTES = {
        CLUSTERED, PERSISTENCE_ENABLED, SCHEDULED_THREAD_POOL_MAX_SIZE,
        THREAD_POOL_MAX_SIZE, SECURITY_DOMAIN, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL, WILD_CARD_ROUTING_ENABLED, MANAGEMENT_ADDRESS,
        MANAGEMENT_NOTIFICATION_ADDRESS, CLUSTER_USER, CLUSTER_PASSWORD, JMX_MANAGEMENT_ENABLED, JMX_DOMAIN, MESSAGE_COUNTER_ENABLED,
        MESSAGE_COUNTER_SAMPLE_PERIOD, MESSAGE_COUNTER_MAX_DAY_HISTORY, CONNECTION_TTL_OVERRIDE, ASYNC_CONNECTION_EXECUTION_ENABLED,
        TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_SCAN_PERIOD, MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY,
        ID_CACHE_SIZE, PERSIST_ID_CACHE, REMOTING_INTERCEPTORS, BACKUP, ALLOW_FAILBACK, FAILBACK_DELAY, FAILOVER_ON_SHUTDOWN,
        SHARED_STORE, PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY, LIVE_CONNECTOR_REF, CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE,
        JOURNAL_BUFFER_TIMEOUT, JOURNAL_BUFFER_SIZE, JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE,
        JOURNAL_FILE_SIZE, JOURNAL_MIN_FILES, JOURNAL_COMPACT_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_MAX_IO, PERF_BLAST_PAGES,
        RUN_SYNC_SPEED_TEST, SERVER_DUMP_INTERVAL, MEMORY_WARNING_THRESHOLD, MEMORY_MEASURE_INTERVAL
    };

    AttributeDefinition[]  SIMPLE_ROOT_RESOURCE_WRITE_ATTRIBUTES = {
        FAILOVER_ON_SHUTDOWN, MESSAGE_COUNTER_ENABLED, MESSAGE_COUNTER_MAX_DAY_HISTORY, MESSAGE_COUNTER_SAMPLE_PERIOD
    };

    AttributeDefinition[] DIVERT_ATTRIBUTES = {
        ROUTING_NAME, DIVERT_ADDRESS, DIVERT_FORWARDING_ADDRESS, FILTER, TRANSFORMER_CLASS_NAME, EXCLUSIVE
    };

    AttributeDefinition[] BROADCAST_GROUP_ATTRIBUTES = { SOCKET_BINDING_ALTERNATIVE, LOCAL_BIND_ADDRESS, LOCAL_BIND_PORT, GROUP_ADDRESS, GROUP_PORT,
            BROADCAST_PERIOD, ConnectorRefsAttribute.BROADCAST_GROUP
    };

    AttributeDefinition[] DISCOVERY_GROUP_ATTRIBUTES = { SOCKET_BINDING_ALTERNATIVE, LOCAL_BIND_ADDRESS, GROUP_ADDRESS, GROUP_PORT,
            REFRESH_TIMEOUT, INITIAL_WAIT_TIMEOUT,
    };

    AttributeDefinition[] GROUPING_HANDLER_ATTRIBUTES = { TYPE, GROUPING_HANDLER_ADDRESS, TIMEOUT};

    AttributeDefinition[] CORE_QUEUE_ATTRIBUTES = { QUEUE_ADDRESS, FILTER, DURABLE };

    AttributeDefinition[] BRIDGE_ATTRIBUTES = {
            QUEUE_NAME, BRIDGE_FORWARDING_ADDRESS, HA, FILTER, TRANSFORMER_CLASS_NAME,
            RETRY_INTERVAL, RETRY_INTERVAL_MULTIPLIER, BRIDGE_RECONNECT_ATTEMPTS, FAILOVER_ON_SERVER_SHUTDOWN,
            BRIDGE_USE_DUPLICATE_DETECTION, BRIDGE_CONFIRMATION_WINDOW_SIZE, USER, PASSWORD, ConnectorRefsAttribute.BRIDGE_CONNECTORS,
            DISCOVERY_GROUP_NAME
    };

    AttributeDefinition[] CLUSTER_CONNECTION_ATTRIBUTES = {
        CLUSTER_CONNECTION_ADDRESS,  CONNECTOR_REF, RETRY_INTERVAL, CLUSTER_CONNECTION_USE_DUPLICATE_DETECTION,
        FORWARD_WHEN_NO_CONSUMERS,  MAX_HOPS, BRIDGE_CONFIRMATION_WINDOW_SIZE, ConnectorRefsAttribute.CLUSTER_CONNECTION_CONNECTORS,
        DISCOVERY_GROUP_NAME, ALLOW_DIRECT_CONNECTIONS_ONLY
    };

    AttributeDefinition[] JMS_QUEUE_ATTRIBUTES = { ENTRIES, SELECTOR, DURABLE };

    AttributeDefinition[] CONNECTOR_SERVICE_ATTRIBUTES = { FACTORY_CLASS };

    String[] PATHS = new String[] {CommonAttributes.BINDINGS_DIRECTORY, CommonAttributes.JOURNAL_DIRECTORY, CommonAttributes.LARGE_MESSAGES_DIRECTORY, CommonAttributes.PAGING_DIRECTORY};


}
