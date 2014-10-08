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

import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidator;
import org.jboss.as.clustering.jgroups.ProtocolConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectListAttributeDefinition;
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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for /subsystem=jgroups/stack=X/protocol=Y
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ProtocolResourceDefinition extends SimpleResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.PROTOCOL, name);
    }

    // attributes
    public static final SimpleAttributeDefinition TYPE = new SimpleAttributeDefinitionBuilder(ModelKeys.TYPE, ModelType.STRING, false)
            .setXmlName(Attribute.TYPE.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(ModelKeys.SOCKET_BINDING, ModelType.STRING, true)
            .setXmlName(Attribute.SOCKET_BINDING.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING, true)
            .setXmlName(Attribute.MODULE.getLocalName())
            .setDefaultValue(new ModelNode(ProtocolConfiguration.DEFAULT_MODULE.getName()))
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new ModuleIdentifierValidator(true))
            .build();

    static final SimpleAttributeDefinition PROPERTY = new SimpleAttributeDefinition(ModelKeys.PROPERTY, ModelType.PROPERTY, true);
    static final SimpleListAttributeDefinition PROPERTIES = new SimpleListAttributeDefinition.Builder(ModelKeys.PROPERTIES, PROPERTY)
            .setAllowNull(true)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { TYPE, MODULE, SOCKET_BINDING };
    static final AttributeDefinition[] PARAMETERS = new AttributeDefinition[] { TYPE, MODULE, SOCKET_BINDING, PROPERTIES };

    static final ObjectTypeAttributeDefinition PROTOCOL = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.PROTOCOL, ATTRIBUTES)
            .setAllowNull(true)
            .setSuffix(null)
            .setSuffix("protocol")
            .build();

    static final ObjectListAttributeDefinition PROTOCOLS = ObjectListAttributeDefinition.Builder.of(ModelKeys.PROTOCOLS, PROTOCOL)
            .setAllowNull(true)
            .build();

    // operations
    static final OperationDefinition ADD = new SimpleOperationDefinitionBuilder(ModelKeys.ADD_PROTOCOL, JGroupsExtension.getResourceDescriptionResolver("stack"))
            .setParameters(PARAMETERS)
            .build();

    static final OperationDefinition REMOVE = new SimpleOperationDefinitionBuilder(ModelKeys.REMOVE_PROTOCOL, JGroupsExtension.getResourceDescriptionResolver("stack"))
            .setParameters(TYPE)
            .build();

    static final OperationStepHandler ADD_HANDLER = new ProtocolAddHandler(PARAMETERS);
    static final OperationStepHandler REMOVE_HANDLER = new ProtocolRemoveHandler();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        buildTransformation(version, parent, WILDCARD_PATH);
    }

    /*
     * Builds transformations common to both protocols and transport.
     */
    static ResourceTransformationDescriptionBuilder buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent, PathElement path) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(path);

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(MODULE.getDefaultValue()), MODULE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, MODULE)
                    .end();
        }
        if (JGroupsModel.VERSION_1_2_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, PROPERTIES);
        }

        PropertyResourceDefinition.buildTransformation(version, builder);

        return builder;
    }

    // registration
    ProtocolResourceDefinition() {
        super(WILDCARD_PATH, JGroupsExtension.getResourceDescriptionResolver(ModelKeys.PROTOCOL));
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
