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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.as.messaging.logging.MessagingLogger;

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
   ADDRESS(getAttributeDefinitions(QueueDefinition.ADDRESS, DivertDefinition.ADDRESS,
           GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS, ClusterConnectionDefinition.ADDRESS)),
   ADDRESS_SETTINGS(CommonAttributes.ADDRESS_SETTINGS),
   ALLOW_FAILBACK(CommonAttributes.ALLOW_FAILBACK),
   ASYNC_CONNECTION_EXECUTION_ENABLED(CommonAttributes.ASYNC_CONNECTION_EXECUTION_ENABLED),
   BACKUP(CommonAttributes.BACKUP),
   BINDINGS_DIRECTORY(CommonAttributes.BINDINGS_DIRECTORY),
   BRIDGE(CommonAttributes.BRIDGE),
   BRIDGES(CommonAttributes.BRIDGES),
   BROADCAST_GROUP(CommonAttributes.BROADCAST_GROUP),
   BROADCAST_GROUPS(CommonAttributes.BROADCAST_GROUPS),
   BROADCAST_PERIOD(BroadcastGroupDefinition.BROADCAST_PERIOD),
   CHECK_FOR_LIVE_SERVER(CommonAttributes.CHECK_FOR_LIVE_SERVER),
   CLASS_NAME(CommonAttributes.CLASS_NAME),
   @Deprecated
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
   EXCLUSIVE(DivertDefinition.EXCLUSIVE),
   FAILBACK_DELAY(CommonAttributes.FAILBACK_DELAY),
   FAILOVER_ON_SHUTDOWN(CommonAttributes.FAILOVER_ON_SHUTDOWN),
   FILE_DEPLOYMENT_ENABLED(CommonAttributes.FILE_DEPLOYMENT_ENABLED),
   FORWARDING_ADDRESS(getForwardingAddressDefinitions()),
   FORWARD_WHEN_NO_CONSUMERS(ClusterConnectionDefinition.FORWARD_WHEN_NO_CONSUMERS),
   GROUP_ADDRESS(CommonAttributes.GROUP_ADDRESS),
   GROUP_PORT(CommonAttributes.GROUP_PORT),
   GROUPING_HANDLER(CommonAttributes.GROUPING_HANDLER),
   GROUP_TIMEOUT(GroupingHandlerDefinition.GROUP_TIMEOUT),
   HORNETQ_SERVER(CommonAttributes.HORNETQ_SERVER),
   HTTP_ACCEPTOR(CommonAttributes.HTTP_ACCEPTOR),
   HTTP_CONNECTOR(CommonAttributes.HTTP_CONNECTOR),
   ID_CACHE_SIZE(CommonAttributes.ID_CACHE_SIZE),
   INITIAL_WAIT_TIMEOUT(DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT),
   IN_VM_ACCEPTOR(CommonAttributes.IN_VM_ACCEPTOR),
   IN_VM_CONNECTOR(CommonAttributes.IN_VM_CONNECTOR),
   JGROUPS_CHANNEL(CommonAttributes.JGROUPS_CHANNEL),
   JGROUPS_STACK(CommonAttributes.JGROUPS_STACK),
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
   @Deprecated
   LIVE_CONNECTOR_REF(CommonAttributes.LIVE_CONNECTOR_REF),
   LOCAL_BIND_ADDRESS(CommonAttributes.LOCAL_BIND_ADDRESS),
   LOCAL_BIND_PORT(CommonAttributes.LOCAL_BIND_PORT),
   LOG_JOURNAL_WRITE_RATE(CommonAttributes.LOG_JOURNAL_WRITE_RATE),
   MANAGEMENT_ADDRESS(CommonAttributes.MANAGEMENT_ADDRESS),
   MANAGEMENT_NOTIFICATION_ADDRESS(CommonAttributes.MANAGEMENT_NOTIFICATION_ADDRESS),
   MAX_HOPS(ClusterConnectionDefinition.MAX_HOPS),
   MAX_REDELIVERY_DELAY(AddressSettingDefinition.MAX_REDELIVERY_DELAY),
   MAX_SAVED_REPLICATED_JOURNAL_SIZE(CommonAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE),
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
   BACKUP_GROUP_NAME(CommonAttributes.BACKUP_GROUP_NAME),
   OVERRIDE_IN_VM_SECURITY(CommonAttributes.OVERRIDE_IN_VM_SECURITY),
   PAGE_MAX_CONCURRENT_IO(CommonAttributes.PAGE_MAX_CONCURRENT_IO),
   PAGING_DIRECTORY(CommonAttributes.PAGING_DIRECTORY),
   PERF_BLAST_PAGES(CommonAttributes.PERF_BLAST_PAGES),
   PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY(CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY),
   PERSIST_ID_CACHE(CommonAttributes.PERSIST_ID_CACHE),
   PERSISTENCE_ENABLED(CommonAttributes.PERSISTENCE_ENABLED),
   QUEUE(CommonAttributes.QUEUE),
   REAPER_PERIOD(GroupingHandlerDefinition.REAPER_PERIOD),
   REDELIVERY_MULTIPLIER(AddressSettingDefinition.REDELIVERY_MULTIPLIER),
   REFRESH_TIMEOUT(DiscoveryGroupDefinition.REFRESH_TIMEOUT),
   REMOTING_INTERCEPTORS(CommonAttributes.REMOTING_INTERCEPTORS),
   REMOTING_INCOMING_INTERCEPTORS(CommonAttributes.REMOTING_INCOMING_INTERCEPTORS),
   REMOTING_OUTGOING_INTERCEPTORS(CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS),
   REPLICATION_CLUSTERNAME(CommonAttributes.REPLICATION_CLUSTERNAME),
   ROUTING_NAME(DivertDefinition.ROUTING_NAME),
   RUN_SYNC_SPEED_TEST(CommonAttributes.RUN_SYNC_SPEED_TEST),
   SECURITY_DOMAIN(CommonAttributes.SECURITY_DOMAIN),
   SECURITY_ENABLED(CommonAttributes.SECURITY_ENABLED),
   SECURITY_INVALIDATION_INTERVAL(CommonAttributes.SECURITY_INVALIDATION_INTERVAL),
   SECURITY_SETTINGS(CommonAttributes.SECURITY_SETTINGS),
   SERVER_DUMP_INTERVAL(CommonAttributes.SERVER_DUMP_INTERVAL),
   SHARED_STORE(CommonAttributes.SHARED_STORE),
   STATISTICS_ENABLED(CommonAttributes.STATISTICS_ENABLED),
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
   DEAD_LETTER_ADDRESS(CommonAttributes.DEAD_LETTER_ADDRESS),
   EXPIRY_ADDRESS(CommonAttributes.EXPIRY_ADDRESS),
   EXPIRY_DELAY(AddressSettingDefinition.EXPIRY_DELAY),
   REDELIVERY_DELAY(AddressSettingDefinition.REDELIVERY_DELAY),
   MAX_DELIVERY_ATTEMPTS(AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS),
   MAX_SIZE_BYTES(AddressSettingDefinition.MAX_SIZE_BYTES),
   ADDRESS_FULL_MESSAGE_POLICY(AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY),
   PAGE_MAX_CACHE_SIZE(AddressSettingDefinition.PAGE_MAX_CACHE_SIZE),
   PAGE_SIZE_BYTES(AddressSettingDefinition.PAGE_SIZE_BYTES),
   MESSAGE_COUNTER_HISTORY_DAY_LIMIT(AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT),
   LVQ(AddressSettingDefinition.LAST_VALUE_QUEUE),
   REDISTRIBUTION_DELAY(AddressSettingDefinition.REDISTRIBUTION_DELAY),
   SEND_TO_DLA_ON_NO_ROUTE(AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE),
   SLOW_CONSUMER_CHECK_PERIOD(AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD),
   SLOW_CONSUMER_POLICY(AddressSettingDefinition.SLOW_CONSUMER_POLICY),
   SLOW_CONSUMER_THRESHOLD(AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD),
   STATIC_CONNECTORS(CommonAttributes.STATIC_CONNECTORS),
   TIMEOUT(GroupingHandlerDefinition.TIMEOUT),
   TYPE(GroupingHandlerDefinition.TYPE),

   //JMS Stuff
   AUTO_GROUP(Common.AUTO_GROUP),
   BLOCK_ON_ACK(Common.BLOCK_ON_ACKNOWLEDGE),
   BLOCK_ON_DURABLE_SEND(Common.BLOCK_ON_DURABLE_SEND),
   BLOCK_ON_NON_DURABLE_SEND(Common.BLOCK_ON_NON_DURABLE_SEND),
   CACHE_LARGE_MESSAGE_CLIENT(Common.CACHE_LARGE_MESSAGE_CLIENT),
   CALL_TIMEOUT(CommonAttributes.CALL_TIMEOUT),
   CALL_FAILOVER_TIMEOUT(CommonAttributes.CALL_FAILOVER_TIMEOUT),
   CHECK_PERIOD(getCheckPeriodDefinitions()),
   CLIENT_FAILURE_CHECK_PERIOD(Common.CLIENT_FAILURE_CHECK_PERIOD),
   CLIENT_ID(CommonAttributes.CLIENT_ID),
   CONNECTION_FACTORY(getConnectionFactoryDefinitions()),
   CONNECTION_FACTORIES(CommonAttributes.JMS_CONNECTION_FACTORIES),
   CONNECTION_TTL(getConnectionTTLDefinitions()),
   CONFIRMATION_WINDOW_SIZE(getConfirmationWindowSizeDefinitions()),
   CONSUMER_MAX_RATE(Common.CONSUMER_MAX_RATE),
   CONSUMER_WINDOW_SIZE(Common.CONSUMER_WINDOW_SIZE),
   DISCOVERY_INITIAL_WAIT_TIMEOUT(Common.DISCOVERY_INITIAL_WAIT_TIMEOUT),
   DISCOVERY_GROUP_REF(CommonAttributes.DISCOVERY_GROUP_REF),
   DUPS_OK_BATCH_SIZE(Common.DUPS_OK_BATCH_SIZE),
   ENTRIES(Common.ENTRIES),
   ENTRY(CommonAttributes.ENTRY),
   FAILOVER_ON_INITIAL_CONNECTION(Common.FAILOVER_ON_INITIAL_CONNECTION),
   FAILOVER_ON_SERVER_SHUTDOWN(Common.FAILOVER_ON_SERVER_SHUTDOWN),
   GROUP_ID(Common.GROUP_ID),
   HA(CommonAttributes.HA),
   INITIAL_CONNECT_ATTEMPTS(getInitialConnectAttemptsDefinitions()),
   INITIAL_MESSAGE_PACKET_SIZE(Pooled.INITIAL_MESSAGE_PACKET_SIZE),
   JMS_DESTINATIONS(CommonAttributes.JMS_DESTINATIONS),
   JMS_TOPIC(CommonAttributes.JMS_TOPIC),
   JMS_QUEUE(CommonAttributes.JMS_QUEUE),
   LOAD_BALANCING_CLASS_NAME(Common.CONNECTION_LOAD_BALANCING_CLASS_NAME),
   MAX_POOL_SIZE(Pooled.MAX_POOL_SIZE),
   MAX_RETRY_INTERVAL(getMaxRetryIntervalDefinitions()),
   MIN_LARGE_MESSAGE_SIZE(getMinLargeMessageSizeDefinitions()),
   MIN_POOL_SIZE(Pooled.MIN_POOL_SIZE),
   NOTIFICATION_ATTEMPTS(ClusterConnectionDefinition.NOTIFICATION_ATTEMPTS),
   NOTIFICATION_INTERVAL(ClusterConnectionDefinition.NOTIFICATION_INTERVAL),
   PASSWORD(getPasswordDefinitions()),
   PRE_ACK(Common.PRE_ACKNOWLEDGE),
   PRODUCER_WINDOW_SIZE(Common.PRODUCER_WINDOW_SIZE),
   PRODUCER_MAX_RATE(Common.PRODUCER_MAX_RATE),
   QUEUE_NAME(BridgeDefinition.QUEUE_NAME),
   RECONNECT_ATTEMPTS(getReconnectAttemptsDefinitions()),
   RECONNECT_ATTEMPTS_ON_SAME_NODE(BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE),
   RETRY_INTERVAL(getRetryIntervalDefinitions()),
   RETRY_INTERVAL_MULTIPLIER(getRetryIntervalMultiplierDefinitions()),
   SELECTOR(CommonAttributes.SELECTOR),
   SCHEDULED_THREAD_POOL_MAX_SIZE(getScheduledThreadPoolDefinitions()),
   THREAD_POOL_MAX_SIZE(getThreadPoolDefinitions()),
   TRANSACTION_BATH_SIZE(Common.TRANSACTION_BATCH_SIZE),
   USER(getUserDefinitions()),
   USE_DUPLICATE_DETECTION(getDuplicateDetectionDefinitions()),
   USE_AUTO_RECOVERY(Pooled.USE_AUTO_RECOVERY),
   USE_GLOBAL_POOLS(Common.USE_GLOBAL_POOLS),
   POOLED_CONNECTION_FACTORY(CommonAttributes.POOLED_CONNECTION_FACTORY),
   TRANSACTION(ConnectionFactoryAttributes.Pooled.TRANSACTION),
   MODE(CommonAttributes.MODE),
   INBOUND_CONFIG(CommonAttributes.INBOUND_CONFIG),
   USE_JNDI(Pooled.USE_JNDI),
   JNDI_PARAMS(Pooled.JNDI_PARAMS),
   USE_LOCAL_TX(Pooled.USE_LOCAL_TX),
   COMPRESS_LARGE_MESSAGES(ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES),
   CONNECTION_FACTORY_TYPE(ConnectionFactoryAttributes.Regular.FACTORY_TYPE),
   SETUP_ATTEMPTS(Pooled.SETUP_ATTEMPTS),
   SETUP_INTERVAL(Pooled.SETUP_INTERVAL),
   SOCKET_BINDING(RemoteTransportDefinition.SOCKET_BINDING.getName()),

   // JMS Bridge
   JMS_BRIDGE(CommonAttributes.JMS_BRIDGE),
   SOURCE(JMSBridgeDefinition.SOURCE),
   TARGET(JMSBridgeDefinition.TARGET),
   DESTINATION(getDestinationDefinitions()),
   CONTEXT(getContextDefinitions()),
   PROPERTY("property"),
   QUALITY_OF_SERVICE(JMSBridgeDefinition.QUALITY_OF_SERVICE),
   FAILURE_RETRY_INTERVAL(JMSBridgeDefinition.FAILURE_RETRY_INTERVAL),
   MAX_RETRIES(JMSBridgeDefinition.MAX_RETRIES),
   MAX_BATCH_SIZE(JMSBridgeDefinition.MAX_BATCH_SIZE),
   MAX_BATCH_TIME(JMSBridgeDefinition.MAX_BATCH_TIME),
   SUBSCRIPTION_NAME(JMSBridgeDefinition.SUBSCRIPTION_NAME),
   ADD_MESSAGE_ID_IN_HEADER(JMSBridgeDefinition.ADD_MESSAGE_ID_IN_HEADER),
   MODULE(JMSBridgeDefinition.MODULE),
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
                throw MessagingLogger.ROOT_LOGGER.attributeDefinitionsMustMatch(def.getXmlName(), ourName);
            }
            if (this.definitions.put(def.getName(), def) != null) {
                throw MessagingLogger.ROOT_LOGGER.attributeDefinitionsNotUnique(def.getName());
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
                throw MessagingLogger.ROOT_LOGGER.attributeDefinitionsMustMatch(xmlName, ourName);
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
        result.put("connection", Common.SCHEDULED_THREAD_POOL_MAX_SIZE);
        return result;
    }

    private static Map<String, AttributeDefinition> getThreadPoolDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("server", CommonAttributes.THREAD_POOL_MAX_SIZE);
        result.put("connection", Common.THREAD_POOL_MAX_SIZE);
        return result;
    }

    private static Map<String, AttributeDefinition> getConnectorRefDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("simple", ClusterConnectionDefinition.CONNECTOR_REF);
        result.put("broadcast-group", BroadcastGroupDefinition.CONNECTOR_REFS);
        result.put("bridge", BridgeDefinition.CONNECTOR_REFS);
        result.put("cluster-connection", ClusterConnectionDefinition.CONNECTOR_REFS);
        return result;

    }

    private static Map<String, AttributeDefinition> getReconnectAttemptsDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("connection", Common.RECONNECT_ATTEMPTS);
        result.put("pooled-connection", Pooled.RECONNECT_ATTEMPTS);
        result.put("bridge", BridgeDefinition.RECONNECT_ATTEMPTS);
        result.put("cluster", ClusterConnectionDefinition.RECONNECT_ATTEMPTS);
        return result;

    }

    private static Map<String, AttributeDefinition> getConfirmationWindowSizeDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("connection", ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE);
        result.put("bridge", CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE);
        return result;

    }

    private static Map<String, AttributeDefinition> getForwardingAddressDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("divert", DivertDefinition.FORWARDING_ADDRESS);
        result.put("bridge", BridgeDefinition.FORWARDING_ADDRESS);
        return result;

    }

    private static Map<String, AttributeDefinition> getDuplicateDetectionDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("cluster", ClusterConnectionDefinition.USE_DUPLICATE_DETECTION);
        result.put("bridge", BridgeDefinition.USE_DUPLICATE_DETECTION);
        return result;

    }

    private static Map<String, AttributeDefinition> getRetryIntervalDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("cluster", ClusterConnectionDefinition.RETRY_INTERVAL);
        result.put("connection", ConnectionFactoryAttributes.Common.RETRY_INTERVAL);
        result.put("default", CommonAttributes.RETRY_INTERVAL);
        return result;
    }

    private static Map<String, AttributeDefinition> getRetryIntervalMultiplierDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("cluster", ClusterConnectionDefinition.RETRY_INTERVAL_MULTIPLIER);
        result.put("connection", ConnectionFactoryAttributes.Common.RETRY_INTERVAL_MULTIPLIER);
        result.put("default", CommonAttributes.RETRY_INTERVAL_MULTIPLIER);
        return result;
    }

    private static Map<String, AttributeDefinition> getMaxRetryIntervalDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("cluster", ClusterConnectionDefinition.MAX_RETRY_INTERVAL);
        result.put("connection", ConnectionFactoryAttributes.Common.MAX_RETRY_INTERVAL);
        result.put("default", CommonAttributes.MAX_RETRY_INTERVAL);
        return result;
    }

    private static Map<String, AttributeDefinition> getMinLargeMessageSizeDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("connection", ConnectionFactoryAttributes.Common.MIN_LARGE_MESSAGE_SIZE);
        result.put("default", CommonAttributes.MIN_LARGE_MESSAGE_SIZE);
        return result;
    }

    private static Map<String, AttributeDefinition> getConnectionTTLDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("cluster", ClusterConnectionDefinition.CONNECTION_TTL);
        result.put("connection", ConnectionFactoryAttributes.Common.CONNECTION_TTL);
        result.put("default", CommonAttributes.CONNECTION_TTL);
        return result;
    }

    private static Map<String, AttributeDefinition> getCheckPeriodDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("cluster", ClusterConnectionDefinition.CHECK_PERIOD);
        result.put("default", CommonAttributes.CHECK_PERIOD);
        return result;
    }

    private static Map<String, AttributeDefinition> getUserDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("source", JMSBridgeDefinition.SOURCE_USER);
        result.put("target", JMSBridgeDefinition.TARGET_USER);
        result.put("default", BridgeDefinition.USER);
        return result;
    }

    private static Map<String, AttributeDefinition> getPasswordDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("source", JMSBridgeDefinition.SOURCE_PASSWORD);
        result.put("target", JMSBridgeDefinition.TARGET_PASSWORD);
        result.put("default", BridgeDefinition.PASSWORD);
        return result;
    }

    private static Map<String, AttributeDefinition> getConnectionFactoryDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("source", JMSBridgeDefinition.SOURCE_CONNECTION_FACTORY);
        result.put("target", JMSBridgeDefinition.TARGET_CONNECTION_FACTORY);
        return result;
    }

    private static Map<String, AttributeDefinition> getDestinationDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("source", JMSBridgeDefinition.SOURCE_DESTINATION);
        result.put("target", JMSBridgeDefinition.TARGET_DESTINATION);
        return result;
    }

    private static Map<String, AttributeDefinition> getContextDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("source", JMSBridgeDefinition.SOURCE_CONTEXT);
        result.put("target", JMSBridgeDefinition.TARGET_CONTEXT);
        return result;
    }

    private static Map<String, AttributeDefinition>  getInitialConnectAttemptsDefinitions() {
        final Map<String, AttributeDefinition> result = new HashMap<String, AttributeDefinition>();
        result.put("bridge", BridgeDefinition.INITIAL_CONNECT_ATTEMPTS);
        result.put("cluster", ClusterConnectionDefinition.INITIAL_CONNECT_ATTEMPTS);
        result.put("pooled", Pooled.INITIAL_CONNECT_ATTEMPTS);
        return result;
    }


}
