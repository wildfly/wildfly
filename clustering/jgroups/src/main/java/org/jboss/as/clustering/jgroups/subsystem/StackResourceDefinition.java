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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=jgroups/stack=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StackResourceDefinition extends SimpleResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.STACK, name);
    }

    private final boolean allowRuntimeOnlyRegistration;

    static final StringListAttributeDefinition PROTOCOLS = new StringListAttributeDefinition.Builder(ModelKeys.PROTOCOLS)
            .setAllowNull(true)
            .build();

    // operations
    private static final OperationDefinition ADD = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, JGroupsExtension.getResourceDescriptionResolver(ModelKeys.STACK))
            .addParameter(TransportResourceDefinition.TRANSPORT)
            .addParameter(ProtocolResourceDefinition.PROTOCOLS)
            .setAttributeResolver(JGroupsExtension.getResourceDescriptionResolver("stack.add"))
            .build();

    static final OperationDefinition EXPORT_NATIVE_CONFIGURATION = new SimpleOperationDefinitionBuilder(ModelKeys.EXPORT_NATIVE_CONFIGURATION, JGroupsExtension.getResourceDescriptionResolver("stack"))
            .setReplyType(ModelType.STRING)
            .build();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (JGroupsModel.VERSION_2_0_0.requiresTransformation(version)) {
            builder.rejectChildResource(RelayResourceDefinition.PATH);
        } else {
            RelayResourceDefinition.buildTransformation(version, builder);
        }

        TransportResourceDefinition.buildTransformation(version, builder);
        ProtocolResourceDefinition.buildTransformation(version, builder);
    }

    // registration
    public StackResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(WILDCARD_PATH, JGroupsExtension.getResourceDescriptionResolver(ModelKeys.STACK), null, StackRemoveHandler.INSTANCE);
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        // override to set up TRANSPORT and PROTOCOLS parameters
        registration.registerOperationHandler(ADD, new StackAddHandler());
        // register protocol add and remove
        registration.registerOperationHandler(ProtocolResourceDefinition.ADD, ProtocolResourceDefinition.ADD_HANDLER);
        registration.registerOperationHandler(ProtocolResourceDefinition.REMOVE, ProtocolResourceDefinition.REMOVE_HANDLER);
        // register export-native-configuration
        if (this.allowRuntimeOnlyRegistration) {
            registration.registerOperationHandler(StackResourceDefinition.EXPORT_NATIVE_CONFIGURATION, new ExportNativeConfiguration());
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        registration.registerReadOnlyAttribute(PROTOCOLS, null);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new TransportResourceDefinition());
        registration.registerSubModel(new ProtocolResourceDefinition());
        registration.registerSubModel(new RelayResourceDefinition());
    }
}
