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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 */
enum Element {
    UNKNOWN(null),

    AUTO_GROUP("auto-group"),

    BLOCK_ON_ACK("block-on-acknowledge"),
    BLOCK_ON_DURABLE_SEND("block-on-durable-send"),
    BLOCK_ON_NON_DURABLE_SEND("block-on-non-durable-send"),
    CACHE_LARGE_MESSAGE_CLIENT("cache-large-message-client"),
    CALL_TIMEOUT("call-timeout"),
    CLIENT_FAILURE_CHECK_PERIOD("client-failure-check-period"),
    CLIENT_ID("client-id"),
    CONNECTION_FACTORY("connection-factory"),
    CONNECTOR_REF("connector-ref"),
    CONNECTORS("connectors"),
    CONNECTION_TTL("connection-ttl"),
    CONFIRMATION_WINDOW_SIZE("confirmation-window-size"),
    CONSUMER_MAX_RATE("consumer-max-rate"),
    CONSUMER_WINDOW_SIZE("consumer-window-size"),
    DISCOVERY_INITIAL_WAIT_TIMEOUT("discovery-initial-wait-timeout"),
    DISCOVERY_GROUP_REF("discovery-group-ref"),
    DUPS_OK_BATCH_SIZE("dups-ok-batch-size"),
    DURABLE("durable"),
    ENTRIES("entries"),
    ENTRY("entry"),
    FAILOVER_ON_INITIAL_CONNECTION("failover-on-initial-connection"),
    FAILOVER_ON_SERVER_SHUTDOWN("failover-on-server-shutdown"),
    GROUP_ID("group-id"),
    LOAD_BALANCING_CLASS_NAME("connection-load-balancing-policy-class-name"),
    MAX_RETRY_INTERVAL("max-retry-interval"),
    MIN_LARGE_MESSAGE_SIZE("min-large-message-size"),

    PRE_ACK("pre-acknowledge"),
    PRODUCER_WINDOW_SIZE("producer-window-size"),
    PRODUCER_MAX_RATE("producer-max-rate"),
    QUEUE("queue"),
    RECONNECT_ATTEMPTS("reconnect-attempts"),
    RETRY_INTERVAL("retry-interval"),
    RETRY_INTERVAL_MULTIPLIER("retry-interval-multiplier"),

    SELECTOR("selector"),
    SCHEDULED_THREAD_POOL_MAX_SIZE("scheduled-thread-pool-max-size"),
    THREAD_POOL_MAX_SIZE("thread-pool-max-size"),
    TOPIC("topic"),
    TRANSACTION_BATH_SIZE("transaction-batch-size"),
    USE_GLOBAL_POOLS("use-global-pools"),
    ;

    private final String name;

    Element(final String name) {
       this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
       return name;
    }

    private static final Map<String, Element> MAP;

    static {
       final Map<String, Element> map = new HashMap<String, Element>();
       for (Element element : values()) {
          final String name = element.getLocalName();
          if (name != null) map.put(name, element);
       }
       MAP = map;
    }

    public static Element forName(String localName) {
       final Element element = MAP.get(localName);
       return element == null ? UNKNOWN : element;
    }
}
