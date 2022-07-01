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


import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.CHECK_FOR_LIVE_SERVER;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.CLUSTER_NAME;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.GROUP_NAME;
import static org.wildfly.extension.messaging.activemq.ha.HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.ha.ManagementHelper.createAddOperation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.messaging.activemq.ActiveMQReloadRequiredHandlers;
import org.wildfly.extension.messaging.activemq.MessagingExtension;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class ReplicationPrimaryDefinition extends PersistentResourceDefinition {

    public static final Collection<AttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(
            (AttributeDefinition) CLUSTER_NAME,
            GROUP_NAME,
            CHECK_FOR_LIVE_SERVER,
            INITIAL_REPLICATION_SYNC_TIMEOUT
    ));

    public static final ReplicationPrimaryDefinition INSTANCE = new ReplicationPrimaryDefinition(MessagingExtension.REPLICATION_PRIMARY_PATH, false, true);
    public static final ReplicationPrimaryDefinition HC_INSTANCE = new ReplicationPrimaryDefinition(MessagingExtension.REPLICATION_PRIMARY_PATH, false, false);
    public static final ReplicationPrimaryDefinition CONFIGURATION_INSTANCE = new ReplicationPrimaryDefinition(MessagingExtension.CONFIGURATION_PRIMARY_PATH, true, true);

    private final boolean registerRuntime;
    private ReplicationPrimaryDefinition(PathElement path, boolean allowSibling, boolean registerRuntime) {
        super(path,
                MessagingExtension.getResourceDescriptionResolver(HA_POLICY),
                createAddOperation(path.getKey(), allowSibling, ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
        this.registerRuntime = registerRuntime;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        AbstractWriteAttributeHandler writeAttribute = new ActiveMQReloadRequiredHandlers.WriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, writeAttribute);
        }
        if(registerRuntime) {
            HAPolicySynchronizationStatusReadHandler.registerMasterAttributes(resourceRegistration);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}