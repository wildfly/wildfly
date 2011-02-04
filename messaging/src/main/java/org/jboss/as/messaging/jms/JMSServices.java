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

import static org.jboss.as.messaging.jms.CommonAttributes.AUTO_GROUP;
import static org.jboss.as.messaging.jms.CommonAttributes.BLOCK_ON_ACK;
import static org.jboss.as.messaging.jms.CommonAttributes.BLOCK_ON_DURABLE_SEND;
import static org.jboss.as.messaging.jms.CommonAttributes.BLOCK_ON_NON_DURABLE_SEND;
import static org.jboss.as.messaging.jms.CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT;
import static org.jboss.as.messaging.jms.CommonAttributes.CALL_TIMEOUT;
import static org.jboss.as.messaging.jms.CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD;
import static org.jboss.as.messaging.jms.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.jms.CommonAttributes.CONFIRMATION_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.CONNECTION_TTL;
import static org.jboss.as.messaging.jms.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.jms.CommonAttributes.CONSUMER_MAX_RATE;
import static org.jboss.as.messaging.jms.CommonAttributes.CONSUMER_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.jms.CommonAttributes.DUPS_OK_BATCH_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.jms.CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION;
import static org.jboss.as.messaging.jms.CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN;
import static org.jboss.as.messaging.jms.CommonAttributes.GROUP_ID;
import static org.jboss.as.messaging.jms.CommonAttributes.MAX_RETRY_INTERVAL;
import static org.jboss.as.messaging.jms.CommonAttributes.MIN_LARGE_MESSAGE_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.PRE_ACK;
import static org.jboss.as.messaging.jms.CommonAttributes.PRODUCER_MAX_RATE;
import static org.jboss.as.messaging.jms.CommonAttributes.PRODUCER_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.jms.CommonAttributes.RETRY_INTERVAL;
import static org.jboss.as.messaging.jms.CommonAttributes.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.jms.CommonAttributes.SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.TRANSACTION_BATCH_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.USE_GLOBAL_POOLS;

import org.jboss.as.messaging.MessagingSubsystemElement;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public class JMSServices {

    public static final ServiceName JMS = MessagingSubsystemElement.JBOSS_MESSAGING.append("jms");
    public static final ServiceName JMS_MANAGER = JMS.append("manager");
    public static final ServiceName JMS_QUEUE_BASE = JMS.append("queue");
    public static final ServiceName JMS_TOPIC_BASE = JMS.append("topic");
    public static final ServiceName JMS_CF_BASE = JMS.append("connection-factory");

    static String[] CF_ATTRIBUTES = new String[] { AUTO_GROUP, ENTRIES, CONNECTOR, BLOCK_ON_ACK, BLOCK_ON_DURABLE_SEND, BLOCK_ON_NON_DURABLE_SEND, CACHE_LARGE_MESSAGE_CLIENT,
        CALL_TIMEOUT, CLIENT_FAILURE_CHECK_PERIOD, CLIENT_ID, CONFIRMATION_WINDOW_SIZE, CONNECTION_TTL, CONNECTOR, CONSUMER_MAX_RATE, CONSUMER_WINDOW_SIZE, DISCOVERY_GROUP_NAME,
        DUPS_OK_BATCH_SIZE, FAILOVER_ON_INITIAL_CONNECTION, FAILOVER_ON_SERVER_SHUTDOWN, GROUP_ID, MAX_RETRY_INTERVAL, MIN_LARGE_MESSAGE_SIZE, PRE_ACK, PRODUCER_MAX_RATE,
        PRODUCER_WINDOW_SIZE, RECONNECT_ATTEMPTS, RETRY_INTERVAL, RETRY_INTERVAL_MULTIPLIER, SCHEDULED_THREAD_POOL_MAX_SIZE, THREAD_POOL_MAX_SIZE, TRANSACTION_BATCH_SIZE,
        USE_GLOBAL_POOLS,  };

}