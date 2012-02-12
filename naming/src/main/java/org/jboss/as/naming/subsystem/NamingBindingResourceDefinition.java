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

package org.jboss.as.naming.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for JNDI bindings
 *
 */
public class NamingBindingResourceDefinition extends SimpleResourceDefinition {

    public static final NamingBindingResourceDefinition INSTANCE = new NamingBindingResourceDefinition();

    public static final SimpleAttributeDefinition BINDING_TYPE =
            new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.BINDING_TYPE, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition VALUE =
            new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.VALUE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

        public static final SimpleAttributeDefinition TYPE =
            new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.TYPE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();


    public static final SimpleAttributeDefinition CLASS =
            new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.CLASS, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition MODULE =
            new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.MODULE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();


    public static final SimpleAttributeDefinition LOOKUP =
            new SimpleAttributeDefinitionBuilder(NamingSubsystemModel.LOOKUP, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = {BINDING_TYPE, VALUE, TYPE, CLASS, MODULE, LOOKUP};

    private NamingBindingResourceDefinition() {
        super(NamingSubsystemModel.BINDING_PATH,
                NamingExtension.getResourceDescriptionResolver(NamingSubsystemModel.BINDING),
                NamingBindingAdd.INSTANCE, NamingBindingRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }
}
