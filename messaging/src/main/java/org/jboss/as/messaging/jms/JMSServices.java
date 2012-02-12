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
import static org.jboss.as.messaging.CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.CommonAttributes.COMPRESS_LARGE_MESSAGES;
import static org.jboss.as.messaging.CommonAttributes.CONFIRMATION_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
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
import static org.jboss.as.messaging.CommonAttributes.MAX_RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.MIN_LARGE_MESSAGE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.PCF_PASSWORD;
import static org.jboss.as.messaging.CommonAttributes.PCF_USER;
import static org.jboss.as.messaging.CommonAttributes.PRE_ACK;
import static org.jboss.as.messaging.CommonAttributes.PRODUCER_MAX_RATE;
import static org.jboss.as.messaging.CommonAttributes.PRODUCER_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY_RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.CommonAttributes.SETUP_ATTEMPTS;
import static org.jboss.as.messaging.CommonAttributes.SETUP_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_ATTRIBUTE;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_BATCH_SIZE;
import static org.jboss.as.messaging.CommonAttributes.USE_GLOBAL_POOLS;
import static org.jboss.as.messaging.CommonAttributes.USE_JNDI;
import static org.jboss.as.messaging.CommonAttributes.USE_LOCAL_TX;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
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

    static String AUTO_GROUP_METHOD = "autoGroup";
    static String BLOCK_ON_ACK_METHOD = "blockOnAcknowledge";
    static String BLOCK_ON_DURABLE_SEND_METHOD = "blockOnDurableSend";
    static String BLOCK_ON_NON_DURABLE_SEND_METHOD = "blockOnNonDurableSend";
    static String CACHE_LARGE_MESSAGE_CLIENT_METHOD = "cacheLargeMessageClient"; // TODO HornetQResourceAdapter does not have this method
    static String CALL_TIMEOUT_METHOD = "callTimeout";
    static String CLIENT_FAILURE_CHECK_PERIOD_METHOD = "clientFailureCheckPeriod";
    static String CLIENT_ID_METHOD = "clientID";
    static String CONFIRMATION_WINDOW_SIZE_METHOD = "confirmationWindowSize";
    static String CONNECTION_TTL_METHOD = "connectionTTL";
    static String CONSUMER_MAX_RATE_METHOD = "consumerMaxRate";
    static String CONSUMER_WINDOW_SIZE_METHOD = "consumerWindowSize";
    static String DISCOVERY_GROUP_NAME_METHOD = "discoveryGroupName";
    static String DISCOVERY_INITIAL_WAIT_TIMEOUT_METHOD = "discoveryInitialWaitTimeout";
    static String DUPS_OK_BATCH_SIZE_METHOD = "dupsOKBatchSize";
    static String FAILOVER_ON_INITIAL_CONNECTION_METHOD = "failoverOnInitialConnection";  // TODO HornetQResourceAdapter does not have this method
    static String FAILOVER_ON_SERVER_SHUTDOWN_METHOD = "failoverOnServerShutdown";  // TODO HornetQResourceAdapter does not have this method
    static String GROUP_ID_METHOD = "groupId";
    static String HA_METHOD = "hA"; // TODO HornetQResourceAdapter does not have this method
    static String LOAD_BALANCING_POLICY_CLASS_NAME_METHOD = "loadBalancingPolicyClassName";  // TODO HornetQResourceAdapter does not have this method
    static String MAX_RETRY_INTERVAL_METHOD = "maxRetryInterval";    // TODO HornetQResourceAdapter does not have this method
    static String MIN_LARGE_MESSAGE_SIZE_METHOD = "minLargeMessageSize";
    static String PASSWORD_METHOD = "password";
    static String PRE_ACK_METHOD = "preAcknowledge";
    static String PRODUCER_MAX_RATE_METHOD = "producerMaxRate";
    static String PRODUCER_WINDOW_SIZE_METHOD = "producerWindowSize"; // TODO HornetQResourceAdapter does not have this method
    static String RECONNECT_ATTEMPTS_METHOD = "reconnectAttempts";
    static String RETRY_INTERVAL_METHOD = "retryInterval";
    static String RETRY_INTERVAL_MULTIPLIER_METHOD = "retryIntervalMultiplier";
    static String SCHEDULED_THREAD_POOL_MAX_SIZE_METHOD = "scheduledThreadPoolMaxSize";
    static String THREAD_POOL_MAX_SIZE_METHOD = "threadPoolMaxSize";
    static String TRANSACTION_BATCH_SIZE_METHOD = "transactionBatchSize";
    static String USE_GLOBAL_POOLS_METHOD = "useGlobalPools";
    static String USE_JNDI_METHOD = "useJNDI";
    static String USERNAME_METHOD= "userName";
    static String JNDI_PARAMS_METHOD = "jndiParams";
    static String USE_LOCAL_TX_METHOD = "useLocalTx";
    static String SETUP_ATTEMPTS_METHOD = "setupAttempts";
    static String SETUP_INTERVAL_METHOD = "setupInterval";

    public static AttributeDefinition[] CONNECTION_FACTORY_ATTRS = new AttributeDefinition[] {
        //Do these 2 most frequently used ones out of alphabetical order
        new SimpleAttributeDefinition(CONNECTOR, ModelType.OBJECT, true),   //<------
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
        DISCOVERY_INITIAL_WAIT_TIMEOUT, // TODO not used in ConnectionFactoryConfiguration
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

    public static AttributeDefinition[] POOLED_CONNECTION_FACTORY_ATTRS = new AttributeDefinition[] {
        //Do these 2 most frequently used ones out of alphabetical order
        new SimpleAttributeDefinition(CONNECTOR, ModelType.OBJECT, true),   //<------
        JndiEntriesAttribute.CONNECTION_FACTORY,

        AUTO_GROUP,
        BLOCK_ON_ACK,
        BLOCK_ON_DURABLE_SEND,
        BLOCK_ON_NON_DURABLE_SEND,
        CACHE_LARGE_MESSAGE_CLIENT,   // TODO HornetQResourceAdapter does not have this method
        CALL_TIMEOUT,
        CLIENT_FAILURE_CHECK_PERIOD,   // TODO HornetQResourceAdapter does not have this method
        CLIENT_ID,
        CONFIRMATION_WINDOW_SIZE,
        CONNECTION_TTL,
        CONSUMER_MAX_RATE,
        CONSUMER_WINDOW_SIZE,
        DISCOVERY_GROUP_NAME,
        DISCOVERY_INITIAL_WAIT_TIMEOUT,
        DUPS_OK_BATCH_SIZE,
        FAILOVER_ON_INITIAL_CONNECTION,  // TODO HornetQResourceAdapter does not have this method
        FAILOVER_ON_SERVER_SHUTDOWN,   // TODO HornetQResourceAdapter does not have this method
        GROUP_ID,
        HA,  // TODO HornetQResourceAdapter does not have this method
        LOAD_BALANCING_CLASS_NAME,  // TODO HornetQResourceAdapter does not have this method
        MAX_RETRY_INTERVAL,          // TODO HornetQResourceAdapter does not have this method
        MIN_LARGE_MESSAGE_SIZE,
        PCF_PASSWORD,
        PRE_ACK,
        PRODUCER_MAX_RATE,
        PRODUCER_WINDOW_SIZE,     // TODO HornetQResourceAdapter does not have this method
        CONNECTION_FACTORY_RECONNECT_ATTEMPTS,
        RETRY_INTERVAL,
        RETRY_INTERVAL_MULTIPLIER,
        CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE,
        CONNECTION_THREAD_POOL_MAX_SIZE,
        TRANSACTION_BATCH_SIZE,
        USE_GLOBAL_POOLS,
        USE_JNDI,
        PCF_USER,
        JNDI_PARAMS,
        USE_LOCAL_TX,
        SETUP_ATTEMPTS,
        SETUP_INTERVAL,
        TRANSACTION_ATTRIBUTE
    };

    static PooledCFAttribute[] POOLED_CONNECTION_FACTORY_METHOD_ATTRS = new PooledCFAttribute[] {
        new PooledCFAttribute(AUTO_GROUP, AUTO_GROUP_METHOD),
        new PooledCFAttribute(BLOCK_ON_ACK, BLOCK_ON_ACK_METHOD),
        new PooledCFAttribute(BLOCK_ON_DURABLE_SEND, BLOCK_ON_DURABLE_SEND_METHOD),
        new PooledCFAttribute(BLOCK_ON_NON_DURABLE_SEND, BLOCK_ON_NON_DURABLE_SEND_METHOD),
        // TODO HornetQResourceAdapter does not have this method
        //new PooledCFAttribute(CACHE_LARGE_MESSAGE_CLIENT, CACHE_LARGE_MESSAGE_CLIENT_METHOD),
        new PooledCFAttribute(CALL_TIMEOUT, CALL_TIMEOUT_METHOD),
        // TODO HornetQResourceAdapter does not have this method
        //new PooledCFAttribute(CLIENT_FAILURE_CHECK_PERIOD, CLIENT_FAILURE_CHECK_PERIOD_METHOD),
        new PooledCFAttribute(CLIENT_ID, CLIENT_ID_METHOD),
        new PooledCFAttribute(CONFIRMATION_WINDOW_SIZE, CONFIRMATION_WINDOW_SIZE_METHOD),
        new PooledCFAttribute(CONNECTION_TTL, CONNECTION_TTL_METHOD),
        new PooledCFAttribute(CONSUMER_MAX_RATE, CONSUMER_MAX_RATE_METHOD),
        new PooledCFAttribute(CONSUMER_WINDOW_SIZE, CONSUMER_WINDOW_SIZE_METHOD),
        new PooledCFAttribute(DISCOVERY_GROUP_NAME, DISCOVERY_GROUP_NAME_METHOD),
        new PooledCFAttribute(DISCOVERY_INITIAL_WAIT_TIMEOUT, DISCOVERY_INITIAL_WAIT_TIMEOUT_METHOD),
        new PooledCFAttribute(DUPS_OK_BATCH_SIZE, DUPS_OK_BATCH_SIZE_METHOD),
        // TODO HornetQResourceAdapter does not have this method
        //new PooledCFAttribute(FAILOVER_ON_INITIAL_CONNECTION, FAILOVER_ON_INITIAL_CONNECTION_METHOD),
        // TODO HornetQResourceAdapter does not have this method
        // new PooledCFAttribute(FAILOVER_ON_SERVER_SHUTDOWN, FAILOVER_ON_SERVER_SHUTDOWN_METHOD),
        new PooledCFAttribute(GROUP_ID, GROUP_ID_METHOD),
        // TODO HornetQResourceAdapter does not have these three methods
        // new PooledCFAttribute(HA, HA_METHOD),
        // new PooledCFAttribute(LOAD_BALANCING_CLASS_NAME, LOAD_BALANCING_POLICY_CLASS_NAME_METHOD),
        //new PooledCFAttribute(MAX_RETRY_INTERVAL, MAX_RETRY_INTERVAL_METHOD),
        new PooledCFAttribute(MIN_LARGE_MESSAGE_SIZE, MIN_LARGE_MESSAGE_SIZE_METHOD),
        new PooledCFAttribute(PCF_PASSWORD, PASSWORD_METHOD),
        new PooledCFAttribute(PRE_ACK, PRE_ACK_METHOD),
        new PooledCFAttribute(PRODUCER_MAX_RATE, PRODUCER_MAX_RATE_METHOD),
        // TODO HornetQResourceAdapter does not have this method
        //new PooledCFAttribute(PRODUCER_WINDOW_SIZE, PRODUCER_WINDOW_SIZE_METHOD),
        new PooledCFAttribute(CONNECTION_FACTORY_RECONNECT_ATTEMPTS, RECONNECT_ATTEMPTS_METHOD),
        new PooledCFAttribute(RETRY_INTERVAL, RETRY_INTERVAL_METHOD),
        new PooledCFAttribute(RETRY_INTERVAL_MULTIPLIER, RETRY_INTERVAL_MULTIPLIER_METHOD),
        new PooledCFAttribute(CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE, SCHEDULED_THREAD_POOL_MAX_SIZE_METHOD),
        new PooledCFAttribute(CONNECTION_THREAD_POOL_MAX_SIZE, THREAD_POOL_MAX_SIZE_METHOD),
        new PooledCFAttribute(TRANSACTION_BATCH_SIZE, TRANSACTION_BATCH_SIZE_METHOD),
        new PooledCFAttribute(USE_GLOBAL_POOLS, USE_GLOBAL_POOLS_METHOD),
        new PooledCFAttribute(USE_JNDI, USE_JNDI_METHOD),
        new PooledCFAttribute(PCF_USER, USERNAME_METHOD),
        new PooledCFAttribute(JNDI_PARAMS, JNDI_PARAMS_METHOD),
        new PooledCFAttribute(USE_LOCAL_TX, USE_LOCAL_TX_METHOD),
        new PooledCFAttribute(SETUP_ATTEMPTS, SETUP_ATTEMPTS_METHOD),
        new PooledCFAttribute(SETUP_INTERVAL, SETUP_INTERVAL_METHOD)
    };

    static class PooledCFAttribute {
        private final AttributeDefinition def;
        private String methodName;

        public PooledCFAttribute(final AttributeDefinition def, final String methodName) {
            this.def = def;
            this.methodName = methodName;
        }

        public String getName() {
            return def.getName();
        }

        public String getClassType() {
            switch (def.getType()) {
                case BOOLEAN:
                    return Boolean.class.getName();
                case BIG_DECIMAL:
                    return Double.class.getName();
                case LONG:
                    return Long.class.getName();
                case INT:
                    return Integer.class.getName();
                case STRING:
                    return String.class.getName();
                default:
                    throw MESSAGES.invalidAttributeType(def.getName(), def.getType());

            }
        }

        public String getMethodName() {
            return methodName;
        }

        public AttributeDefinition getDefinition() {
            return def;
        }
    }
}
