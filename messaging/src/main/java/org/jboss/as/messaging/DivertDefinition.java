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
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.STRING;

import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Divert resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class DivertDefinition extends SimpleResourceDefinition {

    private static final PathElement DIVERT_PATH = PathElement.pathElement(CommonAttributes.DIVERT);

    public static final SimpleAttributeDefinition ROUTING_NAME = create("routing-name", STRING)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ADDRESS = create("divert-address", STRING)
            .setXmlName("address")
            .setDefaultValue(null)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition FORWARDING_ADDRESS = create("forwarding-address", STRING)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition EXCLUSIVE = create("exclusive", BOOLEAN)
            .setDefaultValue(new ModelNode().set(HornetQDefaultConfiguration.DEFAULT_DIVERT_EXCLUSIVE))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { ROUTING_NAME, ADDRESS, FORWARDING_ADDRESS, CommonAttributes.FILTER,
        CommonAttributes.TRANSFORMER_CLASS_NAME, EXCLUSIVE };

    private final boolean registerRuntimeOnly;

    public DivertDefinition(boolean registerRuntimeOnly) {
        super(DivertDefinition.DIVERT_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.DIVERT),
                DivertAdd.INSTANCE,
                DivertRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, DivertConfigurationWriteHandler.INSTANCE);
            }
        }
    }
}