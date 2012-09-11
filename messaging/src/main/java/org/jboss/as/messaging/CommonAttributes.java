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

import static org.hornetq.api.core.client.HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD;
import static org.hornetq.api.core.client.HornetQClient.DEFAULT_CONNECTION_TTL;
import static org.hornetq.api.core.client.HornetQClient.DEFAULT_MAX_RETRY_INTERVAL;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_MEMORY_MEASURE_INTERVAL;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_MEMORY_WARNING_THRESHOLD;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.PERCENTAGE;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.jboss.dmr.ModelType.BIG_DECIMAL;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.config.impl.FileConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.messaging.jms.SelectorAttribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {

    SimpleAttributeDefinition ALLOW_FAILBACK = new SimpleAttributeDefinition("allow-failback",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ALLOW_AUTO_FAILBACK), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition ASYNC_CONNECTION_EXECUTION_ENABLED = new SimpleAttributeDefinition(
            "async-connection-execution-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ASYNC_CONNECTION_EXECUTION_ENABLED), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition BACKUP = new SimpleAttributeDefinition("backup",
            new ModelNode().set(ConfigurationImpl.DEFAULT_BACKUP), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    AttributeDefinition CALL_TIMEOUT = create("call-timeout", LONG)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_CALL_TIMEOUT)).setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true).build();

    SimpleAttributeDefinition CHECK_PERIOD = create("check-period", LONG)
            .setDefaultValue(new ModelNode()
            .set(DEFAULT_CLIENT_FAILURE_CHECK_PERIOD))
            .setAllowNull(true)
            .setMeasurementUnit(MILLISECONDS)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    AttributeDefinition CLIENT_ID = create("client-id", ModelType.STRING)
            .setAllowNull(true)
            .build();

    SimpleAttributeDefinition CLUSTERED = new SimpleAttributeDefinition("clustered",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTERED), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CLUSTER_PASSWORD = new SimpleAttributeDefinition("cluster-password", "cluster-password",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_PASSWORD), ModelType.STRING, true, true, null,
            RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CLUSTER_USER = new SimpleAttributeDefinition("cluster-user", "cluster-user",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_USER), ModelType.STRING, true, true, null,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    AttributeDefinition CONSUMER_COUNT = create("consumer-count", INT)
            .setStorageRuntime()
            .build();

    SimpleAttributeDefinition BRIDGE_CONFIRMATION_WINDOW_SIZE = create("confirmation-window-size", INT)
            .setDefaultValue(new ModelNode().set(FileConfiguration.DEFAULT_CONFIRMATION_WINDOW_SIZE))
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition CONNECTION_TTL = create("connection-ttl", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_CONNECTION_TTL))
            .setAllowNull(true)
            .setMeasurementUnit(MILLISECONDS)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition CONNECTION_TTL_OVERRIDE = new SimpleAttributeDefinition("connection-ttl-override",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CONNECTION_TTL_OVERRIDE), ModelType.LONG, true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CONSUMER_MAX_RATE = new SimpleAttributeDefinition("consumer-max-rate",
            new ModelNode().set(HornetQClient.DEFAULT_CONSUMER_MAX_RATE), ModelType.INT, true, MeasurementUnit.PER_SECOND);

    SimpleAttributeDefinition CREATE_BINDINGS_DIR = new SimpleAttributeDefinition("create-bindings-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_BINDINGS_DIR), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition CREATE_JOURNAL_DIR = new SimpleAttributeDefinition("create-journal-dir",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CREATE_JOURNAL_DIR), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition DEAD_LETTER_ADDRESS = new SimpleAttributeDefinition("dead-letter-address", ModelType.STRING, true);

    AttributeDefinition DELIVERING_COUNT = create("delivering-count", INT)
            .setStorageRuntime()
            .build();

    SimpleAttributeDefinition DISCOVERY_GROUP_NAME = create("discovery-group-name", ModelType.STRING)
            .setAllowNull(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition DURABLE = create("durable", BOOLEAN)
            .setDefaultValue(new ModelNode().set(true))
            .setAllowNull(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition FACTORY_CLASS = create("factory-class", ModelType.STRING)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition EXPIRY_ADDRESS = new SimpleAttributeDefinition("expiry-address", ModelType.STRING, true);

    SimpleAttributeDefinition FAILBACK_DELAY = new SimpleAttributeDefinition("failback-delay",
            new ModelNode().set(ConfigurationImpl.DEFAULT_FAILBACK_DELAY), ModelType.LONG, true, MeasurementUnit.MILLISECONDS,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = create("failover-on-server-shutdown", ModelType.BOOLEAN)
            .setAllowNull(true)
            .build();

    SimpleAttributeDefinition FAILOVER_ON_SHUTDOWN = new SimpleAttributeDefinition("failover-on-shutdown",
            new ModelNode().set(false /*
                                       * TODO should be ConfigurationImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN but field is
                                       * private
                                       */), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition FILTER = create("filter", ModelType.STRING)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition GROUP_ADDRESS = create("group-address", ModelType.STRING)
            .setDefaultValue(null)
            .setAllowNull(false)
            .setAlternatives("socket-binding")
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition GROUP_PORT = create("group-port", INT)
            .setDefaultValue(null)
            .setAllowNull(false)
            .setAlternatives("socket-binding")
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition HA = create("ha", BOOLEAN)
            .setDefaultValue(new ModelNode()
            .set(HornetQClient.DEFAULT_HA))
            .setAllowNull(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition ID_CACHE_SIZE = new SimpleAttributeDefinition("id-cache-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_ID_CACHE_SIZE), ModelType.INT, true, MeasurementUnit.NONE,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JMX_DOMAIN = new SimpleAttributeDefinition("jmx-domain",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JMX_DOMAIN), ModelType.STRING, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JMX_MANAGEMENT_ENABLED = new SimpleAttributeDefinition("jmx-management-enabled",
            new ModelNode().set(false), ModelType.BOOLEAN, true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_BUFFER_SIZE = new SimpleAttributeDefinition("journal-buffer-size", ModelType.LONG, true,
            MeasurementUnit.BYTES, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_BUFFER_TIMEOUT = new SimpleAttributeDefinition("journal-buffer-timeout", ModelType.LONG,
            true, MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_COMPACT_MIN_FILES = new SimpleAttributeDefinition("journal-compact-min-files",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_COMPACT_MIN_FILES), ModelType.INT, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_COMPACT_PERCENTAGE = new SimpleAttributeDefinition("journal-compact-percentage",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_COMPACT_PERCENTAGE), ModelType.INT, true,
            MeasurementUnit.PERCENTAGE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_FILE_SIZE = new SimpleAttributeDefinition("journal-file-size",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_FILE_SIZE), ModelType.LONG, true, MeasurementUnit.BYTES,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_MAX_IO = new SimpleAttributeDefinition("journal-max-io", ModelType.INT, true,
            MeasurementUnit.NONE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_MIN_FILES = new SimpleAttributeDefinition("journal-min-files",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_MIN_FILES), ModelType.INT, true, MeasurementUnit.NONE,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_SYNC_NON_TRANSACTIONAL = new SimpleAttributeDefinition("journal-sync-non-transactional",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_NON_TRANSACTIONAL), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_SYNC_TRANSACTIONAL = new SimpleAttributeDefinition("journal-sync-transactional",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_TRANSACTIONAL), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition JOURNAL_TYPE = new SimpleAttributeDefinition("journal-type", "journal-type",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_TYPE.toString()), ModelType.STRING, true, false,
            MeasurementUnit.NONE, JournalTypeValidator.INSTANCE, null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    LiveConnectorRefAttribute LIVE_CONNECTOR_REF = LiveConnectorRefAttribute.INSTANCE;

    SimpleAttributeDefinition LOCAL_BIND_ADDRESS = create("local-bind-address", ModelType.STRING)
            .setDefaultValue(null)
            .setAllowNull(true)
            .setAlternatives("socket-binding")
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition LOCAL_BIND_PORT = create("local-bind-port", INT)
            .setDefaultValue(new ModelNode().set(-1))
            .setAllowNull(true)
            .setAlternatives("socket-binding")
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition LOG_JOURNAL_WRITE_RATE = new SimpleAttributeDefinition("log-journal-write-rate",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_LOG_WRITE_RATE), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MANAGEMENT_ADDRESS = new SimpleAttributeDefinition("management-address",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_ADDRESS.toString()), ModelType.STRING, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MANAGEMENT_NOTIFICATION_ADDRESS = new SimpleAttributeDefinition(
            "management-notification-address", new ModelNode().set(ConfigurationImpl.DEFAULT_MANAGEMENT_NOTIFICATION_ADDRESS
                    .toString()), ModelType.STRING, true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    AttributeDefinition MAX_RETRY_INTERVAL = create("max-retry-interval", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_MAX_RETRY_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition MEMORY_MEASURE_INTERVAL = create("memory-measure-interval", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_MEMORY_MEASURE_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition MEMORY_WARNING_THRESHOLD = create("memory-warning-threshold", INT)
            .setDefaultValue(new ModelNode().set(DEFAULT_MEMORY_WARNING_THRESHOLD))
            .setMeasurementUnit(PERCENTAGE)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    AttributeDefinition MESSAGE_COUNT = create("message-count", LONG)
            .setStorageRuntime()
            .build();

    SimpleAttributeDefinition MESSAGE_COUNTER_ENABLED = new SimpleAttributeDefinition("message-counter-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_ENABLED), ModelType.BOOLEAN, true);

    SimpleAttributeDefinition MESSAGE_COUNTER_MAX_DAY_HISTORY = new SimpleAttributeDefinition(
            "message-counter-max-day-history", new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_MAX_DAY_HISTORY),
            ModelType.INT, true, MeasurementUnit.DAYS);

    SimpleAttributeDefinition MESSAGE_COUNTER_SAMPLE_PERIOD = new SimpleAttributeDefinition("message-counter-sample-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_COUNTER_SAMPLE_PERIOD), ModelType.LONG, true,
            MeasurementUnit.MILLISECONDS);

    SimpleAttributeDefinition MESSAGE_EXPIRY_SCAN_PERIOD = new SimpleAttributeDefinition("message-expiry-scan-period",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_EXPIRY_SCAN_PERIOD), ModelType.LONG, true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition MESSAGE_EXPIRY_THREAD_PRIORITY = new SimpleAttributeDefinition("message-expiry-thread-priority",
            new ModelNode().set(ConfigurationImpl.DEFAULT_MESSAGE_EXPIRY_THREAD_PRIORITY), ModelType.INT, true,
            MeasurementUnit.NONE, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    AttributeDefinition MESSAGES_ADDED = create("messages-added", LONG)
            .setStorageRuntime()
            .build();

    AttributeDefinition MIN_LARGE_MESSAGE_SIZE = create("min-large-message-size", INT)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE))
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition PAGE_MAX_CONCURRENT_IO = create("page-max-concurrent-io", INT)
            .setDefaultValue(new ModelNode()
            .set(ConfigurationImpl.DEFAULT_MAX_CONCURRENT_PAGE_IO))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition PATH = create("path", ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition PAUSED = create("paused", BOOLEAN)
            .setStorageRuntime()
            .build();

    SimpleAttributeDefinition PERF_BLAST_PAGES = new SimpleAttributeDefinition("perf-blast-pages",
            new ModelNode().set(ConfigurationImpl.DEFAULT_JOURNAL_PERF_BLAST_PAGES), ModelType.INT, true, MeasurementUnit.NONE,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY = new SimpleAttributeDefinition(
            "persist-delivery-count-before-delivery",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition PERSISTENCE_ENABLED = new SimpleAttributeDefinition("persistence-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSISTENCE_ENABLED), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition PERSIST_ID_CACHE = new SimpleAttributeDefinition("persist-id-cache",
            new ModelNode().set(ConfigurationImpl.DEFAULT_PERSIST_ID_CACHE), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    AttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_RECONNECT_ATTEMPTS))
            .setAllowNull(true)
            .build();

    SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinition("relative-to", ModelType.STRING, true);


    PrimitiveListAttributeDefinition REMOTING_INTERCEPTORS = new PrimitiveListAttributeDefinition.Builder(CommonAttributes.REMOTING_INTERCEPTORS_STRING, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .setMaxSize(Integer.MAX_VALUE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setAttributeMarshaller(new AttributeMarshaller() {
                @Override
                public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                    if (resourceModel.hasDefined(attribute.getName())) {
                        List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                        if (list.size() > 0) {
                            writer.writeStartElement(attribute.getXmlName());

                            for (ModelNode child : list) {
                                writer.writeStartElement(Element.CLASS_NAME.getLocalName());
                                writer.writeCharacters(child.asString());
                                writer.writeEndElement();
                            }

                            writer.writeEndElement();
                        }
                    }
                }
            })
            .build();


    AttributeDefinition RETRY_INTERVAL = create("retry-interval", LONG)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", BIG_DECIMAL)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER))
            .setAllowNull(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition RUN_SYNC_SPEED_TEST = new SimpleAttributeDefinition("run-sync-speed-test",
            new ModelNode().set(ConfigurationImpl.DEFAULT_RUN_SYNC_SPEED_TEST), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    AttributeDefinition SCHEDULED_COUNT = create("scheduled-count", LONG)
            .setStorageRuntime()
            .build();

    AttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = create("scheduled-thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices().build();

    SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinition("security-domain", new ModelNode().set("other"),
            ModelType.STRING, true, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SECURITY_ENABLED = new SimpleAttributeDefinition("security-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_ENABLED), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SECURITY_INVALIDATION_INTERVAL = new SimpleAttributeDefinition("security-invalidation-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SECURITY_INVALIDATION_INTERVAL), ModelType.LONG, true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SelectorAttribute SELECTOR = SelectorAttribute.SELECTOR;

    SimpleAttributeDefinition SERVER_DUMP_INTERVAL = new SimpleAttributeDefinition("server-dump-interval",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SERVER_DUMP_INTERVAL), ModelType.LONG, true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SHARED_STORE = new SimpleAttributeDefinition("shared-store",
            new ModelNode().set(ConfigurationImpl.DEFAULT_SHARED_STORE), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition SOCKET_BINDING_ALTERNATIVE = create("socket-binding", ModelType.STRING)
            .setDefaultValue(null)
            .setAllowNull(false)
            .setAlternatives(GROUP_ADDRESS.getName(),
                            GROUP_PORT.getName(),
                            LOCAL_BIND_ADDRESS.getName(),
                            LOCAL_BIND_PORT.getName())
            .setRestartAllServices()
            .build();

    AttributeDefinition TEMPORARY = create("temporary", BOOLEAN)
            .setStorageRuntime()
            .build();

    AttributeDefinition THREAD_POOL_MAX_SIZE = create("thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(DEFAULT_THREAD_POOL_MAX_SIZE))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition TRANSACTION_TIMEOUT = new SimpleAttributeDefinition("transaction-timeout",
            new ModelNode().set(ConfigurationImpl.DEFAULT_TRANSACTION_TIMEOUT), ModelType.LONG, true,
            MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition TRANSACTION_TIMEOUT_SCAN_PERIOD = new SimpleAttributeDefinition(
            "transaction-timeout-scan-period", new ModelNode().set(ConfigurationImpl.DEFAULT_TRANSACTION_TIMEOUT_SCAN_PERIOD),
            ModelType.LONG, true, MeasurementUnit.MILLISECONDS, AttributeAccess.Flag.RESTART_ALL_SERVICES);

    SimpleAttributeDefinition TRANSFORMER_CLASS_NAME = create("transformer-class-name", ModelType.STRING)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition USER = new SimpleAttributeDefinition("user", "user",
            new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_USER), ModelType.STRING, true, true, null);

    SimpleAttributeDefinition VALUE = new SimpleAttributeDefinition("value", ModelType.STRING, false);

    SimpleAttributeDefinition WILD_CARD_ROUTING_ENABLED = new SimpleAttributeDefinition("wild-card-routing-enabled",
            new ModelNode().set(ConfigurationImpl.DEFAULT_WILDCARD_ROUTING_ENABLED), ModelType.BOOLEAN, true,
            AttributeAccess.Flag.RESTART_ALL_SERVICES);

    String ACCEPTOR = "acceptor";
    String ACCEPTORS = "acceptors";
    String ADDRESS = "address";
    String ADDRESS_SETTING = "address-setting";
    String ADDRESS_SETTINGS = "address-settings";
    String BINDING_NAMES = "binding-names";
    String BINDINGS_DIRECTORY = "bindings-directory";
    String BRIDGE = "bridge";
    String BRIDGES = "bridges";
    String BROADCAST_GROUP = "broadcast-group";
    String BROADCAST_GROUPS = "broadcast-groups";
    String CLASS_NAME = "class-name";
    String CLUSTER_CONNECTION = "cluster-connection";
    String CLUSTER_CONNECTIONS = "cluster-connections";
    String CONNECTION_FACTORY = "connection-factory";
    String CONNECTOR = "connector";
    String CONNECTORS = "connectors";
    String CONNECTOR_NAME = "connector-name";
    String CONNECTOR_REF_STRING = "connector-ref";
    String CONNECTOR_SERVICE = "connector-service";
    String CONNECTOR_SERVICES = "connector-services";
    String CORE_ADDRESS = "core-address";
    String CORE_QUEUE = "core-queue";
    String CORE_QUEUES = "core-queues";
    String DEFAULT = "default";
    String DESTINATION = "destination";
    String DISCOVERY_GROUP = "discovery-group";
    String DISCOVERY_GROUPS = "discovery-groups";
    String DISCOVERY_GROUP_REF = "discovery-group-ref";
    String DIVERT = "divert";
    String DIVERTS = "diverts";
    String DURABLE_MESSAGE_COUNT = "durable-message-count";
    String DURABLE_SUBSCRIPTION_COUNT = "durable-subscription-count";
    String ENTRIES_STRING = "entries";
    String ENTRY = "entry";
    String FILE_DEPLOYMENT_ENABLED = "file-deployment-enabled";
    String GROUPING_HANDLER = "grouping-handler";
    String ID = "id";
    String IN_VM_ACCEPTOR = "in-vm-acceptor";
    String IN_VM_CONNECTOR = "in-vm-connector";
    String JMS_BRIDGE = "jms-bridge";
    String JMS_CONNECTION_FACTORIES = "jms-connection-factories";
    String JMS_DESTINATIONS = "jms-destinations";
    String JMS_QUEUE = "jms-queue";
    String JMS_TOPIC = "jms-topic";
    String JNDI_BINDING = "jndi-binding";
    String JOURNAL_DIRECTORY = "journal-directory";
    String KEY = "key";
    String INBOUND_CONFIG = "inbound-config";
    String LARGE_MESSAGES_DIRECTORY = "large-messages-directory";
    String LAST_VALUE_QUEUE = "last-value=queue";
    String LIVE_CONNECTOR_REF_STRING = "live-connector-ref";
    String LOCAL = "local";
    String LOCAL_TX = "LocalTransaction";
    String MANAGE_XML_NAME = "manage";
    String MATCH = "match";
    String MODE = "mode";
    String NAME = "name";
    String NETTY_ACCEPTOR = "netty-acceptor";
    String NETTY_CONNECTOR = "netty-connector";
    String NONE = "none";
    String NON_DURABLE_MESSAGE_COUNT = "non-durable-message-count";
    String NON_DURABLE_SUBSCRIPTION_COUNT = "non-durable-subscription-count";
    String NO_TX = "NoTransaction";
    String NUMBER_OF_BYTES_PER_PAGE = "number-of-bytes-per-page";
    String NUMBER_OF_PAGES = "number-of-pages";

    String PAGING_DIRECTORY = "paging-directory";
    String PARAM = "param";
    String PERMISSION_ELEMENT_NAME = "permission";
    String POOLED_CONNECTION_FACTORY = "pooled-connection-factory";
    String QUEUE = "queue";
    String QUEUE_NAME = "queue-name";
    String QUEUE_NAMES = "queue-names";
    String REMOTING_INTERCEPTORS_STRING = "remoting-interceptors";
    String REMOTE_ACCEPTOR = "remote-acceptor";
    String REMOTE_CONNECTOR = "remote-connector";
    String REMOTING_INTERCEPTOR = "remoting-interceptor";
    String RESOURCE_ADAPTER = "resource-adapter";
    String ROLE = "role";
    String ROLES_ATTR_NAME = "roles";
    String SECURITY_ROLE = "security-role";
    String SECURITY_SETTING = "security-setting";
    String SECURITY_SETTINGS = "security-settings";
    String SOCKET_BINDING = "socket-binding";
    String HORNETQ_SERVER = "hornetq-server";
    String STARTED = "started";
    String STATIC_CONNECTORS = "static-connectors";
    String STRING = "string";
    String SUBSCRIPTION_COUNT = "subscription-count";
    String SUBSYSTEM = "subsystem";
    String TOPIC_ADDRESS = "topic-address";
    String TYPE_ATTR_NAME = "type";
    String VERSION = "version";
    String XA = "xa";
    String XA_TX = "XATransaction";

    AttributeDefinition[] SIMPLE_ROOT_RESOURCE_ATTRIBUTES = { CLUSTERED, PERSISTENCE_ENABLED, SCHEDULED_THREAD_POOL_MAX_SIZE,
            THREAD_POOL_MAX_SIZE, SECURITY_DOMAIN, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL, WILD_CARD_ROUTING_ENABLED,
            MANAGEMENT_ADDRESS, MANAGEMENT_NOTIFICATION_ADDRESS, CLUSTER_USER, CLUSTER_PASSWORD, JMX_MANAGEMENT_ENABLED,
            JMX_DOMAIN, MESSAGE_COUNTER_ENABLED, MESSAGE_COUNTER_SAMPLE_PERIOD, MESSAGE_COUNTER_MAX_DAY_HISTORY,
            CONNECTION_TTL_OVERRIDE, ASYNC_CONNECTION_EXECUTION_ENABLED, TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_SCAN_PERIOD,
            MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY, ID_CACHE_SIZE, PERSIST_ID_CACHE, REMOTING_INTERCEPTORS,
            BACKUP, ALLOW_FAILBACK, FAILBACK_DELAY, FAILOVER_ON_SHUTDOWN, SHARED_STORE, PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY,
            LIVE_CONNECTOR_REF, CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE, JOURNAL_BUFFER_TIMEOUT,
            JOURNAL_BUFFER_SIZE, JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE,
            JOURNAL_FILE_SIZE, JOURNAL_MIN_FILES, JOURNAL_COMPACT_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_MAX_IO,
            PERF_BLAST_PAGES, RUN_SYNC_SPEED_TEST, SERVER_DUMP_INTERVAL, MEMORY_WARNING_THRESHOLD, MEMORY_MEASURE_INTERVAL,
            PAGE_MAX_CONCURRENT_IO };

    AttributeDefinition[] SIMPLE_ROOT_RESOURCE_WRITE_ATTRIBUTES = { FAILOVER_ON_SHUTDOWN, MESSAGE_COUNTER_ENABLED,
            MESSAGE_COUNTER_MAX_DAY_HISTORY, MESSAGE_COUNTER_SAMPLE_PERIOD };
}
