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

import static org.jboss.as.messaging.CommonAttributes.AUTO_GROUP;
import static org.jboss.as.messaging.CommonAttributes.BLOCK_ON_ACK;
import static org.jboss.as.messaging.CommonAttributes.BLOCK_ON_DURABLE_SEND;
import static org.jboss.as.messaging.CommonAttributes.BLOCK_ON_NON_DURABLE_SEND;
import static org.jboss.as.messaging.CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT;
import static org.jboss.as.messaging.CommonAttributes.CALL_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CF_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.CommonAttributes.COMPRESS_LARGE_MESSAGES;
import static org.jboss.as.messaging.CommonAttributes.CONFIRMATION_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY_RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL;
import static org.jboss.as.messaging.CommonAttributes.CONSUMER_MAX_RATE;
import static org.jboss.as.messaging.CommonAttributes.CONSUMER_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_INITIAL_WAIT_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.DUPS_OK_BATCH_SIZE;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN;
import static org.jboss.as.messaging.CommonAttributes.GROUP_ID;
import static org.jboss.as.messaging.CommonAttributes.HA;
import static org.jboss.as.messaging.CommonAttributes.JNDI_PARAMS;
import static org.jboss.as.messaging.CommonAttributes.LOAD_BALANCING_CLASS_NAME;
import static org.jboss.as.messaging.CommonAttributes.MAX_POOL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.MAX_RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.MIN_LARGE_MESSAGE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.MIN_POOL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.PCF_PASSWORD;
import static org.jboss.as.messaging.CommonAttributes.PCF_USER;
import static org.jboss.as.messaging.CommonAttributes.PRE_ACK;
import static org.jboss.as.messaging.CommonAttributes.PRODUCER_MAX_RATE;
import static org.jboss.as.messaging.CommonAttributes.PRODUCER_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.CommonAttributes.SETUP_ATTEMPTS;
import static org.jboss.as.messaging.CommonAttributes.SETUP_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_ATTRIBUTE;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_BATCH_SIZE;
import static org.jboss.as.messaging.CommonAttributes.USE_AUTO_RECOVERY;
import static org.jboss.as.messaging.CommonAttributes.USE_GLOBAL_POOLS;
import static org.jboss.as.messaging.CommonAttributes.USE_JNDI;
import static org.jboss.as.messaging.CommonAttributes.USE_LOCAL_TX;
import static org.jboss.as.messaging.jms.PooledConnectionFactoryAttribute.create;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc
 */
public class JMSServices {

    private static final String JMS = "jms";
    private static final String JMS_MANAGER = "manager";
    private static final String JMS_QUEUE_BASE = "queue";
    private static final String JMS_TOPIC_BASE = "topic";
    private static final String JMS_CF_BASE = "connection-factory";
    public static final String JMS_POOLED_CF_BASE = "pooled-connection-factory";

    public static ServiceName getJmsManagerBaseServiceName(ServiceName hornetQServiceName) {
        return hornetQServiceName.append(JMS).append(JMS_MANAGER);
    }

    public static ServiceName getJmsQueueBaseServiceName(ServiceName hornetQServiceName) {
        return hornetQServiceName.append(JMS).append(JMS_QUEUE_BASE);
    }

    public static ServiceName getJmsTopicBaseServiceName(ServiceName hornetqServiceName) {
        return hornetqServiceName.append(JMS).append(JMS_TOPIC_BASE);
    }

    public static ServiceName getConnectionFactoryBaseServiceName(ServiceName hornetqServiceName) {
        return hornetqServiceName.append(JMS).append(JMS_CF_BASE);
    }

    public static ServiceName getPooledConnectionFactoryBaseServiceName(ServiceName hornetqServiceName) {
        return hornetqServiceName.append(JMS).append(JMS_POOLED_CF_BASE);
    }

    static String RECONNECT_ATTEMPTS_PROP_NAME = "reconnectAttempts";
    public static String USE_JNDI_PROP_NAME = "useJNDI";
    public static final String SETUP_ATTEMPTS_PROP_NAME = "setupAttempts";
    public static final String SETUP_INTERVAL_PROP_NAME = "setupInterval";

    public static AttributeDefinition[] CONNECTION_FACTORY_ATTRS = new AttributeDefinition[] {
        //Do these 2 most frequently used ones out of alphabetical order
        CF_CONNECTOR,
        JndiEntriesAttribute.DESTINATION,

        AUTO_GROUP,
        BLOCK_ON_ACK,
        BLOCK_ON_DURABLE_SEND,
        BLOCK_ON_NON_DURABLE_SEND,
        CACHE_LARGE_MESSAGE_CLIENT,
        CALL_TIMEOUT,
        CLIENT_FAILURE_CHECK_PERIOD,
        CLIENT_ID,
        COMPRESS_LARGE_MESSAGES,
        CONFIRMATION_WINDOW_SIZE,
        CONNECTION_TTL,
        CONSUMER_MAX_RATE,
        CONSUMER_WINDOW_SIZE,
        DISCOVERY_GROUP_NAME,
        DISCOVERY_INITIAL_WAIT_TIMEOUT, // Not used since messaging 1.2, we keep it for compatibility sake
        DUPS_OK_BATCH_SIZE,
        FAILOVER_ON_INITIAL_CONNECTION,
        FAILOVER_ON_SERVER_SHUTDOWN, // TODO not used in ConnectionFactoryConfiguration
        GROUP_ID,
        HA,
        LOAD_BALANCING_CLASS_NAME,
        MAX_RETRY_INTERVAL,
        MIN_LARGE_MESSAGE_SIZE,
        PRE_ACK,
        PRODUCER_MAX_RATE,
        PRODUCER_WINDOW_SIZE,
        CONNECTION_FACTORY_RECONNECT_ATTEMPTS,
        RETRY_INTERVAL,
        RETRY_INTERVAL_MULTIPLIER,
        CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE,
        CONNECTION_THREAD_POOL_MAX_SIZE,
        TRANSACTION_BATCH_SIZE,
        USE_GLOBAL_POOLS
    };

    /** Connection factory config attributes that can be written at runtime */
    public static AttributeDefinition[] CONNECTION_FACTORY_WRITE_ATTRS = new AttributeDefinition[] {
        CLIENT_ID,
        COMPRESS_LARGE_MESSAGES,
        CLIENT_FAILURE_CHECK_PERIOD,
        CALL_TIMEOUT,
        DUPS_OK_BATCH_SIZE,
        CONSUMER_MAX_RATE,
        CONSUMER_WINDOW_SIZE,
        PRODUCER_MAX_RATE,
        CONFIRMATION_WINDOW_SIZE,
        BLOCK_ON_ACK,
        BLOCK_ON_DURABLE_SEND,
        BLOCK_ON_NON_DURABLE_SEND,
        PRE_ACK,
        CONNECTION_TTL,
        TRANSACTION_BATCH_SIZE,
        MIN_LARGE_MESSAGE_SIZE,
        AUTO_GROUP,
        RETRY_INTERVAL,
        RETRY_INTERVAL_MULTIPLIER,
        CONNECTION_FACTORY_RECONNECT_ATTEMPTS,
        FAILOVER_ON_INITIAL_CONNECTION,
        PRODUCER_WINDOW_SIZE,
        CACHE_LARGE_MESSAGE_CLIENT,
        MAX_RETRY_INTERVAL,
        CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE,
        CONNECTION_THREAD_POOL_MAX_SIZE,
        GROUP_ID,
        USE_GLOBAL_POOLS,
        LOAD_BALANCING_CLASS_NAME
    };

    public static PooledConnectionFactoryAttribute[] POOLED_CONNECTION_FACTORY_ATTRS = new PooledConnectionFactoryAttribute[] {
        create(CF_CONNECTOR, null, false),
        create(JndiEntriesAttribute.CONNECTION_FACTORY, null, false),

        create(AUTO_GROUP, "autoGroup", true),
        create(BLOCK_ON_ACK, "blockOnAcknowledge", true),
        create(BLOCK_ON_DURABLE_SEND, "blockOnDurableSend", true),
        create(BLOCK_ON_NON_DURABLE_SEND, "blockOnNonDurableSend", true),
        create(CACHE_LARGE_MESSAGE_CLIENT, "cacheLargeMessageClient", false), // FIXME HORNETQ-948
        create(CALL_TIMEOUT, "callTimeout", true),
        create(CLIENT_FAILURE_CHECK_PERIOD, "clientFailureCheckPeriod", true),
        create(CLIENT_ID, "clientID", true),
        create(CONFIRMATION_WINDOW_SIZE, "confirmationWindowSize", true),
        create(CONNECTION_TTL, "connectionTTL", true),
        create(CONSUMER_MAX_RATE, "consumerMaxRate", true),
        create(CONSUMER_WINDOW_SIZE, "consumerWindowSize", true),
        create(DISCOVERY_GROUP_NAME, null, false),
        create(DISCOVERY_INITIAL_WAIT_TIMEOUT, null, false), // Not used since messaging 1.2, we keep it for compatibility sake
        create(DUPS_OK_BATCH_SIZE, "dupsOKBatchSize", true),
        create(FAILOVER_ON_INITIAL_CONNECTION, "failoverOnInitialConnection", false), // FIXME HORNETQ-948
        create(FAILOVER_ON_SERVER_SHUTDOWN, "failoverOnServerShutdown", false), // TODO HornetQResourceAdapter does not have this method
        create(GROUP_ID, "groupID", false), // FIXME HORNETQ-948
        create(HA, "HA", true),
        create(LOAD_BALANCING_CLASS_NAME, "connectionLoadBalancingPolicyClassName", true),
        create(MAX_RETRY_INTERVAL, "maxRetryInterval", false), // FIXME HORNETQ-948
        create(MIN_LARGE_MESSAGE_SIZE, "minLargeMessageSize", true),
        create(PCF_PASSWORD, "password", true),
        create(PRE_ACK, "preAcknowledge", true),
        create(PRODUCER_MAX_RATE, "producerMaxRate", true),
        create(PRODUCER_WINDOW_SIZE, "producerWindowSize", false),  // FIXME HORNETQ-948
        create(CONNECTION_FACTORY_RECONNECT_ATTEMPTS, RECONNECT_ATTEMPTS_PROP_NAME, true),
        create(RETRY_INTERVAL, "retryInterval", true),
        create(RETRY_INTERVAL_MULTIPLIER, "retryIntervalMultiplier", true),
        create(CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE, "scheduledThreadPoolMaxSize", true),
        create(CONNECTION_THREAD_POOL_MAX_SIZE, "threadPoolMaxSize", true),
        create(TRANSACTION_BATCH_SIZE, "transactionBatchSize", true),
        create(USE_AUTO_RECOVERY, "useAutoRecovery", true),
        create(USE_GLOBAL_POOLS, "useGlobalPools", true),
        create(USE_JNDI, USE_JNDI_PROP_NAME, true),
        create(PCF_USER, "userName", true),
        create(JNDI_PARAMS, "jndiParams", true),
        create(USE_LOCAL_TX, "useLocalTx", true),
        create(SETUP_ATTEMPTS, SETUP_ATTEMPTS_PROP_NAME, true),
        create(SETUP_INTERVAL, SETUP_INTERVAL_PROP_NAME, true),
        create(TRANSACTION_ATTRIBUTE, null, false),
        create(MIN_POOL_SIZE, null, false),
        create(MAX_POOL_SIZE, null, false),
    };
}
