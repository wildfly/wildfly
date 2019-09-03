/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.ha;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.InfiniteOrPositiveValidators;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class HAAttributes {
    /**
     * @see ActiveMQDefaultConfiguration#isDefaultAllowAutoFailback
     */
    public static final SimpleAttributeDefinition ALLOW_FAILBACK = create("allow-failback", BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultHapolicyBackupPortOffset
     */
    public static final SimpleAttributeDefinition BACKUP_PORT_OFFSET = create("backup-port-offset", INT)
            .setDefaultValue(new ModelNode(100))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultHapolicyBackupRequestRetries
     */
    public static final SimpleAttributeDefinition BACKUP_REQUEST_RETRIES = create("backup-request-retries", INT)
            .setDefaultValue(new ModelNode(-1))
            .setRequired(false)
            .setAllowExpression(true)
            .setCorrector(InfiniteOrPositiveValidators.NEGATIVE_VALUE_CORRECTOR)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultHapolicyBackupRequestRetryInterval
     */
    public static final SimpleAttributeDefinition BACKUP_REQUEST_RETRY_INTERVAL = create("backup-request-retry-interval", LONG)
            .setDefaultValue(new ModelNode(5000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    // WFLY-8256 change default value to true instead of Artemis's false
    public static final SimpleAttributeDefinition CHECK_FOR_LIVE_SERVER = create(CommonAttributes.CHECK_FOR_LIVE_SERVER2, BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CLUSTER_NAME = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CLUSTER_NAME, STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final StringListAttributeDefinition EXCLUDED_CONNECTORS = new StringListAttributeDefinition.Builder("excluded-connectors")
            .setRequired(false)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#isDefaultFailoverOnServerShutdown
     */
    public static final SimpleAttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = create("failover-on-server-shutdown", ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition GROUP_NAME = SimpleAttributeDefinitionBuilder.create(CommonAttributes.GROUP_NAME, STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultInitialReplicationSyncTimeout
     */
    public static final SimpleAttributeDefinition INITIAL_REPLICATION_SYNC_TIMEOUT = create("initial-replication-sync-timeout", LONG)
            .setDefaultValue(new ModelNode(30000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultHapolicyMaxBackups
     */
    public static final SimpleAttributeDefinition MAX_BACKUPS = create("max-backups", INT)
            .setDefaultValue(new ModelNode(1))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultMaxSavedReplicatedJournalsSize
     */
    public static final SimpleAttributeDefinition MAX_SAVED_REPLICATED_JOURNAL_SIZE = create("max-saved-replicated-journal-size", INT)
            .setDefaultValue(new ModelNode(2))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#isDefaultHapolicyRequestBackup
     */
    public static final SimpleAttributeDefinition REQUEST_BACKUP = create("request-backup", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#isDefaultRestartBackup
     */
    public static final SimpleAttributeDefinition RESTART_BACKUP = create("restart-backup", BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
}
