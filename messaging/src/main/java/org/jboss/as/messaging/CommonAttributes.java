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

import static org.hornetq.api.core.client.HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD;
import static org.hornetq.api.core.client.HornetQClient.DEFAULT_CONNECTION_TTL;
import static org.hornetq.api.core.client.HornetQClient.DEFAULT_MAX_RETRY_INTERVAL;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_MEMORY_MEASURE_INTERVAL;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_MEMORY_WARNING_THRESHOLD;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.PERCENTAGE;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.jboss.dmr.ModelType.BIG_DECIMAL;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.OBJECT;

import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.config.impl.FileConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.messaging.jms.ConnectionFactoryTypeValidator;
import org.jboss.as.messaging.jms.JndiEntriesAttribute;
import org.jboss.as.messaging.jms.SelectorAttribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.metadata.ejb.jboss.LocalBindingMetaData;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {

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

    SimpleAttributeDefinition CACHE_LARGE_MESSAGE_CLIENT = new SimpleAttributeDefinition("cache-large-message-client",
            new ModelNode().set(HornetQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition CALL_TIMEOUT = new SimpleAttributeDefinition("call-timeout",
            new ModelNode().set(HornetQClient.DEFAULT_CALL_TIMEOUT), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    // a <connector> used to defined (pooled/non-pooled) JMS CF.
    // It is a map defined by connector name and undefined value.
    // (legacy code, previously the value could be a backup-connector name but HornetQ no longer supports it)
    SimpleAttributeDefinition CF_CONNECTOR = new SimpleAttributeDefinitionBuilder("connector", OBJECT)
            .setAllowNull(true)
            .build();

    SimpleAttributeDefinition CHECK_PERIOD = create("check-period", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_CLIENT_FAILURE_CHECK_PERIOD))
            .setAllowNull(true)
            .setMeasurementUnit(MILLISECONDS)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition CLIENT_FAILURE_CHECK_PERIOD = new SimpleAttributeDefinition("client-failure-check-period",
            new ModelNode().set(HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition CLIENT_ID = create("client-id", ModelType.STRING)
            .setAllowNull(true)
            .build();

    SimpleAttributeDefinition CLUSTERED = new SimpleAttributeDefinition("clustered",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTERED), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CLUSTER_PASSWORD = new SimpleAttributeDefinition("cluster-password", "cluster-password",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_PASSWORD), ModelType.STRING, true, true, null,
            RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CLUSTER_USER = new SimpleAttributeDefinition("cluster-user", "cluster-user",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_USER), ModelType.STRING, true, true, null,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition COMPRESS_LARGE_MESSAGES = new SimpleAttributeDefinition("compress-large-messages",
            new ModelNode().set(HornetQClient.DEFAULT_COMPRESS_LARGE_MESSAGES), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition CONFIRMATION_WINDOW_SIZE = new SimpleAttributeDefinition("confirmation-window-size",
            new ModelNode().set(HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition BRIDGE_CONFIRMATION_WINDOW_SIZE = create("confirmation-window-size", INT)
            .setDefaultValue(new ModelNode().set(FileConfiguration.DEFAULT_CONFIRMATION_WINDOW_SIZE))
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    JndiEntriesAttribute CONNECTION_ENTRIES = JndiEntriesAttribute.CONNECTION_FACTORY;

    SimpleAttributeDefinition CONNECTION_FACTORY_RECONNECT_ATTEMPTS = new SimpleAttributeDefinition("reconnect-attempts",
            new ModelNode().set(HornetQClient.DEFAULT_RECONNECT_ATTEMPTS), ModelType.INT, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinitionBuilder("scheduled-thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE))
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition CONNECTION_THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinitionBuilder("thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(DEFAULT_THREAD_POOL_MAX_SIZE))
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition CONNECTION_TTL = new SimpleAttributeDefinitionBuilder("connection-ttl", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_CONNECTION_TTL))
            .setAllowNull(true)
            .setMeasurementUnit(MILLISECONDS)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition CONNECTION_TTL_OVERRIDE = new SimpleAttributeDefinition("connection-ttl-override",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CONNECTION_TTL_OVERRIDE), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CONSUMER_MAX_RATE = new SimpleAttributeDefinition("consumer-max-rate",
            new ModelNode().set(HornetQClient.DEFAULT_CONSUMER_MAX_RATE), ModelType.INT,  true, MeasurementUnit.PER_SECOND);

    SimpleAttributeDefinition CONSUMER_WINDOW_SIZE = new SimpleAttributeDefinition("consumer-window-size",
            new ModelNode().set(HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition CREATE_BINDINGS_DIR = new SimpleAttributeDefinition("create-bindings-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_BINDINGS_DIR), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CREATE_JOURNAL_DIR = new SimpleAttributeDefinition("create-journal-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_JOURNAL_DIR), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition DEAD_LETTER_ADDRESS = new SimpleAttributeDefinition("dead-letter-address", ModelType.STRING, true);

    SimpleAttributeDefinition DISCOVERY_GROUP_NAME = create("discovery-group-name", ModelType.STRING)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition DISCOVERY_INITIAL_WAIT_TIMEOUT = new SimpleAttributeDefinition("discovery-initial-wait-timeout", ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition DUPS_OK_BATCH_SIZE = new SimpleAttributeDefinition("dups-ok-batch-size",
            new ModelNode().set(HornetQClient.DEFAULT_ACK_BATCH_SIZE), ModelType.INT, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition DURABLE = new SimpleAttributeDefinition("durable", new ModelNode().set(true), ModelType.BOOLEAN,  true);

    JndiEntriesAttribute ENTRIES = JndiEntriesAttribute.DESTINATION;

    SimpleAttributeDefinition FACTORY_CLASS = create("factory-class", ModelType.STRING)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition EXPIRY_ADDRESS = new SimpleAttributeDefinition("expiry-address", ModelType.STRING, true);

    SimpleAttributeDefinition FAILBACK_DELAY = new SimpleAttributeDefinition("failback-delay",
            new ModelNode().set(ConfigurationImpl.DEFAULT_FAILBACK_DELAY), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition FAILOVER_ON_INITIAL_CONNECTION =  new SimpleAttributeDefinition("failover-on-initial-connection",
            new ModelNode().set(HornetQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = create("failover-on-server-shutdown", ModelType.BOOLEAN)
            .setAllowNull(true)
            .build();

    SimpleAttributeDefinition FAILOVER_ON_SHUTDOWN = new SimpleAttributeDefinition("failover-on-shutdown",
            new ModelNode().set(false /*TODO should be ConfigurationImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN but field is private*/), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition FILTER = create("filter", ModelType.STRING)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition GROUP_ADDRESS = create("group-address", ModelType.STRING)
            .setDefaultValue(null)
            .setAllowNull(false)
            .setAlternatives("socket-binding")
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition GROUP_ID = new SimpleAttributeDefinition("group-id", ModelType.STRING, true);

    SimpleAttributeDefinition GROUP_PORT = create("group-port", INT)
            .setDefaultValue(null)
            .setAllowNull(false)
            .setAlternatives("socket-binding")
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition HA = create("ha", BOOLEAN)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_HA))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition ID_CACHE_SIZE = new SimpleAttributeDefinition("id-cache-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ID_CACHE_SIZE), ModelType.INT,  true,
            MeasurementUnit.NONE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

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

    SimpleAttributeDefinition LOCAL_BIND_ADDRESS = create("local-bind-address", ModelType.STRING)
            .setDefaultValue(null)
            .setAllowNull(true)
            .setAlternatives("socket-binding")
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition LOCAL_BIND_PORT = create("local-bind-port", INT)
            .setDefaultValue(new ModelNode().set(-1))
            .setAllowNull(true)
            .setAlternatives("socket-binding")
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition LOG_JOURNAL_WRITE_RATE = new SimpleAttributeDefinition("log-journal-write-rate",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_LOG_WRITE_RATE), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MANAGEMENT_ADDRESS = new SimpleAttributeDefinition("management-address",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_ADDRESS.toString()), ModelType.STRING, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MANAGEMENT_NOTIFICATION_ADDRESS = new SimpleAttributeDefinition("management-notification-address",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_NOTIFICATION_ADDRESS.toString()), ModelType.STRING, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MAX_POOL_SIZE = new SimpleAttributeDefinitionBuilder("max-pool-size", INT)
            .setDefaultValue(new ModelNode().set(-1))
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition MAX_RETRY_INTERVAL = create("max-retry-interval", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_MAX_RETRY_INTERVAL))
            .setAllowNull(true)
            .setMeasurementUnit(MILLISECONDS)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition MEMORY_MEASURE_INTERVAL = new SimpleAttributeDefinitionBuilder("memory-measure-interval", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_MEMORY_MEASURE_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition MEMORY_WARNING_THRESHOLD = new SimpleAttributeDefinitionBuilder("memory-warning-threshold", INT)
            .setDefaultValue(new ModelNode().set(DEFAULT_MEMORY_WARNING_THRESHOLD))
            .setMeasurementUnit(PERCENTAGE)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition MESSAGE_COUNTER_ENABLED = new SimpleAttributeDefinition("message-counter-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_ENABLED), ModelType.BOOLEAN,  true);

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

    SimpleAttributeDefinition MIN_LARGE_MESSAGE_SIZE = create("min-large-message-size", INT)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE))
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition MIN_POOL_SIZE = new SimpleAttributeDefinitionBuilder("min-pool-size", INT)
            .setDefaultValue(new ModelNode().set(-1))
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition PAGE_MAX_CONCURRENT_IO = new SimpleAttributeDefinitionBuilder("page-max-concurrent-io", ModelType.INT)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_MAX_CONCURRENT_PAGE_IO))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition PATH = create("path", ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();

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

    SimpleAttributeDefinition PCF_USER = new SimpleAttributeDefinitionBuilder("user", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition PCF_PASSWORD = new SimpleAttributeDefinitionBuilder("password", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition PRE_ACK = new SimpleAttributeDefinition("pre-acknowledge",
            new ModelNode().set(HornetQClient.DEFAULT_PRE_ACKNOWLEDGE), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition PRODUCER_MAX_RATE = new SimpleAttributeDefinition("producer-max-rate",
            new ModelNode().set(HornetQClient.DEFAULT_PRODUCER_MAX_RATE), ModelType.INT,  true, MeasurementUnit.PER_SECOND);

    SimpleAttributeDefinition PRODUCER_WINDOW_SIZE = new SimpleAttributeDefinition("producer-window-size",
            new ModelNode().set(HornetQClient.DEFAULT_PRODUCER_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition QUEUE_ADDRESS = new SimpleAttributeDefinition("queue-address", "address", null, ModelType.STRING, false, false, MeasurementUnit.NONE);

    SimpleAttributeDefinition RECONNECT_ATTEMPTS = new SimpleAttributeDefinitionBuilder("reconnect-attempts", INT)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_RECONNECT_ATTEMPTS))
            .setAllowNull(true)
            .build();

    SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinition("relative-to", ModelType.STRING, true);

    RemotingInterceptorsAttribute REMOTING_INTERCEPTORS = RemotingInterceptorsAttribute.INSTANCE;

    SimpleAttributeDefinition RETRY_INTERVAL = create("retry-interval", LONG)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", BIG_DECIMAL)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition RUN_SYNC_SPEED_TEST = new SimpleAttributeDefinition("run-sync-speed-test",
            new ModelNode().set(ConfigurationImpl.DEFAULT_RUN_SYNC_SPEED_TEST), ModelType.BOOLEAN,  true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinitionBuilder("scheduled-thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinition("security-domain",
            new ModelNode().set("other"), ModelType.STRING,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SECURITY_ENABLED = new SimpleAttributeDefinition("security-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_ENABLED), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SECURITY_INVALIDATION_INTERVAL = new SimpleAttributeDefinition("security-invalidation-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_INVALIDATION_INTERVAL), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SelectorAttribute SELECTOR = SelectorAttribute.SELECTOR;

    SimpleAttributeDefinition SERVER_DUMP_INTERVAL = new SimpleAttributeDefinition("server-dump-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SERVER_DUMP_INTERVAL), ModelType.LONG,  true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SETUP_ATTEMPTS = new SimpleAttributeDefinition("setup-attempts", ModelType.INT, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition SETUP_INTERVAL = new SimpleAttributeDefinition("setup-interval", ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition SHARED_STORE = new SimpleAttributeDefinition("shared-store",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SHARED_STORE), ModelType.BOOLEAN,  true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SOCKET_BINDING_ALTERNATIVE = create("socket-binding", ModelType.STRING)
            .setDefaultValue(null)
            .setAllowNull(false)
            .setAlternatives(GROUP_ADDRESS.getName(),
                             GROUP_PORT.getName(),
                             LOCAL_BIND_ADDRESS.getName(),
                             LOCAL_BIND_PORT.getName())
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinitionBuilder("thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(DEFAULT_THREAD_POOL_MAX_SIZE))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

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

    SimpleAttributeDefinition TRANSFORMER_CLASS_NAME = create("transformer-class-name", ModelType.STRING)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition CONNECTION_FACTORY_TYPE = new SimpleAttributeDefinition("factory-type", "factory-type",
            null, ModelType.STRING,  true, false, MeasurementUnit.NONE, ConnectionFactoryTypeValidator.INSTANCE);

    SimpleAttributeDefinition USER = new SimpleAttributeDefinition("user", "user",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_USER), ModelType.STRING, true, true, null);

    SimpleAttributeDefinition USE_AUTO_RECOVERY = create("use-auto-recovery", ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(true)) // HornetQResourceAdapter.useAutoRecovery = true but is not exposed publicly
            .build();

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
    String ADDRESS ="address";
    String ADDRESS_SETTING ="address-setting";
    String ADDRESS_SETTINGS ="address-settings";
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
    String DEFAULT ="default";
    String DELIVERING_COUNT ="delivering-count";
    String DESTINATION ="destination";
    String DISCOVERY_GROUP = "discovery-group";
    String DISCOVERY_GROUPS = "discovery-groups";
    String DISCOVERY_GROUP_REF ="discovery-group-ref";
    String DIVERT = "divert";
    String DIVERTS = "diverts";
    String DURABLE_MESSAGE_COUNT = "durable-message-count";
    String DURABLE_SUBSCRIPTION_COUNT = "durable-subscription-count";
    String ENTRIES_STRING = "entries";
    String ENTRY ="entry";
    String FILE_DEPLOYMENT_ENABLED ="file-deployment-enabled";
    String GENERIC_FACTORY = "GENERIC";
    String GROUPING_HANDLER ="grouping-handler";
    String ID ="id";
    String INITIAL_MESSAGE_PACKET_SIZE = "initial-message-packet-size";
    String IN_VM_ACCEPTOR ="in-vm-acceptor";
    String IN_VM_CONNECTOR ="in-vm-connector";
    String JMS_BRIDGE ="jms-bridge";
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
    String QUEUE_NAME ="queue-name";
    String QUEUE_NAMES ="queue-names";
    String  REMOTING_INTERCEPTORS_STRING ="remoting-interceptors";
    String REMOTE_ACCEPTOR = "remote-acceptor";
    String REMOTE_CONNECTOR = "remote-connector";
    String REMOTING_INTERCEPTOR ="remoting-interceptor";
    String RESOURCE_ADAPTER = "resource-adapter";
    String ROLE = "role";
    String ROLES_ATTR_NAME ="roles";
    String SCHEDULED_COUNT = "scheduled-count";
    String SECURITY_ROLE ="security-role";
    String SECURITY_SETTING ="security-setting";
    String SECURITY_SETTINGS ="security-settings";
    String SELECTOR_STRING = "selector";
    String SOCKET_BINDING = "socket-binding";
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
        RUN_SYNC_SPEED_TEST, SERVER_DUMP_INTERVAL, MEMORY_WARNING_THRESHOLD, MEMORY_MEASURE_INTERVAL, PAGE_MAX_CONCURRENT_IO
    };

    AttributeDefinition[]  SIMPLE_ROOT_RESOURCE_WRITE_ATTRIBUTES = {
        FAILOVER_ON_SHUTDOWN, MESSAGE_COUNTER_ENABLED, MESSAGE_COUNTER_MAX_DAY_HISTORY, MESSAGE_COUNTER_SAMPLE_PERIOD
    };

    AttributeDefinition[] CORE_QUEUE_ATTRIBUTES = { QUEUE_ADDRESS, FILTER, DURABLE };

    AttributeDefinition[] JMS_QUEUE_ATTRIBUTES = { ENTRIES, SELECTOR, DURABLE };

    String[] PATHS = new String[] {CommonAttributes.BINDINGS_DIRECTORY, CommonAttributes.JOURNAL_DIRECTORY, CommonAttributes.LARGE_MESSAGES_DIRECTORY, CommonAttributes.PAGING_DIRECTORY};
}
