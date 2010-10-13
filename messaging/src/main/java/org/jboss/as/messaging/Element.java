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
 */
public enum Element {
   // must be first
   UNKNOWN(null),
   // Messaging 1.0 elements in alpha order
   ACCEPTORS("acceptors"),
   ADDRESS("address"),
   ADDRESS_SETTINGS("address-settings"),
   ASYNC_CONNECTION_EXECUTION_ENABLED("async-connection-execution-enabled"),
   BACKUP("backup"),
   BACKUP_CONNECTOR_REF("backup-connector-ref"),
   BINDINGS_DIRECTORY("bindings-directory"),
   BROADCAST_PERIOD("broadcast-period"),
   CLUSTER_PASSWORD("cluster-password"),
   CLUSTER_USER("cluster-user"),
   CLUSTERED("clustered"),
   CONNECTION_TTL_OVERRIDE("connection-ttl-override"),
   CONNECTOR_REF("connector-ref"),
   CREATE_BINDINGS_DIR("create-bindings-dir"),
   CREATE_JOURNAL_DIR("create-journal-dir"),
   DURABLE("durable"),
   FILE_DEPLOYMENT_ENABLED("file-deployment-enabled"),
   GROUP_ADDRESS("group-address"),
   GROUP_PORT("group-port"),
   GROUPING_HANDLER("grouping-handler"),
   ID_CACHE_SIZE("id-cache-size"),

   IN_VM_ACCEPTOR("in-vm-acceptor"),
   IN_VM_CONNECTOR("in-vm-connector"),

   JMX_DOMAIN("jmx-domain"),
   JMX_MANAGEMENT_ENABLED("jmx-management-enabled"),
   JOURNAL_BUFFER_SIZE("journal-buffer-size"),
   JOURNAL_BUFFER_TIMEOUT("journal-buffer-timeout"),
   JOURNAL_COMPACT_MIN_FILES("journal-compact-min-files"),
   JOURNAL_COMPACT_PERCENTAGE("journal-compact-percentage"),
   JOURNAL_DIRECTORY("journal-directory"),
   JOURNAL_FILE_SIZE("journal-file-size"),
   JOURNAL_MAX_IO("journal-max-io"),
   JOURNAL_MIN_FILES("journal-min-files"),
   JOURNAL_SYNC_NON_TRANSACTIONAL("journal-sync-non-transactional"),
   JOURNAL_SYNC_TRANSACTIONAL("journal-sync-transactional"),
   JOURNAL_TYPE("journal-type"),
   LARGE_MESSAGES_DIRECTORY("large-messages-directory"),
   LOCAL_BIND_ADDRESS("local-bind-address"),
   LOCAL_BIND_PORT("local-bind-port"),
   LOG_JOURNAL_WRITE_RATE("log-journal-write-rate"),
   MANAGEMENT_ADDRESS("management-address"),
   MANAGEMENT_NOTIFICATION_ADDRESS("management-notification-address"),
   MEMORY_MEASURE_INTERVAL("memory-measure-interval"),
   MEMORY_WARNING_THRESHOLD("memory-warning-threshold"),
   MESSAGE_COUNTER_ENABLED("message-counter-enabled"),
   MESSAGE_COUNTER_MAX_DAY_HISTORY("message-counter-max-day-history"),
   MESSAGE_COUNTER_SAMPLE_PERIOD("message-counter-sample-period"),
   MESSAGE_EXPIRY_SCAN_PERIOD("message-expiry-scan-period"),
   MESSAGE_EXPIRY_THREAD_PRIORITY("message-expiry-thread-priority"),

   NETTY_ACCEPTOR("netty-acceptor"),
   NETTY_CONNECTOR("netty-connector"),

   PAGING_DIRECTORY("paging-directory"),
   PERF_BLAST_PAGES("perf-blast-pages"),
   PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY("persist-delivery-count-before-delivery"),
   PERSIST_ID_CACHE("persist-id-cache"),
   PERSISTENCE_ENABLED("persistence-enabled"),

   QUEUES("queues"),
   QUEUE("queue"),

   REFRESH_TIMEOUT("refresh-timeout"),
   REMOTING_INTERCEPTORS("remoting-interceptors"),
   RUN_SYNC_SPEED_TEST("run-sync-speed-test"),
   SECURITY_ENABLED("security-enabled"),
   SECURITY_INVALIDATION_INTERVAL("security-invalidation-interval"),
   SECURITY_SETTINGS("security-settings"),
   SERVER_DUMP_INTERVAL("server-dump-interval"),
   SHARED_STORE("shared-store"),
   SUBSYSTEM("subsystem"),
   TRANSACTION_TIMEOUT("transaction-timeout"),
   TRANSACTION_TIMEOUT_SCAN_PERIOD("transaction-timeout-scan-period"),
   WILD_CARD_ROUTING_ENABLED("wild-card-routing-enabled"),
   // connectors/acceptors
   ACCEPTOR("acceptor"),
   CONNECTORS("connectors"),
   CONNECTOR("connector"),

   FACTORY_CLASS("factory-class"),
   FILTER("filter"),
   PARAM("param"),

   // Security Parsing
   SECURITY_SETTING("security-setting"),
   PERMISSION_ELEMENT_NAME("permission"),

   // Address parsing
   ADDRESS_SETTING("address-setting"),
   DEAD_LETTER_ADDRESS_NODE_NAME("dead-letter-address"),
   EXPIRY_ADDRESS_NODE_NAME("expiry-address"),
   REDELIVERY_DELAY_NODE_NAME("redelivery-delay"),
   MAX_DELIVERY_ATTEMPTS("max-delivery-attempts"),
   MAX_SIZE_BYTES_NODE_NAME("max-size-bytes"),
   ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME("address-full-policy"),
   PAGE_SIZE_BYTES_NODE_NAME("page-size-bytes"),
   MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME("message-counter-history-day-limit"),
   LVQ_NODE_NAME("last-value-queue"),
   REDISTRIBUTION_DELAY_NODE_NAME("redistribution-delay"),
   SEND_TO_DLA_ON_NO_ROUTE("send-to-dla-on-no-route"),
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
