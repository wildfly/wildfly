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

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author scott.stark@jboss.org
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public enum Element {

   // must be first
   UNKNOWN((String) null),
   // Messaging 1.0 elements in alpha order
   ACCEPTORS(CommonAttributes.ACCEPTORS),
   ADDRESS(getAttributeDefinitions(CommonAttributes.QUEUE_ADDRESS, CommonAttributes.DIVERT_ADDRESS,
           CommonAttributes.GROUPING_HANDLER_ADDRESS, CommonAttributes.CLUSTER_CONNECTION_ADDRESS)),
   ADDRESS_SETTINGS(CommonAttributes.ADDRESS_SETTINGS),
   ALLOW_FAILBACK(CommonAttributes.ALLOW_FAILBACK),
   ASYNC_CONNECTION_EXECUTION_ENABLED(CommonAttributes.ASYNC_CONNECTION_EXECUTION_ENABLED),
   BACKUP(CommonAttributes.BACKUP),
   BINDINGS_DIRECTORY(CommonAttributes.BINDINGS_DIRECTORY),
   BRIDGE(CommonAttributes.BRIDGE),
   BRIDGES(CommonAttributes.BRIDGES),
   BROADCAST_GROUP(CommonAttributes.BROADCAST_GROUP),
   BROADCAST_GROUPS(CommonAttributes.BROADCAST_GROUPS),
   BROADCAST_PERIOD(CommonAttributes.BROADCAST_PERIOD),
   CLASS_NAME(CommonAttributes.CLASS_NAME),
   CLUSTERED(CommonAttributes.CLUSTERED),
   CLUSTER_CONNECTION(CommonAttributes.CLUSTER_CONNECTION),
   CLUSTER_CONNECTIONS(CommonAttributes.CLUSTER_CONNECTIONS),
   CLUSTER_PASSWORD(CommonAttributes.CLUSTER_PASSWORD),
   CLUSTER_USER(CommonAttributes.CLUSTER_USER),
   CONNECTION_TTL_OVERRIDE(CommonAttributes.CONNECTION_TTL_OVERRIDE),
   CONNECTOR_SERVICE(CommonAttributes.CONNECTOR_SERVICE),
   CONNECTOR_SERVICES(CommonAttributes.CONNECTOR_SERVICES),
   CONNECTOR_REF(getConnectorRefDefinitions()),
   CORE_QUEUES(CommonAttributes.CORE_QUEUES),
   CREATE_BINDINGS_DIR(CommonAttributes.CREATE_BINDINGS_DIR),
   CREATE_JOURNAL_DIR(CommonAttributes.CREATE_JOURNAL_DIR),
   DISCOVERY_GROUP(CommonAttributes.DISCOVERY_GROUP),
   DISCOVERY_GROUPS(CommonAttributes.DISCOVERY_GROUPS),
   DIVERT(CommonAttributes.DIVERT),
   DIVERTS(CommonAttributes.DIVERTS),
   DURABLE(CommonAttributes.DURABLE),
   EXCLUSIVE(CommonAttributes.EXCLUSIVE),
   FAILBACK_DELAY(CommonAttributes.FAILBACK_DELAY),
   FAILOVER_ON_SHUTDOWN(CommonAttributes.FAILOVER_ON_SHUTDOWN),
   FILE_DEPLOYMENT_ENABLED(CommonAttributes.FILE_DEPLOYMENT_ENABLED),
   FORWARDING_ADDRESS(getForwardingAddressDefinitions()),
   FORWARD_WHEN_NO_CONSUMERS(CommonAttributes.FORWARD_WHEN_NO_CONSUMERS),
   GROUP_ADDRESS(CommonAttributes.GROUP_ADDRESS),
   GROUP_PORT(CommonAttributes.GROUP_PORT),
   GROUPING_HANDLER(CommonAttributes.GROUPING_HANDLER),
   HORNETQ_SERVER(CommonAttributes.HORNETQ_SERVER),
   ID_CACHE_SIZE(CommonAttributes.ID_CACHE_SIZE),
   INITIAL_WAIT_TIMEOUT(CommonAttributes.INITIAL_WAIT_TIMEOUT),
   IN_VM_ACCEPTOR(CommonAttributes.IN_VM_ACCEPTOR),
   IN_VM_CONNECTOR(CommonAttributes.IN_VM_CONNECTOR),
   JMX_DOMAIN(CommonAttributes.JMX_DOMAIN),
   JMX_MANAGEMENT_ENABLED(CommonAttributes.JMX_MANAGEMENT_ENABLED),
   JOURNAL_BUFFER_SIZE(CommonAttributes.JOURNAL_BUFFER_SIZE),
   JOURNAL_BUFFER_TIMEOUT(CommonAttributes.JOURNAL_BUFFER_TIMEOUT),
   JOURNAL_COMPACT_MIN_FILES(CommonAttributes.JOURNAL_COMPACT_MIN_FILES),
   JOURNAL_COMPACT_PERCENTAGE(CommonAttributes.JOURNAL_COMPACT_PERCENTAGE),
   JOURNAL_DIRECTORY(CommonAttributes.JOURNAL_DIRECTORY),
   JOURNAL_FILE_SIZE(CommonAttributes.JOURNAL_FILE_SIZE),
   JOURNAL_MAX_IO(CommonAttributes.JOURNAL_MAX_IO),
   JOURNAL_MIN_FILES(CommonAttributes.JOURNAL_MIN_FILES),
   JOURNAL_SYNC_NON_TRANSACTIONAL(CommonAttributes.JOURNAL_SYNC_NON_TRANSACTIONAL),
   JOURNAL_SYNC_TRANSACTIONAL(CommonAttributes.JOURNAL_SYNC_TRANSACTIONAL),
   JOURNAL_TYPE(CommonAttributes.JOURNAL_TYPE),
   LARGE_MESSAGES_DIRECTORY(CommonAttributes.LARGE_MESSAGES_DIRECTORY),
   LIVE_CONNECTOR_REF(CommonAttributes.LIVE_CONNECTOR_REF),
   LOCAL_BIND_ADDRESS(CommonAttributes.LOCAL_BIND_ADDRESS),
   LOCAL_BIND_PORT(CommonAttributes.LOCAL_BIND_PORT),
   LOG_JOURNAL_WRITE_RATE(CommonAttributes.LOG_JOURNAL_WRITE_RATE),
   MANAGEMENT_ADDRESS(CommonAttributes.MANAGEMENT_ADDRESS),
   MANAGEMENT_NOTIFICATION_ADDRESS(CommonAttributes.MANAGEMENT_NOTIFICATION_ADDRESS),
   MAX_HOPS(CommonAttributes.MAX_HOPS),
   MEMORY_MEASURE_INTERVAL(CommonAttributes.MEMORY_MEASURE_INTERVAL),
   MEMORY_WARNING_THRESHOLD(CommonAttributes.MEMORY_WARNING_THRESHOLD),
   MESSAGE_COUNTER_ENABLED(CommonAttributes.MESSAGE_COUNTER_ENABLED),
   MESSAGE_COUNTER_MAX_DAY_HISTORY(CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY),
   MESSAGE_COUNTER_SAMPLE_PERIOD(CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD),
   MESSAGE_EXPIRY_SCAN_PERIOD(CommonAttributes.MESSAGE_EXPIRY_SCAN_PERIOD),
   MESSAGE_EXPIRY_THREAD_PRIORITY(CommonAttributes.MESSAGE_EXPIRY_THREAD_PRIORITY),
   NAME(CommonAttributes.NAME),
   NETTY_ACCEPTOR(CommonAttributes.NETTY_ACCEPTOR),
   NETTY_CONNECTOR(CommonAttributes.NETTY_CONNECTOR),
   PAGING_DIRECTORY(CommonAttributes.PAGING_DIRECTORY),
   PERF_BLAST_PAGES(CommonAttributes.PERF_BLAST_PAGES),
   PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY(CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY),
   PERSIST_ID_CACHE(CommonAttributes.PERSIST_ID_CACHE),
   PERSISTENCE_ENABLED(CommonAttributes.PERSISTENCE_ENABLED),
   QUEUE(CommonAttributes.QUEUE),
   REFRESH_TIMEOUT(CommonAttributes.REFRESH_TIMEOUT),
   REMOTING_INTERCEPTORS(CommonAttributes.REMOTING_INTERCEPTORS),
   ROUTING_NAME(CommonAttributes.ROUTING_NAME),
   RUN_SYNC_SPEED_TEST(CommonAttributes.RUN_SYNC_SPEED_TEST),
   SECURITY_DOMAIN(CommonAttributes.SECURITY_DOMAIN),
   SECURITY_ENABLED(CommonAttributes.SECURITY_ENABLED),
   SECURITY_INVALIDATION_INTERVAL(CommonAttributes.SECURITY_INVALIDATION_INTERVAL),
   SECURITY_SETTINGS(CommonAttributes.SECURITY_SETTINGS),
   SERVER_DUMP_INTERVAL(CommonAttributes.SERVER_DUMP_INTERVAL),
   SHARED_STORE(CommonAttributes.SHARED_STORE),
   SUBSYSTEM(CommonAttributes.SUBSYSTEM),
   TRANSACTION_TIMEOUT(CommonAttributes.TRANSACTION_TIMEOUT),
   TRANSACTION_TIMEOUT_SCAN_PERIOD(CommonAttributes.TRANSACTION_TIMEOUT_SCAN_PERIOD),
   TRANSFORMER_CLASS_NAME(CommonAttributes.TRANSFORMER_CLASS_NAME),
   WILD_CARD_ROUTING_ENABLED(CommonAttributes.WILD_CARD_ROUTING_ENABLED),
   ACCEPTOR(CommonAttributes.ACCEPTOR),
   CONNECTORS(CommonAttributes.CONNECTORS),
   CONNECTOR(CommonAttributes.CONNECTOR),
   FACTORY_CLASS(CommonAttributes.FACTORY_CLASS),
   FILTER(CommonAttributes.FILTER),
   PARAM(CommonAttributes.PARAM),
   SECURITY_SETTING(CommonAttributes.SECURITY_SETTING),
   PERMISSION_ELEMENT_NAME(CommonAttributes.PERMISSION_ELEMENT_NAME),
   ADDRESS_SETTING(CommonAttributes.ADDRESS_SETTING),
   DEAD_LETTER_ADDRESS_NODE_NAME(CommonAttributes.DEAD_LETTER_ADDRESS),
   EXPIRY_ADDRESS_NODE_NAME(CommonAttributes.EXPIRY_ADDRESS),
   REDELIVERY_DELAY_NODE_NAME(CommonAttributes.REDELIVERY_DELAY),
   MAX_DELIVERY_ATTEMPTS(CommonAttributes.MAX_DELIVERY_ATTEMPTS),
   MAX_SIZE_BYTES_NODE_NAME(CommonAttributes.MAX_SIZE_BYTES_NODE_NAME),
   ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME(CommonAttributes.ADDRESS_FULL_MESSAGE_POLICY),
   PAGE_MAX_CACHE_SIZE(CommonAttributes.PAGE_MAX_CACHE_SIZE),
   PAGE_SIZE_BYTES_NODE_NAME(CommonAttributes.PAGE_SIZE_BYTES_NODE_NAME),
   MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME(CommonAttributes.MESSAGE_COUNTER_HISTORY_DAY_LIMIT),
   LVQ_NODE_NAME(CommonAttributes.LVQ),
   REDISTRIBUTION_DELAY_NODE_NAME(CommonAttributes.REDISTRIBUTION_DELAY),
   SEND_TO_DLA_ON_NO_ROUTE(CommonAttributes.SEND_TO_DLA_ON_NO_ROUTE),
   STATIC_CONNECTORS(CommonAttributes.STATIC_CONNECTORS),
   TIMEOUT(CommonAttributes.TIMEOUT),
   TYPE(CommonAttributes.TYPE),

   //JMS Stuff
   AUTO_GROUP(CommonAttributes.AUTO_GROUP),
   BLOCK_ON_ACK(CommonAttributes.BLOCK_ON_ACK),
   BLOCK_ON_DURABLE_SEND(CommonAttributes.BLOCK_ON_DURABLE_SEND),
   BLOCK_ON_NON_DURABLE_SEND(CommonAttributes.BLOCK_ON_NON_DURABLE_SEND),
   CACHE_LARGE_MESSAGE_CLIENT(CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT),
   CALL_TIMEOUT(CommonAttributes.CALL_TIMEOUT),
   CLIENT_FAILURE_CHECK_PERIOD(CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD),
   CLIENT_ID(CommonAttributes.CLIENT_ID),
   CONNECTION_FACTORY(CommonAttributes.CONNECTION_FACTORY),
   CONNECTION_FACTORIES(CommonAttributes.JMS_CONNECTION_FACTORIES),
   CONNECTION_TTL(CommonAttributes.CONNECTION_TTL),
   CONFIRMATION_WINDOW_SIZE(CommonAttributes.CONFIRMATION_WINDOW_SIZE),
   CONSUMER_MAX_RATE(CommonAttributes.CONSUMER_MAX_RATE),
   CONSUMER_WINDOW_SIZE(CommonAttributes.CONSUMER_WINDOW_SIZE),
   DISCOVERY_INITIAL_WAIT_TIMEOUT(CommonAttributes.DISCOVERY_INITIAL_WAIT_TIMEOUT),
   DISCOVERY_GROUP_REF(CommonAttributes.DISCOVERY_GROUP_REF),
   DUPS_OK_BATCH_SIZE(CommonAttributes.DUPS_OK_BATCH_SIZE),
   ENTRIES(CommonAttributes.ENTRIES),
   ENTRY(CommonAttributes.ENTRY),
   FAILOVER_ON_INITIAL_CONNECTION(CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION),
   FAILOVER_ON_SERVER_SHUTDOWN(CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN),
   GROUP_ID(CommonAttributes.GROUP_ID),
   HA(CommonAttributes.HA),
   JMS_DESTINATIONS(CommonAttributes.JMS_DESTINATIONS),
   JMS_TOPIC(CommonAttributes.JMS_TOPIC),
   JMS_QUEUE(CommonAttributes.JMS_QUEUE),
   LOAD_BALANCING_CLASS_NAME(CommonAttributes.LOAD_BALANCING_CLASS_NAME),
   MAX_RETRY_INTERVAL(CommonAttributes.MAX_RETRY_INTERVAL),
   MIN_LARGE_MESSAGE_SIZE(CommonAttributes.MIN_LARGE_MESSAGE_SIZE),
   PASSWORD(CommonAttributes.PASSWORD),
   PRE_ACK(CommonAttributes.PRE_ACK),
   PRODUCER_WINDOW_SIZE(CommonAttributes.PRODUCER_WINDOW_SIZE),
   PRODUCER_MAX_RATE(CommonAttributes.PRODUCER_MAX_RATE),
   QUEUE_NAME(CommonAttributes.QUEUE_NAME),
   RECONNECT_ATTEMPTS(getReconnectAttemptsDefinitions()),
   RETRY_INTERVAL(getRetryIntervalDefinitions()),
   RETRY_INTERVAL_MULTIPLIER(CommonAttributes.RETRY_INTERVAL_MULTIPLIER),
   SELECTOR(CommonAttributes.SELECTOR),
   SCHEDULED_THREAD_POOL_MAX_SIZE(getScheduledThreadPoolDefinitions()),
   THREAD_POOL_MAX_SIZE(getThreadPoolDefinitions()),
   TRANSACTION_BATH_SIZE(CommonAttributes.TRANSACTION_BATCH_SIZE),
   USER(CommonAttributes.USER),
   USE_DUPLICATE_DETECTION(getDuplicateDetectionDefinitions()),
   USE_GLOBAL_POOLS(CommonAttributes.USE_GLOBAL_POOLS),
   POOLED_CONNECTION_FACTORY(CommonAttributes.POOLED_CONNECTION_FACTORY),
   TRANSACTION(CommonAttributes.TRANSACTION),
   MODE(CommonAttributes.MODE),
   INBOUND_CONFIG(CommonAttributes.INBOUND_CONFIG),
   USE_JNDI(CommonAttributes.USE_JNDI),
   JNDI_PARAMS(CommonAttributes.JNDI_PARAMS),
   USE_LOCAL_TX(CommonAttributes.USE_LOCAL_TX),
   CONNECTION_FACTORY_TYPE(CommonAttributes.CONNECTION_FACTORY_TYPE),
   SETUP_ATTEMPTS(CommonAttributes.SETUP_ATTEMPTS),
   SETUP_INTERVAL(CommonAttributes.SETUP_INTERVAL),
   SOCKET_BINDING(CommonAttributes.SOCKET_BINDING.getName()),
   ;

   private final String name;
   private final AttributeDefinition definition;
   private final Map<String, AttributeDefinition> definitions;

   Element(final String name) {
      this.name = name;
       this.definition = null;
       this.definitions = null;
   }

   Element(final AttributeDefinition definition) {
       this.name = definition.getXmlName();
       this.definition = definition;
       this.definitions = null;
   }

   Element(final List<AttributeDefinition> definitions) {
        this.definition = null;
       this.definitions = new HashMap<String, AttributeDefinition>();
        String ourName = null;
        for (AttributeDefinition def : definitions) {
            if (ourName == null) {
                ourName = def.getXmlName();
            } else if (!ourName.equals(def.getXmlName())) {
                throw MESSAGES.attributeDefinitionsMustMatch(def.getXmlName(), ourName);
            }
            if (this.definitions.put(def.getName(), def) != null) {
                throw MESSAGES.attributeDefinitionsNotUnique(def.getName());
            }
        }
       this.name = ourName;
   }

   Element(final Map<String, AttributeDefinition> definitions) {
        this.definition = null;
        this.definitions = new HashMap<String, AttributeDefinition>();
        String ourName = null;
        for (Map.Entry<String, AttributeDefinition> def : definitions.entrySet()) {
            String xmlName = def.getValue().getXmlName();
            if (ourName == null) {
                ourName = xmlName;
            } else if (!ourName.equals(xmlName)) {
                throw MESSAGES.attributeDefinitionsMustMatch(xmlName, ourName);
            }
            this.definitions.put(def.getKey(), def.getValue());
        }
       this.name = ourName;
   }

   /**
    * Get the local name of this element.
    *
    * @return the local name
    */
   public String getLocalName() {
      return name;
   }

   public AttributeDefinition getDefinition() {
       return definition;
   }

   public AttributeDefinition getDefinition(final String name) {
       return definitions.get(name);
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

    private static List<AttributeDefinition> getAttributeDefinitions(final AttributeDefinition... attributeDefinitions) {
        return Arrays.asList(attributeDefinitions);
    }

    private static Map<String, AttributeDefinition> getScheduledThreadPoolDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("server", CommonAttributes.SCHEDULED_THREAD_POOL_MAX_SIZE);
        result.put("connection", CommonAttributes.CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE);
        return result;
    }

    private static Map<String, AttributeDefinition> getThreadPoolDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("server", CommonAttributes.THREAD_POOL_MAX_SIZE);
        result.put("connection", CommonAttributes.CONNECTION_THREAD_POOL_MAX_SIZE);
        return result;
    }

    private static Map<String, AttributeDefinition> getConnectorRefDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("simple", CommonAttributes.CONNECTOR_REF);
        result.put("broadcast-group", ConnectorRefsAttribute.BROADCAST_GROUP);
        result.put("bridge", ConnectorRefsAttribute.BRIDGE_CONNECTORS);
        result.put("cluster-connection", ConnectorRefsAttribute.CLUSTER_CONNECTION_CONNECTORS);
        return result;

    }

    private static Map<String, AttributeDefinition> getReconnectAttemptsDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("connection", CommonAttributes.CONNECTION_FACTORY_RECONNECT_ATTEMPTS);
        result.put("bridge", CommonAttributes.BRIDGE_RECONNECT_ATTEMPTS);
        return result;

    }

    private static Map<String, AttributeDefinition> getForwardingAddressDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("divert", CommonAttributes.DIVERT_FORWARDING_ADDRESS);
        result.put("bridge", CommonAttributes.BRIDGE_FORWARDING_ADDRESS);
        return result;

    }

    private static Map<String, AttributeDefinition> getDuplicateDetectionDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("cluster", CommonAttributes.CLUSTER_CONNECTION_USE_DUPLICATE_DETECTION);
        result.put("bridge", CommonAttributes.BRIDGE_USE_DUPLICATE_DETECTION);
        return result;

    }

    private static Map<String, AttributeDefinition> getRetryIntervalDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("cluster", CommonAttributes.CLUSTER_CONNECTION_RETRY_INTERVAL);
        result.put("default", CommonAttributes.RETRY_INTERVAL);
        return result;

    }
}
