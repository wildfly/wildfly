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
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.messaging.jms.JndiEntriesAttribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {
    SimpleAttributeDefinition ADDRESS = new SimpleAttributeDefinition("address", ModelType.STRING, false);

    SimpleAttributeDefinition ALLOW_FAILBACK =  new SimpleAttributeDefinition("allow-failback",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ALLOW_AUTO_FAILBACK), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition ASYNC_CONNECTION_EXECUTION_ENABLED = new SimpleAttributeDefinition("async-connection-execution-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ASYNC_CONNECTION_EXECUTION_ENABLED), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition AUTO_GROUP = new SimpleAttributeDefinition("auto-group",
            new ModelNode().set(HornetQClient.DEFAULT_AUTO_GROUP), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition BACKUP = new SimpleAttributeDefinition("backup",
            new ModelNode().set(ConfigurationImpl.DEFAULT_BACKUP), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition BLOCK_ON_ACK = new SimpleAttributeDefinition("block-on-acknowledge",
            new ModelNode().set(HornetQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition BLOCK_ON_DURABLE_SEND = new SimpleAttributeDefinition("block-on-durable-send",
            new ModelNode().set(HornetQClient.DEFAULT_BLOCK_ON_DURABLE_SEND), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition BLOCK_ON_NON_DURABLE_SEND = new SimpleAttributeDefinition("block-on-non-durable-send",
            new ModelNode().set(HornetQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND), ModelType.BOOLEAN, true);

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
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTERED), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition CLUSTER_PASSWORD = new SimpleAttributeDefinition("cluster-password",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_PASSWORD),  ModelType.STRING, true);

    SimpleAttributeDefinition CLUSTER_USER = new SimpleAttributeDefinition("cluster-user",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_USER), ModelType.STRING, true);

    SimpleAttributeDefinition CONFIRMATION_WINDOW_SIZE = new SimpleAttributeDefinition("confirmation-window-size",
            new ModelNode().set(HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    JndiEntriesAttribute CONNECTION_ENTRIES = JndiEntriesAttribute.CONNECTION_FACTORY;

    SimpleAttributeDefinition CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinition("scheduled-thread-pool-max-size",
            new ModelNode().set(HornetQClient.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition CONNECTION_THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinition("thread-pool-max-size",
            new ModelNode().set(HornetQClient.DEFAULT_THREAD_POOL_MAX_SIZE), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition CONNECTION_TTL = new SimpleAttributeDefinition("connection-ttl",
            new ModelNode().set(HornetQClient.DEFAULT_CONNECTION_TTL), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition CONNECTION_TTL_OVERRIDE = new SimpleAttributeDefinition("connection-ttl-override",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CONNECTION_TTL_OVERRIDE), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);


    SimpleAttributeDefinition CONNECTOR_REF = new SimpleAttributeDefinition("connector-ref", ModelType.STRING, true);

    ConnectorRefsAttribute CONNECTOR_REFS_OPTIONAL = ConnectorRefsAttribute.BROADCAST_GROUP;

    SimpleAttributeDefinition CONSUMER_MAX_RATE = new SimpleAttributeDefinition("consumer-max-rate",
            new ModelNode().set(HornetQClient.DEFAULT_CONSUMER_MAX_RATE), ModelType.INT,  true, MeasurementUnit.PER_SECOND);

    SimpleAttributeDefinition CONSUMER_WINDOW_SIZE = new SimpleAttributeDefinition("consumer-window-size",
            new ModelNode().set(HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition CREATE_BINDINGS_DIR = new SimpleAttributeDefinition("create-bindings-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_BINDINGS_DIR), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition CREATE_JOURNAL_DIR = new SimpleAttributeDefinition("create-journal-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_JOURNAL_DIR), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition DISCOVERY_GROUP_NAME = new SimpleAttributeDefinition("discovery-group-name", ModelType.STRING, true);

    SimpleAttributeDefinition DISCOVERY_INITIAL_WAIT_TIMEOUT = new SimpleAttributeDefinition("discovery-initial-wait-timeout", ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition DIVERT_ADDRESS = new SimpleAttributeDefinition("divert-address", "address", null, ModelType.STRING, false, false, MeasurementUnit.NONE);

    SimpleAttributeDefinition DUPS_OK_BATCH_SIZE = new SimpleAttributeDefinition("dups-ok-batch-size",
            new ModelNode().set(HornetQClient.DEFAULT_ACK_BATCH_SIZE), ModelType.INT, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition DURABLE = new SimpleAttributeDefinition("durable", new ModelNode().set(false), ModelType.BOOLEAN,  true);

    JndiEntriesAttribute ENTRIES = JndiEntriesAttribute.DESTINATION;

    SimpleAttributeDefinition EXCLUSIVE = new SimpleAttributeDefinition("exclusive", ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition FAILBACK_DELAY = new SimpleAttributeDefinition("failback-delay",
            new ModelNode().set(ConfigurationImpl.DEFAULT_FAILBACK_DELAY), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition FAILOVER_ON_INITIAL_CONNECTION =  new SimpleAttributeDefinition("failover-on-initial-connection",
            new ModelNode().set(HornetQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = new SimpleAttributeDefinition("failover-on-server-shutdown", ModelType.BOOLEAN, true);

    SimpleAttributeDefinition FAILOVER_ON_SHUTDOWN = new SimpleAttributeDefinition("failover-on-shutdown",
            new ModelNode().set(false /*TODO should be ConfigurationImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN but field is private*/), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition FILTER = new SimpleAttributeDefinition("filter", ModelType.STRING, true);

    SimpleAttributeDefinition FORWARDING_ADDRESS = new SimpleAttributeDefinition("forwarding-address", ModelType.STRING, false);

    SimpleAttributeDefinition GROUP_ADDRESS = new SimpleAttributeDefinition("group-address", ModelType.STRING, false);

    SimpleAttributeDefinition GROUP_ID = new SimpleAttributeDefinition("group-id", ModelType.STRING, true);

    SimpleAttributeDefinition GROUP_PORT = new SimpleAttributeDefinition("group-port", ModelType.INT, false);

    SimpleAttributeDefinition HA = new SimpleAttributeDefinition("forwarding-address", new ModelNode().set(HornetQClient.DEFAULT_HA),  ModelType.BOOLEAN, false);

    SimpleAttributeDefinition ID_CACHE_SIZE = new SimpleAttributeDefinition("id-cache-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ID_CACHE_SIZE), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition JMX_DOMAIN = new SimpleAttributeDefinition("jmx-domain",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JMX_DOMAIN), ModelType.STRING, true);

    SimpleAttributeDefinition JMX_MANAGEMENT_ENABLED = new SimpleAttributeDefinition("jmx-management-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JMX_MANAGEMENT_ENABLED), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition JNDI_PARAMS = new SimpleAttributeDefinition("jndi-params", ModelType.STRING, true);

    SimpleAttributeDefinition JOURNAL_BUFFER_SIZE = new SimpleAttributeDefinition("journal-buffer-size", ModelType.LONG,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition JOURNAL_BUFFER_TIMEOUT = new SimpleAttributeDefinition("journal-buffer-timeout", ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition JOURNAL_COMPACT_MIN_FILES = new SimpleAttributeDefinition("journal-compact-min-files",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_COMPACT_MIN_FILES), ModelType.INT,  true);

    SimpleAttributeDefinition JOURNAL_COMPACT_PERCENTAGE = new SimpleAttributeDefinition("journal-compact-percentage",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_COMPACT_PERCENTAGE), ModelType.INT,  true, MeasurementUnit.PERCENTAGE);

    SimpleAttributeDefinition JOURNAL_FILE_SIZE = new SimpleAttributeDefinition("journal-file-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_FILE_SIZE), ModelType.LONG,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition JOURNAL_MAX_IO = new SimpleAttributeDefinition("journal-max-io", ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition JOURNAL_MIN_FILES = new SimpleAttributeDefinition("journal-min-files",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_MIN_FILES), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition JOURNAL_SYNC_NON_TRANSACTIONAL = new SimpleAttributeDefinition("journal-sync-non-transactional",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_NON_TRANSACTIONAL), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition JOURNAL_SYNC_TRANSACTIONAL = new SimpleAttributeDefinition("journal-sync-transactional",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_TRANSACTIONAL), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition JOURNAL_TYPE = new SimpleAttributeDefinition("journal-type", "journal-type",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_TYPE.toString()), ModelType.STRING,  true, false, MeasurementUnit.NONE, JournalTypeValidator.INSTANCE);

    SimpleAttributeDefinition LOAD_BALANCING_CLASS_NAME = new SimpleAttributeDefinition("connection-load-balancing-policy-class-name", ModelType.STRING, true);

    SimpleAttributeDefinition LOCAL_BIND_ADDRESS = new SimpleAttributeDefinition("local-bind-address", ModelType.STRING, true);

    SimpleAttributeDefinition LOCAL_BIND_PORT = new SimpleAttributeDefinition("local-bind-port", new ModelNode().set(-1), ModelType.INT, true);

    SimpleAttributeDefinition LOG_JOURNAL_WRITE_RATE = new SimpleAttributeDefinition("log-journal-write-rate",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_LOG_WRITE_RATE), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition MANAGEMENT_ADDRESS = new SimpleAttributeDefinition("management-address",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_ADDRESS.toString()), ModelType.STRING, true);

    SimpleAttributeDefinition MANAGEMENT_NOTIFICATION_ADDRESS = new SimpleAttributeDefinition("management-notification-address",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_NOTIFICATION_ADDRESS.toString()), ModelType.STRING, true);

    SimpleAttributeDefinition MAX_RETRY_INTERVAL = new SimpleAttributeDefinition("max-retry-interval",
            new ModelNode().set(HornetQClient.DEFAULT_MAX_RETRY_INTERVAL), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition MEMORY_MEASURE_INTERVAL = new SimpleAttributeDefinition("memory-measure-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MEMORY_MEASURE_INTERVAL), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition MEMORY_WARNING_THRESHOLD = new SimpleAttributeDefinition("memory-warning-threshold",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MEMORY_WARNING_THRESHOLD), ModelType.INT,  true, MeasurementUnit.PERCENTAGE);

    SimpleAttributeDefinition MESSAGE_COUNTER_ENABLED = new SimpleAttributeDefinition("message-counter-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_ENABLED), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition MESSAGE_COUNTER_MAX_DAY_HISTORY = new SimpleAttributeDefinition("message-counter-max-day-history",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_MAX_DAY_HISTORY), ModelType.INT,  true, MeasurementUnit.DAYS);

    SimpleAttributeDefinition MESSAGE_COUNTER_SAMPLE_PERIOD = new SimpleAttributeDefinition("message-counter-sample-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_SAMPLE_PERIOD), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition MESSAGE_EXPIRY_SCAN_PERIOD = new SimpleAttributeDefinition("message-expiry-scan-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_EXPIRY_SCAN_PERIOD), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition MESSAGE_EXPIRY_THREAD_PRIORITY = new SimpleAttributeDefinition("message-expiry-thread-priority",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_EXPIRY_THREAD_PRIORITY), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition MIN_LARGE_MESSAGE_SIZE = new SimpleAttributeDefinition("min-large-message-size",
            new ModelNode().set(HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition NAME_OPTIONAL = new SimpleAttributeDefinition("name", ModelType.STRING, true);

    SimpleAttributeDefinition PERF_BLAST_PAGES = new SimpleAttributeDefinition("perf-blast-pages",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_PERF_BLAST_PAGES), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY = new SimpleAttributeDefinition("persist-delivery-count-before-delivery",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition PERSISTENCE_ENABLED = new SimpleAttributeDefinition("persistence-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSISTENCE_ENABLED), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition PERSIST_ID_CACHE = new SimpleAttributeDefinition("persist-id-cache",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSIST_ID_CACHE), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition PRE_ACK = new SimpleAttributeDefinition("pre-acknowledge",
            new ModelNode().set(HornetQClient.DEFAULT_PRE_ACKNOWLEDGE), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition PRODUCER_MAX_RATE = new SimpleAttributeDefinition("producer-max-rate",
            new ModelNode().set(HornetQClient.DEFAULT_PRODUCER_MAX_RATE), ModelType.INT,  true, MeasurementUnit.PER_SECOND);

    SimpleAttributeDefinition PRODUCER_WINDOW_SIZE = new SimpleAttributeDefinition("producer-window-size",
            new ModelNode().set(HornetQClient.DEFAULT_PRODUCER_WINDOW_SIZE), ModelType.INT,  true, MeasurementUnit.BYTES);

    SimpleAttributeDefinition QUEUE_ADDRESS = new SimpleAttributeDefinition("queue-address", "address", null, ModelType.STRING, false, false, MeasurementUnit.NONE);

    SimpleAttributeDefinition RECONNECT_ATTEMPTS = new SimpleAttributeDefinition("reconnect-attempts",
            new ModelNode().set(HornetQClient.DEFAULT_RECONNECT_ATTEMPTS), ModelType.INT, true, MeasurementUnit.NONE);

    RemotingInterceptorsAttribute REMOTING_INTERCEPTORS = RemotingInterceptorsAttribute.INSTANCE;

    SimpleAttributeDefinition RETRY_INTERVAL = new SimpleAttributeDefinition("retry-interval",
            new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL), ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition RETRY_INTERVAL_MULTIPLIER = new SimpleAttributeDefinition("retry-interval-multiplier",
            new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER), ModelType.BIG_DECIMAL, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition ROUTING_NAME = new SimpleAttributeDefinition("routing-name", ModelType.STRING, true);

    SimpleAttributeDefinition RUN_SYNC_SPEED_TEST = new SimpleAttributeDefinition("run-sync-speed-test",
            new ModelNode().set(ConfigurationImpl.DEFAULT_RUN_SYNC_SPEED_TEST), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinition("scheduled-thread-pool-max-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition SECURITY_ENABLED = new SimpleAttributeDefinition("security-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_ENABLED), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition SECURITY_INVALIDATION_INTERVAL = new SimpleAttributeDefinition("security-invalidation-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_INVALIDATION_INTERVAL), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition SELECTOR = new SimpleAttributeDefinition("selector", ModelType.STRING, true);

    SimpleAttributeDefinition SERVER_DUMP_INTERVAL = new SimpleAttributeDefinition("server-dump-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SERVER_DUMP_INTERVAL), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition SETUP_ATTEMPTS = new SimpleAttributeDefinition("setup-attempts", ModelType.INT, true, MeasurementUnit.NONE);

    SimpleAttributeDefinition SETUP_INTERVAL = new SimpleAttributeDefinition("setup-interval", ModelType.LONG, true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition SHARED_STORE = new SimpleAttributeDefinition("shared-store",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SHARED_STORE), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition THREAD_POOL_MAX_SIZE = new SimpleAttributeDefinition("thread-pool-max-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_THREAD_POOL_MAX_SIZE), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition TRANSACTION_ATTRIBUTE = new SimpleAttributeDefinition("transaction",
            new ModelNode().set("transaction"), ModelType.STRING,  true);

    SimpleAttributeDefinition TRANSACTION_BATCH_SIZE = new SimpleAttributeDefinition("transaction-batch-size",
            new ModelNode().set(HornetQClient.DEFAULT_ACK_BATCH_SIZE), ModelType.INT,  true, MeasurementUnit.NONE);

    SimpleAttributeDefinition TRANSACTION_TIMEOUT = new SimpleAttributeDefinition("transaction-timeout",
            new ModelNode().set(ConfigurationImpl.DEFAULT_TRANSACTION_TIMEOUT), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition TRANSACTION_TIMEOUT_SCAN_PERIOD = new SimpleAttributeDefinition("transaction-timeout-scan-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_TRANSACTION_TIMEOUT_SCAN_PERIOD), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition TRANSFORMER_CLASS_NAME = new SimpleAttributeDefinition("transformer-class-name", ModelType.STRING, true);

    SimpleAttributeDefinition USE_GLOBAL_POOLS = new SimpleAttributeDefinition("use-global-pools",
            new ModelNode().set(HornetQClient.DEFAULT_USE_GLOBAL_POOLS), ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition USE_JNDI = new SimpleAttributeDefinition("use-jndi", ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition USE_LOCAL_TX = new SimpleAttributeDefinition("use-local-tx", ModelType.BOOLEAN,  true);

    SimpleAttributeDefinition WILD_CARD_ROUTING_ENABLED = new SimpleAttributeDefinition("wild-card-routing-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_WILDCARD_ROUTING_ENABLED), ModelType.BOOLEAN,  true);

    String ACCEPTOR ="acceptor";
    String ACCEPTORS ="acceptors";
    String ADDRESS_FULL_MESSAGE_POLICY ="address-full-policy";
    String ADDRESS_SETTING ="address-setting";
    String ADDRESS_SETTINGS ="address-settings";
    String ALLOW_DIRECT_CONNECTIONS_ONLY = "allow-direct-connections-only";
    String BINDINGS_DIRECTORY ="bindings-directory";
    String BRIDGE = "bridge";
    String BRIDGES = "bridges";
    String BROADCAST_GROUP = "broadcast-group";
    String BROADCAST_GROUPS = "broadcast-groups";
    String CLASS_NAME = "class-name";
    String CLUSTER_CONNECTION = "cluster-connection";
    String CLUSTER_CONNECTIONS = "cluster-connections";
    String CONNECTION_FACTORY ="connection-factory";
    String CONNECTION_LOAD_BALANCING_CLASS_NAME = "connection-load-balancing-policy-class-name";
    String CONNECTOR ="connector";
    String CONNECTORS ="connectors";
    String CONNECTOR_NAME ="connector-name";
    String CONNECTOR_REF_STRING ="connector-ref";
    String CONNECTOR_SERVICE = "connector-service";
    String CONNECTOR_SERVICES = "connector-services";
    String CONSUME_NAME ="consume";
    String CORE_QUEUE ="core-queue";
    String CORE_QUEUES ="core-queues";
    String CREATEDURABLEQUEUE_NAME ="createDurableQueue";
    String CREATETEMPQUEUE_NAME ="createTempQueue";
    String CREATE_NON_DURABLE_QUEUE_NAME ="createNonDurableQueue";
    String DEAD_LETTER_ADDRESS ="dead-letter-address";
    String DELETEDURABLEQUEUE_NAME ="deleteDurableQueue";
    String DELETETEMPQUEUE_NAME ="deleteTempQueue";
    String DELETE_NON_DURABLE_QUEUE_NAME ="deleteNonDurableQueue";
    String DISCOVERY_GROUP = "discovery-group";
    String DISCOVERY_GROUPS = "discovery-groups";
    String DISCOVERY_GROUP_REF ="discovery-group-ref";
    String DIVERT = "divert";
    String DIVERTS = "diverts";
    String ENTRIES_STRING = "entries";
    String ENTRY ="entry";
    String EXPIRY_ADDRESS ="expiry-address";
    String FACTORY_CLASS ="factory-class";
    String FILE_DEPLOYMENT_ENABLED ="file-deployment-enabled";
    String FORWARD_WHEN_NO_CONSUMERS = "forward-when-no-consumers";
    String GROUPING_HANDLER ="grouping-handler";
    String IN_VM_ACCEPTOR ="in-vm-acceptor";
    String IN_VM_CONNECTOR ="in-vm-connector";
    String JMS_CONNECTION_FACTORIES ="jms-connection-factories";
    String JMS_DESTINATIONS = "jms-destinations";
    String JMS_QUEUE ="jms-queue";
    String JMS_TOPIC ="jms-topic";
    String JOURNAL_DIRECTORY ="journal-directory";
    String KEY ="key";
    String INBOUND_CONFIG = "inbound-config";
    String LARGE_MESSAGES_DIRECTORY ="large-messages-directory";
    String LAST_VALUE_QUEUE = "last-value=queue";
    String LIVE_CONNECTOR_REF ="live-connector-ref";
    String LOCAL = "local";
    String LOCAL_TX = "LocalTransaction";
    String LVQ ="last-value-queue";
    String MANAGE_NAME ="manage";
    String MATCH ="match";
    String MAX_DELIVERY_ATTEMPTS ="max-delivery-attempts";
    String MAX_HOPS ="max-hops";
    String MAX_SIZE_BYTES_NODE_NAME ="max-size-bytes";
    String MESSAGE_COUNTER_HISTORY_DAY_LIMIT = "message-counter-history-day-limit";
    String MODE = "mode";
    String NAME ="name";
    String NETTY_ACCEPTOR ="netty-acceptor";
    String NETTY_CONNECTOR ="netty-connector";
    String NONE = "none";
    String NO_TX = "NoTransaction";
    String PAGE_MAX_CACHE_SIZE = "page-max-cache-size";
    String PAGE_SIZE_BYTES_NODE_NAME ="page-size-bytes";
    String PAGING_DIRECTORY ="paging-directory";
    String PARAM ="param";
    String PASSWORD ="password";
    String PATH ="path";
    String PERMISSION_ELEMENT_NAME ="permission";
    String POOLED_CONNECTION_FACTORY = "pooled-connection-factory";
    String QUEUE ="queue";
    String QUEUE_NAME ="queue-name";
    String REDELIVERY_DELAY ="redelivery-delay";
    String REDISTRIBUTION_DELAY ="redistribution-delay";
    String REFRESH_TIMEOUT ="refresh-timeout";
    String RELATIVE_TO ="relative-to";
    String REMOTING_INTERCEPTORS_STRING ="remoting-interceptors";
    String RESOURCE_ADAPTER = "resource-adapter";
    String ROLE = "role";
    String ROLES_ATTR_NAME ="roles";
    String SECURITY_SETTING ="security-setting";
    String SECURITY_SETTINGS ="security-settings";
    String SEND_NAME ="send";
    String SEND_TO_DLA_ON_NO_ROUTE ="send-to-dla-on-no-route";
    String SERVER_ID ="server-id";
    String SOCKET_BINDING ="socket-binding";
    String STATIC_CONNECTORS = "static-connectors";
    String STRING ="string";
    String SUBSYSTEM ="subsystem";
    String TIMEOUT = "timeout";
    String TRANSACTION = "transaction";
    String TYPE_ATTR_NAME ="type";
    String USE_DUPLICATE_DETECTION = "use-duplicate-detection";
    String USER = "user";
    String VALUE ="value";
    String XA = "xa";
    String XA_TX = "XATransaction";

    AttributeDefinition[] SIMPLE_ROOT_RESOURCE_ATTRIBUTES = {
        NAME_OPTIONAL, CLUSTERED, PERSISTENCE_ENABLED, SCHEDULED_THREAD_POOL_MAX_SIZE,
        THREAD_POOL_MAX_SIZE, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL, WILD_CARD_ROUTING_ENABLED, MANAGEMENT_ADDRESS,
        MANAGEMENT_NOTIFICATION_ADDRESS, CLUSTER_USER, CLUSTER_PASSWORD, JMX_MANAGEMENT_ENABLED, JMX_DOMAIN, MESSAGE_COUNTER_ENABLED,
        MESSAGE_COUNTER_SAMPLE_PERIOD, MESSAGE_COUNTER_MAX_DAY_HISTORY, CONNECTION_TTL_OVERRIDE, ASYNC_CONNECTION_EXECUTION_ENABLED,
        TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_SCAN_PERIOD, MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY,
        ID_CACHE_SIZE, PERSIST_ID_CACHE, REMOTING_INTERCEPTORS, BACKUP, ALLOW_FAILBACK, FAILBACK_DELAY, FAILOVER_ON_SHUTDOWN,
        SHARED_STORE, PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY, CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE, JOURNAL_BUFFER_TIMEOUT,
        JOURNAL_BUFFER_SIZE, JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE, JOURNAL_FILE_SIZE,
        JOURNAL_MIN_FILES, JOURNAL_COMPACT_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_MAX_IO, PERF_BLAST_PAGES, RUN_SYNC_SPEED_TEST,
        SERVER_DUMP_INTERVAL, MEMORY_WARNING_THRESHOLD, MEMORY_MEASURE_INTERVAL
    };

    AttributeDefinition[]  SIMPLE_ROOT_RESOURCE_WRITE_ATTRIBUTES = {
        FAILOVER_ON_SHUTDOWN, MESSAGE_COUNTER_ENABLED, MESSAGE_COUNTER_MAX_DAY_HISTORY, MESSAGE_COUNTER_SAMPLE_PERIOD
    };

    String[] COMPLEX_ROOT_RESOURCE_ATTRIBUTES =  {
        /*TODO remove */ LIVE_CONNECTOR_REF, /* TODO remove */ CONNECTOR, /* TODO remove */ ACCEPTOR,
        /* TODO remove */ BROADCAST_GROUP, /* TODO remove */ DISCOVERY_GROUP,
        /* TODO remove */ BRIDGE, /* TODO remove */ CLUSTER_CONNECTION,
        /* TODO remove */ GROUPING_HANDLER, /* TODO remove */ PAGING_DIRECTORY, /* TODO remove */ BINDINGS_DIRECTORY,
        /* TODO remove */ JOURNAL_DIRECTORY, /* TODO remove */ LARGE_MESSAGES_DIRECTORY,
        /* TODO remove */ SECURITY_SETTING, /* TODO remove */ ADDRESS_SETTING, /* TODO remove */ CONNECTOR_SERVICE
    };

    AttributeDefinition[] DIVERT_ATTRIBUTES = {
        ROUTING_NAME, DIVERT_ADDRESS, FORWARDING_ADDRESS, FILTER, TRANSFORMER_CLASS_NAME, EXCLUSIVE
    };

    AttributeDefinition[] BROADCAST_GROUP_ATTRIBUTES = {
        LOCAL_BIND_ADDRESS, LOCAL_BIND_PORT, GROUP_ADDRESS, GROUP_PORT, ConnectorRefsAttribute.BROADCAST_GROUP
    };

    AttributeDefinition[] CORE_QUEUE_ATTRIBUTES = { QUEUE_ADDRESS, FILTER, DURABLE };

    AttributeDefinition[] JMS_QUEUE_ATTRIBUTES = { ENTRIES, SELECTOR, DURABLE };
}
