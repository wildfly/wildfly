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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for /subsystem=jgroups/stack=X/transport=TRANSPORT
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportResourceDefinition extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);

    // attributes
    static final SimpleAttributeDefinition TYPE = new SimpleAttributeDefinitionBuilder(ModelKeys.TYPE, ModelType.STRING, false)
            .setXmlName(Attribute.TYPE.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SHARED = new SimpleAttributeDefinitionBuilder(ModelKeys.SHARED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.SHARED.getLocalName())
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(ModelKeys.SOCKET_BINDING, ModelType.STRING, true)
            .setXmlName(Attribute.SOCKET_BINDING.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    static final SimpleAttributeDefinition DIAGNOSTICS_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(ModelKeys.DIAGNOSTICS_SOCKET_BINDING, ModelType.STRING, true)
            .setXmlName(Attribute.DIAGNOSTICS_SOCKET_BINDING.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    static final SimpleAttributeDefinition DEFAULT_EXECUTOR = new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_EXECUTOR, ModelType.STRING, true)
            .setXmlName(Attribute.DEFAULT_EXECUTOR.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition OOB_EXECUTOR = new SimpleAttributeDefinitionBuilder(ModelKeys.OOB_EXECUTOR, ModelType.STRING, true)
            .setXmlName(Attribute.OOB_EXECUTOR.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition TIMER_EXECUTOR = new SimpleAttributeDefinitionBuilder(ModelKeys.TIMER_EXECUTOR, ModelType.STRING, true)
            .setXmlName(Attribute.TIMER_EXECUTOR.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition THREAD_FACTORY = new SimpleAttributeDefinitionBuilder(ModelKeys.THREAD_FACTORY, ModelType.STRING, true)
            .setXmlName(Attribute.THREAD_FACTORY.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SITE = new SimpleAttributeDefinitionBuilder(ModelKeys.SITE, ModelType.STRING, true)
            .setXmlName(Attribute.SITE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition RACK = new SimpleAttributeDefinitionBuilder(ModelKeys.RACK, ModelType.STRING, true)
            .setXmlName(Attribute.RACK.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition MACHINE = new SimpleAttributeDefinitionBuilder(ModelKeys.MACHINE, ModelType.STRING, true)
            .setXmlName(Attribute.MACHINE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition PROPERTY = new SimpleAttributeDefinition(ModelKeys.PROPERTY, ModelType.PROPERTY, true);

    static final SimpleListAttributeDefinition PROPERTIES = new SimpleListAttributeDefinition.Builder(ModelKeys.PROPERTIES, PROPERTY)
            .setAllowNull(true)
            .build();

    // the list of attributes used by the transport resource
    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            TYPE, SHARED, SOCKET_BINDING, DIAGNOSTICS_SOCKET_BINDING, DEFAULT_EXECUTOR,
            OOB_EXECUTOR, TIMER_EXECUTOR, THREAD_FACTORY, SITE, RACK, MACHINE
    };

    // the list of parameters accepted by a /subsystem=jgroups/stack=X/transport=TRANSPORT:add() operation
    static final AttributeDefinition[] PARAMETERS = new AttributeDefinition[] {
            TYPE, SHARED, SOCKET_BINDING, DIAGNOSTICS_SOCKET_BINDING, DEFAULT_EXECUTOR,
            OOB_EXECUTOR, TIMER_EXECUTOR, THREAD_FACTORY, SITE, RACK, MACHINE, PROPERTIES
    };

    // representation of the type of a single operation parameter TRANSPORT of type OBJECT holding transport attributes
    // If this type is defined in a resource bundle with prefix X, the type descriptions
    // should be of the form X.transport.type, X.transport.shared, etc.
    static final ObjectTypeAttributeDefinition TRANSPORT = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.TRANSPORT, ATTRIBUTES)
            .setAllowNull(true)
            .setSuffix(null)
            .build();

    // operations
    private static final OperationDefinition ADD = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, JGroupsExtension.getResourceDescriptionResolver(ModelKeys.TRANSPORT))
            .setParameters(PARAMETERS)
            .build();

    static final OperationStepHandler ADD_HANDLER = new TransportAddHandler(PARAMETERS);

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        if (JGroupsModel.VERSION_1_2_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, SHARED, PROPERTIES);
        }

        PropertyResourceDefinition.buildTransformation(version, builder);
    }

    // registration
    TransportResourceDefinition() {
        super(PATH, JGroupsExtension.getResourceDescriptionResolver(ModelKeys.TRANSPORT), null, new TransportRemoveHandler());
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        registration.registerOperationHandler(ADD, ADD_HANDLER);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(PropertyResourceDefinition.INSTANCE);
    }
}
