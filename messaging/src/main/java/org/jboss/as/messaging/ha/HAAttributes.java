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

package org.jboss.as.messaging.ha;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.messaging.AttributeMarshallers;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class HAAttributes {

    public static final SimpleAttributeDefinition ALLOW_FAILBACK = create("allow-failback", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultAllowAutoFailback()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition BACKUP_PORT_OFFSET = create("backup-port-offset", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultHapolicyBackupPortOffset()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition BACKUP_REQUEST_RETRIES = create("backup-request-retries", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultHapolicyBackupRequestRetries()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition BACKUP_REQUEST_RETRY_INTERVAL = create("backup-request-retry-interval", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultHapolicyBackupRequestRetryInterval()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition CHECK_FOR_LIVE_SERVER = create(CommonAttributes.CHECK_FOR_LIVE_SERVER2, BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultCheckForLiveServer()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition CLUSTER_NAME = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CLUSTER_NAME, STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static AttributeDefinition EXCLUDED_CONNECTORS = new SimpleMapAttributeDefinition.Builder("excluded-connectors", true)
            .setAttributeMarshaller(new AttributeMarshallers.ConnectorsMarshaller("excluded-connectors"))
            // we use this corrector so that connectors are represented consistently with other messaging resources
            .setCorrector(new ParameterCorrector() {
                /*
                 * https://issues.jboss.org/browse/WFLY-1796
                 *
                 * For backwards compatibility, the connector attribute must be a map where the key is a
                 * connector name and the value is not taken into account (in previous HornetQ versions, the value
                 * was the backup's server connector).
                 *
                 * This is a source of confusion when creating resources with connector: users expect to pass a
                 * list of connectors and this fails as they must pass a map with undefined values.
                 *
                 * This corrector will replace a list with the map expected to populate the model.
                 */
                @Override
                public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
                    if (newValue.getType() != ModelType.LIST) {
                        return newValue;
                    } else {
                        ModelNode correctValue = new ModelNode();
                        for (ModelNode node : newValue.asList()) {
                            correctValue.get(node.asString());
                        }
                        return correctValue;
                    }
                }
            })
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition FAILBACK_DELAY = create("failback-delay", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultFailbackDelay()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition FAILOVER_ON_SERVER_SHUTDOWN = create("failover-on-server-shutdown", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultFailoverOnServerShutdown()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition GROUP_NAME = SimpleAttributeDefinitionBuilder.create(CommonAttributes.GROUP_NAME, STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition MAX_BACKUPS = create("max-backups", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultHapolicyMaxBackups()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition MAX_SAVED_REPLICATED_JOURNAL_SIZE = create("max-saved-replicated-journal-size", INT)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultMaxSavedReplicatedJournalsSize()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition REQUEST_BACKUP = create("request-backup", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultHapolicyRequestBackup()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RESTART_BACKUP = create("restart-backup", BOOLEAN)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultRestartBackup()))
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
}
