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
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.DAYS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.PERCENTAGE;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.jboss.as.messaging.AttributeMarshallers.NOOP_MARSHALLER;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_0;
import static org.jboss.dmr.ModelType.BIG_DECIMAL;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;

import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.config.impl.FileConfiguration;
import org.hornetq.core.server.JournalType;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {

    String DISCOVERY_GROUP_NAME = "discovery-group-name";
    String ENTRIES = "entries";

    SensitivityClassification MESSAGING_MANAGEMENT =
            new SensitivityClassification(MessagingExtension.SUBSYSTEM_NAME, "messaging-management", false, false, true);

    SensitiveTargetAccessConstraintDefinition MESSAGING_MANAGEMENT_DEF = new SensitiveTargetAccessConstraintDefinition(MESSAGING_MANAGEMENT);

    SensitivityClassification MESSAGING_SECURITY =
            new SensitivityClassification(MessagingExtension.SUBSYSTEM_NAME, "messaging-security", false, false, true);

    SensitiveTargetAccessConstraintDefinition MESSAGING_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(MESSAGING_SECURITY);

    SimpleAttributeDefinition ALLOW_FAILBACK = create("allow-failback", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultAllowAutoFailback()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition ASYNC_CONNECTION_EXECUTION_ENABLED = create( "async-connection-execution-enabled", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultAsyncConnectionExecutionEnabled()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition BACKUP = create("backup", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultBackup()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition CALL_TIMEOUT = create("call-timeout", LONG)
            .setDefaultValue(new ModelNode(HornetQClient.DEFAULT_CALL_TIMEOUT))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition CALL_FAILOVER_TIMEOUT = create("call-failover-timeout",LONG)
            .setDefaultValue(new ModelNode(HornetQClient.DEFAULT_CALL_FAILOVER_TIMEOUT))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .build();

    SimpleAttributeDefinition CHECK_PERIOD = create("check-period", LONG)
            .setDefaultValue(new ModelNode(DEFAULT_CLIENT_FAILURE_CHECK_PERIOD))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    AttributeDefinition CLIENT_ID = create("client-id", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition CHECK_FOR_LIVE_SERVER = create("check-for-live-server", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultCheckForLiveServer()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition CLUSTERED = create("clustered", BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .setDeprecated(VERSION_1_2_0)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition CLUSTER_PASSWORD = create("cluster-password", ModelType.STRING)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultClusterPassword()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    SimpleAttributeDefinition CLUSTER_USER = create("cluster-user", ModelType.STRING)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultClusterUser()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    AttributeDefinition CONSUMER_COUNT = create("consumer-count", INT)
            .setStorageRuntime()
            .build();

    SimpleAttributeDefinition BRIDGE_CONFIRMATION_WINDOW_SIZE = create("confirmation-window-size", INT)
            .setDefaultValue(new ModelNode(FileConfiguration.DEFAULT_CONFIRMATION_WINDOW_SIZE))
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition CONNECTION_TTL = create("connection-ttl", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_CONNECTION_TTL))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition CONNECTION_TTL_OVERRIDE = create("connection-ttl-override", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultConnectionTtlOverride()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition CREATE_BINDINGS_DIR = create("create-bindings-dir", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultCreateBindingsDir()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition CREATE_JOURNAL_DIR = create("create-journal-dir", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultCreateJournalDir()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition DEAD_LETTER_ADDRESS = create("dead-letter-address", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    AttributeDefinition DELIVERING_COUNT = create("delivering-count", INT)
            .setStorageRuntime()
            .build();

    PrimitiveListAttributeDefinition DESTINATION_ENTRIES = PrimitiveListAttributeDefinition.Builder.of(ENTRIES, ModelType.STRING)
            .setAllowNull(false)
            .setValidator(new StringLengthValidator(1, false, true))
            .setAllowExpression(true)
            .setAttributeMarshaller(new AttributeMarshallers.JndiEntriesAttributeMarshaller(true))
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition DURABLE = create("durable", BOOLEAN)
            .setDefaultValue(new ModelNode().set(true))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition FACTORY_CLASS = create("factory-class", ModelType.STRING)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition EXPIRY_ADDRESS = create("expiry-address", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition FAILBACK_DELAY = create("failback-delay", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultFailbackDelay()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = create("failover-on-server-shutdown", ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDeprecated(VERSION_1_2_0)
            .build();

    SimpleAttributeDefinition FAILOVER_ON_SHUTDOWN = create("failover-on-shutdown", BOOLEAN)
            // TODO should be ConfigurationImpl.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN but field is private
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition FILTER = create("filter", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    // do not allow expressions on deprecated attribute
    @Deprecated
    SimpleAttributeDefinition GROUP_ADDRESS = create("group-address", ModelType.STRING)
            .setAllowNull(true)
            .setAlternatives("socket-binding", "jgroups-stack", "jgroups-channel")
            .setDeprecated(VERSION_1_2_0)
            .setFlags(RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();

    // do not allow expressions on deprecated attribute
    @Deprecated
    SimpleAttributeDefinition GROUP_PORT = create("group-port", INT)
            .setAllowNull(true)
            .setAlternatives("socket-binding", "jgroups-stack", "jgroups-channel")
            .setDeprecated(VERSION_1_2_0)
            .setFlags(RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();

    SimpleAttributeDefinition HA = create("ha", BOOLEAN)
            .setDefaultValue(new ModelNode()
            .set(HornetQClient.DEFAULT_HA))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition ID_CACHE_SIZE = create("id-cache-size", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultIdCacheSize()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JMX_DOMAIN = create("jmx-domain", ModelType.STRING)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultJmxDomain()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MESSAGING_MANAGEMENT_DEF)
            .build();

    SimpleAttributeDefinition JMX_MANAGEMENT_ENABLED = create("jmx-management-enabled", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MESSAGING_MANAGEMENT_DEF)
            .build();

    // no default values, depends on whether NIO or AIO is used.
    SimpleAttributeDefinition JOURNAL_BUFFER_SIZE = create("journal-buffer-size", LONG)
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    // no default values, depends on whether NIO or AIO is used.
    SimpleAttributeDefinition JOURNAL_BUFFER_TIMEOUT = create("journal-buffer-timeout", LONG)
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JOURNAL_COMPACT_MIN_FILES = create("journal-compact-min-files", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultJournalCompactMinFiles()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JOURNAL_COMPACT_PERCENTAGE = create("journal-compact-percentage", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultJournalCompactPercentage()))
            .setMeasurementUnit(PERCENTAGE)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JOURNAL_FILE_SIZE = create("journal-file-size", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultJournalFileSize()))
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    // no default values, depends on whether NIO or AIO is used.
    SimpleAttributeDefinition JOURNAL_MAX_IO = create("journal-max-io", INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JOURNAL_MIN_FILES = create("journal-min-files", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultJournalMinFiles()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JOURNAL_SYNC_NON_TRANSACTIONAL = create("journal-sync-non-transactional", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultJournalSyncNonTransactional()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JOURNAL_SYNC_TRANSACTIONAL = create("journal-sync-transactional", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultJournalSyncTransactional()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JOURNAL_TYPE = create("journal-type", ModelType.STRING)
            .setDefaultValue(new ModelNode(ConfigurationImpl.DEFAULT_JOURNAL_TYPE.toString()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<JournalType>(JournalType.class, true, true))
            .setRestartAllServices()
            .build();

    AttributeDefinition LIVE_CONNECTOR_REF = create("live-connector-ref", ModelType.STRING)
            .setAllowNull(true)
            .setDeprecated(VERSION_1_2_0)
            .setRestartAllServices()
            .setAttributeMarshaller(NOOP_MARSHALLER)
            .build();

    // do not allow expressions on deprecated attribute
    @Deprecated
    SimpleAttributeDefinition LOCAL_BIND_ADDRESS = create("local-bind-address", ModelType.STRING)
            .setAllowNull(true)
            .setAlternatives("socket-binding", "jgroups-stack", "jgroups-channel")
            .setDeprecated(VERSION_1_2_0)
            .setFlags(RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();

    // do not allow expressions on deprecated attribute
    @Deprecated
    SimpleAttributeDefinition LOCAL_BIND_PORT = create("local-bind-port", INT)
            .setDefaultValue(new ModelNode().set(-1))
            .setAllowNull(true)
            .setAlternatives("socket-binding", "jgroups-stack", "jgroups-channel")
            .setDeprecated(VERSION_1_2_0)
            .setFlags(RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();

    SimpleAttributeDefinition JGROUPS_STACK = create("jgroups-stack", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setAlternatives("socket-binding",
                    "group-address", "group-port",
                    "local-bind-address", "local-bind-port")
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition JGROUPS_CHANNEL = create("jgroups-channel", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setAlternatives("socket-binding",
                    "group-address", "group-port",
                    "local-bind-address", "local-bind-port")
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition LOG_JOURNAL_WRITE_RATE = create("log-journal-write-rate", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultJournalLogWriteRate()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition MANAGEMENT_ADDRESS = create("management-address", ModelType.STRING)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultManagementAddress().toString()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MESSAGING_MANAGEMENT_DEF)
            .build();

    SimpleAttributeDefinition MANAGEMENT_NOTIFICATION_ADDRESS = create("management-notification-address", ModelType.STRING)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultManagementNotificationAddress().toString()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MESSAGING_MANAGEMENT_DEF)
            .build();

    AttributeDefinition MAX_RETRY_INTERVAL = create("max-retry-interval", LONG)
            .setDefaultValue(new ModelNode(DEFAULT_MAX_RETRY_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition MAX_SAVED_REPLICATED_JOURNAL_SIZE = create("max-saved-replicated-journal-size", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMaxSavedReplicatedJournalsSize()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition MEMORY_MEASURE_INTERVAL = create("memory-measure-interval", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMemoryMeasureInterval()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition MEMORY_WARNING_THRESHOLD = create("memory-warning-threshold", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMemoryWarningThreshold()))
            .setMeasurementUnit(PERCENTAGE)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition MESSAGE_COUNT = create("message-count", LONG)
            .setStorageRuntime()
            .build();

    SimpleAttributeDefinition MESSAGE_COUNTER_ENABLED = create("message-counter-enabled", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultMessageCounterEnabled()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition MESSAGE_COUNTER_MAX_DAY_HISTORY = create("message-counter-max-day-history", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMessageCounterMaxDayHistory()))
            .setMeasurementUnit(DAYS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition MESSAGE_COUNTER_SAMPLE_PERIOD = create("message-counter-sample-period", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMessageCounterSamplePeriod()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition MESSAGE_EXPIRY_SCAN_PERIOD = create("message-expiry-scan-period", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMessageExpiryScanPeriod()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition MESSAGE_EXPIRY_THREAD_PRIORITY = create("message-expiry-thread-priority", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMessageExpiryThreadPriority()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, true, true))
            .setRestartAllServices()
            .build();

    AttributeDefinition MESSAGES_ADDED = create("messages-added", LONG)
            .setStorageRuntime()
            .build();

    AttributeDefinition MIN_LARGE_MESSAGE_SIZE = create("min-large-message-size", INT)
            .setDefaultValue(new ModelNode(HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE))
            .setMeasurementUnit(BYTES)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition BACKUP_GROUP_NAME = create("backup-group-name", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition PAGE_MAX_CONCURRENT_IO = create("page-max-concurrent-io", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMaxConcurrentPageIo()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition PAUSED = create("paused", BOOLEAN)
            .setStorageRuntime()
            .build();

    SimpleAttributeDefinition PERF_BLAST_PAGES = create("perf-blast-pages", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultJournalPerfBlastPages()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY = create("persist-delivery-count-before-delivery", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultPersistDeliveryCountBeforeDelivery()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition PERSISTENCE_ENABLED = create("persistence-enabled", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultPersistenceEnabled()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition PERSIST_ID_CACHE = create("persist-id-cache", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultPersistIdCache()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    @Deprecated
    PrimitiveListAttributeDefinition REMOTING_INTERCEPTORS = new PrimitiveListAttributeDefinition.Builder("remoting-interceptors", ModelType.STRING)
            .setDeprecated(VERSION_1_2_0)
            .setAllowNull(true)
            .setAllowExpression(false)
            .setMinSize(1)
            .setMaxSize(Integer.MAX_VALUE)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1, false, true))
            .setAttributeMarshaller(AttributeMarshallers.INTERCEPTOR_MARSHALLER)
            .build();

    PrimitiveListAttributeDefinition REMOTING_INCOMING_INTERCEPTORS = new PrimitiveListAttributeDefinition.Builder("remoting-incoming-interceptors", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(false)
            .setMinSize(1)
            .setMaxSize(Integer.MAX_VALUE)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1, false, true))
            .setAttributeMarshaller(AttributeMarshallers.INTERCEPTOR_MARSHALLER)
            .build();

    PrimitiveListAttributeDefinition REMOTING_OUTGOING_INTERCEPTORS = new PrimitiveListAttributeDefinition.Builder("remoting-outgoing-interceptors", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(false)
            .setMinSize(1)
            .setMaxSize(Integer.MAX_VALUE)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1, false, true))
            .setAttributeMarshaller(AttributeMarshallers.INTERCEPTOR_MARSHALLER)
            .build();

    SimpleAttributeDefinition REPLICATION_CLUSTERNAME = create("replication-clustername", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition RETRY_INTERVAL = create("retry-interval", LONG)
            .setDefaultValue(new ModelNode().set(HornetQClient.DEFAULT_RETRY_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", BIG_DECIMAL)
            .setDefaultValue(new ModelNode(HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition RUN_SYNC_SPEED_TEST = create("run-sync-speed-test", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultRunSyncSpeedTest()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    AttributeDefinition SCHEDULED_COUNT = create("scheduled-count", LONG)
            .setStorageRuntime()
            .build();

    AttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = create("scheduled-thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(HornetQDefaultConfiguration.getDefaultScheduledThreadPoolMaxSize()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition SECURITY_DOMAIN = create("security-domain", ModelType.STRING)
            .setDefaultValue(new ModelNode("other"))
            .setAllowNull(true)
            .setAllowExpression(false) // references the security domain service name
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    SimpleAttributeDefinition SECURITY_ENABLED = create("security-enabled", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultSecurityEnabled()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    SimpleAttributeDefinition SECURITY_INVALIDATION_INTERVAL = create("security-invalidation-interval", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultSecurityInvalidationInterval()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    SimpleAttributeDefinition SELECTOR = create("selector", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setAttributeMarshaller(AttributeMarshallers.SELECTOR_MARSHALLER)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition SERVER_DUMP_INTERVAL = create("server-dump-interval", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultServerDumpInterval()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition SHARED_STORE = create("shared-store", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultSharedStore()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition SOCKET_BINDING = create("socket-binding", ModelType.STRING)
            .setAllowNull(true)
            .setAlternatives(GROUP_ADDRESS.getName(),
                    GROUP_PORT.getName(),
                    LOCAL_BIND_ADDRESS.getName(),
                    LOCAL_BIND_PORT.getName(),
                    JGROUPS_STACK.getName(),
                    JGROUPS_CHANNEL.getName())
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    AttributeDefinition TEMPORARY = create("temporary", BOOLEAN)
            .setStorageRuntime()
            .build();

    AttributeDefinition THREAD_POOL_MAX_SIZE = create("thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode().set(HornetQDefaultConfiguration.getDefaultThreadPoolMaxSize()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition TRANSACTION_TIMEOUT = create("transaction-timeout", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultTransactionTimeout()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition TRANSACTION_TIMEOUT_SCAN_PERIOD = create("transaction-timeout-scan-period", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultTransactionTimeoutScanPeriod()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition TRANSFORMER_CLASS_NAME = create("transformer-class-name", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    SimpleAttributeDefinition USER = new SimpleAttributeDefinition("user", "user",
            new ModelNode().set(HornetQDefaultConfiguration.getDefaultClusterUser()), ModelType.STRING, true, true, null);

    SimpleAttributeDefinition WILD_CARD_ROUTING_ENABLED = create("wild-card-routing-enabled", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultWildcardRoutingEnabled()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

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
    String ENTRY = "entry";
    String FILE_DEPLOYMENT_ENABLED = "file-deployment-enabled";
    String GROUPING_HANDLER = "grouping-handler";
    String HOST = "host";
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
    String REMOTE_ACCEPTOR = "remote-acceptor";
    String REMOTE_CONNECTOR = "remote-connector";
    String REMOTING_INTERCEPTOR = "remoting-interceptor";
    String REMOTING_INCOMING_INTERCEPTOR = "remoting-incoming-interceptor";
    String REMOTING_OUTGOING_INTERCEPTOR = "remoting-outgoing-interceptor";
    String RESOURCE_ADAPTER = "resource-adapter";
    String ROLE = "role";
    String ROLES_ATTR_NAME = "roles";
    String RUNTIME_QUEUE = "runtime-queue";
    String SECURITY_ROLE = "security-role";
    String SECURITY_SETTING = "security-setting";
    String SECURITY_SETTINGS = "security-settings";
    String SERVLET_PATH = "servlet-path";
    String HORNETQ_SERVER = "hornetq-server";
    String STARTED = "started";
    String STATIC_CONNECTORS = "static-connectors";
    String STRING = "string";
    String SUBSCRIPTION_COUNT = "subscription-count";
    String SUBSYSTEM = "subsystem";
    String TOPIC_ADDRESS = "topic-address";
    String TYPE_ATTR_NAME = "type";
    String USE_INVM = "use-invm";
    String USE_SERVLET = "use-servlet";
    String VERSION = "version";
    String XA = "xa";
    String XA_TX = "XATransaction";

    AttributeDefinition[] SIMPLE_ROOT_RESOURCE_ATTRIBUTES = { CLUSTERED, PERSISTENCE_ENABLED, SCHEDULED_THREAD_POOL_MAX_SIZE,
            THREAD_POOL_MAX_SIZE, SECURITY_DOMAIN, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL, WILD_CARD_ROUTING_ENABLED,
            MANAGEMENT_ADDRESS, MANAGEMENT_NOTIFICATION_ADDRESS, CLUSTER_USER, CLUSTER_PASSWORD, JMX_MANAGEMENT_ENABLED,
            JMX_DOMAIN, MESSAGE_COUNTER_ENABLED, MESSAGE_COUNTER_SAMPLE_PERIOD, MESSAGE_COUNTER_MAX_DAY_HISTORY,
            CONNECTION_TTL_OVERRIDE, ASYNC_CONNECTION_EXECUTION_ENABLED, TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_SCAN_PERIOD,
            MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY, ID_CACHE_SIZE, PERSIST_ID_CACHE,
            REMOTING_INTERCEPTORS, REMOTING_INCOMING_INTERCEPTORS, REMOTING_OUTGOING_INTERCEPTORS,
            BACKUP, ALLOW_FAILBACK, FAILBACK_DELAY, FAILOVER_ON_SHUTDOWN, SHARED_STORE, PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY,
            PAGE_MAX_CONCURRENT_IO, CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE, JOURNAL_BUFFER_TIMEOUT,
            JOURNAL_BUFFER_SIZE, JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE,
            JOURNAL_FILE_SIZE, JOURNAL_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_COMPACT_MIN_FILES, JOURNAL_MAX_IO,
            MAX_SAVED_REPLICATED_JOURNAL_SIZE, PERF_BLAST_PAGES, RUN_SYNC_SPEED_TEST, SERVER_DUMP_INTERVAL, MEMORY_WARNING_THRESHOLD, MEMORY_MEASURE_INTERVAL,
            CHECK_FOR_LIVE_SERVER, BACKUP_GROUP_NAME, REPLICATION_CLUSTERNAME };

    AttributeDefinition[] SIMPLE_ROOT_RESOURCE_WRITE_ATTRIBUTES = { FAILOVER_ON_SHUTDOWN, MESSAGE_COUNTER_ENABLED,
            MESSAGE_COUNTER_MAX_DAY_HISTORY, MESSAGE_COUNTER_SAMPLE_PERIOD };

}
