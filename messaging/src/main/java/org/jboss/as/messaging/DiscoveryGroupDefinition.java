/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.messaging.CommonAttributes.GROUP_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.GROUP_PORT;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_CHANNEL;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_STACK;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_BIND_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.SOCKET_BINDING;

import org.hornetq.api.core.client.HornetQClient;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;


/**
* Discovery group resource definition
*
* @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
*/
public class DiscoveryGroupDefinition extends SimpleResourceDefinition {

   public static final PathElement PATH = PathElement.pathElement(CommonAttributes.DISCOVERY_GROUP);

    public static final SimpleAttributeDefinition REFRESH_TIMEOUT = create("refresh-timeout", ModelType.LONG)
            // FIXME the default value should be set to HornetQDefaultConfiguration.DEFAULT_BROADCAST_REFRESH_TIMEOUT,
            .setDefaultValue(new ModelNode(HornetQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition INITIAL_WAIT_TIMEOUT = create("initial-wait-timeout", ModelType.LONG)
            .setDefaultValue(new ModelNode(HornetQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { JGROUPS_STACK, JGROUPS_CHANNEL, SOCKET_BINDING, LOCAL_BIND_ADDRESS, GROUP_ADDRESS, GROUP_PORT,
            REFRESH_TIMEOUT, INITIAL_WAIT_TIMEOUT
    };

    private final boolean registerRuntimeOnly;

    public DiscoveryGroupDefinition(final boolean registerRuntimeOnly) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.DISCOVERY_GROUP),
                DiscoveryGroupAdd.INSTANCE,
                DiscoveryGroupRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, DiscoveryGroupWriteAttributeHandler.INSTANCE);
            }
        }
    }
}
