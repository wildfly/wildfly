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

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=jgroups/stack=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */

public class StackResource extends SimpleResourceDefinition {

    static final PathElement STACK_PATH = PathElement.pathElement(ModelKeys.STACK);
    static final OperationStepHandler EXPORT_NATIVE_CONFIGURATION_HANDLER = new ExportNativeConfiguration();

    private final boolean runtimeRegistration;

    static final StringListAttributeDefinition PROTOCOLS = new StringListAttributeDefinition.Builder(ModelKeys.PROTOCOLS)
            .setAllowNull(true)
            .build();

    // attributes
    // operations
    private static final OperationDefinition PROTOCOL_STACK_ADD = new SimpleOperationDefinitionBuilder(ADD, JGroupsExtension.getResourceDescriptionResolver(ModelKeys.STACK))
          .addParameter(TransportResource.TRANSPORT)
          .addParameter(ProtocolResource.PROTOCOLS)
          .setAttributeResolver(JGroupsExtension.getResourceDescriptionResolver("stack.add"))
          .build();

    static final OperationDefinition EXPORT_NATIVE_CONFIGURATION = new SimpleOperationDefinitionBuilder(ModelKeys.EXPORT_NATIVE_CONFIGURATION, JGroupsExtension.getResourceDescriptionResolver("stack"))
            .setReplyType(ModelType.STRING)
            .build();

    // registration
    public StackResource(boolean runtimeRegistration) {
        super(STACK_PATH,
                JGroupsExtension.getResourceDescriptionResolver(ModelKeys.STACK),
                // register below with custom signature
                null,
                ProtocolStackRemove.INSTANCE);
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        // override to set up TRANSPORT and PROTOCOLS parameters
        resourceRegistration.registerOperationHandler(PROTOCOL_STACK_ADD, ProtocolStackAdd.INSTANCE);
        // register protocol add and remove
        resourceRegistration.registerOperationHandler(ProtocolResource.PROTOCOL_ADD, ProtocolResource.PROTOCOL_ADD_HANDLER);
        resourceRegistration.registerOperationHandler(ProtocolResource.PROTOCOL_REMOVE, ProtocolResource.PROTOCOL_REMOVE_HANDLER);
        // register export-native-configuration
        if (runtimeRegistration) {
            resourceRegistration.registerOperationHandler(StackResource.EXPORT_NATIVE_CONFIGURATION, StackResource.EXPORT_NATIVE_CONFIGURATION_HANDLER);
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadOnlyAttribute(PROTOCOLS, null);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        // child resources
        resourceRegistration.registerSubModel(TransportResource.INSTANCE);
        resourceRegistration.registerSubModel(ProtocolResource.INSTANCE);
    }
}
