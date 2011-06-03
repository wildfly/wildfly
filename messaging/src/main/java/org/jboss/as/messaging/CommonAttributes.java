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

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public interface CommonAttributes {

    String ACCEPTOR ="acceptor";
    String ACCEPTORS ="acceptors";
    String ADDRESS ="address";
    String ADDRESS_FULL_MESSAGE_POLICY ="address-full-policy";
    String ADDRESS_SETTING ="address-setting";
    String ADDRESS_SETTINGS ="address-settings";
    String ASYNC_CONNECTION_EXECUTION_ENABLED ="async-connection-execution-enabled";
    String BACKUP ="backup";
    String LIVE_CONNECTOR_REF ="live-connector-ref";
    String BINDINGS_DIRECTORY ="bindings-directory";
    String BROADCAST_PERIOD ="broadcast-period";
    String CLUSTERED ="clustered";
    String CLUSTER_PASSWORD ="cluster-password";
    String CLUSTER_USER ="cluster-user";
    String CONNECTION_TTL_OVERRIDE ="connection-ttl-override";
    String CONNECTOR ="connector";
    String CONNECTORS ="connectors";
    String CONNECTOR_REF ="connector-ref";
    String CONSUME_NAME ="consume";
    String CORE_QUEUES ="core-queues";
    String CREATEDURABLEQUEUE_NAME ="createDurableQueue";
    String CREATETEMPQUEUE_NAME ="createTempQueue";
    String CREATE_BINDINGS_DIR ="create-bindings-dir";
    String CREATE_JOURNAL_DIR ="create-journal-dir";
    String CREATE_NON_DURABLE_QUEUE_NAME ="createNonDurableQueue";
    String DEAD_LETTER_ADDRESS ="dead-letter-address";
    String DELETEDURABLEQUEUE_NAME ="deleteDurableQueue";
    String DELETETEMPQUEUE_NAME ="deleteTempQueue";
    String DELETE_NON_DURABLE_QUEUE_NAME ="deleteNonDurableQueue";
    String DURABLE ="durable";
    String EXPIRY_ADDRESS ="expiry-address";
    String FACTORY_CLASS ="factory-class";
    String FILE_DEPLOYMENT_ENABLED ="file-deployment-enabled";
    String FILTER ="filter";
    String GROUPING_HANDLER ="grouping-handler";
    String GROUP_ADDRESS ="group-address";
    String GROUP_PORT ="group-port";
    String ID_CACHE_SIZE ="id-cache-size";
    String IN_VM_ACCEPTOR ="in-vm-acceptor";
    String IN_VM_CONNECTOR ="in-vm-connector";
    String JMX_DOMAIN ="jmx-domain";
    String JMX_MANAGEMENT_ENABLED ="jmx-management-enabled";
    String JOURNAL_BUFFER_SIZE ="journal-buffer-size";
    String JOURNAL_BUFFER_TIMEOUT ="journal-buffer-timeout";
    String JOURNAL_COMPACT_MIN_FILES ="journal-compact-min-files";
    String JOURNAL_COMPACT_PERCENTAGE ="journal-compact-percentage";
    String JOURNAL_DIRECTORY ="journal-directory";
    String JOURNAL_FILE_SIZE ="journal-file-size";
    String JOURNAL_MAX_IO ="journal-max-io";
    String JOURNAL_MIN_FILES ="journal-min-files";
    String JOURNAL_SYNC_NON_TRANSACTIONAL ="journal-sync-non-transactional";
    String JOURNAL_SYNC_TRANSACTIONAL ="journal-sync-transactional";
    String JOURNAL_TYPE ="journal-type";
    String KEY ="key";
    String LARGE_MESSAGES_DIRECTORY ="large-messages-directory";
    String LOCAL_BIND_ADDRESS ="local-bind-address";
    String LOCAL_BIND_PORT ="local-bind-port";
    String LOG_JOURNAL_WRITE_RATE ="log-journal-write-rate";
    String LVQ ="last-value-queue";
    String MANAGEMENT_ADDRESS ="management-address";
    String MANAGEMENT_NOTIFICATION_ADDRESS ="management-notification-address";
    String MANAGE_NAME ="manage";
    String MATCH ="match";
    String MAX_DELIVERY_ATTEMPTS ="max-delivery-attempts";
    String MAX_SIZE_BYTES_NODE_NAME ="max-size-bytes";
    String MEMORY_MEASURE_INTERVAL ="memory-measure-interval";
    String MEMORY_WARNING_THRESHOLD ="memory-warning-threshold";
    String MESSAGE_COUNTER_ENABLED ="message-counter-enabled";
    String MESSAGE_COUNTER_HISTORY_DAY_LIMIT ="message-counter-history-day-limit";
    String MESSAGE_COUNTER_MAX_DAY_HISTORY ="message-counter-max-day-history";
    String MESSAGE_COUNTER_SAMPLE_PERIOD ="message-counter-sample-period";
    String MESSAGE_EXPIRY_SCAN_PERIOD ="message-expiry-scan-period";
    String MESSAGE_EXPIRY_THREAD_PRIORITY ="message-expiry-thread-priority";
    String NAME ="name";
    String NETTY_ACCEPTOR ="netty-acceptor";
    String NETTY_CONNECTOR ="netty-connector";
    String PAGE_SIZE_BYTES_NODE_NAME ="page-size-bytes";
    String PAGING_DIRECTORY ="paging-directory";
    String PARAM ="param";
    String PATH ="path";
    String PERF_BLAST_PAGES ="perf-blast-pages";
    String PERMISSION_ELEMENT_NAME ="permission";
    String PERSISTENCE_ENABLED ="persistence-enabled";
    String PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY ="persist-delivery-count-before-delivery";
    String PERSIST_ID_CACHE ="persist-id-cache";
    String ROLE = "role";
    String QUEUE ="queue";
    String QUEUE_ADDRESS ="queue-address";
    String REDELIVERY_DELAY ="redelivery-delay";
    String REDISTRIBUTION_DELAY ="redistribution-delay";
    String REFRESH_TIMEOUT ="refresh-timeout";
    String RELATIVE_TO ="relative-to";
    String REMOTING_INTERCEPTORS ="remoting-interceptors";
    String ROLES_ATTR_NAME ="roles";
    String RUN_SYNC_SPEED_TEST ="run-sync-speed-test";
    String SECURITY_ENABLED ="security-enabled";
    String SECURITY_INVALIDATION_INTERVAL ="security-invalidation-interval";
    String SECURITY_SETTING ="security-setting";
    String SECURITY_SETTINGS ="security-settings";
    String SEND_NAME ="send";
    String SEND_TO_DLA_ON_NO_ROUTE ="send-to-dla-on-no-route";
    String SERVER_DUMP_INTERVAL ="server-dump-interval";
    String SERVER_ID ="server-id";
    String SHARED_STORE ="shared-store";
    String SOCKET_BINDING ="socket-binding";
    String STRING ="string";
    String SUBSYSTEM ="subsystem";
    String TRANSACTION_TIMEOUT ="transaction-timeout";
    String TRANSACTION_TIMEOUT_SCAN_PERIOD ="transaction-timeout-scan-period";
    String TYPE_ATTR_NAME ="type";
    String VALUE ="value";
    String WILD_CARD_ROUTING_ENABLED ="wild-card-routing-enabled";

   //JMS Stuff

    String HA ="ha";
    String AUTO_GROUP ="auto-group";
    String BLOCK_ON_ACK ="block-on-acknowledge";
    String BLOCK_ON_DURABLE_SEND ="block-on-durable-send";
    String BLOCK_ON_NON_DURABLE_SEND ="block-on-non-durable-send";
    String CACHE_LARGE_MESSAGE_CLIENT ="cache-large-message-client";
    String CALL_TIMEOUT ="call-timeout";
    String CLIENT_FAILURE_CHECK_PERIOD ="client-failure-check-period";
    String CLIENT_ID ="client-id";
    String CONFIRMATION_WINDOW_SIZE ="confirmation-window-size";
    String CONNECTION_FACTORY ="connection-factory";
    String CONNECTION_FACTORIES ="jms-connection-factories";
    String CONNECTION_TTL ="connection-ttl";
    String CONNECTOR_NAME ="connector-name";
    String CONSUMER_MAX_RATE ="consumer-max-rate";
    String CONSUMER_WINDOW_SIZE ="consumer-window-size";
    String DISCOVERY_GROUP_NAME ="discovery-group-name";
    String DISCOVERY_GROUP_REF ="discovery-group-ref";
    String DISCOVERY_INITIAL_WAIT_TIMEOUT ="discovery-initial-wait-timeout";
    String DUPS_OK_BATCH_SIZE ="dups-ok-batch-size";
    String ENTRIES ="entries";
    String ENTRY ="entry";
    String FAILOVER_ON_INITIAL_CONNECTION ="failover-on-initial-connection";
    String FAILOVER_ON_SERVER_SHUTDOWN ="failover-on-server-shutdown";
    String GROUP_ID ="group-id";
    String JMS_DESTINATIONS = "jms-destinations";
    String JMS_TOPIC ="jms-topic";
    String JMS_QUEUE ="jms-queue";
    String LOAD_BALANCING_CLASS_NAME ="connection-load-balancing-policy-class-name";
    String MAX_RETRY_INTERVAL ="max-retry-interval";
    String MIN_LARGE_MESSAGE_SIZE ="min-large-message-size";
    String PRE_ACK ="pre-acknowledge";
    String PRODUCER_MAX_RATE ="producer-max-rate";
    String PRODUCER_WINDOW_SIZE ="producer-window-size";
    String RECONNECT_ATTEMPTS ="reconnect-attempts";
    String RETRY_INTERVAL ="retry-interval";
    String RETRY_INTERVAL_MULTIPLIER ="retry-interval-multiplier";
    String SCHEDULED_THREAD_POOL_MAX_SIZE ="scheduled-thread-pool-max-size";
    String SELECTOR ="selector";
    String THREAD_POOL_MAX_SIZE ="thread-pool-max-size";
    String TRANSACTION_BATCH_SIZE ="transaction-batch-size";
    String USE_GLOBAL_POOLS ="use-global-pools";
    String POOLED_CONNECTION_FACTORY = "pooled-connection-factory";
    String INBOUND_CONFIG = "inbound-config";
    String USE_JNDI = "use-jndi";
    String JNDI_PARAMS = "jndi-params";
    String USE_LOCAL_TX = "use-local-tx";
    String SETUP_ATTEMPTS = "setup-attempts";
    String SETUP_INTERVAL = "setup-interval";
    String TRANSACTION = "transaction";
    String LOCAL = "local";
    String XA = "xa";
    String NONE = "none";
    String MODE = "mode";
    String LOCAL_TX = "LocalTransaction";
    String XA_TX = "XATransaction";
    String NO_TX = "NoTransaction";

}
