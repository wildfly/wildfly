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

import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Connector service resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectorServiceDefinition extends SimpleResourceDefinition {

    public static final AttributeDefinition[] ATTRIBUTES = { CommonAttributes.FACTORY_CLASS };

    private final boolean registerRuntimeOnly;

    public ConnectorServiceDefinition(final boolean registerRuntimeOnly) {
        super(PathElement.pathElement(CommonAttributes.CONNECTOR_SERVICE),
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CONNECTOR_SERVICE, false),
                ConnectorServiceAdd.INSTANCE,
                ConnectorServiceRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, ConnectorServiceWriteAttributeHandler.INSTANCE);
            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registry) {
        registry.registerSubModel(new ConnectorServiceParamDefinition());
    }
}