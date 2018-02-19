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


import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONFIGURATION;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SLAVE;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.BACKUP_PORT_OFFSET;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.BACKUP_REQUEST_RETRIES;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.BACKUP_REQUEST_RETRY_INTERVAL;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.EXCLUDED_CONNECTORS;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.MAX_BACKUPS;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.REQUEST_BACKUP;
import static org.wildfly.extension.messaging.activemq.ha.ManagementHelper.createAddOperation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.activemq.artemis.core.config.HAPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.ColocatedPolicyConfiguration;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.ActiveMQReloadRequiredHandlers;
import org.wildfly.extension.messaging.activemq.MessagingExtension;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class ReplicationColocatedDefinition extends PersistentResourceDefinition {

    public static final Collection<AttributeDefinition> ATTRIBUTES =  Collections.unmodifiableList(Arrays.asList(
            REQUEST_BACKUP,
            BACKUP_REQUEST_RETRIES,
            BACKUP_REQUEST_RETRY_INTERVAL,
            MAX_BACKUPS,
            BACKUP_PORT_OFFSET,
            EXCLUDED_CONNECTORS
    ));

    public static final ReplicationColocatedDefinition INSTANCE = new ReplicationColocatedDefinition();

    private ReplicationColocatedDefinition() {
        super(MessagingExtension.REPLICATION_COLOCATED_PATH,
                MessagingExtension.getResourceDescriptionResolver(HA_POLICY),
                createAddOperation(HA_POLICY, false, ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        AbstractWriteAttributeHandler writeAttribute = new ActiveMQReloadRequiredHandlers.WriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, writeAttribute);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return Collections.unmodifiableList(Arrays.asList(
                ReplicationMasterDefinition.CONFIGURATION_INSTANCE,
                ReplicationSlaveDefinition.CONFIGURATION_INSTANCE));
    }

    static HAPolicyConfiguration buildConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ColocatedPolicyConfiguration haPolicyConfiguration = new ColocatedPolicyConfiguration()
                .setRequestBackup(REQUEST_BACKUP.resolveModelAttribute(context, model).asBoolean())
                .setBackupRequestRetries(BACKUP_REQUEST_RETRIES.resolveModelAttribute(context, model).asInt())
                .setBackupRequestRetryInterval(BACKUP_REQUEST_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong())
                .setMaxBackups(MAX_BACKUPS.resolveModelAttribute(context, model).asInt())
                .setBackupPortOffset(BACKUP_PORT_OFFSET.resolveModelAttribute(context, model).asInt());

        List<String> connectors = EXCLUDED_CONNECTORS.unwrap(context, model);
        if (!connectors.isEmpty()) {
            haPolicyConfiguration.setExcludedConnectors(connectors);
        }

        ModelNode masterConfigurationModel = model.get(CONFIGURATION, MASTER);
        HAPolicyConfiguration masterConfiguration = ReplicationMasterDefinition.buildConfiguration(context, masterConfigurationModel);
        haPolicyConfiguration.setLiveConfig(masterConfiguration);

        ModelNode slaveConfigurationModel = model.get(CONFIGURATION, SLAVE);
        HAPolicyConfiguration slaveConfiguration = ReplicationSlaveDefinition.buildConfiguration(context, slaveConfigurationModel);
        haPolicyConfiguration.setBackupConfig(slaveConfiguration);

        return haPolicyConfiguration;
    }
}