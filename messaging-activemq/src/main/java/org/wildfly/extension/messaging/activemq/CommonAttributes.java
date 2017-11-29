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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.jboss.dmr.ModelType.BIG_DECIMAL;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.wildfly.extension.messaging.activemq.jms.Validators.noDuplicateElements;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {

    String ENTRIES = "entries";
    String MODULE = "module";
    String NAME = "name";

    AttributeDefinition CALL_TIMEOUT = create("call-timeout", LONG)
            .setDefaultValue(new ModelNode(ActiveMQClient.DEFAULT_CALL_TIMEOUT))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition CALL_FAILOVER_TIMEOUT = create("call-failover-timeout", LONG)
            // ActiveMQClient.DEFAULT_CALL_FAILOVER_TIMEOUT was changed from -1 to 30000 in ARTEMIS-255
            // we set it to 60s to leave more time for WildFly to failover
            .setDefaultValue(new ModelNode(60000L))
            .setRequired(false)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition CHECK_PERIOD = create("check-period", LONG)
            .setDefaultValue(new ModelNode(ActiveMQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD))
            .setRequired(false)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    SimpleAttributeDefinition CLIENT_ID = create("client-id", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition CONSUMER_COUNT = create("consumer-count", INT)
            .setStorageRuntime()
            .setRequired(false)
            .build();

    SimpleAttributeDefinition BRIDGE_CONFIRMATION_WINDOW_SIZE = create("confirmation-window-size", INT)
            .setDefaultValue(new ModelNode(FileConfiguration.DEFAULT_CONFIRMATION_WINDOW_SIZE))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition CONNECTION_TTL = create("connection-ttl", LONG)
            .setDefaultValue(new ModelNode().set(ActiveMQClient.DEFAULT_CONNECTION_TTL))
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setMeasurementUnit(MILLISECONDS)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition DEAD_LETTER_ADDRESS = create("dead-letter-address", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(ActiveMQAddressCorrector.CORRECTOR)
            .build();

    AttributeDefinition DELIVERING_COUNT = create("delivering-count", INT)
            .setStorageRuntime()
            .setUndefinedMetricValue(new ModelNode(0))
            .build();

    StringListAttributeDefinition DESTINATION_ENTRIES = new StringListAttributeDefinition.Builder(ENTRIES)
            .setRequired(true)
            .setListValidator(noDuplicateElements(new StringLengthValidator(1, false, true)))
            .setAllowExpression(true)
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setRestartAllServices()
            .build();

    StringListAttributeDefinition LEGACY_ENTRIES = new StringListAttributeDefinition.Builder("legacy-entries")
            .setRequired(false)
            .setListValidator(noDuplicateElements(new StringLengthValidator(1, false, true)))
            .setAllowExpression(true)
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition DURABLE = create("durable", BOOLEAN)
            .setDefaultValue(new ModelNode().set(true))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition FACTORY_CLASS = create("factory-class", ModelType.STRING)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition EXPIRY_ADDRESS = create("expiry-address", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(ActiveMQAddressCorrector.CORRECTOR)
            .build();

    SimpleAttributeDefinition FILTER = create("filter", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition HA = create("ha", BOOLEAN)
            .setDefaultValue(new ModelNode()
                    .set(ActiveMQClient.DEFAULT_HA))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    PropertiesAttributeDefinition PARAMS = new PropertiesAttributeDefinition.Builder("params", true)
            .setWrapXmlElement(false)
            .setAllowExpression(true)
            .setXmlName("param")
            .build();

    SimpleAttributeDefinition JGROUPS_STACK = create("jgroups-stack", ModelType.STRING)
            .setRequired(false)
            // do not allow expression as this may reference another resource
            .setAllowExpression(false)
            .setRequires("jgroups-channel")
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JGROUPS_CHANNEL = create("jgroups-channel", ModelType.STRING)
            .setRequired(false)
            // do not allow expression as this may reference another resource
            .setAllowExpression(false)
            .setAlternatives("socket-binding")
            .setRestartAllServices()
            .build();

    AttributeDefinition MAX_RETRY_INTERVAL = create("max-retry-interval", LONG)
            .setDefaultValue(new ModelNode(ActiveMQClient.DEFAULT_MAX_RETRY_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition MESSAGE_COUNT = create("message-count", LONG)
            .setStorageRuntime()
            .setUndefinedMetricValue(new ModelNode(0))
            .build();

    AttributeDefinition MESSAGES_ADDED = create("messages-added", LONG)
            .setStorageRuntime()
            .setUndefinedMetricValue(new ModelNode(0))
            .build();

    AttributeDefinition MIN_LARGE_MESSAGE_SIZE = create("min-large-message-size", INT)
            .setDefaultValue(new ModelNode(ActiveMQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition PAUSED = create("paused", BOOLEAN)
            .setStorageRuntime()
            .build();

    ObjectTypeAttributeDefinition CLASS = ObjectTypeAttributeDefinition.Builder.of("class",
            create(NAME, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            create(MODULE, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build())
            .build();

    ObjectListAttributeDefinition INCOMING_INTERCEPTORS = ObjectListAttributeDefinition.Builder.of("incoming-interceptors", CommonAttributes.CLASS)
            .setRequired(false)
            .setAllowExpression(false)
            .setMinSize(1)
            .setMaxSize(Integer.MAX_VALUE)
            .build();

    ObjectListAttributeDefinition OUTGOING_INTERCEPTORS = ObjectListAttributeDefinition.Builder.of("outgoing-interceptors", CommonAttributes.CLASS)
            .setRequired(false)
            .setAllowExpression(false)
            .setMinSize(1)
            .setMaxSize(Integer.MAX_VALUE)
            .build();

    AttributeDefinition RETRY_INTERVAL = create("retry-interval", LONG)
            .setDefaultValue(new ModelNode().set(ActiveMQClient.DEFAULT_RETRY_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", BIG_DECIMAL)
            .setDefaultValue(new ModelNode(ActiveMQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition SCHEDULED_COUNT = create("scheduled-count", LONG)
            .setStorageRuntime()
            .setUndefinedMetricValue(new ModelNode(0))
            .build();

    SimpleAttributeDefinition SELECTOR = create("selector", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition SOCKET_BINDING = create("socket-binding", ModelType.STRING)
            .setRequired(false)
            .setAlternatives(JGROUPS_CHANNEL.getName())
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    AttributeDefinition TEMPORARY = create("temporary", BOOLEAN)
            .setStorageRuntime()
            .build();

    SimpleAttributeDefinition TRANSFORMER_CLASS_NAME = create("transformer-class-name", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition USER = create("user", ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterUser()))
            .build();

    String ACCEPTOR = "acceptor";
    String ACCEPTORS = "acceptors";
    String ACTIVEMQ_ADDRESS = "activemq-address";
    String ADDRESS = "address";
    String ADDRESS_SETTING = "address-setting";
    String ADDRESS_SETTINGS = "address-settings";
    String BINDING_NAMES = "binding-names";
    String BINDINGS_DIRECTORY = "bindings-directory";
    String BRIDGE = "bridge";
    String BRIDGES = "bridges";
    String BROADCAST_GROUP = "broadcast-group";
    String BROADCAST_GROUPS = "broadcast-groups";
    String CHECK_FOR_LIVE_SERVER2 = "check-for-live-server";
    String CLASS_NAME = "class-name";
    String CLUSTER_CONNECTION = "cluster-connection";
    String CLUSTER_CONNECTIONS = "cluster-connections";
    String CLUSTER_NAME = "cluster-name";
    String COLOCATED = "colocated";
    String CONFIGURATION = "configuration";
    String CONNECTION_FACTORY = "connection-factory";
    String CONNECTOR = "connector";
    String CONNECTORS = "connectors";
    String CONNECTOR_NAME = "connector-name";
    String CONNECTOR_REF_STRING = "connector-ref";
    String CONNECTOR_SERVICE = "connector-service";
    String CONNECTOR_SERVICES = "connector-services";
    String CORE = "core";
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
    String ENABLED = "enabled";
    String ENTRY = "entry";
    String EXCLUDES = "excludes";
    String FILE_DEPLOYMENT_ENABLED = "file-deployment-enabled";
    String GROUP_NAME = "group-name";
    String GROUPING_HANDLER = "grouping-handler";
    String HA_POLICY = "ha-policy";
    String HOST = "host";
    String HTTP = "http";
    String HTTP_ACCEPTOR = "http-acceptor";
    String HTTP_CONNECTOR = "http-connector";
    String HTTP_LISTENER = "http-listener";
    String ID = "id";
    String IN_VM_ACCEPTOR = "in-vm-acceptor";
    String IN_VM_CONNECTOR = "in-vm-connector";
    String LEGACY = "legacy";
    String LEGACY_CONNECTION_FACTORY = "legacy-connection-factory";
    String JMS_BRIDGE = "jms-bridge";
    String JMS_CONNECTION_FACTORIES = "jms-connection-factories";
    String JMS_DESTINATION = "jms-destination";
    String JMS_DESTINATIONS = "jms-destinations";
    String JMS_QUEUE = "jms-queue";
    String JMS_TOPIC = "jms-topic";
    String JNDI_BINDING = "jndi-binding";
    String JOURNAL_DIRECTORY = "journal-directory";
    String KEY = "key";
    String INBOUND_CONFIG = "inbound-config";
    String LARGE_MESSAGES_DIRECTORY = "large-messages-directory";
    String LAST_VALUE_QUEUE = "last-value=queue";
    String LIVE_ONLY = "live-only";
    String LOCAL = "local";
    String LOCAL_TX = "LocalTransaction";
    String MANAGE_XML_NAME = "manage";
    String MASTER = "master";
    String MATCH = "match";
    String MESSAGE_ID = "message-id";
    String MODE = "mode";
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
    String PASSWORD = "password";
    String PERMISSION_ELEMENT_NAME = "permission";
    String POOLED_CONNECTION_FACTORY = "pooled-connection-factory";
    String QUEUE = "queue";
    String QUEUE_NAME = "queue-name";
    String QUEUE_NAMES = "queue-names";
    String REMOTE_ACCEPTOR = "remote-acceptor";
    String REMOTE_CONNECTOR = "remote-connector";
    String REPLICATION = "replication";
    String REPLICATION_COLOCATED = "replication-colocated";
    String REPLICATION_MASTER = "replication-master";
    String REPLICATION_SLAVE = "replication-slave";
    String RESOURCE_ADAPTER = "resource-adapter";
    String RESOLVE_ADDRESS_SETTING = "resolve-address-setting";
    String ROLE = "role";
    String ROLES_ATTR_NAME = "roles";
    String RUNTIME_QUEUE = "runtime-queue";
    String SCALE_DOWN = "scale-down";
    String SECURITY_ROLE = "security-role";
    String SECURITY_SETTING = "security-setting";
    String SECURITY_SETTINGS = "security-settings";
    String SERVER = "server";
    String SERVLET_PATH = "servlet-path";
    String SHARED_STORE = "shared-store";
    String SHARED_STORE_COLOCATED = "shared-store-colocated";
    String SHARED_STORE_MASTER = "shared-store-master";
    String SHARED_STORE_SLAVE = "shared-store-slave";
    String SLAVE = "slave";
    String SOURCE = "source";
    String STARTED = "started";
    String STATIC_CONNECTORS = "static-connectors";
    String STRING = "string";
    String SUBSCRIPTION_COUNT = "subscription-count";
    String SUBSYSTEM = "subsystem";
    String TARGET = "target";
    String TOPIC_ADDRESS = "topic-address";
    String TYPE_ATTR_NAME = "type";
    String USE_INVM = "use-invm";
    String USE_SERVLET = "use-servlet";
    String VERSION = "version";
    String XA = "xa";
    String XA_TX = "XATransaction";
}
