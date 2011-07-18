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

import org.hornetq.core.config.impl.ConfigurationImpl;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {

    AttributeDefinition ALLOW_FAILBACK =  new AttributeDefinition("allow-failback",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ALLOW_AUTO_FAILBACK), ModelType.BOOLEAN,  true);
    AttributeDefinition ASYNC_CONNECTION_EXECUTION_ENABLED = new AttributeDefinition("async-connection-execution-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ASYNC_CONNECTION_EXECUTION_ENABLED), ModelType.BOOLEAN,  true);
    AttributeDefinition BACKUP = new AttributeDefinition("backup",
            new ModelNode().set(ConfigurationImpl.DEFAULT_BACKUP), ModelType.BOOLEAN,  true);
    AttributeDefinition CLUSTERED = new AttributeDefinition("clustered",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTERED), ModelType.BOOLEAN,  true);
    AttributeDefinition CLUSTER_PASSWORD = new AttributeDefinition("cluster-password",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_PASSWORD),  ModelType.STRING, true);
    AttributeDefinition CLUSTER_USER = new AttributeDefinition("cluster-user",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_USER), ModelType.STRING, true);
    AttributeDefinition CONNECTION_TTL_OVERRIDE = new AttributeDefinition("connection-ttl-override",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CONNECTION_TTL_OVERRIDE), ModelType.LONG,  true);
    AttributeDefinition CREATE_BINDINGS_DIR = new AttributeDefinition("create-bindings-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_BINDINGS_DIR), ModelType.BOOLEAN,  true);
    AttributeDefinition CREATE_JOURNAL_DIR = new AttributeDefinition("create-journal-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_JOURNAL_DIR), ModelType.BOOLEAN,  true);
    AttributeDefinition FAILBACK_DELAY = new AttributeDefinition("failback-delay",
            new ModelNode().set(ConfigurationImpl.DEFAULT_FAILBACK_DELAY), ModelType.LONG,  true);
    AttributeDefinition FAILOVER_ON_SHUTDOWN =  new AttributeDefinition("failover-on-shutdown",
            new ModelNode().set(false /*TODO should be ConfigurationImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN but field is private*/), ModelType.BOOLEAN,  true);
    AttributeDefinition ID_CACHE_SIZE =new AttributeDefinition("id-cache-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ID_CACHE_SIZE), ModelType.INT,  true);
    AttributeDefinition JMX_DOMAIN = new AttributeDefinition("jmx-domain",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JMX_DOMAIN), ModelType.STRING, true);
    AttributeDefinition JMX_MANAGEMENT_ENABLED = new AttributeDefinition("jmx-management-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JMX_MANAGEMENT_ENABLED), ModelType.BOOLEAN,  true);
    AttributeDefinition JOURNAL_BUFFER_SIZE = new AttributeDefinition("journal-buffer-size", ModelType.LONG,  true);
    AttributeDefinition JOURNAL_BUFFER_TIMEOUT = new AttributeDefinition("journal-buffer-timeout", ModelType.LONG,  true);
    AttributeDefinition JOURNAL_COMPACT_MIN_FILES = new AttributeDefinition("journal-compact-min-files",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_COMPACT_MIN_FILES), ModelType.INT,  true);
    AttributeDefinition JOURNAL_COMPACT_PERCENTAGE = new AttributeDefinition("journal-compact-percentage",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_COMPACT_PERCENTAGE), ModelType.INT,  true);
    AttributeDefinition JOURNAL_FILE_SIZE = new AttributeDefinition("journal-file-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_FILE_SIZE), ModelType.LONG,  true);
    AttributeDefinition JOURNAL_MAX_IO = new AttributeDefinition("journal-max-io", ModelType.INT,  true);
    AttributeDefinition JOURNAL_MIN_FILES = new AttributeDefinition("journal-min-files",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_MIN_FILES), ModelType.INT,  true);
    AttributeDefinition JOURNAL_SYNC_NON_TRANSACTIONAL = new AttributeDefinition("journal-sync-non-transactional",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_NON_TRANSACTIONAL), ModelType.BOOLEAN,  true);
    AttributeDefinition JOURNAL_SYNC_TRANSACTIONAL = new AttributeDefinition("journal-sync-transactional",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_TRANSACTIONAL), ModelType.BOOLEAN,  true);
    AttributeDefinition JOURNAL_TYPE = new AttributeDefinition("journal-type", "journal-type",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_TYPE.toString()), ModelType.STRING,  true, false, JournalTypeValidator.INSTANCE);
    AttributeDefinition LOG_JOURNAL_WRITE_RATE = new AttributeDefinition("log-journal-write-rate",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_LOG_WRITE_RATE), ModelType.BOOLEAN,  true);
    AttributeDefinition MANAGEMENT_ADDRESS = new AttributeDefinition("management-address",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_ADDRESS.toString()), ModelType.STRING, true);
    AttributeDefinition MANAGEMENT_NOTIFICATION_ADDRESS = new AttributeDefinition("management-notification-address",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_NOTIFICATION_ADDRESS.toString()), ModelType.STRING, true);
    AttributeDefinition MEMORY_MEASURE_INTERVAL = new AttributeDefinition("memory-measure-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MEMORY_MEASURE_INTERVAL), ModelType.LONG,  true);
    AttributeDefinition MEMORY_WARNING_THRESHOLD = new AttributeDefinition("memory-warning-threshold",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MEMORY_WARNING_THRESHOLD), ModelType.INT,  true);
    AttributeDefinition MESSAGE_COUNTER_ENABLED = new AttributeDefinition("message-counter-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_ENABLED), ModelType.BOOLEAN,  true);
    AttributeDefinition MESSAGE_COUNTER_MAX_DAY_HISTORY = new AttributeDefinition("message-counter-max-day-history",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_MAX_DAY_HISTORY), ModelType.INT,  true);
    AttributeDefinition MESSAGE_COUNTER_SAMPLE_PERIOD = new AttributeDefinition("message-counter-sample-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_SAMPLE_PERIOD), ModelType.LONG,  true);
    AttributeDefinition MESSAGE_EXPIRY_SCAN_PERIOD = new AttributeDefinition("message-expiry-scan-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_EXPIRY_SCAN_PERIOD), ModelType.LONG,  true);
    AttributeDefinition MESSAGE_EXPIRY_THREAD_PRIORITY = new AttributeDefinition("message-expiry-thread-priority",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_EXPIRY_THREAD_PRIORITY), ModelType.INT,  true);
    AttributeDefinition NAME_OPTIONAL = new AttributeDefinition("name", ModelType.STRING, true);
    AttributeDefinition NAME_REQUIRED = new AttributeDefinition("name", "name", null, ModelType.STRING, false, false);
    AttributeDefinition PERF_BLAST_PAGES = new AttributeDefinition("perf-blast-pages",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_PERF_BLAST_PAGES), ModelType.INT,  true);
    AttributeDefinition PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY = new AttributeDefinition("persist-delivery-count-before-delivery",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY), ModelType.BOOLEAN,  true);
    AttributeDefinition PERSISTENCE_ENABLED = new AttributeDefinition("persistence-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSISTENCE_ENABLED), ModelType.BOOLEAN,  true);
    AttributeDefinition PERSIST_ID_CACHE = new AttributeDefinition("persist-id-cache",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSIST_ID_CACHE), ModelType.BOOLEAN,  true);
    AttributeDefinition RUN_SYNC_SPEED_TEST = new AttributeDefinition("run-sync-speed-test",
            new ModelNode().set(ConfigurationImpl.DEFAULT_RUN_SYNC_SPEED_TEST), ModelType.BOOLEAN,  true);
    AttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = new AttributeDefinition("scheduled-thread-pool-max-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE), ModelType.INT,  true);
    AttributeDefinition SECURITY_ENABLED = new AttributeDefinition("security-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_ENABLED), ModelType.BOOLEAN,  true);
    AttributeDefinition SECURITY_INVALIDATION_INTERVAL = new AttributeDefinition("security-invalidation-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_INVALIDATION_INTERVAL), ModelType.LONG,  true);
    AttributeDefinition SERVER_DUMP_INTERVAL = new AttributeDefinition("server-dump-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SERVER_DUMP_INTERVAL), ModelType.LONG,  true);
    AttributeDefinition SHARED_STORE = new AttributeDefinition("shared-store",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SHARED_STORE), ModelType.BOOLEAN,  true);
    AttributeDefinition THREAD_POOL_MAX_SIZE = new AttributeDefinition("thread-pool-max-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_THREAD_POOL_MAX_SIZE), ModelType.INT,  true);
    AttributeDefinition TRANSACTION_TIMEOUT = new AttributeDefinition("transaction-timeout",
            new ModelNode().set(ConfigurationImpl.DEFAULT_TRANSACTION_TIMEOUT), ModelType.LONG,  true);
    AttributeDefinition TRANSACTION_TIMEOUT_SCAN_PERIOD = new AttributeDefinition("transaction-timeout-scan-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_TRANSACTION_TIMEOUT_SCAN_PERIOD), ModelType.LONG,  true);
    AttributeDefinition WILD_CARD_ROUTING_ENABLED = new AttributeDefinition("wild-card-routing-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_WILDCARD_ROUTING_ENABLED), ModelType.BOOLEAN,  true);

    String ACCEPTOR ="acceptor";
    String ACCEPTORS ="acceptors";
    String ADDRESS ="address";
    String ADDRESS_FULL_MESSAGE_POLICY ="address-full-policy";
    String ADDRESS_SETTING ="address-setting";
    String ADDRESS_SETTINGS ="address-settings";
    String ALLOW_DIRECT_CONNECTIONS_ONLY = "allow-direct-connections-only";
    String AUTO_GROUP ="auto-group";
    String BINDINGS_DIRECTORY ="bindings-directory";
    String BLOCK_ON_ACK ="block-on-acknowledge";
    String BLOCK_ON_DURABLE_SEND ="block-on-durable-send";
    String BLOCK_ON_NON_DURABLE_SEND ="block-on-non-durable-send";
    String BRIDGE = "bridge";
    String BRIDGES = "bridges";
    String BROADCAST_GROUP = "broadcast-group";
    String BROADCAST_GROUPS = "broadcast-groups";
    String BROADCAST_PERIOD ="broadcast-period";
    String CACHE_LARGE_MESSAGE_CLIENT ="cache-large-message-client";
    String CALL_TIMEOUT ="call-timeout";
    String CLASS_NAME = "class-name";
    String CLIENT_FAILURE_CHECK_PERIOD ="client-failure-check-period";
    String CLIENT_ID ="client-id";
    String CLUSTER_CONNECTION = "cluster-connection";
    String CLUSTER_CONNECTIONS = "cluster-connections";
    String CONFIRMATION_WINDOW_SIZE ="confirmation-window-size";
    String CONNECTION_FACTORY ="connection-factory";
    String CONNECTION_LOAD_BALANCING_CLASS_NAME = "connection-load-balancing-policy-class-name";
    String CONNECTION_TTL ="connection-ttl";
    String CONNECTOR ="connector";
    String CONNECTORS ="connectors";
    String CONNECTOR_NAME ="connector-name";
    String CONNECTOR_REF ="connector-ref";
    String CONNECTOR_SERVICE = "connector-service";
    String CONNECTOR_SERVICES = "connector-services";
    String CONSUMER_MAX_RATE ="consumer-max-rate";
    String CONSUMER_WINDOW_SIZE ="consumer-window-size";
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
    String DISCOVERY_GROUP_NAME ="discovery-group-name";
    String DISCOVERY_GROUP_REF ="discovery-group-ref";
    String DISCOVERY_INITIAL_WAIT_TIMEOUT ="discovery-initial-wait-timeout";
    String DIVERT = "divert";
    String DIVERTS = "diverts";
    String DUPS_OK_BATCH_SIZE ="dups-ok-batch-size";
    String DURABLE ="durable";
    String ENTRIES ="entries";
    String ENTRY ="entry";
    String EXCLUSIVE = "exclusive";
    String EXPIRY_ADDRESS ="expiry-address";
    String FACTORY_CLASS ="factory-class";
    String FAILOVER_ON_INITIAL_CONNECTION = "failover-on-initial-connection";
    String FAILOVER_ON_SERVER_SHUTDOWN = "failover-on-server-shutdown";
    String FILE_DEPLOYMENT_ENABLED ="file-deployment-enabled";
    String FILTER ="filter";
    String FORWARDING_ADDRESS ="forwarding-address";
    String FORWARD_WHEN_NO_CONSUMERS = "forward-when-no-consumers";
    String GROUP_ADDRESS ="group-address";
    String GROUPING_HANDLER ="grouping-handler";
    String GROUP_ID ="group-id";
    String GROUP_PORT ="group-port";
    String HA ="ha";
    String IN_VM_ACCEPTOR ="in-vm-acceptor";
    String IN_VM_CONNECTOR ="in-vm-connector";
    String JMS_CONNECTION_FACTORIES ="jms-connection-factories";
    String JMS_DESTINATIONS = "jms-destinations";
    String JMS_QUEUE ="jms-queue";
    String JMS_TOPIC ="jms-topic";
    String JNDI_PARAMS = "jndi-params";
    String JOURNAL_DIRECTORY ="journal-directory";
    String KEY ="key";
    String INBOUND_CONFIG = "inbound-config";
    String LARGE_MESSAGES_DIRECTORY ="large-messages-directory";
    String LAST_VALUE_QUEUE = "last-value=queue";
    String LIVE_CONNECTOR_REF ="live-connector-ref";
    String LOCAL = "local";
    String LOAD_BALANCING_CLASS_NAME ="connection-load-balancing-policy-class-name";
    String LOCAL_BIND_ADDRESS ="local-bind-address";
    String LOCAL_BIND_PORT ="local-bind-port";
    String LOCAL_TX = "LocalTransaction";
    String LVQ ="last-value-queue";
    String MANAGE_NAME ="manage";
    String MATCH ="match";
    String MAX_DELIVERY_ATTEMPTS ="max-delivery-attempts";
    String MAX_HOPS ="max-hops";
    String MAX_RETRY_INTERVAL ="max-retry-interval";
    String MAX_SIZE_BYTES_NODE_NAME ="max-size-bytes";
    String MESSAGE_COUNTER_HISTORY_DAY_LIMIT = "message-counter-history-day-limit";
    String MIN_LARGE_MESSAGE_SIZE ="min-large-message-size";
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
    String PRE_ACK ="pre-acknowledge";
    String PRODUCER_MAX_RATE ="producer-max-rate";
    String PRODUCER_WINDOW_SIZE ="producer-window-size";
    String QUEUE ="queue";
    String QUEUE_ADDRESS ="queue-address";
    String QUEUE_NAME ="queue-name";
    String RECONNECT_ATTEMPTS ="reconnect-attempts";
    String REDELIVERY_DELAY ="redelivery-delay";
    String REDISTRIBUTION_DELAY ="redistribution-delay";
    String REFRESH_TIMEOUT ="refresh-timeout";
    String RELATIVE_TO ="relative-to";
    String REMOTING_INTERCEPTOR ="remoting-interceptor";
    String REMOTING_INTERCEPTORS ="remoting-interceptors";
    String RESOURCE_ADAPTER = "resource-adapter";
    String RETRY_INTERVAL ="retry-interval";
    String RETRY_INTERVAL_MULTIPLIER ="retry-interval-multiplier";
    String ROLE = "role";
    String ROLES_ATTR_NAME ="roles";
    String ROUTING_NAME = "routing-name";
    String SECURITY_SETTING ="security-setting";
    String SECURITY_SETTINGS ="security-settings";
    String SELECTOR ="selector";
    String SEND_NAME ="send";
    String SEND_TO_DLA_ON_NO_ROUTE ="send-to-dla-on-no-route";
    String SERVER_ID ="server-id";
    String SETUP_ATTEMPTS = "setup-attempts";
    String SETUP_INTERVAL = "setup-interval";
    String SOCKET_BINDING ="socket-binding";
    String STATIC_CONNECTORS = "static-connectors";
    String STRING ="string";
    String SUBSYSTEM ="subsystem";
    String TIMEOUT = "timeout";
    String TRANSACTION = "transaction";
    String TRANSACTION_BATCH_SIZE ="transaction-batch-size";
    String TRANSFORMER_CLASS_NAME = "transformer-class-name";
    String TYPE_ATTR_NAME ="type";
    String USE_DUPLICATE_DETECTION = "use-duplicate-detection";
    String USE_GLOBAL_POOLS ="use-global-pools";
    String USE_LOCAL_TX = "use-local-tx";
    String USE_JNDI = "use-jndi";
    String USER = "user";
    String VALUE ="value";
    String XA_TX = "XATransaction";



    AttributeDefinition[] SIMPLE_ROOT_RESOURCE_ATTRIBUTES = {
        NAME_OPTIONAL, CLUSTERED, PERSISTENCE_ENABLED, SCHEDULED_THREAD_POOL_MAX_SIZE,
        THREAD_POOL_MAX_SIZE, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL, WILD_CARD_ROUTING_ENABLED, MANAGEMENT_ADDRESS,
        MANAGEMENT_NOTIFICATION_ADDRESS, CLUSTER_USER, CLUSTER_PASSWORD, JMX_MANAGEMENT_ENABLED, JMX_DOMAIN, MESSAGE_COUNTER_ENABLED,
        MESSAGE_COUNTER_SAMPLE_PERIOD, MESSAGE_COUNTER_MAX_DAY_HISTORY, CONNECTION_TTL_OVERRIDE, ASYNC_CONNECTION_EXECUTION_ENABLED,
        TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_SCAN_PERIOD, MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY,
        ID_CACHE_SIZE, PERSIST_ID_CACHE, /* TODO REMOTING_INTERCEPTORS, */BACKUP, ALLOW_FAILBACK, FAILBACK_DELAY, FAILOVER_ON_SHUTDOWN,
        SHARED_STORE, PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY, CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE, JOURNAL_BUFFER_TIMEOUT,
        JOURNAL_BUFFER_SIZE, JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE, JOURNAL_FILE_SIZE,
        JOURNAL_MIN_FILES, JOURNAL_COMPACT_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_MAX_IO, PERF_BLAST_PAGES, RUN_SYNC_SPEED_TEST,
        SERVER_DUMP_INTERVAL, MEMORY_WARNING_THRESHOLD, MEMORY_MEASURE_INTERVAL
    };

    String[] COMPLEX_ROOT_RESOURCE_ATTRIBUTES =  {
        /*TODO remove */ REMOTING_INTERCEPTORS, LIVE_CONNECTOR_REF, /* TODO remove */ CONNECTORS, /* TODO remove */ ACCEPTORS,
        /* TODO remove */ BROADCAST_GROUPS, /* TODO remove */ DISCOVERY_GROUPS, /* TODO remove */ DIVERTS,
        /* TODO remove */ CORE_QUEUES, /* TODO remove */ BRIDGES, /* TODO remove */ CLUSTER_CONNECTIONS,
        /* TODO remove */ GROUPING_HANDLER, /* TODO remove */ PAGING_DIRECTORY, /* TODO remove */ BINDINGS_DIRECTORY,
        /* TODO remove */ JOURNAL_DIRECTORY, /* TODO remove */ LARGE_MESSAGES_DIRECTORY,
        /* TODO remove */ SECURITY_SETTINGS, /* TODO remove */ ADDRESS_SETTINGS, /* TODO remove */ CONNECTOR_SERVICES
    };

}
