/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.controller.client.helpers.MeasurementUnit.DAYS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.PERCENTAGE;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.SECONDS;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.InfiniteOrPositiveValidators.INT_INSTANCE;
import static org.wildfly.extension.messaging.activemq.InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.VERSION_3_0_0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.core.server.NetworkHealthCheck;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import static org.wildfly.extension.messaging.activemq.InfiniteOrPositiveValidators.LONG_INSTANCE;
import org.wildfly.extension.messaging.activemq.ha.LiveOnlyDefinition;
import org.wildfly.extension.messaging.activemq.ha.ReplicationColocatedDefinition;
import org.wildfly.extension.messaging.activemq.ha.ReplicationMasterDefinition;
import org.wildfly.extension.messaging.activemq.ha.ReplicationSlaveDefinition;
import org.wildfly.extension.messaging.activemq.ha.SharedStoreColocatedDefinition;
import org.wildfly.extension.messaging.activemq.ha.SharedStoreMasterDefinition;
import org.wildfly.extension.messaging.activemq.ha.SharedStoreSlaveDefinition;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSQueueDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSServerControlHandler;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition;
import org.wildfly.extension.messaging.activemq.jms.PooledConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the messaging-activemq subsystem server resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerDefinition extends PersistentResourceDefinition {

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultClusterPassword
     */
    public static final SimpleAttributeDefinition CLUSTER_PASSWORD = create("cluster-password", ModelType.STRING, true)
            .setAttributeGroup("cluster")
            .setXmlName("password")
            .setDefaultValue(new ModelNode("CHANGE ME!!"))
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MessagingExtension.MESSAGING_SECURITY_SENSITIVE_TARGET)
            .setAlternatives("cluster-" + CredentialReference.CREDENTIAL_REFERENCE)
            .build();

    public static final ObjectTypeAttributeDefinition CREDENTIAL_REFERENCE =
            CredentialReference.getAttributeBuilder("cluster-" + CredentialReference.CREDENTIAL_REFERENCE, CredentialReference.CREDENTIAL_REFERENCE, true, true)
                    .setAttributeGroup("cluster")
                    .setRestartAllServices()
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
                    .addAccessConstraint(MessagingExtension.MESSAGING_SECURITY_SENSITIVE_TARGET)
                    .setAlternatives(CLUSTER_PASSWORD.getName())
                    .build();

     /**
     * @see ActiveMQDefaultConfiguration#getDefaultClusterUser
     */
    public static final SimpleAttributeDefinition CLUSTER_USER = create("cluster-user", ModelType.STRING)
            .setAttributeGroup("cluster")
            .setXmlName("user")
            .setDefaultValue(new ModelNode("ACTIVEMQ.CLUSTER.ADMIN.USER"))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MessagingExtension.MESSAGING_SECURITY_SENSITIVE_TARGET)
            .build();
     /**
     * @see ActiveMQDefaultConfiguration#getDefaultScheduledThreadPoolMaxSize
     */
    public static final AttributeDefinition SCHEDULED_THREAD_POOL_MAX_SIZE = create("scheduled-thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode(5))
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SECURITY_DOMAIN = create("security-domain", ModelType.STRING)
            .setAttributeGroup("security")
            .setXmlName("domain")
            .setDefaultValue(new ModelNode("other"))
            .setAlternatives("elytron-domain")
            .setRequired(false)
            .setAllowExpression(false) // references the security domain service name
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
            .addAccessConstraint(MessagingExtension.MESSAGING_SECURITY_SENSITIVE_TARGET)
            .setDeprecated(MessagingExtension.VERSION_2_0_0)
            .build();
    public static final SimpleAttributeDefinition ELYTRON_DOMAIN = create("elytron-domain", ModelType.STRING)
            .setAttributeGroup("security")
            .setRequired(false)
            .setAlternatives(SECURITY_DOMAIN.getName())
            .setAllowExpression(false)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.ELYTRON_SECURITY_DOMAIN_REF)
            .addAccessConstraint(MessagingExtension.MESSAGING_SECURITY_SENSITIVE_TARGET)
            .setCapabilityReference(Capabilities.ELYTRON_DOMAIN_CAPABILITY, Capabilities.ACTIVEMQ_SERVER_CAPABILITY)
            .build();
     /**
     * @see ActiveMQDefaultConfiguration#getDefaultThreadPoolMaxSize
     */
    public static final AttributeDefinition THREAD_POOL_MAX_SIZE = create("thread-pool-max-size", INT)
            .setDefaultValue(new ModelNode(30))
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition OVERRIDE_IN_VM_SECURITY = create("override-in-vm-security", BOOLEAN)
            .setAttributeGroup("security")
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultWildcardRoutingEnabled
     */
    public static final SimpleAttributeDefinition WILD_CARD_ROUTING_ENABLED = create("wild-card-routing-enabled", BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition STATISTICS_ENABLED = create(ModelDescriptionConstants.STATISTICS_ENABLED, BOOLEAN)
            .setAttributeGroup("statistics")
            .setXmlName("enabled")
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .build();
    // no default values, depends on whether NIO or AIO is used.
    public static final SimpleAttributeDefinition JOURNAL_BUFFER_SIZE = create("journal-buffer-size", LONG)
            .setAttributeGroup("journal")
            .setXmlName("buffer-size")
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(0,Long.MAX_VALUE,true,true))
            .setRestartAllServices()
            .build();
    // no default values, depends on whether NIO or AIO is used.
    public static final SimpleAttributeDefinition JOURNAL_BUFFER_TIMEOUT = create("journal-buffer-timeout", LONG)
            .setAttributeGroup("journal")
            .setXmlName("buffer-timeout")
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultJournalCompactMinFiles
     */
    public static final SimpleAttributeDefinition JOURNAL_COMPACT_MIN_FILES = create("journal-compact-min-files", INT)
            .setAttributeGroup("journal")
            .setXmlName("compact-min-files")
            .setDefaultValue(new ModelNode(10))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultJournalCompactPercentage
     */
    public static final SimpleAttributeDefinition JOURNAL_COMPACT_PERCENTAGE = create("journal-compact-percentage", INT)
            .setAttributeGroup("journal")
            .setXmlName("compact-percentage")
            .setDefaultValue(new ModelNode(30))
            .setMeasurementUnit(PERCENTAGE)
            .setValidator(new IntRangeValidator(0, 100, true, true))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    // TODO: if this attribute is set, warn/error if any fs-related journal attribute is set.
    // TODO: add capability for data-source https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/data-source/capability.adoc
    public static final SimpleAttributeDefinition JOURNAL_DATASOURCE = create("journal-datasource", STRING)
            .setAttributeGroup("journal")
            .setXmlName("datasource")
            .setRequired(false)
            // references another resource
            .setAllowExpression(false)
            .setCapabilityReference(Capabilities.DATA_SOURCE_CAPABILITY, Capabilities.ACTIVEMQ_SERVER_CAPABILITY)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultBindingsTableName
     */
    public static final SimpleAttributeDefinition JOURNAL_BINDINGS_TABLE  = create("journal-bindings-table", STRING)
            .setAttributeGroup("journal")
            .setXmlName("bindings-table")
            .setRequired(false)
            .setDefaultValue(new ModelNode("BINDINGS"))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    @Deprecated
    public static final SimpleAttributeDefinition JOURNAL_JMS_BINDINGS_TABLE  = create("journal-jms-bindings-table", STRING)
            .setAttributeGroup("journal")
            .setXmlName("jms-bindings-table")
            .setRequired(false)
            .setDefaultValue(new ModelNode("JMS_BINDINGS"))
            .setAllowExpression(true)
            .setDeprecated(VERSION_3_0_0)
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultLargeMessagesTableName
     */
    public static final SimpleAttributeDefinition JOURNAL_LARGE_MESSAGES_TABLE  = create("journal-large-messages-table", STRING)
            .setAttributeGroup("journal")
            .setXmlName("large-messages-table")
            .setRequired(false)
            .setDefaultValue(new ModelNode("LARGE_MESSAGES"))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMessageTableName
     */
    public static final SimpleAttributeDefinition JOURNAL_MESSAGES_TABLE = create("journal-messages-table", STRING)
            .setAttributeGroup("journal")
            .setXmlName("messages-table")
            .setRequired(false)
            .setDefaultValue(new ModelNode("MESSAGES"))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultNodeManagerStoreTableName
     */
    public static final SimpleAttributeDefinition JOURNAL_NODE_MANAGER_STORE_TABLE = create("journal-node-manager-store-table", STRING)
            .setAttributeGroup("journal")
            .setXmlName("node-manager-store-table")
            .setRequired(false)
            .setDefaultValue(new ModelNode("NODE_MANAGER_STORE"))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultPageStoreTableName
     */
    public static final SimpleAttributeDefinition JOURNAL_PAGE_STORE_TABLE  = create("journal-page-store-table", STRING)
            .setAttributeGroup("journal")
            .setXmlName("page-store-table")
            .setRequired(false)
            .setDefaultValue(new ModelNode("PAGE_STORE"))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition JOURNAL_DATABASE = create("journal-database", STRING)
            .setAttributeGroup("journal")
            .setXmlName("database")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultJdbcLockExpirationMillis()
     */
    public static final AttributeDefinition JOURNAL_JDBC_LOCK_EXPIRATION = create("journal-jdbc-lock-expiration", INT)
            .setAttributeGroup("journal")
            .setXmlName("jdbc-lock-expiration")
            .setDefaultValue(new ModelNode(20))
            .setMeasurementUnit(SECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultJdbcLockRenewPeriodMillis()
     */
    public static final AttributeDefinition JOURNAL_JDBC_LOCK_RENEW_PERIOD = create("journal-jdbc-lock-renew-period", INT)
            .setAttributeGroup("journal")
            .setXmlName("jdbc-lock-renew-period")
            .setDefaultValue(new ModelNode(4))
            .setMeasurementUnit(SECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();


    /**
     * @see ActiveMQDefaultConfiguration#getDefaultJdbcNetworkTimeout()
     */
    public static final AttributeDefinition JOURNAL_JDBC_NETWORK_TIMEOUT = create("journal-jdbc-network-timeout", INT)
            .setAttributeGroup("journal")
            .setXmlName("jdbc-network-timeout")
            .setDefaultValue(new ModelNode(20))
            .setMeasurementUnit(SECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultJournalFileSize
     */
    public static final SimpleAttributeDefinition JOURNAL_FILE_SIZE = create("journal-file-size", LONG)
            .setAttributeGroup("journal")
            .setXmlName("file-size")
            .setDefaultValue(new ModelNode(10485760L))
            .setMeasurementUnit(BYTES)
            .setValidator(new LongRangeValidator(1024, Long.MAX_VALUE, true, true))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    // no default values, depends on whether NIO or AIO is used.
    public static final SimpleAttributeDefinition JOURNAL_MAX_IO = create("journal-max-io", INT)
            .setAttributeGroup("journal")
            .setXmlName("max-io")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultJournalMinFiles
     */
    public static final SimpleAttributeDefinition JOURNAL_MIN_FILES = create("journal-min-files", INT)
            .setAttributeGroup("journal")
            .setXmlName("min-files")
            .setDefaultValue(new ModelNode(2))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(2, true, true))
            .build();
    /**
     * This is different from currant Artemis default value.
     * @see ActiveMQDefaultConfiguration#getDefaultJournalPoolFiles
     */
    public static final SimpleAttributeDefinition JOURNAL_POOL_FILES = create("journal-pool-files", INT)
            .setAttributeGroup("journal")
            .setXmlName("pool-files")
            .setDefaultValue(new ModelNode(10))
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultJournalFileOpenTimeout()
     */
    public static final SimpleAttributeDefinition JOURNAL_FILE_OPEN_TIMEOUT = create("journal-file-open-timeout", INT)
            .setAttributeGroup("journal")
            .setXmlName("file-open-timeout")
            .setDefaultValue(new ModelNode(5))
            .setRequired(false)
            .setAllowExpression(true)
            .setMeasurementUnit(SECONDS)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultJournalSyncNonTransactional
     */
    public static final SimpleAttributeDefinition JOURNAL_SYNC_NON_TRANSACTIONAL = create("journal-sync-non-transactional", BOOLEAN)
            .setAttributeGroup("journal")
            .setXmlName("sync-non-transactional")
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultJournalSyncTransactional
     */
    public static final SimpleAttributeDefinition JOURNAL_SYNC_TRANSACTIONAL = create("journal-sync-transactional", BOOLEAN)
            .setAttributeGroup("journal")
            .setXmlName("sync-transactional")
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition JOURNAL_TYPE = create("journal-type", ModelType.STRING)
            .setAttributeGroup("journal")
            .setXmlName("type")
            .setDefaultValue(new ModelNode(JournalType.ASYNCIO.toString()))
            .setRequired(false)
            .setAllowExpression(true)
            // list allowed values explicitly to exclude MAPPED
            .setValidator(new EnumValidator<>(JournalType.class, true, true))
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition JOURNAL_MAX_ATTIC_FILES = create("journal-max-attic-files", ModelType.INT)
            .setAttributeGroup("journal")
            .setXmlName("max-attic-files")
            .setDefaultValue(new ModelNode(10))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#isDefaultJournalLogWriteRate
     */
    public static final SimpleAttributeDefinition LOG_JOURNAL_WRITE_RATE = create("log-journal-write-rate", BOOLEAN)
            .setAttributeGroup("journal")
            .setXmlName("log-write-rate")
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultConnectionTtlOverride
     */
    public static final SimpleAttributeDefinition CONNECTION_TTL_OVERRIDE = create("connection-ttl-override", LONG)
            .setDefaultValue(new ModelNode(-1L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultAsyncConnectionExecutionEnabled
     */
    public static final SimpleAttributeDefinition ASYNC_CONNECTION_EXECUTION_ENABLED = create("async-connection-execution-enabled", BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMessageCounterMaxDayHistory
     */
    public static final SimpleAttributeDefinition MESSAGE_COUNTER_MAX_DAY_HISTORY = create("message-counter-max-day-history", INT)
            .setAttributeGroup("statistics")
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(DAYS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMessageCounterSamplePeriod
     */
    public static final SimpleAttributeDefinition MESSAGE_COUNTER_SAMPLE_PERIOD = create("message-counter-sample-period", LONG)
            .setAttributeGroup("statistics")
            .setDefaultValue(new ModelNode(10000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultTransactionTimeout
     */
    public static final SimpleAttributeDefinition TRANSACTION_TIMEOUT = create("transaction-timeout", LONG)
            .setAttributeGroup("transaction")
            .setXmlName("timeout")
            .setDefaultValue(new ModelNode(300000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultTransactionTimeoutScanPeriod
     */
    public static final SimpleAttributeDefinition TRANSACTION_TIMEOUT_SCAN_PERIOD = create("transaction-timeout-scan-period", LONG)
            .setAttributeGroup("transaction")
            .setXmlName("scan-period")
            .setDefaultValue(new ModelNode(1000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMessageExpiryScanPeriod
     */
    public static final SimpleAttributeDefinition MESSAGE_EXPIRY_SCAN_PERIOD = create("message-expiry-scan-period", LONG)
            .setAttributeGroup("message-expiry")
            .setXmlName("scan-period")
            .setDefaultValue(new ModelNode(30000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMessageExpiryThreadPriority
     */
    public static final SimpleAttributeDefinition MESSAGE_EXPIRY_THREAD_PRIORITY = create("message-expiry-thread-priority", INT)
            .setAttributeGroup("message-expiry")
            .setXmlName("thread-priority")
            .setDefaultValue(new ModelNode(3))
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, true, true))
            .setRestartAllServices()
            .build();

    // Property no longer exists since Artemis 2
    @Deprecated
    public static final SimpleAttributeDefinition PERF_BLAST_PAGES = create("perf-blast-pages", INT)
            .setAttributeGroup("debug")
            .setDefaultValue(new ModelNode(-1))
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .setDeprecated(VERSION_3_0_0)
            .build();

    // Property no longer exists since Artemis 2
    @Deprecated
    public static final SimpleAttributeDefinition RUN_SYNC_SPEED_TEST = create("run-sync-speed-test", BOOLEAN)
            .setAttributeGroup("debug")
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setDeprecated(VERSION_3_0_0)
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultServerDumpInterval
     */
    public static final SimpleAttributeDefinition SERVER_DUMP_INTERVAL = create("server-dump-interval", LONG)
            .setAttributeGroup("debug")
            .setDefaultValue(new ModelNode(-1L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMemoryMeasureInterval
     */
    public static final SimpleAttributeDefinition MEMORY_MEASURE_INTERVAL = create("memory-measure-interval", LONG)
            .setAttributeGroup("debug")
            .setDefaultValue(new ModelNode(-1L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMemoryWarningThreshold
     */
    public static final SimpleAttributeDefinition MEMORY_WARNING_THRESHOLD = create("memory-warning-threshold", INT)
            .setAttributeGroup("debug")
            .setDefaultValue(new ModelNode(25))
            .setMeasurementUnit(PERCENTAGE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultSecurityInvalidationInterval
     */
    public static final SimpleAttributeDefinition SECURITY_INVALIDATION_INTERVAL = create("security-invalidation-interval", LONG)
            .setAttributeGroup("security")
            .setXmlName("invalidation-interval")
            .setDefaultValue(new ModelNode(10000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MessagingExtension.MESSAGING_SECURITY_SENSITIVE_TARGET)
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultSecurityEnabled
     */
    public static final SimpleAttributeDefinition SECURITY_ENABLED = create("security-enabled", BOOLEAN)
            .setAttributeGroup("security")
            .setXmlName("enabled")
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MessagingExtension.MESSAGING_SECURITY_SENSITIVE_TARGET)
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultPersistenceEnabled
     */
    public static final SimpleAttributeDefinition PERSISTENCE_ENABLED = create("persistence-enabled", BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultManagementNotificationAddress
     */
    public static final SimpleAttributeDefinition MANAGEMENT_NOTIFICATION_ADDRESS = create("management-notification-address", ModelType.STRING)
            .setAttributeGroup("management")
            .setXmlName("notification-address")
            .setDefaultValue(new ModelNode("activemq.notifications"))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MessagingExtension.MESSAGING_MANAGEMENT_SENSITIVE_TARGET)
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultManagementAddress
     */
    public static final SimpleAttributeDefinition MANAGEMENT_ADDRESS = create("management-address", ModelType.STRING)
            .setAttributeGroup("management")
            .setXmlName("address")
            .setDefaultValue(new ModelNode("activemq.management"))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MessagingExtension.MESSAGING_MANAGEMENT_SENSITIVE_TARGET)
            .build();
    public static final SimpleAttributeDefinition JMX_MANAGEMENT_ENABLED = create("jmx-management-enabled", BOOLEAN)
            .setAttributeGroup("management")
            .setXmlName("jmx-enabled")
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MessagingExtension.MESSAGING_MANAGEMENT_SENSITIVE_TARGET)
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultJmxDomain
     */
    public static final SimpleAttributeDefinition JMX_DOMAIN = create("jmx-domain", ModelType.STRING)
            .setAttributeGroup("management")
            .setDefaultValue(new ModelNode("org.apache.activemq.artemis"))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(MessagingExtension.MESSAGING_MANAGEMENT_SENSITIVE_TARGET)
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#isDefaultPersistIdCache
     */
    public static final SimpleAttributeDefinition PERSIST_ID_CACHE = create("persist-id-cache", BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultPersistDeliveryCountBeforeDelivery
     */
    public static final SimpleAttributeDefinition PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY = create("persist-delivery-count-before-delivery", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultIdCacheSize
     */
    public static final SimpleAttributeDefinition ID_CACHE_SIZE = create("id-cache-size", INT)
            .setDefaultValue(new ModelNode(20000))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMaxConcurrentPageIo
     */
    public static final SimpleAttributeDefinition PAGE_MAX_CONCURRENT_IO = create("page-max-concurrent-io", INT)
            .setDefaultValue(new ModelNode(5))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultCreateBindingsDir
     */
    public static final SimpleAttributeDefinition CREATE_BINDINGS_DIR = create("create-bindings-dir", BOOLEAN)
            .setAttributeGroup("journal")
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultCreateJournalDir
     */
    public static final SimpleAttributeDefinition CREATE_JOURNAL_DIR = create("create-journal-dir", BOOLEAN)
            .setAttributeGroup("journal")
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMaxDiskUsage
     */
    public static final SimpleAttributeDefinition GLOBAL_MAX_DISK_USAGE = create("global-max-disk-usage", INT)
            .setAttributeGroup("journal")
            .setMeasurementUnit(MeasurementUnit.PERCENTAGE)
            .setDefaultValue(new ModelNode(100))
            .setRequired(false)
            .setValidator(new IntRangeValidator(-1, 100))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultDiskScanPeriod
     */
    public static final SimpleAttributeDefinition DISK_SCAN_PERIOD = create("disk-scan-period", INT)
            .setAttributeGroup("journal")
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setDefaultValue(new ModelNode(5000))
            .setValidator(INT_INSTANCE)
            .setCorrector(NEGATIVE_VALUE_CORRECTOR)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMaxGlobalSize
     */
    public static final SimpleAttributeDefinition GLOBAL_MAX_MEMORY_SIZE = create("global-max-memory-size", LONG)
            .setAttributeGroup("journal")
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setDefaultValue(new ModelNode(-1L))
            .setCorrector(NEGATIVE_VALUE_CORRECTOR)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultNetworkCheckNic
     */
    public static final SimpleAttributeDefinition NETWORK_CHECK_NIC = create("network-check-nic", STRING)
            .setAttributeGroup("network-isolation")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultNetworkCheckPeriod
     */
    public static final SimpleAttributeDefinition NETWORK_CHECK_PERIOD = create("network-check-period", LONG)
            .setAttributeGroup("network-isolation")
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setDefaultValue(new ModelNode(5000L))
            .setRequired(false)
            .setValidator(LONG_INSTANCE)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultNetworkCheckTimeout
     */
    public static final SimpleAttributeDefinition NETWORK_CHECK_TIMEOUT = create("network-check-timeout", LONG)
            .setAttributeGroup("network-isolation")
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setDefaultValue(new ModelNode(1000L))
            .setRequired(false)
            .setValidator(LONG_INSTANCE)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultNetworkCheckList
     */
    public static final SimpleAttributeDefinition NETWORK_CHECK_LIST = create("network-check-list", STRING)
            .setAttributeGroup("network-isolation")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultNetworkCheckURLList
     */
    public static final SimpleAttributeDefinition NETWORK_CHECK_URL_LIST = create("network-check-url-list", STRING)
            .setAttributeGroup("network-isolation")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see NetworkHealthCheck#IPV4_DEFAULT_COMMAND
     */
    public static final SimpleAttributeDefinition NETWORK_CHECK_PING_COMMAND = create("network-check-ping-command", STRING)
            .setAttributeGroup("network-isolation")
            .setDefaultValue(new ModelNode("ping -c 1 -t %d %s"))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see NetworkHealthCheck#IPV6_DEFAULT_COMMAND
     */
    public static final SimpleAttributeDefinition NETWORK_CHECK_PING6_COMMAND = create("network-check-ping6-command", STRING)
            .setAttributeGroup("network-isolation")
            .setDefaultValue(new ModelNode("ping6 -c 1 %2$s"))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CRITICAL_ANALYZER_ENABLED = create("critical-analyzer-enabled", BOOLEAN)
            .setAttributeGroup("critical-analyzer")
            .setXmlName("enabled")
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CRITICAL_ANALYZER_CHECK_PERIOD = create("critical-analyzer-check-period", LONG)
            .setAttributeGroup("critical-analyzer")
            .setXmlName("check-period")
            .setDefaultValue(new ModelNode(0L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getCriticalAnalyzerTimeout
     */
    public static final SimpleAttributeDefinition CRITICAL_ANALYZER_TIMEOUT = create("critical-analyzer-timeout", LONG)
            .setAttributeGroup("critical-analyzer")
            .setXmlName("timeout")
            .setDefaultValue(new ModelNode(120000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CRITICAL_ANALYZER_POLICY = create("critical-analyzer-policy", STRING)
            .setAttributeGroup("critical-analyzer")
            .setXmlName("policy")
            .setDefaultValue(new ModelNode("LOG"))
            .setAllowedValues("HALT", "SHUTDOWN", "LOG")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {PERSISTENCE_ENABLED, SCHEDULED_THREAD_POOL_MAX_SIZE,
            THREAD_POOL_MAX_SIZE, SECURITY_DOMAIN, ELYTRON_DOMAIN, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL,
            OVERRIDE_IN_VM_SECURITY, WILD_CARD_ROUTING_ENABLED, MANAGEMENT_ADDRESS, MANAGEMENT_NOTIFICATION_ADDRESS,
            CLUSTER_USER, CLUSTER_PASSWORD, CREDENTIAL_REFERENCE, JMX_MANAGEMENT_ENABLED, JMX_DOMAIN, STATISTICS_ENABLED, MESSAGE_COUNTER_SAMPLE_PERIOD,
            MESSAGE_COUNTER_MAX_DAY_HISTORY, CONNECTION_TTL_OVERRIDE, ASYNC_CONNECTION_EXECUTION_ENABLED, TRANSACTION_TIMEOUT,
            TRANSACTION_TIMEOUT_SCAN_PERIOD, MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY, ID_CACHE_SIZE, PERSIST_ID_CACHE,
            CommonAttributes.INCOMING_INTERCEPTORS, CommonAttributes.OUTGOING_INTERCEPTORS,
            PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY,
            PAGE_MAX_CONCURRENT_IO, CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE, JOURNAL_BUFFER_TIMEOUT,
            JOURNAL_BUFFER_SIZE,
            JOURNAL_DATASOURCE, JOURNAL_DATABASE,
            JOURNAL_JDBC_LOCK_EXPIRATION, JOURNAL_JDBC_LOCK_RENEW_PERIOD,
            JOURNAL_JDBC_NETWORK_TIMEOUT,
            JOURNAL_MESSAGES_TABLE, JOURNAL_BINDINGS_TABLE, JOURNAL_JMS_BINDINGS_TABLE, JOURNAL_LARGE_MESSAGES_TABLE, JOURNAL_PAGE_STORE_TABLE,
            JOURNAL_NODE_MANAGER_STORE_TABLE,
            JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE,
            JOURNAL_FILE_SIZE, JOURNAL_MIN_FILES, JOURNAL_POOL_FILES, JOURNAL_FILE_OPEN_TIMEOUT, JOURNAL_COMPACT_PERCENTAGE,
            JOURNAL_COMPACT_MIN_FILES, JOURNAL_MAX_IO, JOURNAL_MAX_ATTIC_FILES,
            PERF_BLAST_PAGES, RUN_SYNC_SPEED_TEST, SERVER_DUMP_INTERVAL, MEMORY_WARNING_THRESHOLD, MEMORY_MEASURE_INTERVAL,
            GLOBAL_MAX_DISK_USAGE, DISK_SCAN_PERIOD, GLOBAL_MAX_MEMORY_SIZE,
            NETWORK_CHECK_NIC, NETWORK_CHECK_PERIOD, NETWORK_CHECK_TIMEOUT,
            NETWORK_CHECK_LIST, NETWORK_CHECK_URL_LIST, NETWORK_CHECK_PING_COMMAND, NETWORK_CHECK_PING6_COMMAND,
            CRITICAL_ANALYZER_ENABLED, CRITICAL_ANALYZER_CHECK_PERIOD, CRITICAL_ANALYZER_TIMEOUT, CRITICAL_ANALYZER_POLICY
    };

    private final boolean registerRuntimeOnly;

    ServerDefinition(boolean registerRuntimeOnly) {
        super(new SimpleResourceDefinition.Parameters(MessagingExtension.SERVER_PATH, MessagingExtension.getResourceDescriptionResolver(CommonAttributes.SERVER))
                .setAddHandler(ServerAdd.INSTANCE)
                .setRemoveHandler(ServerRemove.INSTANCE)
                .addCapabilities(Capabilities.ACTIVEMQ_SERVER_CAPABILITY));
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        if (registerRuntimeOnly) {
            ExportJournalOperation.registerOperation(resourceRegistration, getResourceDescriptionResolver());
            ImportJournalOperation.registerOperation(resourceRegistration, getResourceDescriptionResolver());
            PrintDataOperation.INSTANCE.registerOperation(resourceRegistration, getResourceDescriptionResolver());

            ActiveMQServerControlHandler.INSTANCE.registerOperations(resourceRegistration, getResourceDescriptionResolver());
            JMSServerControlHandler.INSTANCE.registerOperations(resourceRegistration, getResourceDescriptionResolver());

            AddressSettingsResolveHandler.registerOperationHandler(resourceRegistration, getResourceDescriptionResolver());
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ActiveMQServerControlWriteHandler.INSTANCE.registerAttributes(resourceRegistration, registerRuntimeOnly);
        if (registerRuntimeOnly) {
            ActiveMQServerControlHandler.INSTANCE.registerAttributes(resourceRegistration);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        List<PersistentResourceDefinition> children = new ArrayList<>();
        // Static resources
        children.addAll(Arrays.asList(
                // HA policy
                LiveOnlyDefinition.INSTANCE,
                registerRuntimeOnly ? ReplicationMasterDefinition.INSTANCE : ReplicationMasterDefinition.HC_INSTANCE,
                registerRuntimeOnly ? ReplicationSlaveDefinition.INSTANCE : ReplicationSlaveDefinition.HC_INSTANCE,
                ReplicationColocatedDefinition.INSTANCE,
                SharedStoreMasterDefinition.INSTANCE,
                SharedStoreSlaveDefinition.INSTANCE,
                SharedStoreColocatedDefinition.INSTANCE,

                AddressSettingDefinition.INSTANCE,
                SecuritySettingDefinition.INSTANCE,

                // Acceptors
                HTTPAcceptorDefinition.INSTANCE,

                DivertDefinition.INSTANCE,
                ConnectorServiceDefinition.INSTANCE,
                GroupingHandlerDefinition.INSTANCE,

                // JMS resources
                LegacyConnectionFactoryDefinition.INSTANCE,
                PooledConnectionFactoryDefinition.INSTANCE));

        // Dynamic resources (depending on registerRuntimeOnly)
        // acceptors
        children.add(GenericTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));
        children.add(InVMTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));
        children.add(RemoteTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));
        // connectors
        children.add(GenericTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        children.add(InVMTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        children.add(RemoteTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        children.add(new HTTPConnectorDefinition(registerRuntimeOnly));

        children.add(new BridgeDefinition(registerRuntimeOnly));
        children.add(new BroadcastGroupDefinition(registerRuntimeOnly));
        children.add(new SocketBroadcastGroupDefinition(registerRuntimeOnly));
        children.add(new JGroupsBroadcastGroupDefinition(registerRuntimeOnly));
        // WFLY-10518 - keep discovery-group resource under server
        children.add(new DiscoveryGroupDefinition(registerRuntimeOnly, false));
        children.add(new JGroupsDiscoveryGroupDefinition(registerRuntimeOnly, false));
        children.add(new SocketDiscoveryGroupDefinition(registerRuntimeOnly, false));
        children.add(new ClusterConnectionDefinition(registerRuntimeOnly));
        children.add(new QueueDefinition(registerRuntimeOnly, MessagingExtension.QUEUE_PATH));
        children.add(new JMSQueueDefinition(false, registerRuntimeOnly));
        children.add(new JMSTopicDefinition(false, registerRuntimeOnly));
        children.add(new ConnectionFactoryDefinition(registerRuntimeOnly));

        return children;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        // runtime queues and core-address are only registered when it is ok to register runtime resource (ie they are not registered on HC).
        if (registerRuntimeOnly) {
            resourceRegistration.registerSubModel(new QueueDefinition(registerRuntimeOnly,  MessagingExtension.RUNTIME_QUEUE_PATH));
            resourceRegistration.registerSubModel(CoreAddressDefinition.INSTANCE);
        }
    }

    private enum JournalType {
        NIO, ASYNCIO;
    }
}
