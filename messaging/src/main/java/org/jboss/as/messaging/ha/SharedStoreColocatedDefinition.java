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

import static org.jboss.as.messaging.CommonAttributes.CONFIGURATION;
import static org.jboss.as.messaging.CommonAttributes.HA_POLICY;
import static org.jboss.as.messaging.CommonAttributes.MASTER;
import static org.jboss.as.messaging.CommonAttributes.SHARED_STORE_COLOCATED;
import static org.jboss.as.messaging.CommonAttributes.SLAVE;
import static org.jboss.as.messaging.ha.HAAttributes.BACKUP_PORT_OFFSET;
import static org.jboss.as.messaging.ha.HAAttributes.BACKUP_REQUEST_RETRIES;
import static org.jboss.as.messaging.ha.HAAttributes.BACKUP_REQUEST_RETRY_INTERVAL;
import static org.jboss.as.messaging.ha.HAAttributes.MAX_BACKUPS;
import static org.jboss.as.messaging.ha.HAAttributes.REQUEST_BACKUP;
import static org.jboss.as.messaging.ha.ManagementHelper.createAddOperation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hornetq.core.config.HAPolicyConfiguration;
import org.hornetq.core.config.ha.ColocatedPolicyConfiguration;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.HornetQReloadRequiredHandlers;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class SharedStoreColocatedDefinition extends PersistentResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(HA_POLICY, SHARED_STORE_COLOCATED);

    public static Collection<AttributeDefinition> ATTRIBUTES =  Collections.unmodifiableList(Arrays.asList(
            (AttributeDefinition)REQUEST_BACKUP,
            BACKUP_REQUEST_RETRIES,
            BACKUP_REQUEST_RETRY_INTERVAL,
            MAX_BACKUPS,
            BACKUP_PORT_OFFSET
    ));

    public static final SharedStoreColocatedDefinition INSTANCE = new SharedStoreColocatedDefinition();

    private SharedStoreColocatedDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(HA_POLICY ),
                createAddOperation(HA_POLICY, false, ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        AbstractWriteAttributeHandler writeAttribute = new HornetQReloadRequiredHandlers.WriteAttributeHandler(ATTRIBUTES);
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
                new SharedStoreMasterDefinition(PathElement.pathElement(CONFIGURATION, MASTER), true),
                new SharedStoreSlaveDefinition(PathElement.pathElement(CONFIGURATION, SLAVE), true)
        ));
    }

    static HAPolicyConfiguration buildConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ColocatedPolicyConfiguration haPolicyConfiguration = new ColocatedPolicyConfiguration()
                .setRequestBackup(REQUEST_BACKUP.resolveModelAttribute(context, model).asBoolean())
                .setBackupRequestRetries(BACKUP_REQUEST_RETRIES.resolveModelAttribute(context, model).asInt())
                .setBackupRequestRetryInterval(BACKUP_REQUEST_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong())
                .setMaxBackups(MAX_BACKUPS.resolveModelAttribute(context, model).asInt())
                .setBackupPortOffset(BACKUP_PORT_OFFSET.resolveModelAttribute(context, model).asInt());

        ModelNode masterConfigurationModel = model.get(CONFIGURATION, MASTER);
        if (masterConfigurationModel.isDefined()) {
            HAPolicyConfiguration masterConfiguration = ReplicationMasterDefinition.buildConfiguration(context, masterConfigurationModel);
            haPolicyConfiguration.setLiveConfig(masterConfiguration);
        }

        ModelNode slaveConfigurationModel = model.get(CONFIGURATION, SLAVE);
        if (slaveConfigurationModel.isDefined()) {
            HAPolicyConfiguration slaveConfiguration = ReplicationSlaveDefinition.buildConfiguration(context, slaveConfigurationModel);
            haPolicyConfiguration.setBackupConfig(slaveConfiguration);
        }

        return haPolicyConfiguration;
    }
}