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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.AttributeTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LoggingRootResource extends TransformerResourceDefinition {
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, LoggingExtension.SUBSYSTEM_NAME);
    static final LoggingRootResource INSTANCE = new LoggingRootResource();

    static final SimpleAttributeDefinition ADD_LOGGING_API_DEPENDENCIES = SimpleAttributeDefinitionBuilder.create("add-logging-api-dependencies", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(ElementAttributeMarshaller.VALUE_ATTRIBUTE_MARSHALLER)
            .setDefaultValue(new ModelNode(true))
            .setFlags(Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            ADD_LOGGING_API_DEPENDENCIES,
    };

    private LoggingRootResource() {
        super(SUBSYSTEM_PATH,
                LoggingExtension.getResourceDescriptionResolver(),
                LoggingSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (SimpleAttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }

    @Override
    public void registerTransformers(final KnownModelVersion modelVersion, final ResourceTransformationDescriptionBuilder rootResourceBuilder, final ResourceTransformationDescriptionBuilder loggingProfileBuilder) {
        if (modelVersion.hasTransformers()) {
            switch (modelVersion) {
                case VERSION_1_1_0:
                case VERSION_1_2_0:
                case VERSION_1_3_0: {
                    AttributeTransformationDescriptionBuilder attributeBuilder = rootResourceBuilder.getAttributeBuilder();
                    for (SimpleAttributeDefinition attribute : ATTRIBUTES) {
                        attributeBuilder.setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, attribute.getDefaultValue()), attribute)
                                .addRejectCheck(RejectAttributeChecker.DEFINED, attribute);
                    }
                    attributeBuilder.end();
                }
            }
        }
    }
}
