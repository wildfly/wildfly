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
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;

/**
 * The root resource of the JGroups subsystem.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class JGroupsSubsystemResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME);

    // attributes
    public static final SimpleAttributeDefinition DEFAULT_CHANNEL = new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_CHANNEL, ModelType.STRING, true)
            .setXmlName(Attribute.DEFAULT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition DEFAULT_STACK = new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_STACK, ModelType.STRING, true)
            .setXmlName(Attribute.DEFAULT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build()
    ;

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { DEFAULT_CHANNEL, DEFAULT_STACK };

    private final boolean allowRuntimeOnlyRegistration;

    static TransformationDescription buildTransformers(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        if (JGroupsModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, DEFAULT_CHANNEL)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, DEFAULT_CHANNEL)
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, DEFAULT_STACK)
                    .end();

            builder.rejectChildResource(ChannelResourceDefinition.WILDCARD_PATH);
        } else {
            ChannelResourceDefinition.buildTransformation(version, builder);
        }

        StackResourceDefinition.buildTransformation(version, builder);

        return builder.build();
    }

    JGroupsSubsystemResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, JGroupsExtension.getResourceDescriptionResolver(), new JGroupsSubsystemAddHandler(), new JGroupsSubsystemRemoveHandler(allowRuntimeOnlyRegistration));
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new ChannelResourceDefinition(this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new StackResourceDefinition(this.allowRuntimeOnlyRegistration));
    }
}
