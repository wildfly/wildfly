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

package org.jboss.as.messaging.jms;

/**
 * @author Emanuel Muckenhuber
 */
public interface CommonAttributes {

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
    String CONNECTION_TTL ="connection-ttl";
    String CONNECTOR = "connector";
    String CONNECTORS ="connectors";
    String CONNECTOR_BACKUP_NAME ="backup-connector-name";
    String CONNECTOR_NAME ="connector-name";
    String CONNECTOR_REF ="connector-ref";
    String CONSUMER_MAX_RATE ="consumer-max-rate";
    String CONSUMER_WINDOW_SIZE ="consumer-window-size";
    String DISCOVERY_GROUP_NAME ="discovery-group-name";
    String DISCOVERY_GROUP_REF ="discovery-group-ref";
    String DISCOVERY_INITIAL_WAIT_TIMEOUT ="discovery-initial-wait-timeout";
    String DUPS_OK_BATCH_SIZE ="dups-ok-batch-size";
    String DURABLE ="durable";
    String ENTRIES ="entries";
    String ENTRY ="entry";
    String FAILOVER_ON_INITIAL_CONNECTION ="failover-on-initial-connection";
    String FAILOVER_ON_SERVER_SHUTDOWN ="failover-on-server-shutdown";
    String GROUP_ID ="group-id";
    String LOAD_BALANCING_CLASS_NAME ="connection-load-balancing-policy-class-name";
    String MAX_RETRY_INTERVAL ="max-retry-interval";
    String MIN_LARGE_MESSAGE_SIZE ="min-large-message-size";
    String NAME ="name";
    String PRE_ACK ="pre-acknowledge";
    String PRODUCER_MAX_RATE ="producer-max-rate";
    String PRODUCER_WINDOW_SIZE ="producer-window-size";
    String QUEUE ="queue";
    String RECONNECT_ATTEMPTS ="reconnect-attempts";
    String RETRY_INTERVAL ="retry-interval";
    String RETRY_INTERVAL_MULTIPLIER ="retry-interval-multiplier";
    String SCHEDULED_THREAD_POOL_MAX_SIZE ="scheduled-thread-pool-max-size";
    String SELECTOR ="selector";
    String THREAD_POOL_MAX_SIZE ="thread-pool-max-size";
    String TOPIC ="topic";
    String TRANSACTION_BATCH_SIZE ="transaction-batch-size";
    String USE_GLOBAL_POOLS ="use-global-pools";

}
