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

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author scott.stark@jboss.org
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
enum Element {
   // must be first
   UNKNOWN(null),
   // Messaging 1.0 elements in alpha order
   ACCEPTORS(CommonAttributes.ACCEPTORS),
   ADDRESS(CommonAttributes.ADDRESS),
   ADDRESS_SETTINGS(CommonAttributes.ADDRESS_SETTINGS),
   ASYNC_CONNECTION_EXECUTION_ENABLED(CommonAttributes.ASYNC_CONNECTION_EXECUTION_ENABLED),
   BACKUP(CommonAttributes.BACKUP),
   BINDINGS_DIRECTORY(CommonAttributes.BINDINGS_DIRECTORY),
   BROADCAST_PERIOD(CommonAttributes.BROADCAST_PERIOD),
   CLUSTER_PASSWORD(CommonAttributes.CLUSTER_PASSWORD),
   CLUSTER_USER(CommonAttributes.CLUSTER_USER),
   CLUSTERED(CommonAttributes.CLUSTERED),
   CONNECTION_TTL_OVERRIDE(CommonAttributes.CONNECTION_TTL_OVERRIDE),
   CONNECTOR_REF(CommonAttributes.CONNECTOR_REF),
   CORE_QUEUES(CommonAttributes.CORE_QUEUES),
   CREATE_BINDINGS_DIR(CommonAttributes.CREATE_BINDINGS_DIR),
   CREATE_JOURNAL_DIR(CommonAttributes.CREATE_JOURNAL_DIR),
   DURABLE(CommonAttributes.DURABLE),
   FILE_DEPLOYMENT_ENABLED(CommonAttributes.FILE_DEPLOYMENT_ENABLED),
   GROUP_ADDRESS(CommonAttributes.GROUP_ADDRESS),
   GROUP_PORT(CommonAttributes.GROUP_PORT),
   GROUPING_HANDLER(CommonAttributes.GROUPING_HANDLER),
   ID_CACHE_SIZE(CommonAttributes.ID_CACHE_SIZE),
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
   LOCAL_BIND_ADDRESS(CommonAttributes.LOCAL_BIND_ADDRESS),
   LOCAL_BIND_PORT(CommonAttributes.LOCAL_BIND_PORT),
   LOG_JOURNAL_WRITE_RATE(CommonAttributes.LOG_JOURNAL_WRITE_RATE),
   MANAGEMENT_ADDRESS(CommonAttributes.MANAGEMENT_ADDRESS),
   MANAGEMENT_NOTIFICATION_ADDRESS(CommonAttributes.MANAGEMENT_NOTIFICATION_ADDRESS),
   MEMORY_MEASURE_INTERVAL(CommonAttributes.MEMORY_MEASURE_INTERVAL),
   MEMORY_WARNING_THRESHOLD(CommonAttributes.MEMORY_WARNING_THRESHOLD),
   MESSAGE_COUNTER_ENABLED(CommonAttributes.MESSAGE_COUNTER_ENABLED),
   MESSAGE_COUNTER_MAX_DAY_HISTORY(CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY),
   MESSAGE_COUNTER_SAMPLE_PERIOD(CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD),
   MESSAGE_EXPIRY_SCAN_PERIOD(CommonAttributes.MESSAGE_EXPIRY_SCAN_PERIOD),
   MESSAGE_EXPIRY_THREAD_PRIORITY(CommonAttributes.MESSAGE_EXPIRY_THREAD_PRIORITY),
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
   RUN_SYNC_SPEED_TEST(CommonAttributes.RUN_SYNC_SPEED_TEST),
   SECURITY_ENABLED(CommonAttributes.SECURITY_ENABLED),
   SECURITY_INVALIDATION_INTERVAL(CommonAttributes.SECURITY_INVALIDATION_INTERVAL),
   SECURITY_SETTINGS(CommonAttributes.SECURITY_SETTINGS),
   SERVER_DUMP_INTERVAL(CommonAttributes.SERVER_DUMP_INTERVAL),
   SHARED_STORE(CommonAttributes.SHARED_STORE),
   SUBSYSTEM(CommonAttributes.SUBSYSTEM),
   TRANSACTION_TIMEOUT(CommonAttributes.TRANSACTION_TIMEOUT),
   TRANSACTION_TIMEOUT_SCAN_PERIOD(CommonAttributes.TRANSACTION_TIMEOUT_SCAN_PERIOD),
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
   PAGE_SIZE_BYTES_NODE_NAME(CommonAttributes.PAGE_SIZE_BYTES_NODE_NAME),
   MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME(CommonAttributes.MESSAGE_COUNTER_HISTORY_DAY_LIMIT),
   LVQ_NODE_NAME(CommonAttributes.LVQ),
   REDISTRIBUTION_DELAY_NODE_NAME(CommonAttributes.REDISTRIBUTION_DELAY),
   SEND_TO_DLA_ON_NO_ROUTE(CommonAttributes.SEND_TO_DLA_ON_NO_ROUTE),

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
   CONNECTION_FACTORIES(CommonAttributes.CONNECTION_FACTORIES),
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
   JMS_DESTINATIONS(CommonAttributes.JMS_DESTINATIONS),
   JMS_TOPIC(CommonAttributes.JMS_TOPIC),
   JMS_QUEUE(CommonAttributes.JMS_QUEUE),
   LOAD_BALANCING_CLASS_NAME(CommonAttributes.LOAD_BALANCING_CLASS_NAME),
   MAX_RETRY_INTERVAL(CommonAttributes.MAX_RETRY_INTERVAL),
   MIN_LARGE_MESSAGE_SIZE(CommonAttributes.MIN_LARGE_MESSAGE_SIZE),
   PRE_ACK(CommonAttributes.PRE_ACK),
   PRODUCER_WINDOW_SIZE(CommonAttributes.PRODUCER_WINDOW_SIZE),
   PRODUCER_MAX_RATE(CommonAttributes.PRODUCER_MAX_RATE),
   RECONNECT_ATTEMPTS(CommonAttributes.RECONNECT_ATTEMPTS),
   RETRY_INTERVAL(CommonAttributes.RETRY_INTERVAL),
   RETRY_INTERVAL_MULTIPLIER(CommonAttributes.RETRY_INTERVAL_MULTIPLIER),
   SELECTOR(CommonAttributes.SELECTOR),
   SCHEDULED_THREAD_POOL_MAX_SIZE(CommonAttributes.SCHEDULED_THREAD_POOL_MAX_SIZE),
   THREAD_POOL_MAX_SIZE(CommonAttributes.THREAD_POOL_MAX_SIZE),
   TRANSACTION_BATH_SIZE(CommonAttributes.TRANSACTION_BATCH_SIZE),
   USE_GLOBAL_POOLS(CommonAttributes.USE_GLOBAL_POOLS),
   POOLED_CONNECTION_FACTORY(CommonAttributes.POOLED_CONNECTION_FACTORY),
   TRANSACTION(CommonAttributes.TRANSACTION),
   MODE(CommonAttributes.MODE),
   INBOUND_CONFIG(CommonAttributes.INBOUND_CONFIG),
   USE_JNDI(CommonAttributes.USE_JNDI),
   JNDI_PARAMS(CommonAttributes.JNDI_PARAMS),
   USE_LOCAL_TX(CommonAttributes.USE_LOCAL_TX),
   SETUP_ATTEMPTS(CommonAttributes.SETUP_ATTEMPTS),
   SETUP_INTERVAL(CommonAttributes.SETUP_INTERVAL),
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
