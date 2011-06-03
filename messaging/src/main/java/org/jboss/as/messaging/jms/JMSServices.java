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

import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.messaging.CommonAttributes.*;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class JMSServices {

    public static final ServiceName JMS = MessagingServices.JBOSS_MESSAGING.append("jms");
    public static final ServiceName JMS_MANAGER = JMS.append("manager");
    public static final ServiceName JMS_QUEUE_BASE = JMS.append("queue");
    public static final ServiceName JMS_TOPIC_BASE = JMS.append("topic");
    public static final ServiceName JMS_CF_BASE = JMS.append("connection-factory");

    static String AUTO_GROUP_METHOD = "autoGroup";
    static String BLOCK_ON_ACK_METHOD = "blockOnAcknowledge";
    static String BLOCK_ON_DURABLE_SEND_METHOD = "blockOnDurableSend";
    static String BLOCK_ON_NON_DURABLE_SEND_METHOD = "blockOnNonDurableSend";
    static String CACHE_LARGE_MESSAGE_CLIENT_METHOD = "cacheLargeMessageClient";
    static String CALL_TIMEOUT_METHOD = "callTimeout";
    static String CLIENT_FAILURE_CHECK_PERIOD_METHOD = "clientFailureCheckPeriod";
    static String CLIENT_ID_METHOD = "clientId";
    static String CONFIRMATION_WINDOW_SIZE_METHOD = "confirmationWindowSize";
    static String CONNECTION_TTL_METHOD = "connectionTtl";
    static String CONSUMER_MAX_RATE_METHOD = "consumerMaxRate";
    static String CONSUMER_WINDOW_SIZE_METHOD = "consumerWindowSize";
    static String DISCOVERY_GROUP_NAME_METHOD = "discoveryGroupName";
    static String DISCOVERY_INITIAL_WAIT_TIMEOUT_METHOD = "discoveryInitialWaitTimeout";
    static String DUPS_OK_BATCH_SIZE_METHOD = "dupsOkBatchSize";
    static String FAILOVER_ON_INITIAL_CONNECTION_METHOD = "failoverOnInitialConnection";
    static String FAILOVER_ON_SERVER_SHUTDOWN_METHOD = "failoverOnServerShutdown";
    static String GROUP_ID_METHOD = "groupId";
    static String MAX_RETRY_INTERVAL_METHOD = "maxRetryInterval";
    static String MIN_LARGE_MESSAGE_SIZE_METHOD = "minLargeMessageSize";
    static String PRE_ACK_METHOD = "preAcknowledge";
    static String PRODUCER_MAX_RATE_METHOD = "producerMaxRate";
    static String PRODUCER_WINDOW_SIZE_METHOD = "producerWindowSize";
    static String RECONNECT_ATTEMPTS_METHOD = "reconnectAttempts";
    static String RETRY_INTERVAL_METHOD = "retryInterval";
    static String RETRY_INTERVAL_MULTIPLIER_METHOD = "retryIntervalMultiplier";
    static String SCHEDULED_THREAD_POOL_MAX_SIZE_METHOD = "scheduledThreadPoolMaxSize";
    static String THREAD_POOL_MAX_SIZE_METHOD = "threadPoolMaxSize";
    static String TRANSACTION_BATCH_SIZE_METHOD = "transactionBatchSize";
    static String USE_GLOBAL_POOLS_METHOD = "useGlobalPools";
    static String USE_JNDI_METHOD = "useJNDI";
    static String JNDI_PARAMS_METHOD = "jndiParams";
    static String USE_LOCAL_TX_METHOD = "useLocalTx";
    static String SETUP_ATTEMPTS_METHOD = "setupAttempts";
    static String SETUP_INTERVAL_METHOD = "setupInterval";

    public static NodeAttribute[] CONNECTION_FACTORY_ATTRS = new NodeAttribute[] {
        //Do these 2 most frequently used ones out of alphabetical order
        new NodeAttribute(CONNECTOR, ModelType.OBJECT, false),   //<------
        new NodeAttribute(ENTRIES, ModelType.LIST, ModelType.STRING, false),

        new NodeAttribute(AUTO_GROUP, ModelType.BOOLEAN, false),
        new NodeAttribute(BLOCK_ON_ACK, ModelType.BOOLEAN, false),
        new NodeAttribute(BLOCK_ON_DURABLE_SEND, ModelType.BOOLEAN, false),
        new NodeAttribute(BLOCK_ON_NON_DURABLE_SEND, ModelType.BOOLEAN, false),
        new NodeAttribute(CACHE_LARGE_MESSAGE_CLIENT, ModelType.BOOLEAN, false),
        new NodeAttribute(CALL_TIMEOUT, ModelType.LONG, false),
        new NodeAttribute(CLIENT_FAILURE_CHECK_PERIOD, ModelType.LONG, false),
        new NodeAttribute(CLIENT_ID, ModelType.STRING, false),
        new NodeAttribute(CONFIRMATION_WINDOW_SIZE, ModelType.INT, false),
        new NodeAttribute(CONNECTION_TTL, ModelType.LONG, false),
        new NodeAttribute(CONSUMER_MAX_RATE, ModelType.INT, false),
        new NodeAttribute(CONSUMER_WINDOW_SIZE, ModelType.INT, false),
        new NodeAttribute(DISCOVERY_GROUP_NAME, ModelType.STRING, false),
        new NodeAttribute(DISCOVERY_INITIAL_WAIT_TIMEOUT, ModelType.LONG, false),
        new NodeAttribute(DUPS_OK_BATCH_SIZE, ModelType.INT, false),
        new NodeAttribute(FAILOVER_ON_INITIAL_CONNECTION, ModelType.BOOLEAN, false),
        new NodeAttribute(FAILOVER_ON_SERVER_SHUTDOWN, ModelType.BOOLEAN, false),
        new NodeAttribute(GROUP_ID, ModelType.STRING, false),
        new NodeAttribute(MAX_RETRY_INTERVAL, ModelType.LONG, false),
        new NodeAttribute(MIN_LARGE_MESSAGE_SIZE, ModelType.LONG, false),
        new NodeAttribute(PRE_ACK, ModelType.BOOLEAN, false),
        new NodeAttribute(PRODUCER_MAX_RATE, ModelType.INT, false),
        new NodeAttribute(PRODUCER_WINDOW_SIZE, ModelType.INT, false),
        new NodeAttribute(RECONNECT_ATTEMPTS, ModelType.INT, false),
        new NodeAttribute(RETRY_INTERVAL, ModelType.LONG, false),
        new NodeAttribute(RETRY_INTERVAL_MULTIPLIER, ModelType.BIG_DECIMAL, false),
        new NodeAttribute(SCHEDULED_THREAD_POOL_MAX_SIZE, ModelType.INT, false),
        new NodeAttribute(THREAD_POOL_MAX_SIZE, ModelType.INT, false),
        new NodeAttribute(TRANSACTION_BATCH_SIZE, ModelType.INT, false),
        new NodeAttribute(USE_GLOBAL_POOLS, ModelType.BOOLEAN, false)};

    public static NodeAttribute[] POOLED_CONNECTION_FACTORY_ATTRS = new NodeAttribute[] {
        //Do these 2 most frequently used ones out of alphabetical order
        new NodeAttribute(CONNECTOR, ModelType.OBJECT, false),   //<------
        new NodeAttribute(ENTRIES, ModelType.LIST, ModelType.STRING, false),

        new NodeAttribute(AUTO_GROUP, ModelType.BOOLEAN, false),
        new NodeAttribute(BLOCK_ON_ACK, ModelType.BOOLEAN, false),
        new NodeAttribute(BLOCK_ON_DURABLE_SEND, ModelType.BOOLEAN, false),
        new NodeAttribute(BLOCK_ON_NON_DURABLE_SEND, ModelType.BOOLEAN, false),
        new NodeAttribute(CACHE_LARGE_MESSAGE_CLIENT, ModelType.BOOLEAN, false),
        new NodeAttribute(CALL_TIMEOUT, ModelType.LONG, false),
        new NodeAttribute(CLIENT_FAILURE_CHECK_PERIOD, ModelType.LONG, false),
        new NodeAttribute(CLIENT_ID, ModelType.STRING, false),
        new NodeAttribute(CONFIRMATION_WINDOW_SIZE, ModelType.INT, false),
        new NodeAttribute(CONNECTION_TTL, ModelType.LONG, false),
        new NodeAttribute(CONSUMER_MAX_RATE, ModelType.INT, false),
        new NodeAttribute(CONSUMER_WINDOW_SIZE, ModelType.INT, false),
        new NodeAttribute(DISCOVERY_GROUP_NAME, ModelType.STRING, false),
        new NodeAttribute(DISCOVERY_INITIAL_WAIT_TIMEOUT, ModelType.LONG, false),
        new NodeAttribute(DUPS_OK_BATCH_SIZE, ModelType.INT, false),
        new NodeAttribute(FAILOVER_ON_INITIAL_CONNECTION, ModelType.BOOLEAN, false),
        new NodeAttribute(FAILOVER_ON_SERVER_SHUTDOWN, ModelType.BOOLEAN, false),
        new NodeAttribute(GROUP_ID, ModelType.STRING, false),
        new NodeAttribute(MAX_RETRY_INTERVAL, ModelType.LONG, false),
        new NodeAttribute(MIN_LARGE_MESSAGE_SIZE, ModelType.LONG, false),
        new NodeAttribute(PRE_ACK, ModelType.BOOLEAN, false),
        new NodeAttribute(PRODUCER_MAX_RATE, ModelType.INT, false),
        new NodeAttribute(PRODUCER_WINDOW_SIZE, ModelType.INT, false),
        new NodeAttribute(RECONNECT_ATTEMPTS, ModelType.INT, false),
        new NodeAttribute(RETRY_INTERVAL, ModelType.LONG, false),
        new NodeAttribute(RETRY_INTERVAL_MULTIPLIER, ModelType.BIG_DECIMAL, false),
        new NodeAttribute(SCHEDULED_THREAD_POOL_MAX_SIZE, ModelType.INT, false),
        new NodeAttribute(THREAD_POOL_MAX_SIZE, ModelType.INT, false),
        new NodeAttribute(TRANSACTION_BATCH_SIZE, ModelType.INT, false),
        new NodeAttribute(USE_GLOBAL_POOLS, ModelType.BOOLEAN, false),
        new NodeAttribute(USE_JNDI, ModelType.BOOLEAN, false),
        new NodeAttribute(JNDI_PARAMS, ModelType.STRING, false),
        new NodeAttribute(USE_LOCAL_TX, ModelType.BOOLEAN, false),
        new NodeAttribute(SETUP_ATTEMPTS, ModelType.STRING, false),
        new NodeAttribute(SETUP_INTERVAL, ModelType.STRING, false)};

    static PooledCFAttribute[] POOLED_CONNECTION_FACTORY_METHOD_ATTRS = new PooledCFAttribute[] {
        new PooledCFAttribute(AUTO_GROUP, Boolean.class.getName(), AUTO_GROUP_METHOD),
        new PooledCFAttribute(BLOCK_ON_ACK, Boolean.class.getName(), BLOCK_ON_ACK_METHOD),
        new PooledCFAttribute(BLOCK_ON_DURABLE_SEND, Boolean.class.getName(), BLOCK_ON_DURABLE_SEND_METHOD),
        new PooledCFAttribute(BLOCK_ON_NON_DURABLE_SEND, Boolean.class.getName(), BLOCK_ON_NON_DURABLE_SEND_METHOD),
        new PooledCFAttribute(CACHE_LARGE_MESSAGE_CLIENT, Boolean.class.getName(), CACHE_LARGE_MESSAGE_CLIENT_METHOD),
        new PooledCFAttribute(CALL_TIMEOUT, Long.class.getName(), CALL_TIMEOUT_METHOD),
        new PooledCFAttribute(CLIENT_FAILURE_CHECK_PERIOD, Long.class.getName(), CLIENT_FAILURE_CHECK_PERIOD_METHOD),
        new PooledCFAttribute(CLIENT_ID, String.class.getName(), CLIENT_ID_METHOD),
        new PooledCFAttribute(CONFIRMATION_WINDOW_SIZE, Integer.class.getName(), CONFIRMATION_WINDOW_SIZE_METHOD),
        new PooledCFAttribute(CONNECTION_TTL, Long.class.getName(), CONNECTION_TTL_METHOD),
        new PooledCFAttribute(CONSUMER_MAX_RATE, Integer.class.getName(), CONSUMER_MAX_RATE_METHOD),
        new PooledCFAttribute(CONSUMER_WINDOW_SIZE, Integer.class.getName(), CONSUMER_WINDOW_SIZE_METHOD),
        new PooledCFAttribute(DISCOVERY_GROUP_NAME, String.class.getName(), DISCOVERY_GROUP_NAME_METHOD),
        new PooledCFAttribute(DISCOVERY_INITIAL_WAIT_TIMEOUT, Long.class.getName(), DISCOVERY_INITIAL_WAIT_TIMEOUT_METHOD),
        new PooledCFAttribute(DUPS_OK_BATCH_SIZE, Integer.class.getName(), DUPS_OK_BATCH_SIZE_METHOD),
        new PooledCFAttribute(FAILOVER_ON_INITIAL_CONNECTION, Boolean.class.getName(), FAILOVER_ON_INITIAL_CONNECTION_METHOD),
        new PooledCFAttribute(FAILOVER_ON_SERVER_SHUTDOWN, Boolean.class.getName(), FAILOVER_ON_SERVER_SHUTDOWN_METHOD),
        new PooledCFAttribute(GROUP_ID, String.class.getName(), GROUP_ID_METHOD),
        new PooledCFAttribute(MAX_RETRY_INTERVAL, Long.class.getName(), MAX_RETRY_INTERVAL_METHOD),
        new PooledCFAttribute(MIN_LARGE_MESSAGE_SIZE, Long.class.getName(), MIN_LARGE_MESSAGE_SIZE_METHOD),
        new PooledCFAttribute(PRE_ACK, Boolean.class.getName(), PRE_ACK_METHOD),
        new PooledCFAttribute(PRODUCER_MAX_RATE, Integer.class.getName(), PRODUCER_MAX_RATE_METHOD),
        new PooledCFAttribute(PRODUCER_WINDOW_SIZE, Integer.class.getName(), PRODUCER_WINDOW_SIZE_METHOD),
        new PooledCFAttribute(RECONNECT_ATTEMPTS, Integer.class.getName(), RECONNECT_ATTEMPTS_METHOD),
        new PooledCFAttribute(RETRY_INTERVAL, Long.class.getName(), RETRY_INTERVAL_METHOD),
        new PooledCFAttribute(RETRY_INTERVAL_MULTIPLIER, Double.class.getName(), RETRY_INTERVAL_MULTIPLIER_METHOD),
        new PooledCFAttribute(SCHEDULED_THREAD_POOL_MAX_SIZE, Integer.class.getName(), SCHEDULED_THREAD_POOL_MAX_SIZE_METHOD),
        new PooledCFAttribute(THREAD_POOL_MAX_SIZE, Integer.class.getName(), THREAD_POOL_MAX_SIZE_METHOD),
        new PooledCFAttribute(TRANSACTION_BATCH_SIZE, Integer.class.getName(), TRANSACTION_BATCH_SIZE_METHOD),
        new PooledCFAttribute(USE_GLOBAL_POOLS, Boolean.class.getName(), USE_GLOBAL_POOLS_METHOD),
        new PooledCFAttribute(USE_JNDI, Boolean.class.getName(), USE_JNDI_METHOD),
        new PooledCFAttribute(JNDI_PARAMS, String.class.getName(), JNDI_PARAMS_METHOD),
        new PooledCFAttribute(USE_LOCAL_TX, Boolean.class.getName(), USE_LOCAL_TX_METHOD),
        new PooledCFAttribute(SETUP_ATTEMPTS, Integer.class.getName(), SETUP_ATTEMPTS_METHOD),
        new PooledCFAttribute(SETUP_INTERVAL, Long.class.getName(), SETUP_INTERVAL_METHOD)};

    public static class NodeAttribute {
        private final String name;
        private final ModelType type;
        private final ModelType valueType;
        private final boolean required;

        NodeAttribute(String name, ModelType type, boolean required) {
            this(name, type, null, required);
        }


        NodeAttribute(String name, ModelType type, ModelType valueType, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.valueType = valueType;
        }

        public String getName() {
            return name;
        }

        public ModelType getType() {
            return type;
        }

        public ModelType getValueType() {
            return valueType;
        }

        public boolean isRequired() {
            return required;
        }
    }

    static class PooledCFAttribute {
        private String name;
        private String classType;
        private String methodName;

        public PooledCFAttribute(String name, String classType, String methodName) {
            this.name = name;
            this.classType = classType;
            this.methodName = methodName;
        }

        public String getName() {
            return name;
        }

        public String getClassType() {
            return classType;
        }

        public String getMethodName() {
            return methodName;
        }
    }
}